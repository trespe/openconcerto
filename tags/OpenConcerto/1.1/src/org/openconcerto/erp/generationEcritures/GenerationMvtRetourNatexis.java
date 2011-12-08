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
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;
import java.util.Date;


public class GenerationMvtRetourNatexis extends GenerationEcritures {

    private static final SQLTable mvtTable = base.getTable("MOUVEMENT");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtRetourNatexis(SQLRow rowFacture) {

        if (rowFacture.getBoolean("AFFACTURAGE") && !rowFacture.getBoolean("RETOUR_NATEXIS")) {

            SQLRowAccessor rowClient = rowFacture.getForeign("ID_CLIENT");
            SQLRowAccessor modeRegl = rowFacture.getForeign("ID_MODE_REGLEMENT");
            SQLRowAccessor mvtSource = rowFacture.getForeign("ID_MOUVEMENT");
            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegl.getInt("AJOURS"), modeRegl.getInt("LENJOUR"), rowFacture.getDate("DATE").getTime());

            System.out.println("Echéance client");

            // Ajout dans echeance
            SQLRowValues valEcheance = new SQLRowValues(base.getTable("ECHEANCE_CLIENT"));

            this.idMvt = getNewMouvement("ECHEANCE_CLIENT", 1, mvtSource.getID(), mvtSource.getInt("ID_PIECE"));
            valEcheance.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
            valEcheance.put("DATE", dateEch);
            valEcheance.put("MONTANT", rowFacture.getObject("T_TTC"));
            valEcheance.put("ID_CLIENT", rowClient.getID());
            valEcheance.put("RETOUR_NATEXIS", Boolean.TRUE);

            try {

                // ajout de l'ecriture
                SQLRow row = valEcheance.insert();
                SQLRowValues rowVals = new SQLRowValues(mvtTable);
                rowVals.put("IDSOURCE", row.getID());
                rowVals.update(this.idMvt);
            } catch (SQLException e) {
                System.err.println("Error insert in Table " + valEcheance.getTable().getName());
            }

            this.nom = "Retour natexis facture " + rowFacture.getObject("NUMERO").toString();
            this.date = new Date();
            this.mEcritures.put("DATE", this.date);
            this.mEcritures.put("NOM", this.nom);
           
            this.mEcritures.put("ID_JOURNAL", GenerationMvtSaisieVenteFacture.journal);

            int idJrnlFactor = rowPrefsCompte.getInt("ID_JOURNAL_FACTOR");
            if (idJrnlFactor > 1) {
                this.mEcritures.put("ID_JOURNAL", idJrnlFactor);
            }

            this.mEcritures.put("ID_MOUVEMENT", this.idMvt);

            // compte Factor
            int idComptefactor = rowPrefsCompte.getInt("ID_COMPTE_PCE_FACTOR");
            if (idComptefactor <= 1) {
                idComptefactor = rowPrefsCompte.getInt("ID_COMPTE_PCE_FACTOR");
                if (idComptefactor <= 1) {
                    try {
                        idComptefactor = ComptePCESQLElement.getIdComptePceDefault("Factor");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            this.mEcritures.put("ID_COMPTE_PCE", idComptefactor);
            this.mEcritures.put("CREDIT", rowFacture.getObject("T_TTC"));
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            ajoutEcriture();

            // compte Clients
            int idCompteClient = rowClient.getInt("ID_COMPTE_PCE");
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
            this.mEcritures.put("DEBIT", rowFacture.getObject("T_TTC"));
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            ajoutEcriture();

            SQLRowValues rowValsFacture = rowFacture.asRowValues();
            rowValsFacture.put("RETOUR_NATEXIS", Boolean.TRUE);
            try {
                rowValsFacture.update();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
