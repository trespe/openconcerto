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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;

import java.sql.SQLException;
import java.util.Date;

public final class GenerationMvtAcompte extends GenerationEcritures implements Runnable {

    private int idSalarie;
    private long montant;
    private int idAcompte;

    private static final SQLTable tableSalarie = base.getTable("SALARIE");
    private static final SQLTable tableAcompte = base.getTable("ACOMPTE");
    // Journal OD
    private static final Integer journalOD = Integer.valueOf(JournalSQLElement.OD);
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtAcompte(int idAcompte) throws SQLException {
        this.idAcompte = idAcompte;
        SQLRow rowAcompte = tableAcompte.getRow(idAcompte);
        this.idSalarie = rowAcompte.getInt("ID_SALARIE");
        this.montant = GestionDevise.parseLongCurrency(String.valueOf(rowAcompte.getFloat("MONTANT")));
        SQLRow rowSal = tableSalarie.getRow(this.idSalarie);
        this.idMvt = getNewMouvement("ACOMPTE", this.idAcompte, 1, "Acompte " + rowSal.getString("NOM"));
        new Thread(GenerationMvtAcompte.this).start();
    }

    private void genereComptaAcompte() throws Exception {

        System.out.println("Génération des ecritures du mouvement " + this.idMvt);

        SQLRow rowSal = tableSalarie.getRow(this.idSalarie);
        // iniatilisation des valeurs de la map
        this.date = new Date();
        // TODO recuperer le mois et l'année
        this.nom = "Acompte " + rowSal.getString("NOM");
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", journalOD);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        // Acompte
        int idCompteAcompte = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACOMPTE");
        if (idCompteAcompte <= 1) {
            idCompteAcompte = ComptePCESQLElement.getIdComptePceDefault("PayeAcompte");
        }

        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteAcompte));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("DEBIT", Long.valueOf(this.montant));
        this.mEcritures.put("CREDIT", Long.valueOf(0));

        ajoutEcriture();

        // Trésorie
        int idCompteTresor = rowPrefsCompte.getInt("ID_COMPTE_PCE_ACOMPTE_REGL");
        if (idCompteTresor <= 1) {
            try {
                idCompteTresor = ComptePCESQLElement.getIdComptePceDefault("PayeReglementAcompte");
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteTresor));
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("DEBIT", Long.valueOf(0));
        this.mEcritures.put("CREDIT", Long.valueOf(this.montant));

        ajoutEcriture();

        // Replace mvt
        SQLRowValues rowVals = new SQLRowValues(tableAcompte);
        rowVals.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        rowVals.update(this.idAcompte);

    }

    public void run() {

        try {
            genereComptaAcompte();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur lors de la génération des mouvements", e);
        }
    }
}
