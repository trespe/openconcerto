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
import org.openconcerto.erp.core.sales.product.ui.QteUnitRowValuesRenderer;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ToolTipManager;
import javax.swing.table.TableCellRenderer;

public abstract class AbstractAchatArticleItemTable extends AbstractArticleItemTable {

    public AbstractAchatArticleItemTable() {
        super();
    }

    AutoCompletionManager m;
    AutoCompletionManager m2, m3;
    private final SQLTable tableArticle = getSQLElement().getTable().getTable("ARTICLE");

    /**
     * 
     */
    protected void init() {

        final SQLElement e = getSQLElement();

        SQLPreferences prefs = new SQLPreferences(getSQLElement().getTable().getDBRoot());
        final boolean selectArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.USE_CREATED_ARTICLE, false);
        final boolean createAuto = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));

        final SQLTableElement tableElementArticle = new SQLTableElement(e.getTable().getField("ID_ARTICLE"), true, true, true);
        list.add(tableElementArticle);

        if (e.getTable().getFieldsName().contains("ID_FAMILLE_ARTICLE")) {
            final SQLTableElement tableFamille = new SQLTableElement(e.getTable().getField("ID_FAMILLE_ARTICLE"));
            list.add(tableFamille);
        }

        if (e.getTable().getFieldsName().contains("PREBILAN")) {
            final SQLTableElement tableElementPre = new SQLTableElement(e.getTable().getField("PREBILAN"), Long.class, new DeviseCellEditor());
            tableElementPre.setRenderer(new DeviseNiceTableCellRenderer());
            list.add(tableElementPre);
        }

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

        final SQLTableElement tableElement_Devise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
        final SQLTableElement tableElement_PA_Devise = new SQLTableElement(e.getTable().getField("PA_DEVISE"), Long.class, new DeviseCellEditor());
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            list.add(tableElement_Devise);

            // Prix d'achat HT devise
            tableElement_PA_Devise.setRenderer(new DeviseNiceTableCellRenderer());
            list.add(tableElement_PA_Devise);
        }
        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // Prix d'achat unitaire HT
        this.ha = new SQLTableElement(e.getTable().getField("PA_HT"), Long.class, new DeviseCellEditor());
        this.ha.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.ha);

        SQLTableElement qteU = new SQLTableElement(e.getTable().getField("QTE_UNITAIRE"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals) {

                SQLRowAccessor row = vals.getForeign("ID_UNITE_VENTE");
                if (row != null && !row.isUndefined() && row.getBoolean("A_LA_PIECE")) {
                    return false;
                } else {
                    return super.isCellEditable(vals);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }
        };
        list.add(qteU);

        SQLTableElement uniteVente = new SQLTableElement(e.getTable().getField("ID_UNITE_VENTE"));
        list.add(uniteVente);

        // Quantité
        final SQLTableElement qteElement = new SQLTableElement(e.getTable().getField("QTE"), Integer.class) {
            protected Object getDefaultNullValue() {
                return Integer.valueOf(0);
            }
        };
        list.add(qteElement);
        // TVA
        this.tableElementTVA = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(this.tableElementTVA);
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

        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Prix d'achat HT devise
            this.tableElementTotalDevise = new SQLTableElement(e.getTable().getField("PA_DEVISE_T"), Long.class, new DeviseCellEditor());
            tableElementTotalDevise.setRenderer(new DeviseNiceTableCellRenderer());
            list.add(tableElementTotalDevise);
        }
        // Total HT
        this.totalHT = new SQLTableElement(e.getTable().getField("T_PA_HT"), Long.class, new DeviseCellEditor());
        this.totalHT.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.totalHT);
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PA_TTC"), Long.class, new DeviseCellEditor());
        this.tableElementTotalTTC.setRenderer(new DeviseNiceTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        this.model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"), false, null);

        this.table = new RowValuesTable(this.model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        // Autocompletion
        List<String> completionFields = new ArrayList<String>();
        completionFields.add("ID_UNITE_VENTE");
        completionFields.add("PA_HT");
        completionFields.add("PV_HT");
        completionFields.add("POIDS");
        completionFields.add("ID_TAXE");
        completionFields.add("PRIX_METRIQUE_HA_1");
        completionFields.add("PRIX_METRIQUE_HA_2");
        completionFields.add("PRIX_METRIQUE_HA_3");
        completionFields.add("VALEUR_METRIQUE_1");
        completionFields.add("VALEUR_METRIQUE_2");
        completionFields.add("VALEUR_METRIQUE_3");
        completionFields.add("ID_MODE_VENTE_ARTICLE");
        completionFields.add("PRIX_METRIQUE_VT_1");
        completionFields.add("PRIX_METRIQUE_VT_2");
        completionFields.add("PRIX_METRIQUE_VT_3");
        completionFields.add("SERVICE");
        completionFields.add("ID_DEVISE");
        completionFields.add("PA_DEVISE");
        if (e.getTable().getFieldsName().contains("COLORIS")) {
            completionFields.add("COLORIS");
        }
        if (e.getTable().getFieldsName().contains("ID_FAMILLE_ARTICLE")) {
            completionFields.add("ID_FAMILLE_ARTICLE");
        }

        this.m = new AutoCompletionManager(tableElementCode, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.CODE"), this.table,
                this.table.getRowValuesTableModel()) {
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
        m.fill("ID", "ID_ARTICLE");
        for (String string : completionFields) {
            m.fill(string, string);
        }
        this.m2 = new AutoCompletionManager(tableElementNom, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel()) {
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
        m2.fill("ID", "ID_ARTICLE");
        for (String string : completionFields) {
            m2.fill(string, string);
        }

        this.m3 = new AutoCompletionManager(tableElementArticle, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel(), ITextWithCompletion.MODE_CONTAINS, true, true) {
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
        m3.fill("CODE", "CODE");
        m3.fill("NOM", "NOM");
        for (String string : completionFields) {
            m3.fill(string, string);
        }

        // Calcul automatique du total HT
        qteElement.addModificationListener(this.totalHT);
        qteU.addModificationListener(this.totalHT);
        this.ha.addModificationListener(this.totalHT);
        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row) {

                // Object qteObject = row.getObject("QTE");
                // int qte = (qteObject == null) ? 0 : Integer.parseInt(qteObject.toString());
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                Number f = (Number) row.getObject("PA_HT");
                System.err.println("Qte:" + qte + " et PA_HT:" + f);
                // long longValue = (f == null) ? 0 : f.longValue();
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                long r = b.multiply(new BigDecimal(f.longValue() * qte), MathContext.DECIMAL128).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                return new Long(r);
            }

        });
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            qteElement.addModificationListener(this.tableElementTotalDevise);
            qteU.addModificationListener(this.tableElementTotalDevise);
            tableElement_PA_Devise.addModificationListener(this.tableElementTotalDevise);
            this.tableElementTotalDevise.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row) {

                    // Object qteObject = row.getObject("QTE");
                    // int qte = (qteObject == null) ? 0 : Integer.parseInt(qteObject.toString());
                    int qte = Integer.parseInt(row.getObject("QTE").toString());
                    Number f = (Number) row.getObject("PA_DEVISE");
                    // long longValue = (f == null) ? 0 : f.longValue();
                    BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                    long r = b.multiply(new BigDecimal(f.longValue() * qte), MathContext.DECIMAL128).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                    return r;
                }

            });
        }
        // Calcul automatique du total TTC
        qteElement.addModificationListener(this.tableElementTotalTTC);
        qteU.addModificationListener(this.tableElementTotalTTC);
        this.ha.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTVA.addModificationListener(this.tableElementTotalTTC);
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

                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                long r = b.multiply(new BigDecimal(f.longValue() * qte), MathContext.DECIMAL128).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
                PrixHT pHT = new PrixHT(r);
                // System.err.println("Calcul du total TTC : taux " + resultTaux + " : HT " + pHT);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                // float taux = resultTaux.floatValue();
                Long total = new Long(pHT.calculLongTTC(taux / 100f));
                return total;
            }

        });

        this.table.readState();

        tableElementCode.addModificationListener(tableElementArticle);
        tableElementArticle.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row) {
                SQLRowAccessor foreign = row.getForeign("ID_ARTICLE");
                if (foreign != null && !foreign.isUndefined() && foreign.getObject("CODE") != null && foreign.getString("CODE").equals(row.getString("CODE"))) {
                    return foreign.getID();
                } else {
                    return tableArticle.getUndefinedID();
                }
            }
        });

        // Mode Gestion article avancé
        String valModeAvanceVt = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bModeAvance = Boolean.valueOf(valModeAvanceVt);
        boolean view = !(bModeAvance != null && !bModeAvance.booleanValue());
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_1"), view);
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_2"), view);
        setColumnVisible(this.model.getColumnForField("VALEUR_METRIQUE_3"), view);
        setColumnVisible(this.model.getColumnForField("PRIX_METRIQUE_VT_1"), view);
        setColumnVisible(this.model.getColumnForField("ID_MODE_VENTE_ARTICLE"), view);
        setColumnVisible(this.model.getColumnForField("PA_HT"), view);

        // Gestion des unités de vente
        final boolean gestionUV = prefs.getBoolean(GestionArticleGlobalPreferencePanel.UNITE_VENTE, true);
        setColumnVisible(this.model.getColumnForField("QTE_UNITAIRE"), gestionUV);
        setColumnVisible(this.model.getColumnForField("ID_UNITE_VENTE"), gestionUV);

        setColumnVisible(this.model.getColumnForField("ID_STYLE"), DefaultNXProps.getInstance().getBooleanValue("ArticleShowStyle", true));

        setColumnVisible(this.model.getColumnForField("ID_ARTICLE"), selectArticle);
        setColumnVisible(this.model.getColumnForField("CODE"), !selectArticle || (selectArticle && createAuto));
        setColumnVisible(this.model.getColumnForField("NOM"), !selectArticle || (selectArticle && createAuto));

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
        qteU.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                System.err.println("Calcul du poids total ");
                Number f = (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());

                // return new Float(f.floatValue() * qte);
                //
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                // FIXME convertir en float autrement pour éviter une valeur non transposable
                // avec floatValue ou passer POIDS en bigDecimal
                return b.multiply(new BigDecimal(f.floatValue() * qte)).floatValue();
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

        uniteVente.addModificationListener(qteU);
        qteU.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row) {
                SQLRowAccessor rowUnite = row.getForeign("ID_UNITE_VENTE");
                if (rowUnite != null && !rowUnite.isUndefined() && rowUnite.getBoolean("A_LA_PIECE")) {
                    return BigDecimal.ONE;
                } else {
                    return row.getObject("QTE_UNITAIRE");
                }
            }

        });

        for (String string : visibilityMap.keySet()) {
            setColumnVisible(this.model.getColumnForField(string), visibilityMap.get(string));
        }

        // On réécrit la configuration au cas ou les preferences aurait changé
        this.table.writeState();
    }

    private static Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

    public static Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    private Object tarifCompletion(SQLRow row, String field) {

        if (getDevise() != null && !getDevise().isUndefined()) {
            if ((field.equalsIgnoreCase("ID_DEVISE") || field.equalsIgnoreCase("ID_DEVISE_HA"))) {
                return getDevise().getID();
            } else if ((field.equalsIgnoreCase("PA_DEVISE"))) {
                return row.getObject("PA_DEVISE");
            }
        } else {
            if ((field.equalsIgnoreCase("ID_DEVISE") || field.equalsIgnoreCase("ID_DEVISE_HA"))) {
                return Configuration.getInstance().getDirectory().getElement("DEVISE").getTable().getUndefinedID();
            } else if ((field.equalsIgnoreCase("PA_DEVISE"))) {

                return Long.valueOf(0);
            }
        }
        return null;
    }

    SQLRowAccessor rowDevise;

    public SQLRowAccessor getDevise() {
        return this.rowDevise;
    }

    public void setDevise(SQLRowAccessor deviseRow) {
        this.rowDevise = deviseRow;
    }

    public void setFournisseurFilterOnCompletion(SQLRow row) {
        if (row != null && !row.isUndefined()) {

            Where w = new Where(tableArticle.getField("ID_FOURNISSEUR"), "=", row.getID());
            w = w.or(new Where(tableArticle.getField("ID_FOURNISSEUR"), "IS", (Object) null));
            w = w.or(new Where(tableArticle.getField("ID_FOURNISSEUR"), "=", getSQLElement().getTable().getTable("FOURNISSEUR").getUndefinedID()));
            this.m.setWhere(w);
            this.m2.setWhere(w);
            this.m3.setWhere(w);
        } else {
            this.m.setWhere(null);
            this.m2.setWhere(null);
            this.m3.setWhere(null);
        }
    }

}
