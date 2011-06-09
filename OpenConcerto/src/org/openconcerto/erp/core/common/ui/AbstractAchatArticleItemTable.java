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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.ui.table.XTableColumnModel;

import java.util.List;
import java.util.Vector;

import javax.swing.ToolTipManager;

public abstract class AbstractAchatArticleItemTable extends AbstractArticleItemTable {

    public AbstractAchatArticleItemTable() {
        super();
    }

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));
        // Code article
        final SQLTableElement tableElementCode = new SQLTableElement(e.getTable().getField("CODE"));
        list.add(tableElementCode);
        // Désignation de l'article
        final SQLTableElement tableElementNom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElementNom);
        // Valeur des métriques
        final SQLTableElement tableElement_ValeurMetrique2 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_2"), Float.class);
        list.add(tableElement_ValeurMetrique2);
        final SQLTableElement tableElement_ValeurMetrique3 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_3"), Float.class);
        list.add(tableElement_ValeurMetrique3);
        final SQLTableElement tableElement_ValeurMetrique1 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_1"), Float.class);
        list.add(tableElement_ValeurMetrique1);
        // Prix d'achat HT de la métrique 1
        final SQLTableElement tableElement_PrixMetrique1_AchatHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), Long.class, new DeviseCellEditor());
        tableElement_PrixMetrique1_AchatHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(tableElement_PrixMetrique1_AchatHT);
        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);
        // Quantité
        final SQLTableElement qteElement = new SQLTableElement(e.getTable().getField("QTE"), Integer.class);
        list.add(qteElement);
        // Prix d'achat unitaire HT
        this.ha = new SQLTableElement(e.getTable().getField("PA_HT"), Long.class, new DeviseCellEditor());
        this.ha.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.ha);
        // TVA
        final SQLTableElement tableElement_Taxe = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(tableElement_Taxe);
        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class);
        list.add(this.tableElementPoidsTotal);

        // Service
        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);
        if (b != null && b.booleanValue()) {
            this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
            list.add(this.service);
        }

        // Total HT
        this.totalHT = new SQLTableElement(e.getTable().getField("T_PA_HT"), Long.class, new DeviseCellEditor());
        this.totalHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.totalHT);
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PA_TTC"), Long.class, new DeviseCellEditor());
        this.tableElementTotalTTC.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"));

        this.table = new RowValuesTable(this.model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Autocompletion
        final AutoCompletionManager m = new AutoCompletionManager(tableElementCode, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.CODE"), this.table,
                this.table.getRowValuesTableModel());
        m.fill("NOM", "NOM");
        m.fill("PA_HT", "PA_HT");
        m.fill("PV_HT", "PV_HT");
        m.fill("POIDS", "POIDS");
        m.fill("ID_TAXE", "ID_TAXE");
        m.fill("PRIX_METRIQUE_HA_1", "PRIX_METRIQUE_HA_1");
        m.fill("PRIX_METRIQUE_HA_2", "PRIX_METRIQUE_HA_2");
        m.fill("PRIX_METRIQUE_HA_3", "PRIX_METRIQUE_HA_3");
        m.fill("VALEUR_METRIQUE_1", "VALEUR_METRIQUE_1");
        m.fill("VALEUR_METRIQUE_2", "VALEUR_METRIQUE_2");
        m.fill("VALEUR_METRIQUE_3", "VALEUR_METRIQUE_3");
        m.fill("ID_MODE_VENTE_ARTICLE", "ID_MODE_VENTE_ARTICLE");
        m.fill("PRIX_METRIQUE_VT_1", "PRIX_METRIQUE_VT_1");
        m.fill("PRIX_METRIQUE_VT_2", "PRIX_METRIQUE_VT_2");
        m.fill("PRIX_METRIQUE_VT_3", "PRIX_METRIQUE_VT_3");
        m.fill("SERVICE", "SERVICE");
        final AutoCompletionManager m2 = new AutoCompletionManager(tableElementNom, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel());
        m2.fill("CODE", "CODE");
        m2.fill("PA_HT", "PA_HT");
        m2.fill("PV_HT", "PV_HT");
        m2.fill("POIDS", "POIDS");
        m2.fill("ID_TAXE", "ID_TAXE");
        m2.fill("PRIX_METRIQUE_HA_1", "PRIX_METRIQUE_HA_1");
        m2.fill("PRIX_METRIQUE_HA_2", "PRIX_METRIQUE_HA_2");
        m2.fill("PRIX_METRIQUE_HA_3", "PRIX_METRIQUE_HA_3");
        m2.fill("ID_MODE_VENTE_ARTICLE", "ID_MODE_VENTE_ARTICLE");
        m2.fill("VALEUR_METRIQUE_1", "VALEUR_METRIQUE_1");
        m2.fill("VALEUR_METRIQUE_2", "VALEUR_METRIQUE_2");
        m2.fill("VALEUR_METRIQUE_3", "VALEUR_METRIQUE_3");
        m2.fill("PRIX_METRIQUE_VT_1", "PRIX_METRIQUE_VT_1");
        m2.fill("PRIX_METRIQUE_VT_2", "PRIX_METRIQUE_VT_2");
        m2.fill("PRIX_METRIQUE_VT_3", "PRIX_METRIQUE_VT_3");
        m2.fill("SERVICE", "SERVICE");
        // Calcul automatique du total HT
        qteElement.addModificationListener(this.totalHT);
        this.ha.addModificationListener(this.totalHT);
        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row) {
                System.out.println("Compute totalHT");

                // Object qteObject = row.getObject("QTE");
                // int qte = (qteObject == null) ? 0 : Integer.parseInt(qteObject.toString());
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PA_HT");
                System.err.println("Qte:" + qte + " et PA_HT:" + f);
                // long longValue = (f == null) ? 0 : f.longValue();
                long r = f.longValue() * qte;
                return new Long(r);
            }

        });
        // Calcul automatique du total TTC
        qteElement.addModificationListener(this.tableElementTotalTTC);
        this.ha.addModificationListener(this.tableElementTotalTTC);
        tableElement_Taxe.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                // System.err.println("Calcul du total TTC");

                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PA_HT");
                int idTaux = Integer.parseInt(row.getObject("ID_TAXE").toString());
                if (idTaux < 0) {
                    System.out.println(row);
                }
                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                PrixHT pHT = new PrixHT(f.longValue() * qte);
                // System.err.println("Calcul du total TTC : taux " + resultTaux + " : HT " + pHT);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                // float taux = resultTaux.floatValue();
                Long r = new Long(pHT.calculLongTTC(taux / 100f));
                return r;
            }

        });

        this.table.readState();

        // Mode Gestion article avancé
        String valModeAvanceVt = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bModeAvance = Boolean.valueOf(valModeAvanceVt);
        if (bModeAvance != null && !bModeAvance.booleanValue()) {
            hideColumn(this.model.getColumnForField("VALEUR_METRIQUE_1"));
            hideColumn(this.model.getColumnForField("VALEUR_METRIQUE_2"));
            hideColumn(this.model.getColumnForField("VALEUR_METRIQUE_3"));
            hideColumn(this.model.getColumnForField("PRIX_METRIQUE_VT_1"));
            hideColumn(this.model.getColumnForField("ID_MODE_VENTE_ARTICLE"));
        }

        // Calcul automatique du poids unitaire
        tableElement_ValeurMetrique1.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique2.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique3.addModificationListener(tableElementPoids);
        tableElementPoids.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                return new Float(ReferenceArticleSQLElement.getPoidsFromDetails(row));
            }

        });
        // Calcul automatique du poids total
        tableElementPoids.addModificationListener(this.tableElementPoidsTotal);
        qteElement.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                System.err.println("Calcul du poids total ");
                Number f = (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                return new Float(f.floatValue() * qte);
            }

        });

        // Calcul automatique du prix d'achat unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(this.ha);
        tableElement_ValeurMetrique2.addModificationListener(this.ha);
        tableElement_ValeurMetrique3.addModificationListener(this.ha);
        tableElement_PrixMetrique1_AchatHT.addModificationListener(this.ha);
        this.ha.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {

                if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    return Long.valueOf(((Number) row.getObject("PRIX_METRIQUE_HA_1")).longValue());
                } else {

                    final long prixHAFromDetails = ReferenceArticleSQLElement.getPrixHAFromDetails(row);
                    return Long.valueOf(prixHAFromDetails);
                }

            }

        });

        //On réécrit la configuration au cas ou les preferences aurait changé
        this.table.writeState();
    }

    private void hideColumn(int col) {
        if (col >= 0) {
            // this.table.getColumnModel().getColumn(col).setResizable(false);
            // this.table.getColumnModel().getColumn(col).setMinWidth(0);
            // this.table.getColumnModel().getColumn(col).setMaxWidth(0);
            // this.table.getColumnModel().getColumn(col).setPreferredWidth(0);
            // this.table.getColumnModel().getColumn(col).setWidth(0);

            XTableColumnModel columnModel = this.table.getColumnModel();

            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), false);
        }
    }
}
