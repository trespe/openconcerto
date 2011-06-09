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
 
 package org.openconcerto.erp.core.sales.product.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;


/**
 * Singleton pour la manipulation des Articles, prix, modifs..
 */
public class ArticleManager {

    private static ArticleManager instance;
    private List cacheCode;
    private final SQLTable table = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("ARTICLE");

    private ArticleManager() {
        super();
    }

    public synchronized static ArticleManager getInstance() {
        if (instance == null) {
            instance = new ArticleManager();
        }
        return instance;
    }

    public void clearCache() {
        this.cacheCode = null;
    }

    public void insertOrUpdate(String code, String nom, long prixAchatHT) {
        if (cacheCode == null) {
            fillCache();
        }
        code = code.toLowerCase().trim();
        nom = nom.toLowerCase().trim();
        SQLRowValues rowParDefaut = new SQLRowValues(table);
        boolean update = false;

        for (int i = 0; i < cacheCode.size(); i++) {
            Map m = (Map) cacheCode.get(i);
            String c = (String) m.get("CODE");
            String n = (String) m.get("NOM");
            if ((code.length() > 0 && c.toLowerCase().trim().equals(code)) || n.toLowerCase().trim().equals(nom)) {
                // on doit faire juste l'update
                int idAUpdater = ((Number) m.get("ID")).intValue();
                rowParDefaut.loadAbsolutelyAll(table.getRow(idAUpdater));

                update = true;
                break;
            }

        }
        // on l'insere
        if (!update) {
            rowParDefaut.loadAllSafe(table.getRow(table.getUndefinedID()));
            rowParDefaut.put("NOM", nom);
            rowParDefaut.put("CODE", code);
        }

        rowParDefaut.put("PA_HT", new Long(prixAchatHT));
        rowParDefaut.put("PV_HT", new Long(prixDeVente(prixAchatHT)));

        try {
            // on ne peut pas utiliser commit car l'id est 1 si on insert
            if (update)
                rowParDefaut.update();
            else
                rowParDefaut.insert();
        } catch (SQLException e) {

            e.printStackTrace();
        }

    }

    /**
     * Retourne le prix de vente HT d'un produit en fonction de son prix d'achat HT
     * 
     * @return le bon prix
     */
    public long prixDeVente(long prixAchatHT) {
        // PV TTC = 1.6 PA HT
        // et PV TTC sans centimes
        double pVTTC = (prixAchatHT * 1.6);

        long pi = Math.round(pVTTC);
        double pV = (pi * 100) / 1.196;

        return Math.round(pV);
    }

    /**
     * Rempli le cache des articles
     */

    private void fillCache() {
        this.cacheCode = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getDataSource().execute("SELECT ID,NOM,CODE FROM ARTICLE");

    }

}
