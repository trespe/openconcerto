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
 
 package org.openconcerto.erp.core.sales.shipment.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ArticleRowValuesRenderer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.ToolTipManager;
import javax.swing.table.TableCellRenderer;

public class BonDeLivraisonItemTable extends AbstractVenteArticleItemTable {

    // private final Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();

    private SQLTableElement tableElementPoidsTotalLivree;

    public BonDeLivraisonItemTable(List<JButton> l) {
        super(l);
    }

    @Override
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
        final SQLTableElement tableElement_ValeurMetrique2 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_2"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals) {
                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                // int mode = vals.getInt("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null
                        && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LONGUEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique2);
        final SQLTableElement tableElement_ValeurMetrique3 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_3"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (!(modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE))) {
                    return false;
                } else {
                    return super.isCellEditable(vals);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique3);
        final SQLTableElement tableElement_ValeurMetrique1 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_1"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null
                        && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LARGEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique1);
        // Prix de vente HT de la métrique 1
        final SQLTableElement tableElement_PrixMetrique1_VenteHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_VT_1"), Long.class, new DeviseCellEditor()) {
            @Override
            public TableCellRenderer getTableCellRenderer() {

                List<Integer> l = new ArrayList<Integer>();
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_CARRE));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LARGEUR));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LONGUEUR));
                l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_POID_METRECARRE));
                return new ArticleRowValuesRenderer(l);
            }
        };
        list.add(tableElement_PrixMetrique1_VenteHT);
        // Prix d'achat HT de la métrique 1
        // final SQLTableElement tableElement_PrixMetrique1_AchatHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique1_AchatHT);

        // Prix de vente HT de la métrique 3
        // final SQLTableElement tableElement_PrixMetrique3_VenteHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_VT_3"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique3_VenteHT);
        // Prix d'achat HT de la métrique 3
        // final SQLTableElement tableElement_PrixMetrique3_AchatHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_3"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique3_AchatHT);

        // Quantité
        final SQLTableElement tableElement_Quantite = new SQLTableElement(e.getTable().getField("QTE"), Integer.class);
        list.add(tableElement_Quantite);

        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // Prix d'achat unitaire HT
        // final SQLTableElement tableElement_PrixAchat_HT = new
        // SQLTableElement(e.getTable().getField("PA_HT"), Long.class, new DeviseCellEditor());
        // list.add(tableElement_PrixAchat_HT);
        // Prix de vente unitaire HT
        final SQLTableElement tableElement_PrixVente_HT = new SQLTableElement(e.getTable().getField("PV_HT"), Long.class, new DeviseCellEditor()) {
            @Override
            public TableCellRenderer getTableCellRenderer() {
                List<Integer> l = new ArrayList<Integer>();
                l.add(Integer.valueOf(ReferenceArticleSQLElement.A_LA_PIECE));
                return new ArticleRowValuesRenderer(l);
            }
        };
        list.add(tableElement_PrixVente_HT);

        // TVA
        final SQLTableElement tableElement_Taxe = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(tableElement_Taxe);

        // Quantité Livrée
        final SQLTableElement tableElement_QuantiteLivree = new SQLTableElement(e.getTable().getField("QTE_LIVREE"), Integer.class);
        list.add(tableElement_QuantiteLivree);

        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class);
        list.add(this.tableElementPoidsTotal);

        // Poids total Livré
        this.tableElementPoidsTotalLivree = new SQLTableElement(e.getTable().getField("T_POIDS_LIVREE"), Float.class);
        list.add(this.tableElementPoidsTotalLivree);

        // Service
        // this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
        // list.add(this.service);
        // Total HT
        this.totalHT = new SQLTableElement(e.getTable().getField("T_PV_HT"), Long.class, new DeviseCellEditor());
        this.totalHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.totalHT);
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PV_TTC"), Long.class, new DeviseCellEditor());
        this.tableElementTotalTTC.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"));

        this.table = new RowValuesTable(model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Autocompletion
        final AutoCompletionManager m = new AutoCompletionManager(tableElementCode, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.CODE"), this.table,
                this.table.getRowValuesTableModel());
        m.fill("NOM", "NOM");
        m.fill("PA_HT", "PA_HT");
        m.fill("PV_HT", "PV_HT");
        m.fill("ID_MODE_VENTE_ARTICLE", "ID_MODE_VENTE_ARTICLE");
        m.fill("POIDS", "POIDS");
        m.fill("PRIX_METRIQUE_HA_1", "PRIX_METRIQUE_HA_1");
        m.fill("PRIX_METRIQUE_HA_2", "PRIX_METRIQUE_HA_2");
        m.fill("PRIX_METRIQUE_HA_3", "PRIX_METRIQUE_HA_3");
        m.fill("VALEUR_METRIQUE_1", "VALEUR_METRIQUE_1");
        m.fill("VALEUR_METRIQUE_2", "VALEUR_METRIQUE_2");
        m.fill("VALEUR_METRIQUE_3", "VALEUR_METRIQUE_3");
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
        m2.fill("ID_MODE_VENTE_ARTICLE", "ID_MODE_VENTE_ARTICLE");
        m2.fill("PRIX_METRIQUE_HA_1", "PRIX_METRIQUE_HA_1");
        m2.fill("PRIX_METRIQUE_HA_2", "PRIX_METRIQUE_HA_2");
        m2.fill("PRIX_METRIQUE_HA_3", "PRIX_METRIQUE_HA_3");
        m2.fill("VALEUR_METRIQUE_1", "VALEUR_METRIQUE_1");
        m2.fill("VALEUR_METRIQUE_2", "VALEUR_METRIQUE_2");
        m2.fill("VALEUR_METRIQUE_3", "VALEUR_METRIQUE_3");
        m2.fill("PRIX_METRIQUE_VT_1", "PRIX_METRIQUE_VT_1");
        m2.fill("PRIX_METRIQUE_VT_2", "PRIX_METRIQUE_VT_2");
        m2.fill("PRIX_METRIQUE_VT_3", "PRIX_METRIQUE_VT_3");
        m2.fill("SERVICE", "SERVICE");

        // Calcul automatique du total HT
        tableElement_Quantite.addModificationListener(totalHT);
        tableElement_PrixVente_HT.addModificationListener(totalHT);
        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row) {
                System.out.println("Compute totalHT");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PV_HT");
                System.out.println("Qte:" + qte + " et PV_HT:" + f);
                long r = f.longValue() * qte;
                return new Long(r);
            }

        });
        // Calcul automatique du total TTC
        tableElement_Quantite.addModificationListener(tableElementTotalTTC);
        tableElement_PrixVente_HT.addModificationListener(tableElementTotalTTC);
        tableElement_Taxe.addModificationListener(tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PV_HT");
                int idTaux = Integer.parseInt(row.getObject("ID_TAXE").toString());

                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                PrixHT pHT = new PrixHT(f.longValue() * qte);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                Long r = new Long(pHT.calculLongTTC(taux / 100f));
                return r;
            }

        });
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
        tableElement_Quantite.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                Number f = (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                return new Float(f.floatValue() * qte);
            }

        });

        // Calcul automatique du poids total livrée
        tableElementPoids.addModificationListener(this.tableElementPoidsTotalLivree);
        tableElement_QuantiteLivree.addModificationListener(this.tableElementPoidsTotalLivree);
        this.tableElementPoidsTotalLivree.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {

                Number f = (Number) row.getObject("POIDS");

                Object qteOb = row.getObject("QTE_LIVREE");
                int qte = (qteOb == null) ? 0 : Integer.parseInt(qteOb.toString());

                float fValue = (f == null) ? 0.0F : f.floatValue();
                return new Float(fValue * qte);
            }
        });

        // Calcul automatique du prix de vente unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixMetrique1_VenteHT.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixVente_HT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    System.err.println("Don't computeValue PV_HT --> " + row.getObject("PV_HT") + row);
                    return new Long(((Number) row.getObject("PRIX_METRIQUE_VT_1")).longValue());
                } else {

                    final long prixVTFromDetails = ReferenceArticleSQLElement.getPrixVTFromDetails(row);
                    System.out.println("Prix de vente calculé au détail:" + prixVTFromDetails);
                    return new Long(prixVTFromDetails);
                }
            }
        });

        this.table.readState();

        // Mode Gestion article avancé
        String valModeAvanceVt = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bModeAvance = Boolean.valueOf(valModeAvanceVt);
        if (bModeAvance != null && !bModeAvance.booleanValue()) {
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_1"));
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_2"));
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_3"));
            hideColumn(model.getColumnForField("PV_HT"));
            hideColumn(model.getColumnForField("ID_MODE_VENTE_ARTICLE"));
        }
        // On réécrit la configuration au cas ou les preferences aurait changé
        this.table.writeState();

        // Calcul automatique du prix d'achat unitaire HT
        // tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixAchat_HT);
        // //tableElement_PrixMetrique1_AchatHT.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_PrixAchat_HT.setModifier(new CellDynamicModifier() {
        // public Object computeValueFrom(SQLRowValues row) {
        // if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
        // return new Long(((Number) row.getObject("PA_HT")).longValue());
        // } else {
        // return new Long(ReferenceArticleSQLElement.getPrixHAFromDetails(row));
        // }
        // }
        // });

        // Mode de vente non éditable
        // int modeVenteColumn = model.getColumnForField("ID_MODE_VENTE_ARTICLE");
        // if (modeVenteColumn >= 0) {
        // model.setEditable(false, modeVenteColumn);
        // }

        // for (int i = 0; i < model.getColumnCount(); i++) {
        //
        // this.table.getColumnModel().getColumn(i).setCellRenderer(new
        // ArticleRowValuesRenderer(model));
        // }
    }

    @Override
    public float getPoidsTotal() {

        float poids = 0.0F;
        int poidsTColIndex = model.getColumnIndexForElement(this.tableElementPoidsTotalLivree);
        if (poidsTColIndex >= 0) {
            for (int i = 0; i < table.getRowCount(); i++) {
                Number tmp = (Number) model.getValueAt(i, poidsTColIndex);
                if (tmp != null) {
                    poids += tmp.floatValue();
                }
            }
        }
        return poids;
    }

    @Override
    protected String getConfigurationFileName() {
        return "Table_Bon_Livraison.xml";
    }

    @Override
    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON_ELEMENT");
    }

    private void hideColumn(int col) {
        if (col >= 0) {
            // this.table.getColumnModel().getColumn(col).setResizable(false);
            // this.table.getColumnModel().getColumn(col).setMinWidth(0);
            // this.table.getColumnModel().getColumn(col).setMaxWidth(0);
            // this.table.getColumnModel().getColumn(col).setPreferredWidth(0);
            // this.table.getColumnModel().getColumn(col).setWidth(0);
            // this.table.getMaskTableModel().hideColumn(col);
            XTableColumnModel columnModel = this.table.getColumnModel();

            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), false);

        }
    }
}
