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
 
 package org.openconcerto.erp.core.sales.product.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.product.component.ReferenceArticleSQLComponent;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ReferenceArticleSQLElement extends ComptaSQLConfElement {
    public static final int AU_METRE_LONGUEUR = 2;
    public static final int AU_METRE_CARRE = 3;
    public static final int AU_POID_METRECARRE = 4;
    public static final int A_LA_PIECE = 5;
    public static final int AU_METRE_LARGEUR = 6;
    private static final int PRIX_HA = 1;
    private static final int PRIX_VT = 2;

    public ReferenceArticleSQLElement() {
        super("ARTICLE", "un article", "articles");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("CODE");
        l.add("NOM");
        String articleAdvanced = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bArticleAdvanced = Boolean.valueOf(articleAdvanced);

        if (bArticleAdvanced) {
            l.add("POIDS");
            l.add("PRIX_METRIQUE_HA_1");
            l.add("PRIX_METRIQUE_VT_1");
        }
        l.add("PA_HT");
        l.add("PV_HT");
            l.add("ID_TAXE");
        l.add("PV_TTC");
        l.add("ID_FAMILLE_ARTICLE");
        l.add("ID_FOURNISSEUR");
        l.add("ID_STOCK");
        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);
        if (b != null && b.booleanValue()) {
            l.add("SERVICE");
        }
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_STOCK");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new ReferenceArticleSQLComponent(this);
    }

    /**
     * Calcul le prix d'HA total par rapport aux metriques
     * 
     * @param rowVals
     * @return le prix d'achat en centimes
     */
    public static BigDecimal getPrixHAFromDetails(SQLRowValues rowVals) {
        return getValuePiece(rowVals, PRIX_HA);
    }

    /**
     * Calcul le prix de VT total par rapport aux metriques
     * 
     * @param rowVals
     * @return le prix de vente en centimes
     */
    public static BigDecimal getPrixVTFromDetails(SQLRowValues rowVals) {
        return getValuePiece(rowVals, PRIX_VT);
    }

    /**
     * Calcul le poids total par rapport aux metriques
     * 
     * @param rowVals
     * @return le poids total arrondi à trois chiffres apres la virgule
     */
    public static float getPoidsFromDetails(SQLRowValues rowVals) {

        // Valeur
        float valMetrique1 = (rowVals.getObject("VALEUR_METRIQUE_1") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_1");
        float valMetrique2 = (rowVals.getObject("VALEUR_METRIQUE_2") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_2");
        float valMetrique3 = (rowVals.getObject("VALEUR_METRIQUE_3") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_3");

        final float produit = valMetrique1 * valMetrique2 * valMetrique3;
        if (produit > 0.0F) {
            return Math.round(produit * 1000.0F) / 1000.0F;
        }
        if (rowVals.getObject("POIDS") != null) {
            float p = rowVals.getFloat("POIDS");
            return p;
        }
        return 0.0F;
    }

    private static BigDecimal getValuePiece(SQLRowValues rowVals, int value) {
        if (rowVals.getObject("ID_MODE_VENTE_ARTICLE") == null) {
            throw new IllegalArgumentException("La SQLRowValues ne contient pas ID_MODE_VENTE_ARTICLE");
        }
        int mode = rowVals.getInt("ID_MODE_VENTE_ARTICLE");
        if (mode == 1) {
            mode = A_LA_PIECE;
        }
        // prix HA
        BigDecimal metrique1HA = rowVals.getObject("PRIX_METRIQUE_HA_1") == null ? BigDecimal.ZERO : ((BigDecimal) rowVals.getObject("PRIX_METRIQUE_HA_1"));

        // Prix VT
        BigDecimal metrique1VT = rowVals.getObject("PRIX_METRIQUE_VT_1") == null ? BigDecimal.ZERO : ((BigDecimal) rowVals.getObject("PRIX_METRIQUE_VT_1"));

        // Valeur
        float valMetrique1 = (rowVals.getObject("VALEUR_METRIQUE_1") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_1");
        float valMetrique2 = (rowVals.getObject("VALEUR_METRIQUE_2") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_2");
        float valMetrique3 = (rowVals.getObject("VALEUR_METRIQUE_3") == null) ? 0.0F : rowVals.getFloat("VALEUR_METRIQUE_3");

        // Mode de vente à la piece
        if (mode == A_LA_PIECE) {
            if (value == PRIX_HA) {
                if (rowVals.getObject("PA_HT") != null) {
                    return (BigDecimal) rowVals.getObject("PA_HT");

                }
                return BigDecimal.ZERO;

            }
            if (rowVals.getObject("PV_HT") != null) {
                return (BigDecimal) rowVals.getObject("PV_HT");
            }
            return BigDecimal.ZERO;

        }
        // Mode de vente au metre carré
        if (mode == AU_METRE_CARRE) {
            float surface = valMetrique1 * valMetrique2;
            if (value == PRIX_HA) {
                return metrique1HA.multiply(BigDecimal.valueOf(surface), MathContext.DECIMAL128);
            }
            return metrique1VT.multiply(BigDecimal.valueOf(surface), MathContext.DECIMAL128);
        }
        // Mode de vente au metre, largeur
        if (mode == AU_METRE_LARGEUR) {
            if (value == PRIX_HA) {
                return metrique1HA.multiply(BigDecimal.valueOf(valMetrique2), MathContext.DECIMAL128);
            }
            return metrique1VT.multiply(BigDecimal.valueOf(valMetrique2), MathContext.DECIMAL128);
        }
        // Mode de vente au metre, longueur
        if (mode == AU_METRE_LONGUEUR) {
            if (value == PRIX_HA) {
                return metrique1HA.multiply(BigDecimal.valueOf(valMetrique1), MathContext.DECIMAL128);
            }
            return metrique1VT.multiply(BigDecimal.valueOf(valMetrique1), MathContext.DECIMAL128);
        }
        // Mode de vente au poids / m2
        if (mode == AU_POID_METRECARRE) {
            float surface = valMetrique1 * valMetrique2;
            float p = surface * valMetrique3;
            if (value == PRIX_HA) {
                return metrique1HA.multiply(BigDecimal.valueOf(p), MathContext.DECIMAL128);
            }
            return metrique1VT.multiply(BigDecimal.valueOf(p), MathContext.DECIMAL128);
        }
        throw new IllegalStateException("Unknown mode:" + mode);

    }

    /**
     * retourne l'id d'un article ayant le meme Code, Nom et valeur Metrique, le cas échéant -1
     * 
     * @param row
     * @param createIfNotExist
     * @return id de l'article correspondant
     */
    public static int getIdForCNM(SQLRowValues row, boolean createIfNotExist) {

        return getIdFor(row, true, createIfNotExist);
    }

    /**
     * retourne l'id d'un article ayant le meme Code, Nom le cas échéant -1
     * 
     * @param row
     * @param createIfNotExist
     * @return id de l'article correspondant
     */
    public static int getIdForCN(SQLRowValues row, boolean createIfNotExist) {

        return getIdFor(row, false, createIfNotExist);
    }

    private static int getIdFor(SQLRowValues row, boolean includeMetrique, boolean createIfNotExist) {

        // On cherche l'article qui lui correspond
        SQLTable tableArt = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(tableArt);
        String req = getMatchRequest(row, includeMetrique);
        List result = (List) eltArticle.getTable().getBase().getDataSource().execute(req, new ArrayListHandler());

        if (result != null && result.size() != 0) {
            Object[] tmp = (Object[]) result.get(0);
            return ((Number) tmp[0]).intValue();
        }

        if (createIfNotExist) {
            SQLRowValues vals = new SQLRowValues(row);
            SQLRow rowNew;
            try {
                rowNew = vals.insert();
                return rowNew.getID();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return -1;

    }

    /**
     * teste si un article ayant le meme nom et code existe
     * 
     * @param row
     * @return true si au moins un article correspond
     */
    public static boolean isArticleForCNExist(SQLRowValues row) {
        return isArticleMatchExist(row, false);
    }

    /**
     * teste si un article ayant le meme nom, code et valeur metrique existe
     * 
     * @param row
     * @return true si au moins un article correspond
     */
    public static boolean isArticleForCNMExist(SQLRowValues row) {
        return isArticleMatchExist(row, true);
    }

    private static boolean isArticleMatchExist(SQLRowValues row, boolean includeMetrique) {
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        String req = getMatchRequest(row, includeMetrique);
        List result = (List) eltArticle.getTable().getBase().getDataSource().execute(req, new ArrayListHandler());

        return (result != null && result.size() != 0);
    }

    private static String getMatchRequest(SQLRowValues row, boolean includeMetrique) {
        // On cherche l'article qui lui correspond
        SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        SQLSelect sel = new SQLSelect(eltArticle.getTable().getBase());
        sel.addSelect(eltArticle.getTable().getField("ID"));

        Where w = new Where(eltArticle.getTable().getField("CODE"), "=", row.getString("CODE"));
        if (includeMetrique) {

            float value1 = ((Number) row.getObject("VALEUR_METRIQUE_1")).floatValue();
            float value2 = ((Number) row.getObject("VALEUR_METRIQUE_2")).floatValue();
            float value3 = ((Number) row.getObject("VALEUR_METRIQUE_3")).floatValue();
            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_1"), "<=", new Float(value1 + 0.00001)));
            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_1"), ">=", new Float(value1 - 0.00001)));

            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_2"), "<=", new Float(value2 + 0.00001)));
            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_2"), ">=", new Float(value2 - 0.00001)));

            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_3"), "<=", new Float(value3 + 0.00001)));
            w = w.and(new Where(eltArticle.getTable().getField("VALEUR_METRIQUE_3"), ">=", new Float(value3 - 0.00001)));

        }
        sel.setWhere(w);
        return sel.asString();
    }

    public static boolean isReferenceEquals(SQLRowValues rowVals1, SQLRowValues rowVals2) {
        return (rowVals1.getObject("CODE").equals(rowVals2.getObject("CODE")) && rowVals1.getString("VALEUR_METRIQUE_1").equals(rowVals2.getString("VALEUR_METRIQUE_1"))
                && rowVals1.getString("VALEUR_METRIQUE_2").equals(rowVals2.getString("VALEUR_METRIQUE_2")) && rowVals1.getString("VALEUR_METRIQUE_3").equals(rowVals2.getString("VALEUR_METRIQUE_3")));
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".ref";
    }
}
