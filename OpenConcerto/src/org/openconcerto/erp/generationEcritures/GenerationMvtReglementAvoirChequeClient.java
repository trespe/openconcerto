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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.util.Date;


public class GenerationMvtReglementAvoirChequeClient extends GenerationEcritures {

    private long montant;

    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    // private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
    private int idCheque;

    public void genere() {

        System.err.println("generation du reglement par cheque");
        SQLRow chequeAvoirRow = base.getTable("CHEQUE_AVOIR_CLIENT").getRow(this.idCheque);
        SQLRow clientRow = base.getTable("CLIENT").getRow(chequeAvoirRow.getInt("ID_CLIENT"));

        // iniatilisation des valeurs de la map
        // this.date = new Date();
        this.nom = "Reglement avoir client par chéque";
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
        SQLRow rowBanque = chequeAvoirRow.getForeignRow("ID_BANQUE_POLE_PRODUIT");
        if (rowBanque != null && rowBanque.getID() > 1) {
            SQLRow rowJournal = rowBanque.getForeignRow("ID_JOURNAL");
            if (rowJournal != null && rowJournal.getID() > 1) {
                this.mEcritures.put("ID_JOURNAL", rowJournal.getID());
            }
        }
        this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));

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

        try {
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteClient));
            this.mEcritures.put("DEBIT", new Long(this.montant));
            this.mEcritures.put("CREDIT", new Long(0));
            ajoutEcriture();

            // compte de reglement cheque, ...
            int idPce = base.getTable("TYPE_REGLEMENT").getRow(2).getInt("ID_COMPTE_PCE_CLIENT");
            if (idPce <= 1) {
                try {
                    idPce = ComptePCESQLElement.getIdComptePceDefault("VenteCheque");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idPce));
            this.mEcritures.put("DEBIT", new Long(0));
            this.mEcritures.put("CREDIT", new Long(this.montant));
            ajoutEcriture();

        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }

    public GenerationMvtReglementAvoirChequeClient(int idMvt, long montant, Date d, int idCheque) {

        // SQLRow rowMvtPere = tableMouvement.getRow(idMvt);
        // this.idMvt = getNewMouvement("CHEQUE_A_ENCAISSER", idCheque, idMvt,
        // rowMvtPere.getInt("ID_PIECE"));
        this.montant = montant;
        this.date = d;
        this.idMvt = idMvt;
        this.idCheque = idCheque;
    }
}
