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

import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

// FIXME probleme lors de certaines generation tout reste figer

/**
 * Génération des ecritures associées à une saisie de vente avec facture. Entaine la génération du
 * reglement de la vente
 */
public class GenerationMvtSaisieVenteFacture extends GenerationEcritures implements Runnable {

    private static final String source = "SAISIE_VENTE_FACTURE";
    public static final Integer journal = Integer.valueOf(JournalSQLElement.VENTES);
    private int idSaisieVenteFacture;
    private static final SQLTable saisieVFTable = base.getTable("SAISIE_VENTE_FACTURE");
    private static final SQLTable taxeTable = base.getTable("TAXE");
    private static final SQLTable mvtTable = base.getTable("MOUVEMENT");
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

        this.idMvt = 1;
        this.idSaisieVenteFacture = idSaisieVenteFacture;
        new Thread(GenerationMvtSaisieVenteFacture.this).start();
    }

    private void genereMouvement() throws IllegalArgumentException {

        SQLRow saisieRow = GenerationMvtSaisieVenteFacture.saisieVFTable.getRow(this.idSaisieVenteFacture);
        SQLRow clientRow = saisieRow.getForeignRow("ID_CLIENT");

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue());
        PrixHT prixTVA = new PrixHT(((Long) saisieRow.getObject("T_TVA")).longValue());
        PrixHT prixHT = new PrixHT(((Long) saisieRow.getObject("T_HT")).longValue());
        PrixHT prixService = new PrixHT(((Long) saisieRow.getObject("T_SERVICE")).longValue());

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        this.nom = "Saisie Vente facture " + saisieRow.getObject("NUMERO").toString();
        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieVenteFacture.journal);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            getNewMouvement(GenerationMvtSaisieVenteFacture.source, this.idSaisieVenteFacture, 1, "Saisie vente facture " + saisieRow.getObject("NUMERO").toString());
        } else {
            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe
        int idCompteVenteService = saisieRow.getInt("ID_COMPTE_PCE_SERVICE");
        if (idCompteVenteService <= 1) {
            idCompteVenteService = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_SERVICE");
            if (idCompteVenteService <= 1) {
                try {
                    idCompteVenteService = ComptePCESQLElement.getIdComptePceDefault("VentesServices");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // compte Vente Produits
        final long produitHT = prixHT.getLongValue() - prixService.getLongValue();
        if (produitHT >= 0) {

            if (produitHT > 0) {

                int idCompteVenteProduit = saisieRow.getInt("ID_COMPTE_PCE_VENTE");
                if (idCompteVenteProduit <= 1) {
                    idCompteVenteProduit = rowPrefsCompte.getInt("ID_COMPTE_PCE_VENTE_PRODUIT");
                    if (idCompteVenteProduit <= 1) {
                        try {
                            idCompteVenteProduit = ComptePCESQLElement.getIdComptePceDefault("VentesProduits");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteProduit));
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(produitHT));
                int idEcr = ajoutEcriture();
            }

            // si on a des frais de service
            if (prixService.getLongValue() > 0) {
                // compte Vente Services

                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteService));
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(prixService.getLongValue()));
                int idEcr = ajoutEcriture();

            }
        } else// la remise déborde sur les frais de service donc aucun frais pour les produits
        {
            // compte Vente Services
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteVenteService));
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", Long.valueOf(prixHT.getLongValue()));
            int idEcr = ajoutEcriture();

        }

        // compte TVA
        if (prixTVA.getLongValue() > 0) {
            int idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_VENTE");
            if (idCompteTVA <= 1) {
                try {
                    idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVACollectee");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Map<Integer, Long> m = getMultiTVAFromRow(saisieRow, saisieVFTable.getTable("SAISIE_VENTE_FACTURE_ELEMENT"), true);
            long allTaxe = 0;
            for (Integer i : m.keySet()) {
                Long l = m.get(i);
                if (l != null && l > 0) {
                    // FIXME
                    int idCpt = i;
                    if (idCpt <= 1) {
                        idCpt = idCompteTVA;
                    }
                    this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCpt));
                    this.mEcritures.put("DEBIT", Long.valueOf(0));
                    this.mEcritures.put("CREDIT", Long.valueOf(l));
                    ajoutEcriture();
                    allTaxe += l;
                }
            }
            if (allTaxe < prixTVA.getLongValue()) {
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(prixTVA.getLongValue() - allTaxe));
                ajoutEcriture();
            }

        }
        // compte Clients
        int idCompteClient = clientRow.getInt("ID_COMPTE_PCE");
        if (idCompteClient <= 1) {
            idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
            if (idCompteClient <= 1) {
                try {
                    idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
        this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
        this.mEcritures.put("CREDIT", Long.valueOf(0));
        ajoutEcriture();

            // Génération du reglement
            SQLRow modeRegl = saisieRow.getForeignRow("ID_MODE_REGLEMENT");
            final SQLRow typeRegRow = modeRegl.getForeignRow("ID_TYPE_REGLEMENT");
            String label = "Saisie Vente facture " + saisieRow.getObject("NUMERO").toString() + " (" + typeRegRow.getString("NOM") + ")";
            int idAvoir = saisieRow.getInt("ID_AVOIR_CLIENT");
            if (idAvoir > 1) {
                // SQLRow avoirRow = base.getTable("AVOIR_CLIENT").getRow(idAvoir);
                long l = ((Number) saisieRow.getObject("T_AVOIR_TTC")).longValue();
                prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue() - l);
            }
            if (prixTTC.getLongValue() > 0) {
                new GenerationReglementVenteNG(label, clientRow, prixTTC, this.date, modeRegl, saisieRow, mvtTable.getRow(idMvt));
            }
        // Mise à jour de mouvement associé à la facture

        SQLRowValues valSasieVF = new SQLRowValues(GenerationMvtSaisieVenteFacture.saisieVFTable);
        valSasieVF.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        try {
            if (valSasieVF.getInvalid() == null) {

                valSasieVF.update(this.idSaisieVenteFacture);
            }
        } catch (SQLException e) {
            System.err.println("Erreur à l'insertion dans la table " + valSasieVF.getTable().getName() + " : " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.err.println("****Start genere Mouvement");
            genereMouvement();
            System.err.println("****End genere Mouvement");
        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }
}
