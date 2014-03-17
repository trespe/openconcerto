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
import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProvider;
import org.openconcerto.erp.generationEcritures.provider.AccountingRecordsProviderManager;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

public class GenerationMvtAvoirClient extends GenerationEcritures {

    public static final String ID = "accounting.records.sales.credit";

    private static final String source = "AVOIR_CLIENT";
    // TODO dans quel journal les ecritures des avoirs? OD?
    private static final Integer journal = Integer.valueOf(JournalSQLElement.VENTES);
    private int idAvoirClient;
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtAvoirClient(int idAvoirClient) {
        this.idMvt = 1;
        this.idAvoirClient = idAvoirClient;
    }

    public GenerationMvtAvoirClient(int idAvoirClient, int idMvt) {
        this.idMvt = idMvt;
        this.idAvoirClient = idAvoirClient;
    }

    public int genereMouvement() throws Exception {

        SQLTable avoirClientTable = base.getTable("AVOIR_CLIENT");

        SQLRow avoirRow = avoirClientTable.getRow(this.idAvoirClient);

        boolean affacturage = avoirRow.getBoolean("AFFACTURE");

        SQLRow rowClient;
            rowClient = avoirRow.getForeignRow("ID_CLIENT");

        // iniatilisation des valeurs de la map
        this.date = (Date) avoirRow.getObject("DATE");
        this.nom = avoirRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", new java.sql.Date(this.date.getTime()));
        AccountingRecordsProvider provider = AccountingRecordsProviderManager.get(ID);
        provider.putLabel(avoirRow, this.mEcritures);

        this.mEcritures.put("ID_JOURNAL", GenerationMvtAvoirClient.journal);
        if (affacturage) {

            int idJrnlFactor = rowPrefsCompte.getInt("ID_JOURNAL_FACTOR");
            if (idJrnlFactor > 1) {
                this.mEcritures.put("ID_JOURNAL", idJrnlFactor);
            }
        }

        this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(1));

        // on cree un nouveau mouvement
        if (this.idMvt == 1) {
            SQLRowValues rowValsPiece = new SQLRowValues(pieceTable);
            provider.putPieceLabel(avoirRow, rowValsPiece);
            getNewMouvement(GenerationMvtAvoirClient.source, this.idAvoirClient, 1, rowValsPiece);
        } else {
            this.mEcritures.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

            SQLRowValues rowValsPiece = pieceTable.getTable("MOUVEMENT").getRow(idMvt).getForeign("ID_PIECE").asRowValues();
            provider.putPieceLabel(avoirRow, rowValsPiece);
            rowValsPiece.update();
        }
        BigDecimal portHT = BigDecimal.valueOf(avoirRow.getLong("PORT_HT")).movePointLeft(2);

        TotalCalculator calc = getValuesFromElement(avoirRow, avoirRow.getTable().getTable("AVOIR_CLIENT_ELEMENT"), portHT, null, null);

        for (SQLRowAccessor row : calc.getMapHt().keySet()) {
            long b = calc.getMapHt().get(row).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
            if (b != 0) {
                this.mEcritures.put("ID_COMPTE_PCE", row.getID());
                this.mEcritures.put("DEBIT", Long.valueOf(b));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();
            }
        }

        // compte TVA
        Map<SQLRowAccessor, BigDecimal> tvaMap = calc.getMapHtTVA();
        for (SQLRowAccessor rowAc : tvaMap.keySet()) {
            long longValue = tvaMap.get(rowAc).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
            if (longValue != 0) {
                this.mEcritures.put("ID_COMPTE_PCE", rowAc.getID());
                this.mEcritures.put("DEBIT", Long.valueOf(longValue));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                ajoutEcriture();
            }
        }

        // compte Clients
        int idCompteClient = avoirRow.getForeignRow("ID_CLIENT").getInt("ID_COMPTE_PCE");
        if (idCompteClient <= 1) {
            idCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
            if (idCompteClient <= 1) {
                idCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
            }
        }

        this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
        this.mEcritures.put("DEBIT", Long.valueOf(0));
        long ttc = calc.getTotalTTC().movePointRight(2).longValue();
        this.mEcritures.put("CREDIT", Long.valueOf(ttc));
        ajoutEcriture();

        // Mise à jour de mouvement associé à la facture d'avoir
        SQLRowValues valAvoir = new SQLRowValues(avoirClientTable);
        valAvoir.put("ID_MOUVEMENT", Integer.valueOf(this.idMvt));

        if (valAvoir.getInvalid() == null) {

            valAvoir.update(this.idAvoirClient);
        }

        if (affacturage) {
            this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(idCompteClient));
            this.mEcritures.put("DEBIT", Long.valueOf(ttc));
            this.mEcritures.put("CREDIT", Long.valueOf(0));
            ajoutEcriture();

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
            this.mEcritures.put("DEBIT", Long.valueOf(0));
            this.mEcritures.put("CREDIT", Long.valueOf(ttc));
            ajoutEcriture();
        }

        if (avoirRow.getInt("ID_MODE_REGLEMENT") > 1) {
            new GenerationMvtReglementAvoir(this.idAvoirClient, this.idMvt);
        } else {
            valAvoir.put("SOLDE", Boolean.FALSE);
            valAvoir.update(this.idAvoirClient);
            displayMvtNumber();
        }

        return this.idMvt;
    }
}
