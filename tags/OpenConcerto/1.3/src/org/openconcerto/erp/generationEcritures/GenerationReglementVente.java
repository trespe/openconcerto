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
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class GenerationReglementVente extends GenerationEcritures {

    // Journal Caisse
    private static final Integer journalCaisse = Integer.valueOf(JournalSQLElement.CAISSES);
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationReglementVente(int idEncaisseRegl) throws Exception {

        SQLRow encaisseMontantRow = base.getTable("ENCAISSER_MONTANT").getRow(idEncaisseRegl);

        int idEchCli = encaisseMontantRow.getInt("ID_ECHEANCE_CLIENT");

        SQLRow modeRegRow = base.getTable("MODE_REGLEMENT").getRow(encaisseMontantRow.getInt("ID_MODE_REGLEMENT"));
        SQLRow typeRegRow = base.getTable("TYPE_REGLEMENT").getRow(modeRegRow.getInt("ID_TYPE_REGLEMENT"));

        PrixTTC prixTTC = new PrixTTC(((Long) encaisseMontantRow.getObject("MONTANT")).longValue());

        // iniatilisation des valeurs de la map
        this.date = (Date) encaisseMontantRow.getObject("DATE");

        // TODO Nommage des ecritures
        String s = encaisseMontantRow.getString("NOM");
        this.nom = "Règlement vente " + ((s == null) ? "" : s) + " (" + typeRegRow.getString("NOM") + ")";

        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);
        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        // si paiement comptant
        if ((modeRegRow.getInt("AJOURS") == 0) && (modeRegRow.getInt("LENJOUR") == 0)) {

            // On fixe la date du paiement

            // test Cheque
            if (typeRegRow.getID() == TypeReglementSQLElement.CHEQUE) {

                // Ajout dans cheque à encaisser sans date minimum d'encaissement
                paiementCheque(this.date, idEncaisseRegl);
            } else {
                setDateReglement(idEncaisseRegl, idEchCli, this.date);
                if (typeRegRow.getID() == TypeReglementSQLElement.ESPECE) {
                    this.mEcritures.put("ID_JOURNAL", GenerationReglementVente.journalCaisse);
                }

                SQLRow echeanceRow = null;

                int idCompteClient = -1;
                if (idEchCli > 1) {
                    echeanceRow = base.getTable("ECHEANCE_CLIENT").getRow(idEchCli);
                    SQLRow clientRow = base.getTable("CLIENT").getRow(echeanceRow.getInt("ID_CLIENT"));
                    idCompteClient = clientRow.getInt("ID_COMPTE_PCE");
                    SQLRow rowMvt = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
                    this.idMvt = getNewMouvement("ENCAISSER_MONTANT", idEncaisseRegl, rowMvt.getID(), rowMvt.getInt("ID_PIECE"));
                } else {
                    this.idMvt = getNewMouvement("ENCAISSER_MONTANT", idEncaisseRegl, 1, "");
                }

                // compte Clients
                if (idCompteClient <= 1) {
                    idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                    if (idCompteClient <= 1) {
                        idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                    }
                }

                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(prixTTC.getLongValue()));
                ajoutEcriture();

                // compte de reglement, caisse, cheque, ...
                int idCompteRegl = typeRegRow.getInt("ID_COMPTE_PCE_CLIENT");
                if (idCompteRegl <= 1) {
                    idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("VenteCB");
                }
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteRegl));
                this.mEcritures.put("DEBIT", Long.valueOf(prixTTC.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();

            }
        } else {

            Date dateEch = ModeDeReglementSQLElement.calculDate(modeRegRow.getInt("AJOURS"), modeRegRow.getInt("LENJOUR"), this.date);

            // Cheque
            if (typeRegRow.getID() == TypeReglementSQLElement.CHEQUE) {
                // Ajout dans cheque à encaisser avec date minimum d'encaissement
                paiementCheque(dateEch, idEncaisseRegl);
            } else {

                // Par traite
                System.out.println("Echéance client");

                // Ajout dans echeance
                SQLRowValues valEcheance = new SQLRowValues(base.getTable("ECHEANCE_CLIENT"));

                SQLRow echeanceRow = base.getTable("ECHEANCE_CLIENT").getRow(idEchCli);
                SQLRow rowMouv = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
                this.idMvt = getNewMouvement("ECHEANCE_CLIENT", 1, rowMouv.getID(), rowMouv.getInt("ID_PIECE"));
                valEcheance.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
                valEcheance.put("DATE", dateEch);
                valEcheance.put("MONTANT", Long.valueOf(prixTTC.getLongValue()));

                if (idEchCli > 1) {
                    valEcheance.put("ID_CLIENT", Integer.valueOf(echeanceRow.getInt("ID_CLIENT")));
                }

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

    private void setDateReglement(int idEncaisseRegl, int idEchCli, Date d) throws SQLException {

        if (idEchCli > 1) {

            SQLRow echeanceRow = Configuration.getInstance().getBase().getTable("ECHEANCE_CLIENT").getRow(idEchCli);
            SQLRow encaisseRow = Configuration.getInstance().getBase().getTable("ENCAISSER_MONTANT").getRow(idEncaisseRegl);

            long montant = ((Long) encaisseRow.getObject("MONTANT")).longValue();

            if (montant >= ((Long) echeanceRow.getObject("MONTANT")).longValue()) {

                final int sourceId = MouvementSQLElement.getSourceId(echeanceRow.getInt("ID_MOUVEMENT"));
                SQLRow rowMvt = Configuration.getInstance().getBase().getTable("MOUVEMENT").getRow(sourceId);

                if (rowMvt.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                    SQLElement eltFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
                    SQLRow saisieRow = eltFacture.getTable().getRow(rowMvt.getInt("IDSOURCE"));
                    // On fixe la date du paiement
                    SQLRowValues rowValsUpdateVF = saisieRow.createEmptyUpdateRow();
                    rowValsUpdateVF.put("DATE_REGLEMENT", new Timestamp(d.getTime()));
                    rowValsUpdateVF.update();

                }
            }
        }

    }

    private void paiementCheque(Date dateEch, int idEncaisseRegl) throws SQLException {

        SQLRow encaisseMontantRow = base.getTable("ENCAISSER_MONTANT").getRow(idEncaisseRegl);

        int idEchCli = encaisseMontantRow.getInt("ID_ECHEANCE_CLIENT");
        SQLRow echeanceRow = base.getTable("ECHEANCE_CLIENT").getRow(idEchCli);
        PrixTTC prixTTC = new PrixTTC(((Long) encaisseMontantRow.getObject("MONTANT")).longValue());

        SQLRowValues valCheque = new SQLRowValues(base.getTable("CHEQUE_A_ENCAISSER"));

        if (idEchCli > 1) {
            valCheque.put("ID_CLIENT", Integer.valueOf(echeanceRow.getInt("ID_CLIENT")));
        }

        valCheque.put("DATE_VENTE", this.date);
        SQLRow rowMvtPere = tableMouvement.getRow(echeanceRow.getInt("ID_MOUVEMENT"));
        this.idMvt = getNewMouvement("CHEQUE_A_ENCAISSER", 1, rowMvtPere.getID(), rowMvtPere.getInt("ID_PIECE"));
        valCheque.put("DATE_MIN_DEPOT", dateEch);
        valCheque.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        valCheque.put("MONTANT", Long.valueOf(prixTTC.getLongValue()));

        if (valCheque.getInvalid() == null) {
            // ajout de l'ecriture
            SQLRow row = valCheque.insert();
            SQLRowValues rowVals = new SQLRowValues(tableMouvement);
            rowVals.put("IDSOURCE", row.getID());
            rowVals.update(this.idMvt);
        }

    }
}
