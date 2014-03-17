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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GenerationReglementVenteNG extends GenerationEcritures {

    private static final SQLTable tableMouvement = base.getTable("MOUVEMENT");
    private static final SQLTable tableEncaisse = base.getTable("ENCAISSER_MONTANT");
    private static final SQLTable tableEncaisseElt = base.getTable("ENCAISSER_MONTANT_ELEMENT");
    private static final SQLTable tableSaisieVenteFacture = base.getTable("SAISIE_VENTE_FACTURE");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationReglementVenteNG(String label, SQLRow rowClient, PrixTTC ttc, Date d, SQLRow modeReglement, SQLRow source, SQLRow mvtSource) throws Exception {
        this(label, rowClient, ttc, d, modeReglement, source, mvtSource, true);
    }

    public GenerationReglementVenteNG(String label, SQLRow rowClient, PrixTTC ttc, Date d, SQLRow modeReglement, SQLRow source, SQLRow mvtSource, boolean createEncaisse) throws Exception {

        SQLRow typeRegRow = modeReglement.getForeignRow("ID_TYPE_REGLEMENT");

        // iniatilisation des valeurs de la map
        this.date = d;

        // TODO Nommage des ecritures

        this.nom = label;

        this.mEcritures.put("DATE", this.date);
        this.mEcritures.put("NOM", this.nom);
        this.mEcritures.put("ID_JOURNAL", JournalSQLElement.BANQUES);

        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        if (source.getTable().getName().equalsIgnoreCase("ENCAISSER_MONTANT")) {
            List<SQLRow> l = source.getReferentRows(source.getTable().getTable("ENCAISSER_MONTANT_ELEMENT"));
            for (SQLRow sqlRow : l) {
                SQLRow mvtEch = sqlRow.getForeignRow("ID_MOUVEMENT_ECHEANCE");
                if (mvtEch.getID() != mvtSource.getID()) {
                    getNewMouvement(source.getTable().getName(), source.getID(), mvtEch.getID(), mvtEch.getInt("ID_PIECE"));
                }
            }
        }
        // si paiement comptant
        if ((!typeRegRow.getBoolean("ECHEANCE"))
                && ((modeReglement.getBoolean("COMPTANT")) || (!modeReglement.getBoolean("DATE_FACTURE") && (modeReglement.getInt("AJOURS") == 0 && modeReglement.getInt("LENJOUR") == 0)))) {

            SQLRow rowEncaisse = source;

            SQLRow rowEncaisseElt = null;
            // On crée un encaissement
            if (createEncaisse) {
                SQLRowValues rowVals = new SQLRowValues(tableEncaisse);
                rowVals.put("MONTANT", ttc.getLongValue());
                rowVals.put("ID_CLIENT", rowClient.getID());
                rowVals.put("DATE", this.date);
                if (typeRegRow.getID() >= TypeReglementSQLElement.TRAITE) {
                    Calendar c2 = modeReglement.getDate("DATE_VIREMENT");
                    if (c2 != null) {
                        rowVals.put("DATE", c2.getTime());
                    }
                }
                SQLRowValues rowValsRegl = new SQLRowValues(modeReglement.asRowValues());
                SQLRow copy = rowValsRegl.insert();
                rowVals.put("ID_MODE_REGLEMENT", copy.getID());
                rowVals.put("NOM", label);
                rowEncaisse = rowVals.insert();
                SQLRowValues rowValsElt = new SQLRowValues(tableEncaisseElt);
                rowValsElt.put("MONTANT_REGLE", ttc.getLongValue());
                rowValsElt.put("ID_ENCAISSER_MONTANT", rowEncaisse.getID());
                rowEncaisseElt = rowValsElt.insert();

            }

            this.idMvt = getNewMouvement(rowEncaisse.getTable().getName(), rowEncaisse.getID(), mvtSource.getID(), mvtSource.getInt("ID_PIECE"));

            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

            SQLRowValues rowVals = rowEncaisse.createEmptyUpdateRow();
            rowVals.put("ID_MOUVEMENT", this.idMvt);

            rowVals.update();

            if (rowEncaisseElt != null) {
                SQLRowValues rowVals2 = rowEncaisseElt.createEmptyUpdateRow();
                rowVals2.put("ID_MOUVEMENT_ECHEANCE", this.idMvt);
                rowVals2.update();
            }

            // Cheque
            if (typeRegRow.getID() == TypeReglementSQLElement.CHEQUE) {

                Date dateTmp = this.date;
                if (modeReglement.getObject("DATE") != null) {
                    dateTmp = modeReglement.getDate("DATE").getTime();
                }
                // On fixe la date du règlement de la facture
                setDateReglement(source, dateTmp);

                Calendar c = modeReglement.getDate("DATE_DEPOT");
                if (c != null) {
                    paiementCheque(c.getTime(), source, ttc, rowClient.getID(), modeReglement, mvtSource.getTable().getRow(idMvt));
                } else {
                    paiementCheque(this.date, source, ttc, rowClient.getID(), modeReglement, mvtSource.getTable().getRow(idMvt));
                }

            } else {
                // On fixe la date du règlement de la facture
                if (typeRegRow.getID() >= TypeReglementSQLElement.TRAITE) {
                    Calendar c2 = modeReglement.getDate("DATE_VIREMENT");
                    if (c2 == null) {
                        setDateReglement(source, this.date);
                    } else {
                        setDateReglement(source, c2.getTime());
                    }

                } else {
                    setDateReglement(source, this.date);
                }
                if (typeRegRow.getID() == TypeReglementSQLElement.ESPECE) {
                    this.mEcritures.put("ID_JOURNAL", JournalSQLElement.CAISSES);
                }

                int idCompteRegl = typeRegRow.getInt("ID_COMPTE_PCE_CLIENT");
                if (idCompteRegl <= 1) {
                    if (typeRegRow.getID() == TypeReglementSQLElement.ESPECE) {
                        idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("VenteEspece");
                    } else {
                        idCompteRegl = ComptePCESQLElement.getIdComptePceDefault("VenteCB");
                    }
                }

                // compte Clients

                int idCompteClient = rowClient.getInt("ID_COMPTE_PCE");
                if (idCompteClient <= 1) {
                    idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                    if (idCompteClient <= 1) {
                        idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                    }
                }

                this.mEcritures.put("ID_COMPTE_PCE", idCompteClient);
                this.mEcritures.put("DEBIT", Long.valueOf(0));
                this.mEcritures.put("CREDIT", Long.valueOf(ttc.getLongValue()));
                ajoutEcriture();

                // compte de reglement, caisse, cheque, ...
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteRegl));
                this.mEcritures.put("DEBIT", Long.valueOf(ttc.getLongValue()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();

            }
        } else {

                Date dateEch = ModeDeReglementSQLElement.calculDate(modeReglement.getInt("AJOURS"), modeReglement.getInt("LENJOUR"), this.date);

                System.out.println("Echéance client");

                // Ajout dans echeance
                final SQLTable tableEch = base.getTable("ECHEANCE_CLIENT");
                SQLRowValues valEcheance = new SQLRowValues(tableEch);

                this.idMvt = getNewMouvement("ECHEANCE_CLIENT", 1, mvtSource.getID(), mvtSource.getInt("ID_PIECE"));
                valEcheance.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
                valEcheance.put("DATE", dateEch);
                valEcheance.put("MONTANT", Long.valueOf(ttc.getLongValue()));
                valEcheance.put("ID_CLIENT", rowClient.getID());
                if (source.getTable().equals(tableSaisieVenteFacture)) {
                    valEcheance.put("ID_SAISIE_VENTE_FACTURE", source.getID());
                }

                // ajout de l'ecriture
                SQLRow row = valEcheance.insert();
                SQLRowValues rowVals = new SQLRowValues(tableMouvement);
                rowVals.put("IDSOURCE", row.getID());
                rowVals.update(this.idMvt);

        }
    }

    private void setDateReglement(SQLRow source, Date d) throws SQLException {
        List<SQLRow> sources = new ArrayList<SQLRow>();
        if (source.getTable().getName().equalsIgnoreCase("ENCAISSER_MONTANT")) {

            List<SQLRow> rows = source.getReferentRows(source.getTable().getTable("ENCAISSER_MONTANT_ELEMENT"));
            for (SQLRow sqlRow : rows) {
                SQLRow rowEch = sqlRow.getForeignRow("ID_ECHEANCE_CLIENT");
                if (rowEch != null && rowEch.getID() > 1) {
                    SQLRow rowMvt = tableMouvement.getRow(MouvementSQLElement.getSourceId(rowEch.getInt("ID_MOUVEMENT")));
                    if (rowMvt.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                        sources.add(tableSaisieVenteFacture.getRow(rowMvt.getInt("IDSOURCE")));
                    }
                }
            }

        } else {
            sources.add(source);
        }
        for (SQLRow sqlRow : sources) {
            if (sqlRow.getTable().getName().equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                SQLRowValues rowValsUpdateVF = sqlRow.createEmptyUpdateRow();
                rowValsUpdateVF.put("DATE_REGLEMENT", new Timestamp(d.getTime()));
                rowValsUpdateVF.update();
            }
        }

    }

    private void paiementCheque(Date dateEch, SQLRow source, PrixTTC ttc, int idClient, SQLRow modeRegl, SQLRow mvtSource) throws SQLException {

        SQLRowValues valCheque = new SQLRowValues(base.getTable("CHEQUE_A_ENCAISSER"));
        valCheque.put("ID_CLIENT", idClient);
        valCheque.put("NUMERO", modeRegl.getObject("NUMERO"));
        valCheque.put("DATE", modeRegl.getObject("DATE"));
        valCheque.put("ETS", modeRegl.getObject("ETS"));
        valCheque.put("DATE_VENTE", this.date);
        this.idMvt = getNewMouvement("CHEQUE_A_ENCAISSER", 1, mvtSource.getID(), mvtSource.getInt("ID_PIECE"));
        valCheque.put("DATE_MIN_DEPOT", dateEch);
        valCheque.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));
        valCheque.put("MONTANT", Long.valueOf(ttc.getLongValue()));

        if (valCheque.getInvalid() == null) {

            // ajout de l'ecriture
            SQLRow row = valCheque.insert();
            SQLRowValues rowVals = new SQLRowValues(tableMouvement);
            rowVals.put("IDSOURCE", row.getID());
            rowVals.update(this.idMvt);
        }

    }
}
