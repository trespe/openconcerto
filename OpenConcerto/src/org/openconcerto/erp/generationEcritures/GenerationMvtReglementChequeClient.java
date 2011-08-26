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
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;


public class GenerationMvtReglementChequeClient extends GenerationEcritures {

    private long montant;

    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
    private int idCheque;

    public void genere() {

        System.err.println("génération des ecritures de règlement d'un cheque client du mouvement " + this.idMvt);
        SQLRow chequeRow = base.getTable("CHEQUE_A_ENCAISSER").getRow(this.idCheque);
        SQLRow clientRow = base.getTable("CLIENT").getRow(chequeRow.getInt("ID_CLIENT"));

        // iniatilisation des valeurs de la map
        // this.date = new Date();

        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));

        setDateReglement(this.idCheque, this.date);

        // compte Clients
        int idCompteClient = -1;
        if (clientRow != null) {
            idCompteClient = clientRow.getInt("ID_COMPTE_PCE");
        }
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
            int idPce = base.getTable("TYPE_REGLEMENT").getRow(2).getInt("ID_COMPTE_PCE_CLIENT");
            if (idPce <= 1) {
                try {
                    idPce = ComptePCESQLElement.getIdComptePceDefault("VenteCheque");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteClient));
            this.mEcritures.put("DEBIT", new Long(0));
            this.mEcritures.put("CREDIT", new Long(this.montant));
            ajoutEcriture();
            System.err.println("First ECriture for mvt " + this.idMvt);

            // compte de reglement cheque, ...
            this.mEcritures.put("ID_COMPTE_PCE", new Integer(idPce));
            this.mEcritures.put("DEBIT", new Long(this.montant));
            this.mEcritures.put("CREDIT", new Long(0));
            ajoutEcriture();
            System.err.println("Ecritures générées pour le mouvement " + this.idMvt);
        } catch (IllegalArgumentException e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }

    private void setDateReglement(int idCheque, Date d) {

        if (idCheque > 1) {

            SQLRow chequeRow = Configuration.getInstance().getBase().getTable("CHEQUE_A_ENCAISSER").getRow(idCheque);

            final int sourceId = MouvementSQLElement.getSourceId(chequeRow.getInt("ID_MOUVEMENT"));
            SQLRow rowMvt = Configuration.getInstance().getBase().getTable("MOUVEMENT").getRow(sourceId);

            if (rowMvt.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                SQLElement eltFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                SQLRow saisieRow = eltFacture.getTable().getRow(rowMvt.getInt("IDSOURCE"));
                // On fixe la date du paiement
                SQLRowValues rowValsUpdateVF = saisieRow.createEmptyUpdateRow();
                rowValsUpdateVF.put("DATE_REGLEMENT", new Timestamp(d.getTime()));
                try {
                    rowValsUpdateVF.update();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

        }

    }

    public GenerationMvtReglementChequeClient(int idMvt, long montant, Date d, int idCheque, String s) {

        this.montant = montant;
        this.date = d;
        this.idMvt = idMvt;
        this.idCheque = idCheque;
        if (s != null && s.trim().length() > 0) {
            this.nom = s;
        } else {
            this.nom = "Reglement cheque client";
        }
    }
}
