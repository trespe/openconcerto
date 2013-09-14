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

import java.util.Date;

public class GenerationMvtReglementChequeFourn extends GenerationEcritures {

    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableCheque = base.getTable("CHEQUE_FOURNISSEUR");
    private static final SQLTable tableFourn = base.getTable("FOURNISSEUR");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtReglementChequeFourn(int idMvt, long montant, int idCheque, Date d) throws Exception {

        this.idMvt = idMvt;

        System.out.println("génération des ecritures de règlement d'un cheque fournisseur");
        SQLRow rowCheque = tableCheque.getRow(idCheque);

        this.date = d;
        SQLRow rowFournisseur = tableFourn.getRow(rowCheque.getInt("ID_FOURNISSEUR"));
        this.nom = "Reglement cheque " + rowFournisseur.getString("NOM");
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));

        // compte Fournisseurs
        int idCompteFourn = rowFournisseur.getInt("ID_COMPTE_PCE");

        if (idCompteFourn <= 1) {
            idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
            if (idCompteFourn <= 1) {

                idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");

            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteFourn));
        this.mEcritures.put("DEBIT", new Long(montant));
        this.mEcritures.put("CREDIT", new Long(0));
        ajoutEcriture();

        int idPce = base.getTable("TYPE_REGLEMENT").getRow(2).getInt("ID_COMPTE_PCE_FOURN");
        if (idPce <= 1) {

            idPce = ComptePCESQLElement.getIdComptePceDefault("AchatCheque");

        }
        // compte de reglement cheque, ...
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idPce));
        this.mEcritures.put("DEBIT", new Long(0));
        this.mEcritures.put("CREDIT", new Long(montant));
        ajoutEcriture();

    }
}
