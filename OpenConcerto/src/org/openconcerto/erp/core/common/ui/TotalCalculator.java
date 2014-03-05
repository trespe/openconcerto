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

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TotalCalculator {

    private static String FIELD_SERVICE = "SERVICE";
    private static String FIELD_POIDS = "T_POIDS";
    private final String fieldHT, fieldHA, fieldDevise;

    private SQLRowAccessor rowDefaultCptProduit, rowDefaultCptService, rowDefaultCptTVACollecte, rowDefaultCptTVADeductible, rowDefaultCptAchat;
    private static final SQLTable tablePrefCompte = Configuration.getInstance().getRoot().findTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    private double totalPoids;

    private BigDecimal totalDevise, totalDeviseSel;
    private BigDecimal totalHA, totalHASel;
    private BigDecimal totalService, totalServiceSel;
    private BigDecimal totalTTC, totalTTCSel;
    private long remiseHT, remiseRestante;
    private final boolean achat;

    // Total des HT par comptes
    private Map<SQLRowAccessor, BigDecimal> mapHt = new HashMap<SQLRowAccessor, BigDecimal>();
    private Map<SQLRowAccessor, BigDecimal> mapHtSel = new HashMap<SQLRowAccessor, BigDecimal>();

    // Total des TVA par comptes
    private Map<SQLRowAccessor, BigDecimal> mapHtTVA = new HashMap<SQLRowAccessor, BigDecimal>();
    private Map<SQLRowAccessor, BigDecimal> mapHtTVASel = new HashMap<SQLRowAccessor, BigDecimal>();
    private int[] selectedRows;

    private Boolean bServiceActive;
    private BigDecimal totalHTAvantRemise;

    public TotalCalculator(String fieldHA, String fieldHT, String fieldDeviseTotal) {
        this(fieldHA, fieldHT, fieldDeviseTotal, false, null);
    }

    public TotalCalculator(String fieldHA, String fieldHT, String fieldDeviseTotal, boolean achat, SQLRowAccessor defaultCompte) {

        this.achat = achat;
        initValues();

        this.fieldDevise = fieldDeviseTotal;
        this.fieldHA = fieldHA;
        this.fieldHT = fieldHT;

        // Comptes par défaut
        this.rowDefaultCptService = rowPrefsCompte.getForeign("ID_COMPTE_PCE_VENTE_SERVICE");
        if (this.rowDefaultCptService == null || this.rowDefaultCptService.isUndefined()) {
            try {
                this.rowDefaultCptService = ComptePCESQLElement.getRowComptePceDefault("VentesServices");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.rowDefaultCptProduit = rowPrefsCompte.getForeign("ID_COMPTE_PCE_VENTE_PRODUIT");
        if (this.rowDefaultCptProduit == null || this.rowDefaultCptProduit.isUndefined()) {
            try {
                this.rowDefaultCptProduit = ComptePCESQLElement.getRowComptePceDefault("VentesProduits");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.rowDefaultCptTVACollecte = rowPrefsCompte.getForeign("ID_COMPTE_PCE_TVA_VENTE");
        if (this.rowDefaultCptTVACollecte == null || this.rowDefaultCptTVACollecte.isUndefined()) {
            try {
                this.rowDefaultCptTVACollecte = ComptePCESQLElement.getRowComptePceDefault("TVACollectee");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.rowDefaultCptTVADeductible = rowPrefsCompte.getForeign("ID_COMPTE_PCE_TVA_ACHAT");
        if (this.rowDefaultCptTVADeductible == null || this.rowDefaultCptTVADeductible.isUndefined()) {
            try {
                this.rowDefaultCptTVADeductible = ComptePCESQLElement.getRowComptePceDefault("TVADeductible");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (defaultCompte == null || defaultCompte.isUndefined()) {
            this.rowDefaultCptAchat = rowPrefsCompte.getForeign("ID_COMPTE_PCE_ACHAT");
            if (this.rowDefaultCptAchat == null || this.rowDefaultCptAchat.isUndefined()) {
                try {
                    this.rowDefaultCptAchat = ComptePCESQLElement.getRowComptePceDefault("Achats");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            this.rowDefaultCptAchat = defaultCompte;
        }

    }

    /**
     * Définition d'une remise HT à appliquer
     * 
     * @param remiseHT montant de la remise en cents
     * @param totalHTAvantRemise montant de la facture avant remise
     */
    public void setRemise(long remiseHT, BigDecimal totalHTAvantRemise) {
        this.remiseHT = remiseHT;
        this.remiseRestante = remiseHT;
        this.totalHTAvantRemise = totalHTAvantRemise;
    }

    /**
     * Gestion de la vente de service
     * 
     * @param b
     */
    public void setServiceActive(boolean b) {
        this.bServiceActive = b;
    }

    /**
     * Remise à zéro des valeurs de calcul
     */
    public void initValues() {
        this.remiseHT = 0;
        this.remiseRestante = 0;
        this.totalHTAvantRemise = BigDecimal.ZERO;

        this.selectedRows = null;

        this.totalTTC = BigDecimal.ZERO;
        this.totalTTCSel = BigDecimal.ZERO;

        this.totalHA = BigDecimal.ZERO;
        this.totalHASel = BigDecimal.ZERO;

        this.totalService = BigDecimal.ZERO;
        this.totalServiceSel = BigDecimal.ZERO;

        this.totalDeviseSel = BigDecimal.ZERO;
        this.totalDevise = BigDecimal.ZERO;

        this.totalPoids = 0;

        // Total des HT par comptes
        this.mapHt.clear();
        this.mapHtSel.clear();

        // Total des TVA par comptes
        this.mapHtTVA.clear();
        this.mapHtTVASel.clear();

    }

    public void setSelectedRows(int[] selectedRows) {
        this.selectedRows = selectedRows;
    }

    public void addEchantillon(BigDecimal ht, SQLRowAccessor tva) {
        addHT(ht, tva, this.rowDefaultCptProduit, false);
    }

    private Map<Integer, SQLRowAccessor> mapTVA;
    final SQLTable tvaTable = Configuration.getInstance().getRoot().findTable("TAXE");
    final SQLTable compteTable = Configuration.getInstance().getRoot().findTable("COMPTE_PCE");

    /**
     * Mise en cache des comptes de TVA
     */
    private void fetchTVA() {
        mapTVA = new HashMap<Integer, SQLRowAccessor>();
        SQLRowValues rowVals = new SQLRowValues(tvaTable);
        SQLRowValues rowValsC1 = new SQLRowValues(compteTable);
        rowValsC1.put("NUMERO", null);
        rowValsC1.put("ID", null);

        SQLRowValues rowValsC2 = new SQLRowValues(compteTable);
        rowValsC2.put("NUMERO", null);
        rowValsC2.put("ID", null);

        rowVals.put(tvaTable.getKey().getName(), null);
        rowVals.put("ID_COMPTE_PCE", rowValsC1);
        rowVals.put("ID_COMPTE_PCE_DED", rowValsC2);

        SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
        List<SQLRowValues> rowValsList = fetch.fetch();

        for (SQLRowValues sqlRowValues : rowValsList) {
            mapTVA.put(sqlRowValues.getID(), sqlRowValues);
        }
    }

    private void addHT(BigDecimal ht, SQLRowAccessor tva, SQLRowAccessor cptArticle, boolean selection) {

        BigDecimal ttc;
        BigDecimal totalTVA;

        if (tva == null || tva.isUndefined()) {
            ttc = ht;
            totalTVA = BigDecimal.ZERO;
        } else {
            BigDecimal tauxTVA = BigDecimal.valueOf(TaxeCache.getCache().getTauxFromId(tva.getID())).movePointLeft(2);
            ttc = tauxTVA.add(BigDecimal.ONE).multiply(ht, MathContext.DECIMAL128);
            totalTVA = ttc.subtract(ht);
        }

        if (tva != null && !tva.isUndefined()) {
            SQLRowAccessor rowCptTva;
            if (this.achat) {
                rowCptTva = tva.getForeign("ID_COMPTE_PCE_DED");
                if (rowCptTva == null || rowCptTva.isUndefined()) {
                    rowCptTva = this.rowDefaultCptTVADeductible;
                }
            } else {
                rowCptTva = tva.getForeign("ID_COMPTE_PCE");
                if (rowCptTva == null || rowCptTva.isUndefined()) {
                    rowCptTva = this.rowDefaultCptTVACollecte;
                }
            }
            if (mapHtTVA.get(rowCptTva) == null) {
                mapHtTVA.put(rowCptTva, totalTVA);
            } else {
                BigDecimal l = mapHtTVA.get(rowCptTva);
                mapHtTVA.put(rowCptTva, l.add(totalTVA));
            }
            if (selection) {
                if (mapHtTVASel.get(rowCptTva) == null) {
                    mapHtTVASel.put(rowCptTva, totalTVA);
                } else {
                    BigDecimal l = mapHtTVASel.get(rowCptTva);
                    mapHtTVASel.put(rowCptTva, l.add(totalTVA));
                }
            }
        }

        if (mapHt.get(cptArticle) == null) {
            mapHt.put(cptArticle, ht);
        } else {
            BigDecimal l = mapHt.get(cptArticle);
            mapHt.put(cptArticle, l.add(ht));
        }

        this.totalTTC = this.totalTTC.add(ttc);

        if (selection) {

            if (mapHtSel.get(cptArticle) == null) {
                mapHtSel.put(cptArticle, ht);
            } else {
                BigDecimal l = mapHtSel.get(cptArticle);
                mapHtSel.put(cptArticle, l.add(ht));
            }
            this.totalTTCSel = this.totalTTCSel.add(ttc);
        }
    }

    private static boolean containsInt(int[] tab, int i) {
        if (tab == null) {
            return false;
        }

        for (int j = 0; j < tab.length; j++) {
            if (tab[j] == i) {
                return true;
            }
        }
        return false;
    }

    public void addLine(SQLRowAccessor rowAccessorLine, SQLRowAccessor article, int lineNumber, boolean last) {

        // Total HT de la ligne
        BigDecimal totalLineHT = rowAccessorLine.getObject(fieldHT) == null ? BigDecimal.ZERO : (BigDecimal) rowAccessorLine.getObject(fieldHT);

        // Prix Unitaire de la ligne
        // TODO voir pour passer le prix total et non le prix unitaire
        BigDecimal totalHALigne = rowAccessorLine.getObject(fieldHA) == null ? BigDecimal.ZERO : (BigDecimal) rowAccessorLine.getObject(fieldHA);

        Boolean service = rowAccessorLine.getBoolean(FIELD_SERVICE);

        BigDecimal totalLineDevise = (fieldDevise == null || rowAccessorLine.getObject(fieldDevise) == null) ? BigDecimal.ZERO : (BigDecimal) rowAccessorLine.getObject(fieldDevise);

        Number nPoids = (Number) rowAccessorLine.getObject(FIELD_POIDS);

        // Si il y a une remise à appliquer
        if (this.remiseHT != 0 && this.remiseRestante > 0) {

            // Si c'est la derniere ligne, on applique le restant de la remise
            if (last) {
                totalLineHT = totalLineHT.subtract(new BigDecimal(this.remiseRestante).movePointLeft(2));
                this.remiseRestante = 0;
            } else {
                BigDecimal percent = totalLineHT.divide(this.totalHTAvantRemise, MathContext.DECIMAL128);

                BigDecimal remiseApply = percent.multiply(new BigDecimal(this.remiseHT), MathContext.DECIMAL128).setScale(0, RoundingMode.HALF_UP);
                totalLineHT = totalLineHT.subtract(remiseApply.movePointLeft(2));
                this.remiseRestante -= remiseApply.longValue();
            }
        }

        SQLRowAccessor cpt = (achat ? this.rowDefaultCptAchat : this.rowDefaultCptProduit);
        if (article != null && !article.isUndefined()) {
            String suffix = (this.achat ? "_ACHAT" : "");
            SQLRowAccessor compteArticle = article.getForeign("ID_COMPTE_PCE" + suffix);
            if (compteArticle != null && !compteArticle.isUndefined()) {
                cpt = compteArticle;
            } else {
                SQLRowAccessor familleArticle = article.getForeign("ID_FAMILLE_ARTICLE");
                Set<SQLRowAccessor> unique = new HashSet<SQLRowAccessor>();
                while (familleArticle != null && !familleArticle.isUndefined() && !unique.contains(familleArticle)) {

                    unique.add(familleArticle);
                    SQLRowAccessor compteFamilleArticle = familleArticle.getForeign("ID_COMPTE_PCE" + suffix);
                    if (compteFamilleArticle != null && !compteFamilleArticle.isUndefined()) {
                        cpt = compteFamilleArticle;
                        break;
                    }

                    familleArticle = familleArticle.getForeign("ID_FAMILLE_ARTICLE_PERE");
                }
            }
        }

        // Total Service
        if (bServiceActive != null && bServiceActive) {
            if (service != null && service.booleanValue()) {
                totalService = totalService.add(totalLineHT);
                cpt = this.rowDefaultCptService;
            }
        }

        // Total HA
        this.totalHA = this.totalHA.add(totalHALigne);

        // Total Devise
        if (totalLineDevise != null) {
            totalDevise = totalDevise.add(totalLineDevise);
        }

        // Total Poids

        totalPoids += nPoids == null ? 0 : nPoids.doubleValue();

        // Calcul total sélectionné
        boolean selection = containsInt(selectedRows, lineNumber);
        if (selection) {

            totalHASel = totalHASel.add(totalHALigne);

            if (bServiceActive != null && bServiceActive) {
                if (service != null && service.booleanValue()) {
                    totalServiceSel = totalServiceSel.add(totalLineHT);
                }
            }

            if (totalLineDevise != null) {
                totalDeviseSel = totalDeviseSel.add(totalLineDevise);
            }
        }

        // TODO Ne pas fetcher la TVA pour chaque instance de TotalCalculator utiliser un cache
        if (mapTVA == null) {
            fetchTVA();
        }
        final SQLRowAccessor foreignTVA = rowAccessorLine.getForeign("ID_TAXE");
        Integer idTVA = null;
        if (foreignTVA != null) {
            idTVA = foreignTVA.getID();
        }
        addHT(totalLineHT, mapTVA.get(idTVA), cpt, selection);
    }

    /**
     * Vérifie si ht + tva = ttc
     */
    public void checkResult() {
        BigDecimal ht = getTotalHT();
        BigDecimal tva = getTotalTVA();
        BigDecimal totalTTC2 = getTotalTTC();
        BigDecimal reste = totalTTC2.subtract(ht.add(tva));
        if (reste.compareTo(BigDecimal.ZERO) != 0) {
            System.err.print("Ecarts: " + reste + "(HT:" + ht);
            System.err.print(" TVA:" + tva);
            System.err.println(" TTC:" + totalTTC2);
            SQLRow row = ComptePCESQLElement.getRow("758", "Ecarts arrondis");
            // TODO Check if row already exist in MAP ??
            this.mapHt.put(row, reste);
        }
    }

    public BigDecimal getTotalDevise() {
        return totalDevise;
    }

    public BigDecimal getTotalDeviseSel() {
        return totalDeviseSel;
    }

    public BigDecimal getTotalHA() {
        return totalHA;
    }

    public BigDecimal getTotalHASel() {
        return totalHASel;
    }

    public double getTotalPoids() {
        return totalPoids;
    }

    public BigDecimal getTotalService() {
        return totalService;
    }

    public BigDecimal getTotalServiceSel() {
        return totalServiceSel;
    }

    public BigDecimal getTotalHT() {
        BigDecimal ht = BigDecimal.ZERO;
        for (SQLRowAccessor row : this.mapHt.keySet()) {
            ht = ht.add(this.mapHt.get(row).setScale(2, RoundingMode.HALF_UP));
        }

        return ht;
    }

    public BigDecimal getTotalTVA() {
        BigDecimal tva = BigDecimal.ZERO;
        for (SQLRowAccessor row : this.mapHtTVA.keySet()) {
            tva = tva.add(this.mapHtTVA.get(row).setScale(2, RoundingMode.HALF_UP));
        }
        return tva;
    }

    public BigDecimal getTotalTTC() {
        return this.totalTTC.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalHTSel() {
        BigDecimal ht = BigDecimal.ZERO;
        for (SQLRowAccessor row : this.mapHtSel.keySet()) {
            ht = ht.add(this.mapHtSel.get(row).setScale(2, RoundingMode.HALF_UP));
        }

        return ht;
    }

    public BigDecimal getTotalTVASel() {
        BigDecimal tva = BigDecimal.ZERO;
        for (SQLRowAccessor row : this.mapHtTVASel.keySet()) {
            tva = tva.add(this.mapHtTVASel.get(row).setScale(2, RoundingMode.HALF_UP));
        }
        return tva;
    }

    public BigDecimal getTotalTTCSel() {

        return this.totalTTCSel.setScale(2, RoundingMode.HALF_UP);
    }

    public Map<SQLRowAccessor, BigDecimal> getMapHt() {
        return mapHt;
    }

    public Map<SQLRowAccessor, BigDecimal> getMapHtTVA() {
        return mapHtTVA;
    }
}
