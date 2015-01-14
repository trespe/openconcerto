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
 
 package org.openconcerto.erp.core.supplychain.product.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.supplychain.product.component.ArticleFournisseurSQLComponent;
import org.openconcerto.erp.generationDoc.gestcomm.FicheArticleXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.utils.CollectionMap;

import java.util.ArrayList;
import java.util.List;

public class ArticleFournisseurSQLElement extends ComptaSQLConfElement {

    public ArticleFournisseurSQLElement() {
        super("ARTICLE_FOURNISSEUR", "un article fournisseur", "articles fournisseurs");

        getRowActions().addAll(new MouseSheetXmlListeListener(FicheArticleXmlSheet.class).getRowActions());

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
        l.add("ID_FAMILLE_ARTICLE_FOURNISSEUR");
        l.add("ID_FOURNISSEUR");
        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);
        if (b != null && b.booleanValue()) {
            l.add("SERVICE");
        }
        return l;
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        final CollectionMap<String, String> res = new CollectionMap<String, String>();
        res.put(null, "NOM");
        res.put(null, "ID_FAMILLE_ARTICLE_FOURNISSEUR");
        return res;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        SQLPreferences prefs = new SQLPreferences(getTable().getDBRoot());
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.SHOW_PRODUCT_BAR_CODE, false)) {
            l.add("CODE_BARRE");
        }

        l.add("NOM");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new ArticleFournisseurSQLComponent(this);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
    }
}
