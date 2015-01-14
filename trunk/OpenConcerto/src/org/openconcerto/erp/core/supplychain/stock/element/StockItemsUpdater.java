/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.supplychain.stock.element;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

public class StockItemsUpdater {

    private final StockLabel label;
    private final List<? extends SQLRowAccessor> items;
    private final Type type;
    private final boolean createMouvementStock;
    private final SQLRowAccessor rowSource;

    public static enum Type {

        VIRTUAL_RECEPT(true, true), REAL_RECEPT(true, false), VIRTUAL_DELIVER(false, true), REAL_DELIVER(false, false);

        private boolean entry, virtual;

        Type(boolean entry, boolean virtual) {
            this.entry = entry;
            this.virtual = virtual;
        }

        public boolean isEntry() {
            return entry;
        }

        public boolean isVirtual() {
            return virtual;
        }
    };

    public StockItemsUpdater(StockLabel label, SQLRowAccessor rowSource, List<? extends SQLRowAccessor> items, Type t) {
        this(label, rowSource, items, t, true);
    }

    public StockItemsUpdater(StockLabel label, SQLRowAccessor rowSource, List<? extends SQLRowAccessor> items, Type t, boolean createMouvementStock) {
        this.label = label;
        this.items = items;
        this.type = t;
        this.createMouvementStock = createMouvementStock;
        this.rowSource = rowSource;
    }

    List<String> requests = new ArrayList<String>();

    public void update() throws SQLException {
        final SQLTable stockTable = this.rowSource.getTable().getTable("STOCK");

        if (this.createMouvementStock) {
            clearExistingMvt(this.rowSource);
        }

        // Mise à jour des stocks des articles non composés
        List<StockItem> stockItems = fetch();

        for (StockItem stockItem : stockItems) {
            if (stockItem.isStockInit()) {
                requests.add(stockItem.getUpdateRequest());
            } else {
                SQLRowValues rowVals = new SQLRowValues(stockTable);
                rowVals.put("QTE_REEL", stockItem.getRealQty());
                rowVals.put("QTE_TH", stockItem.getVirtualQty());
                rowVals.put("QTE_LIV_ATTENTE", stockItem.getDeliverQty());
                rowVals.put("QTE_RECEPT_ATTENTE", stockItem.getReceiptQty());
                SQLRowValues rowValsArt = stockItem.getArticle().createEmptyUpdateRow();
                rowValsArt.put("ID_STOCK", rowVals);
                rowValsArt.commit();
            }
        }

        List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(requests.size());
        for (String s : requests) {
            handlers.add(null);
        }
        // FIXME FIRE TABLE CHANGED TO UPDATE ILISTE ??
        SQLUtils.executeMultiple(stockTable.getDBSystemRoot(), requests, handlers);

        final DBRoot root = this.rowSource.getTable().getDBRoot();
        if (root.contains("ARTICLE_ELEMENT")) {
            ComposedItemStockUpdater comp = new ComposedItemStockUpdater(root, stockItems);
            comp.update();
        }

    }

    /**
     * Suppression des anciens mouvements
     * 
     * @param rowSource
     * @throws SQLException
     * @throws RTInterruptedException
     */
    private void clearExistingMvt(SQLRowAccessor rowSource) throws RTInterruptedException, SQLException {

        List<String> multipleRequests = new ArrayList<String>();

        final SQLTable table = this.rowSource.getTable().getTable("MOUVEMENT_STOCK");
        SQLRowValues rowVals = new SQLRowValues(table);
        rowVals.put("QTE", null);
        rowVals.put("REEL", null);
        SQLRowValues rowValsArt = new SQLRowValues(this.rowSource.getTable().getTable("ARTICLE"));
        SQLRowValues rowValsStock = new SQLRowValues(this.rowSource.getTable().getTable("STOCK"));
        rowValsStock.put("QTE_REEL", null);
        rowValsStock.put("QTE_TH", null);
        rowValsStock.put("QTE_RECEPT_ATTENTE", null);
        rowValsStock.put("QTE_LIV_ATTENTE", null);

        rowValsArt.put("ID_STOCK", rowValsStock);
        rowVals.put("ID_ARTICLE", rowValsArt);

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Where w = new Where(table.getField("SOURCE"), "=", StockItemsUpdater.this.rowSource.getTable().getName());
                w = w.and(new Where(table.getField("IDSOURCE"), "=", StockItemsUpdater.this.rowSource.getID()));
                input.setWhere(w);
                return input;
            }
        });

        List<SQLRowValues> result = fetcher.fetch();
        for (SQLRowValues sqlRowValues : result) {
            StockItem item = new StockItem(sqlRowValues.getForeign("ID_ARTICLE"));
            final StockItem.Type t;
            if (sqlRowValues.getBoolean("REEL")) {
                t = StockItem.Type.REEL;
            } else {
                t = StockItem.Type.THEORIQUE;
            }
            item.updateQty(sqlRowValues.getFloat("QTE"), t, true);
            String req = "UPDATE " + sqlRowValues.getTable().getSQLName().quote() + " SET \"ARCHIVE\"=1 WHERE \"ID\"=" + sqlRowValues.getID();
            multipleRequests.add(req);
            multipleRequests.add(item.getUpdateRequest());
        }

        List<? extends ResultSetHandler> handlers = new ArrayList<ResultSetHandler>(multipleRequests.size());
        for (String s : multipleRequests) {
            handlers.add(null);
        }
        SQLUtils.executeMultiple(table.getDBSystemRoot(), multipleRequests, handlers);
    }

    /**
     * Récupére les stocks associés aux articles non composés et les met à jour
     * 
     * @return la liste des stocks à jour
     */
    private List<StockItem> fetch() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<StockItem> stockItems = new ArrayList<StockItem>(items.size());
        StockItem.Type stockItemType = this.type.isVirtual() ? StockItem.Type.THEORIQUE : StockItem.Type.REEL;
        for (SQLRowAccessor item : items) {

            if (!item.isForeignEmpty("ID_ARTICLE")) {
                SQLRowAccessor article = item.getForeign("ID_ARTICLE");

                // FIXME Create FIELD COMPOSED
                // if (!article.getBoolean("COMPOSED") && article.getBoolean("GESTION_STOCK")) {
                if (article.getBoolean("GESTION_STOCK")) {
                    StockItem stockItem = new StockItem(article);

                    final int qte = item.getInt("QTE");
                    final BigDecimal qteUV = item.getBigDecimal("QTE_UNITAIRE");
                    double qteFinal = qteUV.multiply(new BigDecimal(qte), MathContext.DECIMAL128).doubleValue();
                    if (!this.type.isEntry()) {
                        qteFinal = -qteFinal;
                    }
                    stockItem.updateQty(qteFinal, stockItemType);
                    stockItems.add(stockItem);
                    if (this.createMouvementStock) {
                        String mvtStockQuery = "INSERT INTO " + article.getTable().getTable("MOUVEMENT_STOCK").getSQLName().quote()
                                + " (\"QTE\",\"DATE\",\"ID_ARTICLE\",\"SOURCE\",\"IDSOURCE\",\"NOM\",\"REEL\") VALUES(" + qteFinal + ",'" + dateFormat.format(this.rowSource.getDate("DATE").getTime())
                                + "'," + article.getID() + ",'" + this.rowSource.getTable().getName() + "'," + this.rowSource.getID() + ",'" + this.label.getLabel(this.rowSource, item) + "',"
                                + String.valueOf(!this.type.isVirtual()) + ")";
                        this.requests.add(mvtStockQuery);
                    }
                }
            }
        }
        return stockItems;
    }
}
