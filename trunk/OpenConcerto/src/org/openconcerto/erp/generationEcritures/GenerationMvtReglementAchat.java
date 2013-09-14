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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// FIXME mettre toute les generations dans des threads à part
public final class GenerationMvtReglementAchat extends GenerationEcritures implements Runnable {

    private int idSaisieAchat;

    // Journal Caisse
    private static final Integer journalCaisse = Integer.valueOf(JournalSQLElement.CAISSES);

    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);
    private int idPere = 1; // Id du mouvement pere

    public GenerationMvtReglementAchat(int idSaisieAchat, int idMvt) {
        this.idSaisieAchat = idSaisieAchat;
        this.idPere = idMvt;
        new Thread(GenerationMvtReglementAchat.this).start();
    }

    private void genereReglement() throws Exception {

        System.out.println("Génération des ecritures du reglement du mouvement " + this.idMvt);

        SQLRow saisieRow = base.getTable("SAISIE_ACHAT").getRow(this.idSaisieAchat);
        SQLRow modeRegRow = base.getTable("MODE_REGLEMENT").getRow(saisieRow.getInt("ID_MODE_REGLEMENT"));
        SQLRow typeRegRow = base.getTable("TYPE_REGLEMENT").getRow(modeRegRow.getInt("ID_TYPE_REGLEMENT"));

        System.out.println("Mode de reglement " + saisieRow.getInt("ID_MODE_REGLEMENT"));

        // PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());

        PrixTTC prixTTC;
        int idAvoir = saisieRow.getInt("ID_AVOIR_FOURNISSEUR");
        if (idAvoir > 1) {
            SQLRow avoirRow = base.getTable("AVOIR_FOURNISSEUR").getRow(idAvoir);
            long l = ((Number) avoirRow.getObject("MONTANT_TTC")).longValue();
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue() - l);
        } else {
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());
        }

        this.date = (Date) saisieRow.getObject("DATE");
        SQLRow rowFournisseur = base.getTable("FOURNISSEUR").getRow(saisieRow.getInt("ID_FOURNISSEUR"));
        this.nom = "Règlement Achat : " + rowFournisseur.getString("NOM") + " Facture : " + saisieRow.getObject("NUMERO_FACTURE").toString() + " (" + typeRegRow.getString("NOM") + ")";
        // this.nom = saisieRow.getObject("NOM").toString();

        // si paiement comptant
        if ((modeRegRow.getInt("AJOURS") == 0) && (modeRegRow.getInt("LENJOUR") == 0)) {

            System.out.println("Règlement Comptant");
            // test Cheque
            if (typeRegRow.getID() == 2) {
                Calendar c = modeRegRow.getDate("DATE_DEPOT");
                if (c != null) {
                    paiementCheque(c.getTime());
                } else {
                    paiementCheque(this.date);
                }
            } else {

                this.idMvt = this.idPere;

                // iniatilisation des valeurs de la map
                this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
                this.mEcritures.put("NOM", this.nom);
                this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

                if (typeRegRow.getID() == 4) {
                    this.mEcritures.put("ID_JOURNAL", GenerationMvtReglementAchat.journalCaisse);
                } else {

                    this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);


                }

                // compte Fournisseurs
                int compteFourn = rowFournisseur.getInt("ID_COMPTE_PCE");
                if (compteFourn <= 1) {
                    compteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
                    if (compteFourn <= 1) {
                        compteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
                    }
                }

                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(compteFourn));
                this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();

                // compte de reglement, caisse, CB, ...
                int idCompteRegl = typeRegRow.getInt("ID_COMPTE_PCE_FOURN");
                if (idCompteRegl <= 1) {
                    idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("VenteCB");
                }
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteRegl));
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(prixTTC.getLongValue()));
                ajoutEcriture();
            }
        } else {

            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), this.date);
            DateFormat dateFormat = new SimpleDateFormat();
            System.out.println("Date d'échéance " + dateFormat.format(dateEch));
            // System.out.println("Echeance" + dateEch);

            // Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque fournisseur
                paiementCheque(dateEch);
            } else {

                // Ajout dans echeance
                Map<String, Object> mEcheance = new HashMap<String, Object>();

                SQLRow rowMvtPere = tableMouvement.getRow(this.idPere);
                this.idMvt = getNewMouvement("ECHEANCE_FOURNISSEUR", 1, this.idPere, rowMvtPere.getInt("ID_PIECE"));

                mEcheance.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
                mEcheance.put("DATE", new java.sql.Date(dateEch.getTime()));
                mEcheance.put("MONTANT", Long.valueOf(prixTTC.getLongValue()));
                mEcheance.put("ID_FOURNISSEUR", Integer.valueOf(saisieRow.getInt("ID_FOURNISSEUR")));

                SQLTable echeanceTable = base.getTable("ECHEANCE_FOURNISSEUR");
                SQLRowValues valEcheance = new SQLRowValues(echeanceTable, mEcheance);

                if (valEcheance.getInvalid() == null) {
                    // ajout de l'ecriture
                    SQLRow row = valEcheance.insert();
                    SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                    rowVals.put("IDSOURCE", row.getID());
                    rowVals.update(this.idMvt);
                }

            }
        }
    }

    /**
     * Reglement par cheque. Crée un cheque fournisseur à décaisser.
     * 
     * @param dateEch date d'echeance d'encaissement du cheque
     * @throws SQLException
     */
    private void paiementCheque(Date dateEch) throws SQLException {

        SQLRow saisieRow = base.getTable("SAISIE_ACHAT").getRow(this.idSaisieAchat);
        // PrixTTC prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());

        PrixTTC prixTTC;
        int idAvoir = saisieRow.getInt("ID_AVOIR_FOURNISSEUR");
        if (idAvoir > 1) {
            SQLRow avoirRow = base.getTable("AVOIR_FOURNISSEUR").getRow(idAvoir);
            long l = ((Number) avoirRow.getObject("MONTANT_TTC")).longValue();
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue() - l);
        } else {
            prixTTC = new PrixTTC(((Long) saisieRow.getObject("MONTANT_TTC")).longValue());
        }

        // Ajout dans cheque fournisseur
        Map<String, Object> mEncaisse = new HashMap<String, Object>();
        mEncaisse.put("ID_FOURNISSEUR", Integer.valueOf(saisieRow.getInt("ID_FOURNISSEUR")));
        mEncaisse.put("DATE_ACHAT", new java.sql.Date(this.date.getTime()));
        mEncaisse.put("DATE_MIN_DECAISSE", new java.sql.Date(dateEch.getTime()));
        mEncaisse.put("MONTANT", Long.valueOf(prixTTC.getLongValue()));
        SQLRow rowMvtPere = tableMouvement.getRow(this.idPere);
        this.idMvt = getNewMouvement("CHEQUE_FOURNISSEUR", 1, this.idPere, rowMvtPere.getInt("ID_PIECE"));

        mEncaisse.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        SQLTable chqFournTable = base.getTable("CHEQUE_FOURNISSEUR");

        SQLRowValues valDecaisse = new SQLRowValues(chqFournTable, mEncaisse);

        if (valDecaisse.getInvalid() == null) {

            // ajout de l'ecriture
            SQLRow row = valDecaisse.insert();

            SQLRowValues rowVals = new SQLRowValues(tableMouvement);
            rowVals.put("IDSOURCE", row.getID());
            rowVals.update(this.idMvt);
        }

    }

    public void run() {
        try {
            genereReglement();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
            e.printStackTrace();
        }
    }
}
