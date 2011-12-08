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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;



public class GenerationReglementAchat extends GenerationEcritures {

    // Journal Caisse
    private static final Integer journalCaisse = new Integer(JournalSQLElement.CAISSES);
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationReglementAchat(int idRegMontant) {

        SQLRow regMontantRow = base.getTable("REGLER_MONTANT").getRow(idRegMontant);

        int idEchFourn = regMontantRow.getInt("ID_ECHEANCE_FOURNISSEUR");
        SQLRow echeanceRow = base.getTable("ECHEANCE_FOURNISSEUR").getRow(idEchFourn);
        SQLRow rowFournisseur = base.getTable("FOURNISSEUR").getRow(echeanceRow.getInt("ID_FOURNISSEUR"));

        System.err.println("Génération des ecritures du reglement du mouvement " + this.idMvt);

        SQLRow modeRegRow = base.getTable("MODE_REGLEMENT").getRow(regMontantRow.getInt("ID_MODE_REGLEMENT"));
        SQLRow typeRegRow = base.getTable("TYPE_REGLEMENT").getRow(modeRegRow.getInt("ID_TYPE_REGLEMENT"));

        System.err.println("Mode de reglement " + regMontantRow.getInt("ID_MODE_REGLEMENT"));

        PrixTTC prixTTC = new PrixTTC(((Long) regMontantRow.getObject("MONTANT")).longValue());

        // iniatilisation des valeurs de la map
        this.date = (Date) regMontantRow.getObject("DATE");

        // "Règlement achat" + SOURCE.getNom() ??
        this.nom = "Règlement achat " + rowFournisseur.getString("NOM") + " (" + typeRegRow.getString("NOM") + ")";

        // si paiement comptant
        if ((modeRegRow.getInt("AJOURS") == 0) && (modeRegRow.getInt("LENJOUR") == 0)) {

            System.err.println("Règlement Comptant");
            // test Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque fournisseur
                paiementCheque(this.date, idEchFourn, idRegMontant);
            } else {

                if (typeRegRow.getID() == 4) {
                    this.mEcritures.put("ID_JOURNAL", GenerationReglementAchat.journalCaisse);
                } else {
                    this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
                }

                // SQLRow echeanceRow = base.getTable("ECHEANCE_FOURNISSEUR").getRow(idEchFourn);
                SQLRow rowMouv = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
                this.idMvt = getNewMouvement("REGLER_MONTANT", idRegMontant, rowMouv.getID(), rowMouv.getInt("ID_PIECE"));

                this.mEcritures.put("DATE", this.date);
                this.mEcritures.put("NOM", this.nom);
                this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));

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
                try {
                    this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteFourn));
                    this.mEcritures.put("DEBIT", new Long(prixTTC.getLongValue()));
                    this.mEcritures.put("CREDIT", new Long(0));
                    ajoutEcriture();

                    // compte de reglement, caisse, CB, ...
                    int idCompteRegl = typeRegRow.getInt("ID_COMPTE_PCE_FOURN");
                    if (idCompteRegl <= 1) {
                        try {
                            idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("AchatCB");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteRegl));
                    this.mEcritures.put("DEBIT", new Long(0));
                    this.mEcritures.put("CREDIT", new Long(prixTTC.getLongValue()));
                    ajoutEcriture();
                } catch (IllegalArgumentException e) {
                    ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
                    e.printStackTrace();
                }
            }
        } else {

            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), this.date);
            DateFormat dateFormat = new SimpleDateFormat();
            System.out.println("Date d'échéance " + dateFormat.format(dateEch));
            // System.out.println("Echeance" + dateEch);

            // Cheque
            if (typeRegRow.getID() == 2) {

                // Ajout dans cheque fournisseur
                paiementCheque(dateEch, idEchFourn, idRegMontant);
            } else {

                // Ajout dans echeance
                Map mEcheance = new HashMap();
                // SQLRow echeanceRow = base.getTable("ECHEANCE_FOURNISSEUR").getRow(idEchFourn);
                SQLRow rowMouv = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
                this.idMvt = getNewMouvement("ECHEANCE_FOURNISSEUR", 1, rowMouv.getID(), rowMouv.getInt("ID_PIECE"));
                mEcheance.put("ID_MOUVEMENT", new Integer(this.idMvt));

                mEcheance.put("DATE", dateEch);
                mEcheance.put("MONTANT", new Long(prixTTC.getLongValue()));

                if (idEchFourn > 1) {
                    mEcheance.put("ID_FOURNISSEUR", new Integer(echeanceRow.getInt("ID_FOURNISSEUR")));
                }

                SQLRowValues valEcheance = new SQLRowValues(base.getTable("ECHEANCE_FOURNISSEUR"), mEcheance);

                try {
                    if (valEcheance.getInvalid() == null) {

                        // ajout de l'ecriture
                        SQLRow row = valEcheance.insert();
                        SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                        rowVals.put("IDSOURCE", row.getID());
                        rowVals.update(this.idMvt);
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur modification" + valEcheance.getTable().getName());
                }
            }
        }
    }

    private void paiementCheque(Date dateEch, int idEchFourn, int idRegMontant) {

        SQLRow regMontantRow = base.getTable("REGLER_MONTANT").getRow(idRegMontant);
        SQLRow echeanceRow = base.getTable("ECHEANCE_FOURNISSEUR").getRow(idEchFourn);
        PrixTTC prixTTC = new PrixTTC(((Long) regMontantRow.getObject("MONTANT")).longValue());

        SQLRowValues valCheque = new SQLRowValues(base.getTable("CHEQUE_FOURNISSEUR"));

        if (idEchFourn > 1) {
            valCheque.put("ID_FOURNISSEUR", new Integer(echeanceRow.getInt("ID_FOURNISSEUR")));
        }
        valCheque.put("DATE_ACHAT", this.date);
        valCheque.put("DATE_MIN_DECAISSE", dateEch);
        SQLRow rowMvtPere = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
        this.idMvt = getNewMouvement("CHEQUE_FOURNISSEUR", 1, rowMvtPere.getID(), rowMvtPere.getInt("ID_PIECE"));
        valCheque.put("ID_MOUVEMENT", new Integer(this.idMvt));
        valCheque.put("MONTANT", new Long(prixTTC.getLongValue()));

        try {
            if (valCheque.getInvalid() == null) {

                // ajout de l'ecriture
                SQLRow row = valCheque.insert();
                SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                rowVals.put("IDSOURCE", row.getID());
                rowVals.update(this.idMvt);
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout " + valCheque.getTable().getName());
        }
    }
}
