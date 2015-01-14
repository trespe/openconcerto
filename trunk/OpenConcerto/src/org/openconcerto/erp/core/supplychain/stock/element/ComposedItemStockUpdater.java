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
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.ResultSetHandler;

public class ComposedItemStockUpdater {

    private final List<StockItem> itemsUpdated;
    private final DBRoot root;

    /**
     * 
     * @param root
     * @param itemsUpdated liste des StockItem non composés qui ont été mis à jour
     */
    public ComposedItemStockUpdater(DBRoot root, List<StockItem> itemsUpdated) {
        this.itemsUpdated = itemsUpdated;
        this.root = root;
    }

    /**
     * Mise à jour des stocks en fonction des composants de l'article
     * 
     * @throws SQLException
     */
    public void update() throws SQLException {
        // Liste des articles composés
        List<StockItem> items = getAllComposedItemToUpdate();

        // Fecth des articles liés
        getAllChildren(items);

        // Mise à jour des stocks
        for (StockItem stockItem : items) {
            stockItem.updateQtyFromChildren();
        }

        SQLTable stockTable = root.getTable("STOCK");
        List<String> requests = new ArrayList<String>();
        for (StockItem stockItem : items) {
            if (stockItem.isStockInit()) {
                UpdateBuilder update = new UpdateBuilder(stockTable);
                update.setWhere(new Where(stockTable.getKey(), "=", stockItem.getArticle().getForeign("ID_STOCK").getID()));
                update.setObject("QTE_REEL", stockItem.getRealQty());
                update.setObject("QTE_TH", stockItem.getVirtualQty());
                update.setObject("QTE_LIV_ATTENTE", stockItem.getDeliverQty());
                update.setObject("QTE_RECEPT_ATTENTE", stockItem.getReceiptQty());
                requests.add(update.asString());
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
    }

    /**
     * Associe les StockItems liés aux items passés en parametres
     * 
     * @param items liste des stockitems d'article composé
     */
    private void getAllChildren(List<StockItem> items) {
        final SQLTable tableArticle = this.root.getTable("ARTICLE");
        final SQLRowValues rowValsArt = new SQLRowValues(tableArticle);
        rowValsArt.put(tableArticle.getKey().getName(), null);

        SQLRowValues rowValsStock = new SQLRowValues(tableArticle.getForeignTable("ID_STOCK"));
        rowValsStock.put("QTE_REEL", null);
        rowValsStock.put("QTE_TH", null);
        rowValsStock.put("QTE_RECEPT_ATTENTE", null);
        rowValsStock.put("QTE_LIV_ATTENTE", null);
        rowValsArt.put("ID_STOCK", rowValsStock);

        final SQLTable tableArticleElt = this.root.getTable("ARTICLE_ELEMENT");
        SQLRowValues rowValsArtItem = new SQLRowValues(tableArticleElt);
        rowValsArtItem.put("ID_ARTICLE", rowValsArt);
        rowValsArtItem.put("QTE", null);
        rowValsArtItem.put("QTE_UNITAIRE", null);
        rowValsArtItem.put("ID_ARTICLE_PARENT", null);

        final List<Integer> ids = new ArrayList<Integer>();
        Map<Integer, StockItem> mapItem = new HashMap<Integer, StockItem>();
        for (StockItem stockItem : items) {
            final int id = stockItem.getArticle().getID();
            ids.add(id);
            mapItem.put(id, stockItem);
        }

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowValsArtItem);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Where w = new Where(tableArticleElt.getField("ID_ARTICLE_PARENT"), ids);
                input.setWhere(w);
                return input;
            }
        });

        List<SQLRowValues> values = fetcher.fetch();
        for (SQLRowValues sqlRowValues : values) {

            final SQLRowAccessor article = sqlRowValues.getForeign("ID_ARTICLE");
            final SQLRowAccessor articleParent = sqlRowValues.getForeign("ID_ARTICLE_PARENT");
            mapItem.get(articleParent.getID()).addItemComponent(new StockItemComponent(new StockItem(article), sqlRowValues.getBigDecimal("QTE_UNITAIRE"), sqlRowValues.getInt("QTE")));
        }
    }

    /**
     * @return l'ensemble des stockItems composés à mettre à jour
     */
    private List<StockItem> getAllComposedItemToUpdate() {
        List<Integer> ids = new ArrayList<Integer>(itemsUpdated.size());
        for (StockItem stockItem : itemsUpdated) {
            ids.add(stockItem.getArticle().getID());
        }
        List<SQLRowValues> list = getComposedItemToUpdate(ids);
        int size = list.size();

        while (size > 0) {

            List<SQLRowValues> l = getComposedItemToUpdate(ids);
            list.removeAll(l);
            list.addAll(l);
            size = l.size();
            if (size > 0) {
                ids.clear();
                for (SQLRowValues r : l) {
                    ids.add(r.getID());
                }
            }
        }

        List<StockItem> items = new ArrayList<StockItem>(list.size());
        for (SQLRowValues rowVals : list) {

            StockItem item = new StockItem(rowVals);
            items.add(item);
        }
        return items;
    }

    /**
     * 
     * @param ids
     * @return l'ensemble des Articles composés avec un des articles en parametres
     */
    private List<SQLRowValues> getComposedItemToUpdate(final List<Integer> ids) {

        final SQLTable tableArticle = this.root.getTable("ARTICLE");
        final SQLRowValues rowValsArt = new SQLRowValues(tableArticle);
        rowValsArt.put(tableArticle.getKey().getName(), null);

        SQLRowValues rowValsStock = new SQLRowValues(tableArticle.getForeignTable("ID_STOCK"));
        rowValsStock.put("QTE_REEL", null);
        rowValsStock.put("QTE_TH", null);
        rowValsStock.put("QTE_RECEPT_ATTENTE", null);
        rowValsStock.put("QTE_LIV_ATTENTE", null);
        rowValsArt.put("ID_STOCK", rowValsStock);

        final SQLTable tableArticleElt = this.root.getTable("ARTICLE_ELEMENT");
        SQLRowValues rowValsArtItem = new SQLRowValues(tableArticleElt);
        rowValsArtItem.put("ID_ARTICLE_PARENT", rowValsArt);
        // rowValsArtItem.put("QTE", null);
        // rowValsArtItem.put("QTE_UNITAIRE", null);

        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowValsArt);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                final SQLSelectJoin joinFromField = input.getJoinFromField(tableArticleElt.getField("ID_ARTICLE_PARENT"));
                Where w = new Where(joinFromField.getJoinedTable().getField("ID_ARTICLE"), ids);
                joinFromField.setWhere(w);
                Where w2 = new Where(joinFromField.getJoinedTable().getKey(), "is not", (Object) null);
                input.setWhere(w2);
                return input;
            }
        });

        return fetcher.fetch();
    }
}
