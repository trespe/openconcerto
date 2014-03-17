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
 
 package org.openconcerto.erp.generationEcritures;

import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProviderManager;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

// FIXME probleme lors de certaines generation tout reste figer

/**
 * Génération des ecritures associées à une saisie de vente avec facture. Entaine la génération du
 * reglement de la vente
 */
public class GenerationMvtSaisieVenteFacture extends GenerationEcritures implements Runnable {

    public static final String ID = "accounting.records.invoice.sales";
    private static final String source = "SAISIE_VENTE_FACTURE";
    public static final Integer journal = Integer.valueOf(JournalSQLElement.VENTES);
    private int idSaisieVenteFacture;
    private static final SQLTable saisieVFTable = base.getTable("SAISIE_VENTE_FACTURE");
    private static final SQLTable mvtTable = base.getTable("MOUVEMENT");
    private static final SQLTable ecrTable = base.getTable("ECRITURE");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    /**
     * Generation de la comptabilité associée à la modification d'une saisie de vente facture
     * 
     * @param idSaisieVenteFacture
     * @param idMvt id du mouvement qui est dejà associé à la facture
     */
    public GenerationMvtSaisieVenteFacture(int idSaisieVenteFacture, int idMvt) {
        System.err.println("********* init GeneRation");
        this.idMvt = idMvt;
        this.idSaisieVenteFacture = idSaisieVenteFacture;
        new Thread(GenerationMvtSaisieVenteFacture.this).start();
    }

    /**
     * Generation de la comptabilité associée à la création d'une saisie de vente facture
     * 
     * @param idSaisieVenteFacture
     */
    public GenerationMvtSaisieVenteFacture(int idSaisieVenteFacture) {
        this(idSaisieVenteFacture, 1);
    }

    private void genereMouvement() throws Exception {

        SQLRow saisieRow = GenerationMvtSaisieVenteFacture.saisieVFTable.getRow(this.idSaisieVenteFacture);
        SQLRow clientRow = saisieRow.getForeignRow("ID_CLIENT");

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue());
        // Total des acomptes déjà versés sur la facture
        long montantAcompteTTC = 0;

        int idCompteClient = clientRow.getInt("ID_COMPTE_PCE");

        Boolean acompte = saisieRow.getBoolean("ACOMPTE");
        if (acompte != null && acompte) {
            this.nom = "Fact. acompte client" + saisieRow.getObject("NUMERO").toString();
        } else {
            this.nom = "Fact. vente " + saisieRow.getObject("NUMERO").toString();
        }

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        AccountingRecordsProvider provider = AccountingRecordsProviderManager.get(ID);
        provider.putLabel(saisieRow, this.mEcritures);

        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieVenteFacture.journal);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            SQLRowValues rowValsPiece = new SQLRowValues(pieceTable);
            provider.putPieceLabel(saisieRow, rowValsPiece);
            getNewMouvement(GenerationMvtSaisieVenteFacture.source, this.idSaisieVenteFacture, 1, rowValsPiece);
        } else {
            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
            SQLRowValues rowValsPiece = mvtTable.getRow(idMvt).getForeign("ID_PIECE").asRowValues();
            provider.putPieceLabel(saisieRow, rowValsPiece);
            rowValsPiece.update();
        }

        SQLTable tableEchantillon = null;
        if (saisieVFTable.getDBRoot().contains("ECHANTILLON_ELEMENT")) {
            tableEchantillon = saisieVFTable.getTable("ECHANTILLON_ELEMENT");
        }
        BigDecimal portHT = BigDecimal.valueOf(saisieRow.getLong("PORT_HT")).movePointLeft(2);
        TotalCalculator calc = getValuesFromElement(saisieRow, saisieVFTable.getTable("SAISIE_VENTE_FACTURE_ELEMENT"), portHT, saisieRow.getForeign("ID_TAXE_PORT"), tableEchantillon);

        // On génére les ecritures si la facture n'est pas un acompte
        long ttcLongValue = calc.getTotalTTC().movePointRight(2).longValue();
        if (acompte == null || !acompte) {

            for (SQLRowAccessor row : calc.getMapHt().keySet()) {
                long b = calc.getMapHt().get(row).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
                if (b != 0) {
                    this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(row.getID()));
                    this.mEcritures.put("DEBIT", Long.valueOf(0));
                    this.mEcritures.put("CREDIT", Long.valueOf(b));
                    int idEcr = ajoutEcriture();
                }
            }

            Map<SQLRowAccessor, BigDecimal> tvaMap = calc.getMapHtTVA();
            for (SQLRowAccessor rowAc : tvaMap.keySet()) {
                long longValue = tvaMap.get(rowAc).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
                if (longValue != 0) {
                    this.mEcritures.put("ID_COMPTE_PCE", rowAc.getID());
                    this.mEcritures.put("DEBIT", Long.valueOf(0));
                    this.mEcritures.put("CREDIT", longValue);
                    ajoutEcriture();
                }
            }

            // compte Clients

            if (idCompteClient <= 1) {
                idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                if (idCompteClient <= 1) {
                    idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
            if (ecrTable.contains("CODE_CLIENT")) {
                this.mEcritures.put("CODE_CLIENT", clientRow.getString("CODE"));
            }
            this.mEcritures.put("DEBIT", ttcLongValue);
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            ajoutEcriture();

            // TODO Gestion des factures d'acomptes
            // Solde des acomptes
            // List<SQLRow> rowsAcompte =
            // saisieRow.getReferentRows(saisieVFTable.getField("ID_SAISIE_VENTE_FACTURE_ACOMPTE"));
            // if (rowsAcompte != null && rowsAcompte.size() > 0) {
            // // Compte acompte
            // int idCompteAcompteClient = ComptePCESQLElement.getId("4191",
            // "Clients - Avances et acomptes reçus sur commandes");
            // int idTVAAcompte = ComptePCESQLElement.getId("44587",
            // "Taxes sur le chiffre d'affaire à régulariser ou en attente");
            // for (SQLRow sqlRow : rowsAcompte) {
            // long acompteTTC = sqlRow.getLong("T_TTC");
            // long acompteHT = sqlRow.getLong("T_HT");
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteAcompteClient));
            // this.mEcritures.put("DEBIT", acompteTTC);
            // this.mEcritures.put("CREDIT", Long.valueOf(0));
            // ajoutEcriture();
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
            // this.mEcritures.put("DEBIT", Long.valueOf(0));
            // this.mEcritures.put("CREDIT", acompteTTC);
            // ajoutEcriture();
            //
            // montantAcompteTTC += acompteTTC;
            //
            // long tva = acompteTTC - acompteHT;
            // if (tva > 0) {
            //
            //
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idTVAAcompte));
            // this.mEcritures.put("DEBIT", Long.valueOf(0));
            // this.mEcritures.put("CREDIT", Long.valueOf(tva));
            // ajoutEcriture();
            //
            // Map<Integer, Long> m = getMultiTVAFromRow(saisieRow,
            // saisieVFTable.getTable("SAISIE_VENTE_FACTURE_ELEMENT"), true);
            // long allTaxe = 0;
            // for (Integer i : m.keySet()) {
            // Long l = m.get(i);
            // if (l != null && l > 0) {
            // // FIXME
            // int idCpt = i;
            // if (idCpt <= 1) {
            // idCpt = idCompteTVA;
            // }
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCpt));
            // this.mEcritures.put("DEBIT", Long.valueOf(0));
            // this.mEcritures.put("CREDIT", Long.valueOf(l));
            // ajoutEcriture();
            // allTaxe += l;
            // }
            // }
            // if (allTaxe < prixTVA.getLongValue()) {
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
            // this.mEcritures.put("DEBIT", Long.valueOf(0));
            // this.mEcritures.put("CREDIT", Long.valueOf(prixTVA.getLongValue() - allTaxe));
            // ajoutEcriture();
            // }
            //
            // this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(compteDebitTvaAcompte));
            // this.mEcritures.put("DEBIT", Long.valueOf(tva));
            // this.mEcritures.put("CREDIT", Long.valueOf(0));
            // ajoutEcriture();
            // }
            // }
            // }
        }

        {
            SQLRowValues valSasieVF = new SQLRowValues(GenerationMvtSaisieVenteFacture.saisieVFTable);
            valSasieVF.put("DATE_REGLEMENT", null);
            valSasieVF.update(this.idSaisieVenteFacture);
        }

            // Génération du reglement
            SQLRow modeRegl = saisieRow.getForeignRow("ID_MODE_REGLEMENT");
            final SQLRow typeRegRow = modeRegl.getForeignRow("ID_TYPE_REGLEMENT");
            String label = this.nom + " (" + typeRegRow.getString("NOM") + ")";
            int idAvoir = saisieRow.getInt("ID_AVOIR_CLIENT");
            if (idAvoir > 1) {
                // SQLRow avoirRow = base.getTable("AVOIR_CLIENT").getRow(idAvoir);
                long l = ((Number) saisieRow.getObject("T_AVOIR_TTC")).longValue();
                prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue() - l);
            }
            prixTTC = new PrixTTC(prixTTC.getLongValue() - montantAcompteTTC);
            if (prixTTC.getLongValue() > 0) {
                new GenerationReglementVenteNG(label, clientRow, prixTTC, this.date, modeRegl, saisieRow, mvtTable.getRow(idMvt));
            }
        // Mise à jour de mouvement associé à la facture

        SQLRowValues valSasieVF = new SQLRowValues(GenerationMvtSaisieVenteFacture.saisieVFTable);
        valSasieVF.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        if (valSasieVF.getInvalid() == null) {
            valSasieVF.update(this.idSaisieVenteFacture);
            displayMvtNumber();
        }

    }

    public void run() {
        try {
            genereMouvement();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);

        }
    }
}
