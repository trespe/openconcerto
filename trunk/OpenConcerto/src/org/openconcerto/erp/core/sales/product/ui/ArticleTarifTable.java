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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.component.ReferenceArticleSQLComponent;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.RowValuesTablePanel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.utils.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.TableCellRenderer;

public class ArticleTarifTable extends RowValuesTablePanel {

    private SQLTableElement tarif;
    private SQLTable article = Configuration.getInstance().getBase().getTable("ARTICLE");

    private SQLRowValues rowValuesArticleCompile = new SQLRowValues(article);
    SQLTableElement tableElement_PrixMetrique1_VenteHT;
    ReferenceArticleSQLComponent comp;

    public ArticleTarifTable(ReferenceArticleSQLComponent comp) {

        init();
        uiInit();
        this.comp = comp;
    }

    public void setArticleValues(SQLRowAccessor articleAccessor) {
        rowValuesArticleCompile.put("VALEUR_METRIQUE_1", articleAccessor.getObject("VALEUR_METRIQUE_1"));
        rowValuesArticleCompile.put("VALEUR_METRIQUE_2", articleAccessor.getObject("VALEUR_METRIQUE_2"));
        rowValuesArticleCompile.put("VALEUR_METRIQUE_3", articleAccessor.getObject("VALEUR_METRIQUE_2"));
        rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1", articleAccessor.getObject("PRIX_METRIQUE_VT_1"));
        rowValuesArticleCompile.put("PRIX_METRIQUE_VT_2", articleAccessor.getObject("PRIX_METRIQUE_VT_2"));
        rowValuesArticleCompile.put("PRIX_METRIQUE_VT_3", articleAccessor.getObject("PRIX_METRIQUE_VT_3"));
        rowValuesArticleCompile.put("ID_MODE_VENTE_ARTICLE", articleAccessor.getObject("ID_MODE_VENTE_ARTICLE"));
        rowValuesArticleCompile.put("ID_TAXE", articleAccessor.getObject("ID_TAXE"));
    }

    public void fireModification() {

        rowValuesArticleCompile.putAll(comp.getDetailsRowValues().getAbsolutelyAll());
        rowValuesArticleCompile.put("ID_TAXE", comp.getSelectedTaxe());

        int rows = getRowValuesTable().getRowCount();
        for (int i = 0; i < rows; i++) {
            SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
            rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1", rowVals.getObject("PRIX_METRIQUE_VT_1"));
            this.tableElement_PrixMetrique1_VenteHT.fireModification(rowVals);
        }
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();

        this.tarif = new SQLTableElement(e.getTable().getField("ID_TARIF"));
        this.tarif.setEditable(false);
        list.add(this.tarif);

        // Prix de vente HT de la métrique 1
        final DeviseCellEditor editorPVHT = new DeviseCellEditor();
        editorPVHT.setConvertToTTCEnable(true);
        this.tableElement_PrixMetrique1_VenteHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_VT_1"), Long.class, editorPVHT) {
            @Override
            public TableCellRenderer getTableCellRenderer() {

                List<Integer> l = new ArrayList<Integer>();
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_CARRE));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LARGEUR));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LONGUEUR));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_POID_METRECARRE));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.A_LA_PIECE));
                return new ArticleRowValuesRenderer(l);
            }
        };
        list.add(tableElement_PrixMetrique1_VenteHT);

        // Devise
        final SQLTableElement tableElement_Devise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
        tableElement_Devise.setEditable(false);
        list.add(tableElement_Devise);

        // TVA
        final SQLTableElement tableElement_TVA = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(tableElement_TVA);

        // Prix de vente unitaire HT
        final SQLTableElement tableElement_PrixVente_HT = new SQLTableElement(e.getTable().getField("PV_HT"), Long.class, new DeviseCellEditor());
        tableElement_PrixVente_HT.setRenderer(new DeviseNiceTableCellRenderer());
        tableElement_PrixVente_HT.setEditable(false);
        list.add(tableElement_PrixVente_HT);

        // Prix de vente unitaire TTC
        final SQLTableElement tableElement_PrixVente_TTC = new SQLTableElement(e.getTable().getField("PV_TTC"), Long.class, new DeviseCellEditor());
        tableElement_PrixVente_TTC.setRenderer(new DeviseNiceTableCellRenderer());
        tableElement_PrixVente_TTC.setEditable(false);
        list.add(tableElement_PrixVente_TTC);

        this.defaultRowVals = new SQLRowValues(getSQLElement().getTable());
        this.defaultRowVals.put("PRIX_METRIQUE_VT_1", 0);
        this.defaultRowVals.put("PV_HT", 0);
        this.defaultRowVals.put("PV_TTC", 0);
        this.model = new RowValuesTableModel(e, list, e.getTable().getField("ID_TARIF"), false, this.defaultRowVals);

        this.table = new RowValuesTable(this.model, null);

        // Calcul automatique du prix de vente unitaire HT

        tableElement_PrixMetrique1_VenteHT.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixVente_HT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                rowValuesArticleCompile.putAll(comp.getDetailsRowValues().getAbsolutelyAll());
                rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1", row.getObject("PRIX_METRIQUE_VT_1"));
                Number n = (Number) rowValuesArticleCompile.getObject("ID_MODE_VENTE_ARTICLE");
                if (n.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || n.intValue() <= 1) {
                    return Long.valueOf(((Number) row.getObject("PRIX_METRIQUE_VT_1")).longValue());
                } else {
                    final long prixVTFromDetails = ReferenceArticleSQLElement.getPrixVTFromDetails(rowValuesArticleCompile);
                    return Long.valueOf(prixVTFromDetails);
                }
            }
        });
        // Calcul automatique du prix de vente unitaire TTC

        tableElement_PrixVente_HT.addModificationListener(tableElement_PrixVente_TTC);
        tableElement_PrixVente_TTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {

                rowValuesArticleCompile.putAll(comp.getDetailsRowValues().getAbsolutelyAll());
                rowValuesArticleCompile.put("PRIX_METRIQUE_VT_1", row.getObject("PRIX_METRIQUE_VT_1"));

                Number f = (Number) row.getObject("PV_HT");
                Object object = row.getObject("ID_TAXE");

                int idTaux = 1;
                if (object != null) {
                    idTaux = Integer.parseInt(object.toString());
                }
                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                if (resultTaux == null) {

                    Integer i = TaxeCache.getCache().getFirstTaxe();
                    if (i == null) {
                        ExceptionHandler.handle("Aucune taxe définie!");
                        System.err.println("Aucune Taxe");
                    } else {
                        rowValuesArticleCompile.put("ID_TAXE", i);
                        resultTaux = TaxeCache.getCache().getTauxFromId(i.intValue());
                    }
                }

                PrixHT pHT = new PrixHT(f.longValue());
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                editorPVHT.setTaxe(resultTaux);
                Long r = Long.valueOf(pHT.calculLongTTC(taux / 100f));
                return r;
            }

        });

    }

    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("ARTICLE_TARIF");
    }


}
