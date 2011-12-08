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


public class GenerationMvtSaisieAchat extends GenerationEcritures implements Runnable {

    private int idSaisieAchat;
    private static final String source = "SAISIE_ACHAT";
    private static final Integer journal = new Integer(JournalSQLElement.ACHATS);
    private static final SQLTable tableSaisieAchat = base.getTable("SAISIE_ACHAT");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableFournisseur = base.getTable("FOURNISSEUR");
    private static final SQLTable tableMvt = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtSaisieAchat(int idSaisieAchat, int idMvt) {
        this.idSaisieAchat = idSaisieAchat;
        this.idMvt = idMvt;
        (new Thread(GenerationMvtSaisieAchat.this)).start();
    }

    public GenerationMvtSaisieAchat(int idSaisieAchat) {

        this.idSaisieAchat = idSaisieAchat;
        this.idMvt = 1;
        (new Thread(GenerationMvtSaisieAchat.this)).start();
    }

    public void genereMouvement() throws IllegalArgumentException {

        SQLRow saisieRow = tableSaisieAchat.getRow(this.idSaisieAchat);
        // SQLRow taxeRow = base.getTable("TAXE").getRow(saisieRow.getInt("ID_TAXE"));

        SQLRow rowFournisseur = tableFournisseur.getRow(saisieRow.getInt("ID_FOURNISSEUR"));

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        this.nom = "Achat : " + rowFournisseur.getString("NOM") + " Facture : " + saisieRow.getObject("NUMERO_FACTURE").toString() + " " + saisieRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieAchat.journal);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(1));

        // Calcul des montants
        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());
        PrixHT prixTVA = new PrixHT(((Long) saisieRow.getObject("MONTANT_TVA")).longValue());
        PrixHT prixHT = new PrixHT(((Long) saisieRow.getObject("MONTANT_HT")).longValue());

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            getNewMouvement(GenerationMvtSaisieAchat.source, this.idSaisieAchat, 1, " Saisie Achat " + saisieRow.getObject("NUMERO_FACTURE").toString());
        } else {
            SQLRow rowMvt = tableMvt.getRow(this.idMvt);
            SQLRowValues rowPiece = rowMvt.getForeignRow("ID_PIECE").createEmptyUpdateRow();
            rowPiece.put("NOM", " Saisie Achat " + saisieRow.getObject("NUMERO_FACTURE").toString());
            try {
                rowPiece.update();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        // compte Achat
        int idCompteAchat = saisieRow.getInt("ID_COMPTE_PCE");

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
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteAchat));
        this.mEcritures.put("DEBIT", new Long(prixHT.getLongValue()));
        this.mEcritures.put("CREDIT", new Long(0));
        ajoutEcriture();

        // compte TVA
        if (prixTVA.getLongValue() > 0) {
            int idCompteTVA;
            if (saisieRow.getBoolean("IMMO")) {
                idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_IMMO");
                if (idCompteTVA <= 1) {
                    try {
                        idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVAImmo");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                idCompteTVA = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_ACHAT");
                if (idCompteTVA <= 1) {
                    try {
                        idCompteTVA = ComptePCESQLElement.getIdComptePceDefault("TVADeductible");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVA));
            this.mEcritures.put("DEBIT", new Long(prixTVA.getLongValue()));
            this.mEcritures.put("CREDIT", new Long(0));
            ajoutEcriture();

            if (rowFournisseur.getBoolean("UE")) {
                int idCompteTVAIntra = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_INTRA");
                if (idCompteTVAIntra <= 1) {
                    try {
                        idCompteTVAIntra = ComptePCESQLElement.getIdComptePceDefault("TVAIntraComm");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVAIntra));
                this.mEcritures.put("DEBIT", new Long(0));
                this.mEcritures.put("CREDIT", new Long(prixTVA.getLongValue()));
                ajoutEcriture();
            }
        }

        // compte Fournisseurs
        int idCompteFourn = rowFournisseur.getInt("ID_COMPTE_PCE");

        if (idCompteFourn <= 1) {
            idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
            if (idCompteFourn <= 1) {
                try {
                    idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteFourn));
        this.mEcritures.put("DEBIT", new Long(0));
        if (rowFournisseur.getBoolean("UE")) {
            this.mEcritures.put("CREDIT", new Long(prixHT.getLongValue()));
        } else {
            this.mEcritures.put("CREDIT", new Long(prixTTC.getLongValue()));
        }
        ajoutEcriture();

        new GenerationMvtReglementAchat(this.idSaisieAchat, this.idMvt);

        // Mise à jour de la clef etrangere mouvement sur la saisie achat
        SQLRowValues valEcriture = new SQLRowValues(tableSaisieAchat);
        valEcriture.put("ID_MOUVEMENT", new Integer(this.idMvt));
        try {
            if (valEcriture.getInvalid() == null) {
                // ajout de l'ecriture
                valEcriture.update(this.idSaisieAchat);
            }
        } catch (SQLException e) {
            System.err.println("Erreur à l'insertion dans la table " + tableSaisieAchat.getName() + " : " + e);
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            genereMouvement();
        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }
}
