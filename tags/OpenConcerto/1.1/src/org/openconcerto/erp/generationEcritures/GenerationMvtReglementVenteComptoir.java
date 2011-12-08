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
import org.openconcerto.erp.core.finance.payment.element.ChequeAEncaisserSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.Date;


public class GenerationMvtReglementVenteComptoir extends GenerationEcritures implements Runnable {
    private int idSaisieVenteComptoir;

    // Journal Caisse
    private static final Integer journalCaisse = new Integer(JournalSQLElement.CAISSES);
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private int idPere = 1;

    public void run() {

        System.out.println("génération des ecritures de règlement vente comptoir");

        SQLRow saisieRow = base.getTable("SAISIE_VENTE_COMPTOIR").getRow(this.idSaisieVenteComptoir);
        SQLRow clientRow = base.getTable("CLIENT").getRow(saisieRow.getInt("ID_CLIENT"));
        SQLRow modeRegRow = base.getTable("MODE_REGLEMENT").getRow(saisieRow.getInt("ID_MODE_REGLEMENT"));
        SQLRow typeRegRow = base.getTable("TYPE_REGLEMENT").getRow(modeRegRow.getInt("ID_TYPE_REGLEMENT"));

        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());
        this.date = (Date) saisieRow.getObject("DATE");
        String string = "Vente comptoir ";
        final String rowLib = saisieRow.getObject("NOM").toString();
        if (rowLib != null && rowLib.trim().length() > 0) {
            string += rowLib.trim();
        } else {
            string += saisieRow.getForeignRow("ID_ARTICLE").getString("NOM");
        }
        this.nom = string + " (" + typeRegRow.getString("NOM") + ")";

        // si paiement comptant
        if ((modeRegRow.getInt("AJOURS") == 0) && (modeRegRow.getInt("LENJOUR") == 0)) {

            // test Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque à encaisser sans date minimum d'encaissement
                paiementCheque(this.date);
            } else {

                // si on paye comptant alors l'ensemble ne forme qu'un seul mouvement
                this.idMvt = idPere;

                // iniatilisation des valeurs de la map pour les ecritures
                this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
                this.mEcritures.put("NOM", this.nom);
                this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));

                if (typeRegRow.getID() == 4) {
                    this.mEcritures.put("ID_JOURNAL", GenerationMvtReglementVenteComptoir.journalCaisse);
                } else {
                    this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
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
                try {
                    this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteClient));
                    this.mEcritures.put("DEBIT", new Long(0));
                    this.mEcritures.put("CREDIT", new Long(prixTTC.getLongValue()));
                    ajoutEcriture();

                    // compte de reglement
                    int idCompteRegl = typeRegRow.getInt("ID_COMPTE_PCE_CLIENT");
                    if (idCompteRegl <= 1) {
                        try {
                            idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("VenteCB");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteRegl));
                    this.mEcritures.put("DEBIT", new Long(prixTTC.getLongValue()));
                    this.mEcritures.put("CREDIT", new Long(0));
                    ajoutEcriture();
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
                    e.printStackTrace();
                }
            }
        } else {

            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), this.date);

            // Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque à encaisser avec date minimum d'encaissement
                paiementCheque(dateEch);
            } else {

                System.err.println("Echéance client");

                SQLTable tableEchCli = base.getTable("ECHEANCE_CLIENT");
                SQLRowValues valEcheance = new SQLRowValues(tableEchCli);

                // Ajout dans echeance

                SQLRow rowMvtPere = tableMouvement.getRow(idPere);
                this.idMvt = getNewMouvement("ECHEANCE_CLIENT", 1, idPere, rowMvtPere.getInt("ID_PIECE"));

                valEcheance.put("ID_MOUVEMENT", new Integer(this.idMvt));
                valEcheance.put("DATE", new java.sql.Date(dateEch.getTime()));
                valEcheance.put("MONTANT", new Long(prixTTC.getLongValue()));
                valEcheance.put("ID_CLIENT", new Integer(saisieRow.getInt("ID_CLIENT")));

                try {
                    if (valEcheance.getInvalid() == null) {

                        // ajout de l'ecriture
                        SQLRow row = valEcheance.insert();
                        SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                        rowVals.put("IDSOURCE", row.getID());
                        rowVals.update(this.idMvt);
                    }
                } catch (SQLException e) {
                    System.err.println("Error insert in Table " + valEcheance.getTable().getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reglement par cheque
     * 
     * @param dateEch date d'echeance d'encaissement du cheque
     */
    private void paiementCheque(Date dateEch) {
        SQLRow saisieRow = base.getTable("SAISIE_VENTE_COMPTOIR").getRow(this.idSaisieVenteComptoir);
        PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());

        ChequeAEncaisserSQLElement chqAEncaisserElt = (ChequeAEncaisserSQLElement) Configuration.getInstance().getDirectory().getElement("CHEQUE_A_ENCAISSER");
        SQLRowValues valEncaisse = new SQLRowValues(chqAEncaisserElt.getTable());

        valEncaisse.put("ID_CLIENT", new Integer(saisieRow.getInt("ID_CLIENT")));
        valEncaisse.put("DATE_VENTE", new java.sql.Date(this.date.getTime()));
        valEncaisse.put("DATE_MIN_DEPOT", new java.sql.Date(dateEch.getTime()));
        valEncaisse.put("DATE_DEPOT", new java.sql.Date(this.date.getTime()));
        valEncaisse.put("MONTANT", new Long(prixTTC.getLongValue()));
        // on crée un nouveau mouvement pour l'encaissement futur du cheque
        SQLRow rowMvtPere = tableMouvement.getRow(idPere);
        this.idMvt = getNewMouvement("CHEQUE_A_ENCAISSER", 1, idPere, rowMvtPere.getInt("ID_PIECE"));

        valEncaisse.put("ID_MOUVEMENT", new Integer(this.idMvt));

        try {
            if (valEncaisse.getInvalid() == null) {

                // ajout de l'ecriture
                SQLRow row = valEncaisse.insert();
                SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                rowVals.put("IDSOURCE", row.getID());
                rowVals.update(this.idMvt);
            }
        } catch (SQLException e) {
            System.err.println("Error insert in Table " + valEncaisse.getTable().getName());
            e.printStackTrace();
        }
    }

    public GenerationMvtReglementVenteComptoir(int idSaisieVenteComptoir, int idMvt) {

        // SQLRow rowMvtPere = tableMouvement.getRow(idMvt);
        this.idPere = idMvt;
        // this.idMvt = getNewMouvement("SAISIE_VENTE_COMPTOIR", idSaisieVenteComptoir, idMvt,
        // rowMvtPere.getInt("ID_PIECE"));

        this.idSaisieVenteComptoir = idSaisieVenteComptoir;

        new Thread(GenerationMvtReglementVenteComptoir.this).start();
    }
}
