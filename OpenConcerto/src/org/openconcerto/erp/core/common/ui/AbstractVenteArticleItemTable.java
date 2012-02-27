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
import org.openconcerto.erp.core.sales.product.ui.ArticleRowValuesRenderer;
import org.openconcerto.erp.core.sales.product.ui.QteMultipleRowValuesRenderer;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.utils.ExceptionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.table.TableCellRenderer;

public abstract class AbstractVenteArticleItemTable extends AbstractArticleItemTable {

    public static final String ARTICLE_SHOW_DEVISE = "ArticleShowDevise";
    public static final String ARTICLE_SERVICE = "ArticleService";

    public AbstractVenteArticleItemTable() {
        super();
    }

    public AbstractVenteArticleItemTable(List<JButton> buttons) {
        super(buttons);
    }

    private static Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

    public static Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    private SQLTable tableArticleTarif = Configuration.getInstance().getBase().getTable("ARTICLE_TARIF");
    private SQLTable tableTarif = Configuration.getInstance().getBase().getTable("TARIF");

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));

        // Code article
        // final SQLTableElement tableElementArticle = new
        // SQLTableElement(e.getTable().getField("ID_ARTICLE"));
        // list.add(tableElementArticle);

        // Code article
        final SQLTableElement tableElementCode = new SQLTableElement(e.getTable().getField("CODE"));
        list.add(tableElementCode);
        // Désignation de l'article
        final SQLTableElement tableElementNom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElementNom);

        if (e.getTable().getFieldsName().contains("COLORIS")) {
            final SQLTableElement tableElementColoris = new SQLTableElement(e.getTable().getField("COLORIS"));
            list.add(tableElementColoris);
        }

        if (e.getTable().getFieldsName().contains("DESCRIPTIF")) {
            final SQLTableElement tableElementDesc = new SQLTableElement(e.getTable().getField("DESCRIPTIF"));
            list.add(tableElementDesc);
        }

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Code Douanier
            final SQLTableElement tableElementCodeDouane = new SQLTableElement(e.getTable().getField("CODE_DOUANIER"));
            list.add(tableElementCodeDouane);
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            final SQLTableElement tableElementPays = new SQLTableElement(e.getTable().getField("ID_PAYS"));
            list.add(tableElementPays);
        }

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

        // Prébilan
        if (e.getTable().getFieldsName().contains("PREBILAN")) {
            SQLTableElement prebilan = new SQLTableElement(e.getTable().getField("PREBILAN"), Long.class, new DeviseCellEditor());
            prebilan.setRenderer(new DeviseNiceTableCellRenderer());
            list.add(prebilan);
        }

        // Prix d'achat HT de la métrique 1
        final SQLTableElement tableElement_PrixMetrique1_AchatHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), Long.class, new DeviseCellEditor());
        tableElement_PrixMetrique1_AchatHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(tableElement_PrixMetrique1_AchatHT);

        SQLTableElement eltDevise = null;
        SQLTableElement eltUnitDevise = null;
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            eltDevise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
            list.add(eltDevise);

            // Prix vente devise
            eltUnitDevise = new SQLTableElement(e.getTable().getField("PV_U_DEVISE"), Long.class, new DeviseCellEditor());
            list.add(eltUnitDevise);
        }
        // Prix de vente HT de la métrique 1
        final DeviseCellEditor editorPVHT = new DeviseCellEditor();
        editorPVHT.setConvertToTTCEnable(true);
        final SQLTableElement tableElement_PrixMetrique1_VenteHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_VT_1"), Long.class, editorPVHT) {
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

        // // Prix d'achat HT de la métrique 1
        // final SQLTableElement tableElement_PrixMetrique1_AchatHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique1_AchatHT);
        // Quantité
        this.qte = new SQLTableElement(e.getTable().getField("QTE"), Integer.class, new QteCellEditor()) {
            protected Object getDefaultNullValue() {
                return Integer.valueOf(0);
            }

            public TableCellRenderer getTableCellRenderer() {
                if (getSQLElement().getTable().getFieldsName().contains("QTE_ACHAT")) {
                    return new QteMultipleRowValuesRenderer();
                } else {
                    return super.getTableCellRenderer();
                }
            }
        };
        this.qte.setPreferredSize(20);
        list.add(this.qte);

        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // // Prix d'achat unitaire HT
        this.ha = new SQLTableElement(e.getTable().getField("PA_HT"), Long.class, new DeviseCellEditor());
        this.ha.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.ha);

        // Prix de vente unitaire HT
        final SQLTableElement tableElement_PrixVente_HT = new SQLTableElement(e.getTable().getField("PV_HT"), Long.class, new DeviseCellEditor());
        tableElement_PrixVente_HT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(tableElement_PrixVente_HT);

        // TVA
        final SQLTableElement tableElement_Taxe = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        tableElement_Taxe.setPreferredSize(20);
        list.add(tableElement_Taxe);
        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class);
        tableElementPoids.setPreferredSize(20);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class);
        list.add(this.tableElementPoidsTotal);

        // Service
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SERVICE, false)) {
            this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
            list.add(this.service);
        }

        this.totalHT = new SQLTableElement(e.getTable().getField("T_PV_HT"), Long.class, new DeviseCellEditor());
        if (e.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
            SQLTableElement tableElementAcompte = new SQLTableElement(e.getTable().getField("POURCENT_ACOMPTE"));
            list.add(tableElementAcompte);
            tableElementAcompte.addModificationListener(this.totalHT);
        }
        SQLTableElement tableElementRemise = new SQLTableElement(e.getTable().getField("POURCENT_REMISE"));
        list.add(tableElementRemise);

        SQLTableElement tableElementRG = null;
        if (e.getTable().getFieldsName().contains("POURCENT_RG")) {
            tableElementRG = new SQLTableElement(e.getTable().getField("POURCENT_RG"));
            list.add(tableElementRG);
        }

        // Total HT
        SQLTableElement totalHA = new SQLTableElement(e.getTable().getField("T_PA_HT"), Long.class, new DeviseCellEditor());
        totalHA.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(totalHA);

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Total HT
            this.tableElementTotalDevise = new SQLTableElement(e.getTable().getField("PV_T_DEVISE"), Long.class, new DeviseCellEditor());
            tableElementTotalDevise.setRenderer(new DeviseNiceTableCellRenderer());
            list.add(tableElementTotalDevise);
        }

        // Marge HT

        if (e.getTable().getFieldsName().contains("MARGE_HT")) {

            SQLTableElement marge = new SQLTableElement(e.getTable().getField("MARGE_HT"), Long.class, new DeviseCellEditor());
            marge.setRenderer(new MargeTableCellRenderer());
            marge.setEditable(false);
            list.add(marge);
            this.totalHT.addModificationListener(marge);
            totalHA.addModificationListener(marge);
            marge.setModifier(new CellDynamicModifier() {
                @Override
                public Object computeValueFrom(SQLRowValues row) {

                    Long vt = row.getLong("T_PV_HT");

                    Long ha = row.getLong("T_PA_HT");

                    final Object o = row.getObject("POURCENT_ACOMPTE");
                    double lA = (o == null) ? 100 : ((BigDecimal) o).doubleValue();
                    if (lA >= 0 && lA != 100) {
                        ha = Math.round(ha * (lA / 100.0));
                        vt = Math.round(vt * (lA / 100.0));
                    }

                    Long r = Long.valueOf(vt - ha);
                    return r;
                }

            });

        }

        // Total HT
        this.totalHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.totalHT);
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PV_TTC"), Long.class, new DeviseCellEditor());
        this.tableElementTotalTTC.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"));

        this.table = new RowValuesTable(this.model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Autocompletion
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        List<String> completionField = new ArrayList<String>();

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {

            completionField.add("CODE_DOUANIER");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_PAYS");
        }
        completionField.add("PA_HT");
        completionField.add("PV_HT");
        completionField.add("ID_TAXE");
        completionField.add("POIDS");
        completionField.add("PRIX_METRIQUE_HA_1");
        completionField.add("PRIX_METRIQUE_HA_2");
        completionField.add("PRIX_METRIQUE_HA_3");
        completionField.add("VALEUR_METRIQUE_1");
        completionField.add("VALEUR_METRIQUE_2");
        completionField.add("VALEUR_METRIQUE_3");
        completionField.add("ID_MODE_VENTE_ARTICLE");
        completionField.add("PRIX_METRIQUE_VT_1");
        completionField.add("PRIX_METRIQUE_VT_2");
        completionField.add("PRIX_METRIQUE_VT_3");
        completionField.add("SERVICE");
        if (getSQLElement().getTable().getFieldsName().contains("DESCRIPTIF")) {
            completionField.add("DESCRIPTIF");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_DEVISE");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("PV_U_DEVISE");
        }
        if (getSQLElement().getTable().getFieldsName().contains("QTE_ACHAT") && sqlTableArticle.getTable().getFieldsName().contains("QTE_ACHAT")) {
            completionField.add("QTE_ACHAT");
        }

        final AutoCompletionManager m = new AutoCompletionManager(tableElementCode, sqlTableArticle.getField("CODE"), this.table, this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field) {
                Object res = tarifCompletion(row, field);
                if (res == null) {
                    return super.getValueFrom(row, field);
                } else {
                    return res;
                }
            }
        };
        m.fill("NOM", "NOM");
        // m.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m.fill(string, string);
        }
        m.setWhere(new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE));

        final AutoCompletionManager m2 = new AutoCompletionManager(tableElementNom, sqlTableArticle.getField("NOM"), this.table, this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field) {
                Object res = tarifCompletion(row, field);
                if (res == null) {
                    return super.getValueFrom(row, field);
                } else {
                    return res;
                }
            }

        };
        m2.fill("CODE", "CODE");
        // m2.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m2.fill(string, string);
        }

        m2.setWhere(new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE));

        // final AutoCompletionManager m3 = new AutoCompletionManager(tableElementArticle,
        // sqlTableArticle.getField("NOM"), this.table, this.table.getRowValuesTableModel(),
        // ITextWithCompletion.MODE_CONTAINS, true, true) {
        // @Override
        // protected Object getValueFrom(SQLRow row, String field) {
        // Object res = tarifCompletion(row, field);
        // if (res == null) {
        // return super.getValueFrom(row, field);
        // } else {
        // return res;
        // }
        // }
        //
        // };
        // m3.fill("CODE", "CODE");
        // m3.fill("NOM", "NOM");
        // for (String string : completionField) {
        // m3.fill(string, string);
        // }

        // m3.setWhere(new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE));

        // Calcul automatique du total HT
        this.qte.addModificationListener(this.totalHT);
        this.qte.addModificationListener(totalHA);
        if (tableElementRG != null) {
            tableElementRG.addModificationListener(this.totalHT);
        }
        tableElementRemise.addModificationListener(this.totalHT);

        tableElement_PrixVente_HT.addModificationListener(this.totalHT);
        // tableElement_PrixVente_HT.addModificationListener(tableElement_PrixMetrique1_VenteHT);
        this.ha.addModificationListener(totalHA);

        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row) {
                final Object o2 = row.getObject("POURCENT_REMISE");

                double lremise = (o2 == null) ? 0 : ((BigDecimal) o2).doubleValue();

                if (row.getTable().getFieldsName().contains("POURCENT_RG")) {
                    final Object o3 = row.getObject("POURCENT_RG");
                    lremise += (o3 == null) ? 0 : ((BigDecimal) o3).doubleValue();
                }

                int qte = (row.getObject("QTE") == null) ? 0 : Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PV_HT");
                long r = f.longValue() * qte;
                if (row.getTable().getFieldsName().contains("POURCENT_ACOMPTE")) {
                    final Object o = row.getObject("POURCENT_ACOMPTE");
                    double lA = (o == null) ? 0 : ((BigDecimal) o).doubleValue();
                    if (lA >= 0 && lA != 100) {
                        r = Math.round(r * (lA / 100.0));
                    }
                }
                if (lremise > 0 && lremise != 100) {
                    r = Math.round(r * (100.0 - lremise) / 100.0);
                }
                return Long.valueOf(r);
            }
        });
        totalHA.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PA_HT");
                long r = f.longValue() * qte;
                return Long.valueOf(r);
            }
        });

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            this.qte.addModificationListener(tableElementTotalDevise);
            eltUnitDevise.addModificationListener(tableElementTotalDevise);
            tableElementRemise.addModificationListener(this.tableElementTotalDevise);
            tableElementTotalDevise.setModifier(new CellDynamicModifier() {
                @Override
                public Object computeValueFrom(SQLRowValues row) {
                    final Object o2 = row.getObject("POURCENT_REMISE");
                    double lremise = (o2 == null) ? 0 : ((BigDecimal) o2).doubleValue();
                    int qte = Integer.parseInt(row.getObject("QTE").toString());
                    Number f = (Number) row.getObject("PV_U_DEVISE");
                    long r = f.longValue() * qte;
                    if (lremise > 0 && lremise != 100) {
                        r = Math.round(r * (100.0 - lremise) / 100.0);
                    }
                    return Long.valueOf(r);
                }
            });
        }
        // Calcul automatique du total TTC
        // tableElement_Quantite.addModificationListener(tableElement_TotalTTC);
        // tableElement_PrixVente_HT.addModificationListener(tableElement_TotalTTC);
        this.totalHT.addModificationListener(this.tableElementTotalTTC);
        tableElement_Taxe.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {

                Number f = (Number) row.getObject("T_PV_HT");
                int idTaux = Integer.parseInt(row.getObject("ID_TAXE").toString());

                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                if (resultTaux == null) {
                    System.err.println("Taxe par défaut non valide");
                    Thread.dumpStack();
                    Integer i = TaxeCache.getCache().getFirstTaxe();
                    if (i == null) {
                        ExceptionHandler.handle("Aucune taxe définie!");
                    } else {
                        row.put("ID_TAXE", i);
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
        this.qte.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                Number f = (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                return new Float(f.floatValue() * qte);
            }

        });
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            eltUnitDevise.addModificationListener(tableElement_PrixMetrique1_VenteHT);
            tableElement_PrixMetrique1_VenteHT.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(SQLRowValues row) {
                    if (!row.getForeign("ID_DEVISE").isUndefined()) {

                        // return Long.valueOf(((Number)
                        // row.getObject("PRIX_METRIQUE_VT_1")).longValue());
                        BigDecimal t = (BigDecimal) row.getForeign("ID_DEVISE").getObject("TAUX");

                        BigDecimal bigDecimal = new BigDecimal(row.getLong("PV_U_DEVISE"));
                        return (t.equals(BigDecimal.ZERO) ? row.getObject("PRIX_METRIQUE_VT_1") : bigDecimal.multiply(t).longValue());
                    }
                    return row.getObject("PRIX_METRIQUE_VT_1");
                }

            });
        }

        // Calcul automatique du prix de vente unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixMetrique1_VenteHT.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixVente_HT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    return Long.valueOf(((Number) row.getObject("PRIX_METRIQUE_VT_1")).longValue());
                } else {

                    final long prixVTFromDetails = ReferenceArticleSQLElement.getPrixVTFromDetails(row);
                    return Long.valueOf(prixVTFromDetails);
                }
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

        this.table.readState();

        setColumnVisible(this.model.getColumnForField("T_PA_HT"), true);

        // Mode Gestion article avancé
        final boolean modeAvance = DefaultNXProps.getInstance().getBooleanValue("ArticleModeVenteAvance", false);
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_1"), modeAvance);
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_2"), modeAvance);
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_3"), modeAvance);
        setColumnVisible(this.model.getColumnForField("PV_HT"), modeAvance);
        setColumnVisible(this.model.getColumnForField("PA_HT"), modeAvance);
        setColumnVisible(this.model.getColumnForField("ID_MODE_VENTE_ARTICLE"), modeAvance);

        // Voir le poids
        final boolean showPoids = DefaultNXProps.getInstance().getBooleanValue("ArticleShowPoids", false);
        setColumnVisible(this.model.getColumnForField("POIDS"), showPoids);
        setColumnVisible(this.model.getColumnForField("T_POIDS"), showPoids);

        // Voir le style
        setColumnVisible(this.model.getColumnForField("ID_STYLE"), DefaultNXProps.getInstance().getBooleanValue("ArticleShowStyle", false));
        setColumnVisible(this.model.getColumnForField("POURCENT_ACOMPTE"), false);


        for (String string : visibilityMap.keySet()) {
            setColumnVisible(this.model.getColumnForField(string), visibilityMap.get(string));
        }

        Map<String, Boolean> mapCustom = getCustomVisibilityMap();
        if (mapCustom != null) {
            for (String string : mapCustom.keySet()) {
                setColumnVisible(this.model.getColumnForField(string), mapCustom.get(string));
            }
        }

        // On réécrit la configuration au cas ou les preferences aurait changé (ajout ou suppression
        // du mode de vente specifique)
        this.table.writeState();
    }

    protected Map<String, Boolean> getCustomVisibilityMap() {
        return null;
    }

    private Object tarifCompletion(SQLRowAccessor row, String field) {

        if (getTarif() != null && getTarif().getID() > tableTarif.getUndefinedID()) {
            // SQLRow rowTarifSelect = tableTarif.getRow(getTarif());
            Collection<? extends SQLRowAccessor> rows = row.getReferentRows(tableArticleTarif);

            SQLRowAccessor rowTarif = null;
            for (SQLRowAccessor rowVals : rows) {
                if (rowVals.getInt("ID_TARIF") == getTarif().getID()) {
                    rowTarif = rowVals;
                }
            }

            if (rowTarif == null) {
                if (!getTarif().getForeign("ID_DEVISE").isUndefined()) {
                    if ((field.equalsIgnoreCase("ID_DEVISE"))) {
                        return getTarif().getObject("ID_DEVISE");
                    } else if ((field.equalsIgnoreCase("PV_U_DEVISE"))) {
                        BigDecimal t = (BigDecimal) getTarif().getForeign("ID_DEVISE").getObject("TAUX");
                        return t.multiply(new BigDecimal(row.getLong("PRIX_METRIQUE_VT_1"))).longValue();

                    }
                }
                return null;
            }
            if (field.equalsIgnoreCase("PRIX_METRIQUE_VT_1")) {
                if (rowTarif.getInt("ID_DEVISE") == SQLRow.UNDEFINED_ID)
                    return rowTarif.getObject(field);
                else {
                    BigDecimal t = (BigDecimal) rowTarif.getForeign("ID_DEVISE").getObject("TAUX");
                    return t.multiply(new BigDecimal(rowTarif.getLong(field))).longValue();
                }

            } else if ((field.equalsIgnoreCase("ID_DEVISE"))) {

                return rowTarif.getObject("ID_DEVISE");
            }

            else if ((field.equalsIgnoreCase("PV_U_DEVISE"))) {

                return rowTarif.getObject("PRIX_METRIQUE_VT_1");

            } else if ((field.equalsIgnoreCase("ID_TAXE"))) {

                if (rowTarif.getInt("ID_TAXE") != SQLRow.UNDEFINED_ID) {
                    return rowTarif.getObject("ID_TAXE");

                }
            }
        }
        return null;
    }

    SQLTable tableArticle = Configuration.getInstance().getBase().getTable("ARTICLE");

    @Override
    public void setTarif(SQLRowAccessor rowValuesTarif, boolean ask) {
        // TODO Raccord de méthode auto-généré
        if (rowValuesTarif == null || getTarif() == null || rowValuesTarif.getID() != getTarif().getID()) {
            super.setTarif(rowValuesTarif, ask);
            if (ask && JOptionPane.showConfirmDialog(null, "Appliquer les tarifs associés au client?") == JOptionPane.YES_OPTION) {
                int nbRows = this.table.getRowCount();
                for (int i = 0; i < nbRows; i++) {
                    SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);

                    // on récupére l'article qui lui correspond

                    SQLRowValues rowValsArticle = new SQLRowValues(tableArticle);
                    for (SQLField field : tableArticle.getFields()) {
                        if (rowVals.getTable().getFieldsName().contains(field.getName())) {
                            rowValsArticle.put(field.getName(), rowVals.getObject(field.getName()));
                        }
                    }
                    // rowArticle.loadAllSafe(rowEltFact);

                    int idArticle = ReferenceArticleSQLElement.getIdForCNM(rowValsArticle, true);
                    SQLRow rowArticle = tableArticle.getRow(idArticle);
                    // SQLRowAccessor rowArticle = rowVals.getForeign("ID_ARTICLE");
                    Collection<? extends SQLRowAccessor> rows = rowArticle.getReferentRows(tableArticleTarif);
                    boolean tarifFind = false;
                    if (getTarif() != null) {
                        for (SQLRowAccessor rowValsTarif : rows) {
                            if (rowValsTarif.getInt("ID_TARIF") == getTarif().getID()) {
                                if (rowValsTarif.getForeign("ID_DEVISE").isUndefined()) {
                                    if (!rowValsTarif.getForeign("ID_TAXE").isUndefined()) {
                                        getRowValuesTable().getRowValuesTableModel().putValue(rowValsTarif.getObject("ID_TAXE"), i, "ID_TAXE");
                                    }
                                    getRowValuesTable().getRowValuesTableModel().putValue(rowValsTarif.getObject("PRIX_METRIQUE_VT_1"), i, "PRIX_METRIQUE_VT_1");
                                } else {
                                    if (!rowValsTarif.getForeign("ID_TAXE").isUndefined()) {
                                        getRowValuesTable().getRowValuesTableModel().putValue(rowValsTarif.getObject("ID_TAXE"), i, "ID_TAXE");
                                    }
                                    getRowValuesTable().getRowValuesTableModel().putValue(rowValsTarif.getObject("ID_DEVISE"), i, "ID_DEVISE");
                                    getRowValuesTable().getRowValuesTableModel().putValue(tarifCompletion(rowArticle, "PV_U_DEVISE"), i, "PV_U_DEVISE");
                                    getRowValuesTable().getRowValuesTableModel().putValue(tarifCompletion(rowArticle, "PRIX_METRIQUE_VT_1"), i, "PRIX_METRIQUE_VT_1");
                                }
                                tarifFind = true;
                                break;
                            }
                        }
                    }
                    if (!tarifFind) {
                        getRowValuesTable().getRowValuesTableModel().putValue(rowArticle.getObject("PRIX_METRIQUE_VT_1"), i, "PRIX_METRIQUE_VT_1");

                    }
                }
            }
        }
    }
}
