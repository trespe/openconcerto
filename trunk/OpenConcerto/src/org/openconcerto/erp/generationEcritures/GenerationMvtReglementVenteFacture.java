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
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;


public class GenerationMvtReglementVenteFacture extends GenerationEcritures implements Runnable {

    private int idSaisieVenteFacture;

    // Journal Caisse
    private static final Integer journalCaisse = new Integer(JournalSQLElement.CAISSES);

    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private int idPere = 1;

    public void run() {

        System.err.println("****génération des ecritures de règlement vente facture");

        SQLRow saisieRow = base.getTable("SAISIE_VENTE_FACTURE").getRow(this.idSaisieVenteFacture);
        SQLRow clientRow = base.getTable("CLIENT").getRow(saisieRow.getInt("ID_CLIENT"));
        SQLRow modeRegRow = base.getTable("MODE_REGLEMENT").getRow(saisieRow.getInt("ID_MODE_REGLEMENT"));
        SQLRow typeRegRow = base.getTable("TYPE_REGLEMENT").getRow(modeRegRow.getInt("ID_TYPE_REGLEMENT"));

        PrixTTC prixTTC;
        int idAvoir = saisieRow.getInt("ID_AVOIR_CLIENT");
        if (idAvoir > 1) {

            long l = ((Number) saisieRow.getObject("T_AVOIR_TTC")).longValue();
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue() - l);
        } else {
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue());
        }

        if (prixTTC.getLongValue() == 0) {
            return;
        }

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        this.nom = "Saisie Vente facture " + saisieRow.getObject("NUMERO").toString() + " (" + typeRegRow.getString("NOM") + ")";

        // si paiement comptant
        if ((modeRegRow.getInt("AJOURS") == 0) && (modeRegRow.getInt("LENJOUR") == 0)) {

            // test Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque à encaisser sans date minimum d'encaissement
                paiementCheque(this.date);
            } else {

                setDateReglement(saisieRow);

                if (typeRegRow.getID() == 4) {
                    this.mEcritures.put("ID_JOURNAL", GenerationMvtReglementVenteFacture.journalCaisse);
                } else {
                    this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
                    SQLRow rowBanque = modeRegRow.getForeignRow("ID_BANQUE_POLE_PRODUIT");
                    if (rowBanque != null && rowBanque.getID() > 1) {
                        SQLRow rowJournal = rowBanque.getForeignRow("ID_JOURNAL");
                        if (rowJournal != null && rowJournal.getID() > 1) {
                            this.mEcritures.put("ID_JOURNAL", rowJournal.getID());
                        }
                    }
                }

                this.idMvt = idPere;
                this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
                this.mEcritures.put("NOM", this.nom);
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
                    this.mEcritures.put("DEBIT", new Long(0));
                    this.mEcritures.put("CREDIT", new Long(prixTTC.getLongValue()));
                    ajoutEcriture();

                    // compte de reglement, caisse, cheque, ...
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

                paiementCheque(dateEch);
            } else {

                System.err.println("Echéance client");

                // Ajout dans echeance
                SQLRowValues valEcheance = new SQLRowValues(base.getTable("ECHEANCE_CLIENT"));

                valEcheance.put("DATE", dateEch);
                valEcheance.put("MONTANT", new Long(prixTTC.getLongValue()));
                valEcheance.put("ID_CLIENT", new Integer(saisieRow.getInt("ID_CLIENT")));

                SQLRow rowMvtPere = tableMouvement.getRow(idPere);
                this.idMvt = getNewMouvement("ECHEANCE_CLIENT", 1, idPere, rowMvtPere.getInt("ID_PIECE"));
                valEcheance.put("ID_MOUVEMENT", new Integer(this.idMvt));

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
        System.err.println("****End génération des ecritures de règlement vente facture");
    }

    private void setDateReglement(SQLRow saisieRow) {
        // On fixe la date du paiement
        SQLRowValues rowValsUpdateVF = saisieRow.createEmptyUpdateRow();
        rowValsUpdateVF.put("DATE_REGLEMENT", new Timestamp(this.date.getTime()));
        try {
            rowValsUpdateVF.update();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void paiementCheque(Date dateEch) {

        SQLRow saisieRow = base.getTable("SAISIE_VENTE_FACTURE").getRow(this.idSaisieVenteFacture);

        PrixTTC prixTTC;
        int idAvoir = saisieRow.getInt("ID_AVOIR_CLIENT");
        if (idAvoir > 1) {
            // SQLRow avoirRow = base.getTable("AVOIR_CLIENT").getRow(idAvoir);
            long l = ((Number) saisieRow.getObject("T_AVOIR_TTC")).longValue();
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue() - l);
        } else {
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("T_TTC")).longValue());
        }

        // Ajout dans cheque à encaisser avec date minimum d'encaissement
        SQLRowValues valEncaisse = new SQLRowValues(base.getTable("CHEQUE_A_ENCAISSER"));

        valEncaisse.put("ID_CLIENT", new Integer(saisieRow.getInt("ID_CLIENT")));
        valEncaisse.put("DATE_VENTE", new java.sql.Date(this.date.getTime()));
        valEncaisse.put("DATE_MIN_DEPOT", new java.sql.Date(dateEch.getTime()));
        SQLRow rowModeRegl = saisieRow.getForeignRow("ID_MODE_REGLEMENT");
        if (rowModeRegl != null && rowModeRegl.getID() > 1) {
            valEncaisse.put("ID_BANQUE_POLE_PRODUIT", rowModeRegl.getInt("ID_BANQUE_POLE_PRODUIT"));
        }

        SQLRow rowMvtPere = tableMouvement.getRow(idPere);
        this.idMvt = getNewMouvement("CHEQUE_A_ENCAISSER", 1, idPere, rowMvtPere.getInt("ID_PIECE"));
        valEncaisse.put("ID_MOUVEMENT", new Integer(this.idMvt));
        valEncaisse.put("MONTANT", new Long(prixTTC.getLongValue()));

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

    public GenerationMvtReglementVenteFacture(int idSaisieVenteFacture, int idMvt) {
        this.idSaisieVenteFacture = idSaisieVenteFacture;
        // SQLRow rowMvtPere = tableMouvement.getRow(idMvt);
        // this.idMvt = getNewMouvement("SAISIE_VENTE_FACTURE", idSaisieVenteFacture, idMvt,
        // rowMvtPere.getInt("ID_PIECE"));
        System.err.println("**************Init Generation Reglement");
        this.idPere = idMvt;
        new Thread(GenerationMvtReglementVenteFacture.this).start();
    }
}
