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

import java.util.Date;

public class GenerationMvtAvoirFournisseur extends GenerationEcritures {

    private static final String source = "AVOIR_FOURNISSEUR";
    // TODO dans quel journal les ecritures des avoirs? OD?
    private static final Integer journal = Integer.valueOf(JournalSQLElement.ACHATS);
    private int idAvoirFourn;
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtAvoirFournisseur(int idAvoirFourn) {
        this.idMvt = 1;
        this.idAvoirFourn = idAvoirFourn;
    }

    public GenerationMvtAvoirFournisseur(int idAvoirFourn, int idMvt) {
        this.idMvt = idMvt;
        this.idAvoirFourn = idAvoirFourn;
    }

    public int genereMouvement() throws Exception {

        SQLTable avoirFournTable = base.getTable("AVOIR_FOURNISSEUR");
        SQLTable fournTable = base.getTable("FOURNISSEUR");

        SQLRow avoirRow = avoirFournTable.getRow(this.idAvoirFourn);
        SQLRow rowFourn = fournTable.getRow(avoirRow.getInt("ID_FOURNISSEUR"));

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) avoirRow.getObject("MONTANT_TTC")).longValue());
        PrixHT prixHT = new PrixHT(((Long) avoirRow.getObject("MONTANT_HT")).longValue());
        PrixHT prixTVA = new PrixHT(((Long) avoirRow.getObject("MONTANT_TVA")).longValue());

        // iniatilisation des valeurs de la map
        this.date = (Date) avoirRow.getObject("DATE");
        this.nom = avoirRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", "Avoir fournisseur : " + avoirRow.getString("NUMERO") + " " + rowFourn.getString("NOM"));
        this.mEcritures.put("ID_JOURNAL", GenerationMvtAvoirFournisseur.journal);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on cree un nouveau mouvement
        if (this.idMvt == 1) {
            getNewMouvement(GenerationMvtAvoirFournisseur.source, this.idAvoirFourn, 1, "Avoir Fournisseur : " + avoirRow.getString("NUMERO"));
        } else {
            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        // compte Achat
        int idCompteAchat = avoirRow.getInt("ID_COMPTE_PCE");

        if (idCompteAchat <= 1) {
            idCompteAchat = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACHAT");
            if (idCompteAchat <= 1) {
                try {
                    idCompteAchat = ComptePCESQLElement.getIdComptePceDefault("Achats");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteAchat));
        this.mEcritures.put("DEBIT", Long.valueOf(0));
        this.mEcritures.put("CREDIT", Long.valueOf(prixHT.getLongValue()));

        ajoutEcriture();

        if (prixTVA.getLongValue() > 0) {
            // compte TVA
            int idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_ACHAT");
            if (avoirRow.getBoolean("IMMO")) {
                idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_IMMO");
                if (idCompteTVA <= 1) {
                    idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVAImmo");
                }
            } else {
                idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_ACHAT");
                if (idCompteTVA <= 1) {
                    idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVADeductible");
                }
            }

            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVA));
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", Long.valueOf(prixTVA.getLongValue()));
            ajoutEcriture();

            if (rowFourn.getBoolean("UE")) {
                int idCompteTVAIntra = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_INTRA");
                if (idCompteTVAIntra <= 1) {
                    idCompteTVAIntra = ComptePCESQLElement.getIdComptePceDefault("TVAIntraComm");
                }
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTVAIntra));
                this.mEcritures.put("DEBIT", Long.valueOf(prixTVA.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();
            }
        }
        // compte Fournisseur
        int idCompteFourn = rowFourn.getInt("ID_COMPTE_PCE");
        if (idCompteFourn <= 1) {
            idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
            if (idCompteFourn <= 1) {
                idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteFourn));
        this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
        this.mEcritures.put("CREDIT", Long.valueOf(0));
        ajoutEcriture();

        // Mise à jour de mouvement associé à la facture d'avoir
        SQLRowValues valAvoir = new SQLRowValues(avoirFournTable);
        valAvoir.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        if (valAvoir.getInvalid() == null) {

            valAvoir.update(this.idAvoirFourn);
            displayMvtNumber();
        }

        // if (avoirRow.getInt("ID_MODE_REGLEMENT") > 1) {
        // new GenerationMvtReglementAvoir(this.idAvoirFourn, this.idMvt);
        // } else {
        // valAvoir.put("SOLDE", Boolean.FALSE);
        // try {
        // valAvoir.update(this.idAvoirClient);
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        // }

        return this.idMvt;
    }
}
