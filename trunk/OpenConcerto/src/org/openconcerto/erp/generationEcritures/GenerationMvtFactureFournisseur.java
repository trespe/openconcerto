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

import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

public class GenerationMvtFactureFournisseur extends GenerationEcritures implements Runnable {

    public static final String ID = "accounting.records.supply.order";

    private int idFacture;
    private static final String source = "FACTURE_FOURNISSEUR";
    private static final Integer journal = new Integer(JournalSQLElement.ACHATS);
    private static final SQLTable tableFacture = base.getTable("FACTURE_FOURNISSEUR");
    private static final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
    private static final SQLTable tableFournisseur = base.getTable("FOURNISSEUR");
    private static final SQLTable tableMvt = base.getTable("MOUVEMENT");
    private static final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

    public GenerationMvtFactureFournisseur(int idFacture, int idMvt) {
        this.idFacture = idFacture;
        this.idMvt = idMvt;
        (new Thread(GenerationMvtFactureFournisseur.this)).start();
    }

    public GenerationMvtFactureFournisseur(int idFacture) {

        this.idFacture = idFacture;
        this.idMvt = 1;
        (new Thread(GenerationMvtFactureFournisseur.this)).start();
    }

    public void genereMouvement() throws Exception {

        SQLRow saisieRow = tableFacture.getRow(this.idFacture);
        // SQLRow taxeRow = base.getTable("TAXE").getRow(saisieRow.getInt("ID_TAXE"));

        SQLRow rowFournisseur = tableFournisseur.getRow(saisieRow.getInt("ID_FOURNISSEUR"));

        // iniatilisation des valeurs de la map
        this.date = (Date) saisieRow.getObject("DATE");
        this.nom = "Achat : " + rowFournisseur.getString("NOM") + " Facture : " + saisieRow.getObject("NUMERO").toString() + " " + saisieRow.getObject("NOM").toString();
        this.mEcritures.put("DATE", this.date);
        // AccountingRecordsProvider provider = AccountingRecordsProviderManager.get(ID);
        // provider.putLabel(saisieRow, this.mEcritures);
        this.mEcritures.put("NOM", nom);
        this.mEcritures.put("ID_JOURNAL", GenerationMvtFactureFournisseur.journal);
        this.mEcritures.put("ID_MOUVEMENT", new Integer(1));

        // on calcule le nouveau numero de mouvement
        if (this.idMvt == 1) {
            SQLRowValues rowValsPiece = new SQLRowValues(pieceTable);
            // provider.putPieceLabel(saisieRow, rowValsPiece);
            rowValsPiece.put("NOM", saisieRow.getObject("NUMERO").toString());
            getNewMouvement(GenerationMvtFactureFournisseur.source, this.idFacture, 1, rowValsPiece);
        } else {
            SQLRowValues rowValsPiece = pieceTable.getTable("MOUVEMENT").getRow(idMvt).getForeign("ID_PIECE").asRowValues();
            // provider.putPieceLabel(saisieRow, rowValsPiece);
            rowValsPiece.put("NOM", saisieRow.getObject("NUMERO").toString());
            rowValsPiece.update();

            this.mEcritures.put("ID_MOUVEMENT", new Integer(this.idMvt));
        }

        // generation des ecritures + maj des totaux du compte associe

        // compte Achat
        SQLRow rowCompteAchat = saisieRow.getForeign("ID_COMPTE_PCE");

        if (rowCompteAchat == null || rowCompteAchat.isUndefined()) {
            rowCompteAchat = rowPrefsCompte.getForeign("ID_COMPTE_PCE_ACHAT");
            if (rowCompteAchat == null || rowCompteAchat.isUndefined()) {
                rowCompteAchat = ComptePCESQLElement.getRowComptePceDefault("Achats");
            }
        }

        TotalCalculator calc = getValuesFromElement(true, "T_PA_HT", saisieRow, saisieRow.getTable().getTable("FACTURE_FOURNISSEUR_ELEMENT"), BigDecimal.ZERO, null, null, rowCompteAchat);

        long ttcLongValue = calc.getTotalTTC().movePointRight(2).longValue();
        long htLongValue = calc.getTotalHT().movePointRight(2).longValue();

        for (SQLRowAccessor row : calc.getMapHt().keySet()) {
            long b = calc.getMapHt().get(row).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
            if (b != 0) {
                this.mEcritures.put("ID_COMPTE_PCE", Integer.valueOf(row.getID()));
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                this.mEcritures.put("DEBIT", Long.valueOf(b));
                SQLRow rowEcr = ajoutEcriture();
                addAssocAnalytiqueFromProvider(rowEcr, saisieRow);
            }
        }

        Map<SQLRowAccessor, BigDecimal> tvaMap = calc.getMapHtTVA();
        for (SQLRowAccessor rowAc : tvaMap.keySet()) {
            long longValue = tvaMap.get(rowAc).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
            if (longValue != 0) {
                this.mEcritures.put("ID_COMPTE_PCE", rowAc.getID());
                this.mEcritures.put("CREDIT", Long.valueOf(0));
                this.mEcritures.put("DEBIT", longValue);
                ajoutEcriture();

                if (rowFournisseur.getBoolean("UE")) {
                    int idCompteTVAIntra = rowPrefsCompte.getInt("ID_COMPTE_PCE_TVA_INTRA");
                    if (idCompteTVAIntra <= 1) {
                        idCompteTVAIntra = ComptePCESQLElement.getIdComptePceDefault("TVAIntraComm");
                    }
                    this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteTVAIntra));
                    this.mEcritures.put("DEBIT", new Long(0));
                    this.mEcritures.put("CREDIT", new Long(longValue));
                    ajoutEcriture();
                }
            }
        }

        // compte Fournisseurs
        int idCompteFourn = rowFournisseur.getInt("ID_COMPTE_PCE");

        if (idCompteFourn <= 1) {
            idCompteFourn = rowPrefsCompte.getInt("ID_COMPTE_PCE_FOURNISSEUR");
            if (idCompteFourn <= 1) {
                idCompteFourn = ComptePCESQLElement.getIdComptePceDefault("Fournisseurs");
            }
        }
        this.mEcritures.put("ID_COMPTE_PCE", new Integer(idCompteFourn));
        this.mEcritures.put("DEBIT", new Long(0));
        if (rowFournisseur.getBoolean("UE")) {
            this.mEcritures.put("CREDIT", new Long(htLongValue));
        } else {
            this.mEcritures.put("CREDIT", new Long(ttcLongValue));
        }
        ajoutEcriture();

        new GenerationMvtReglementFactureFournisseur(this.idFacture, this.idMvt);

        // Mise à jour de la clef etrangere mouvement sur la saisie achat
        SQLRowValues valEcriture = new SQLRowValues(tableFacture);
        valEcriture.put("ID_MOUVEMENT", new Integer(this.idMvt));

        if (valEcriture.getInvalid() == null) {
            // ajout de l'ecriture
            valEcriture.update(this.idFacture);
            displayMvtNumber();
        }

    }

    public void run() {
        try {
            genereMouvement();
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur pendant la générations des écritures comptables", e);
        }
    }
}
