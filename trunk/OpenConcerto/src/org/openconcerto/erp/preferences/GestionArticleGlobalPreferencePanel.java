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
 
 /*
 * Créé le 6 mars 2012
 */
package org.openconcerto.erp.preferences;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

public class GestionArticleGlobalPreferencePanel extends JavaPrefPreferencePanel {
    public static String STOCK_FACT = "StockOnOrder";
    public static String UNITE_VENTE = "UniteVenteActive";
    public static String USE_CREATED_ARTICLE = "UseCreatedArticle";
    public static String CREATE_ARTICLE_AUTO = "CreateArticleAuto";
    public static String SUPPLIER_PRODUCT_CODE = "SupplierProductCode";
    public static String SHOW_PRODUCT_BAR_CODE = "ShowProductBarCode";
    public static String ITEM_PACKAGING = "ItemPackaging";

    public GestionArticleGlobalPreferencePanel() {
        super("Gestion des articles", null);
        setPrefs(new SQLPreferences(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete()));
    }

    @Override
    protected void addViews() {

        PrefView<Boolean> viewAchat = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer les codes articles fournisseurs", SUPPLIER_PRODUCT_CODE);
        viewAchat.setDefaultValue(Boolean.FALSE);
        this.addView(viewAchat);

        PrefView<Boolean> view = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer les sorties de stock avec les factures et non les bons de livraison", STOCK_FACT);
        view.setDefaultValue(Boolean.TRUE);
        this.addView(view);

        PrefView<Boolean> view2 = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer différentes unités de vente", UNITE_VENTE);
        view2.setDefaultValue(Boolean.TRUE);
        this.addView(view2);

        PrefView<Boolean> view6 = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Gérer les colis", ITEM_PACKAGING);
        view6.setDefaultValue(Boolean.FALSE);
        this.addView(view6);

        PrefView<Boolean> view3 = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Utiliser uniquement des articles existant", USE_CREATED_ARTICLE);
        view3.setDefaultValue(Boolean.FALSE);
        this.addView(view3);

        PrefView<Boolean> view4 = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Créer automatiquement les articles (si il n'y a aucune correspondance CODE, DESIGNATION)", CREATE_ARTICLE_AUTO);
        view4.setDefaultValue(Boolean.TRUE);
        this.addView(view4);

        PrefView<Boolean> view5 = new PrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Afficher le code barre des articles dans les sélecteurs", SHOW_PRODUCT_BAR_CODE);
        view5.setDefaultValue(Boolean.FALSE);
        this.addView(view5);

    }
}
