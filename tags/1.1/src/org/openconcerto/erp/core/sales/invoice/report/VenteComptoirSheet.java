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
 
 package org.openconcerto.erp.core.sales.invoice.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.SheetInterface;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class VenteComptoirSheet extends SheetInterface {

    private static final SQLTable tableComptoir = base.getTable("SAISIE_VENTE_COMPTOIR");
    private static final SQLTable tableClient = base.getTable("CLIENT");
    private static final SQLTable tableAdresse = base.getTable("ADRESSE");
    private final static SQLTable tableAdresseCommon = Configuration.getInstance().getBase().getTable("ADRESSE_COMMON");
    private static final SQLTable tableArticle = base.getTable("ARTICLE");
    private static final SQLTable tableTaxe = base.getTable("TAXE");
    private static final SQLTable tableModeRegl = base.getTable("MODE_REGLEMENT");
    private static final SQLTable tableTypeRegl = base.getTable("TYPE_REGLEMENT");

    public VenteComptoirSheet(int idFact) {
        super(idFact, tableComptoir);
    }

    public VenteComptoirSheet(SQLRow rowSaisie) {
        super(rowSaisie);
    }

    protected void createMap() {
        // TODO Auto-generated method stub
        this.mCell = new HashMap();

        // Infos societe
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("A1", rowSociete.getObject("TYPE") + " " + rowSociete.getObject("NOM"));
        this.mCell.put("A2", rowSociete.getObject("ADRESSE"));
        this.mCell.put("A3", "Tél  " + rowSociete.getObject("NUM_TEL"));
        this.mCell.put("A4", "Fax " + rowSociete.getObject("NUM_FAX"));

        // infos facture
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        this.mCell.put("A7", this.row.getObject("NOM"));
        this.mCell.put("F7", "Le " + dateFormat.format((Date) this.row.getObject("DATE")));

        // infos client
        SQLRow rowClient = tableClient.getRow(this.row.getInt("ID_CLIENT"));
        this.mCell.put("B9", rowClient.getObject("NOM"));
        SQLRow rowAdrCli = tableAdresse.getRow(rowClient.getInt("ID_ADRESSE"));
        this.mCell.put("B10", rowAdrCli.getObject("RUE") + "\n" + rowAdrCli.getObject("CODE_POSTAL") + " " + rowAdrCli.getObject("VILLE"));

        // mode de reglement
        SQLRow rowRegl = tableModeRegl.getRow(this.row.getInt("ID_MODE_REGLEMENT"));
        SQLRow rowTypeRegl = tableTypeRegl.getRow(rowRegl.getInt("ID_TYPE_REGLEMENT"));
        this.mCell.put("F10", rowTypeRegl.getObject("NOM"));

        // Infos article
        SQLRow rowArticle = tableArticle.getRow(this.row.getInt("ID_ARTICLE"));
        this.mCell.put("A13", rowArticle.getObject("CODE"));
        this.mCell.put("B13", rowArticle.getObject("NOM"));
        this.mCell.put("C13", new Integer(1));
        this.mCell.put("D13", rowArticle.getObject("PV_HT"));

        SQLRow rowTaxe = tableTaxe.getRow(this.row.getInt("ID_TAXE"));
        this.mCell.put("E13", rowTaxe.getObject("TAUX"));
        PrixTTC ttc = new PrixTTC(this.row.getFloat("MONTANT_TTC"));
        this.mCell.put("F13", new Float(ttc.calculHT(rowTaxe.getFloat("TAUX") / 100.0)));
        this.mCell.put("G13", this.row.getObject("MONTANT_TTC"));

        this.mCell.put("G30", new Float(ttc.calculHT(rowTaxe.getFloat("TAUX") / 100.0)));
        this.mCell.put("G31", new Float(ttc.calculTVA(rowTaxe.getFloat("TAUX") / 100.0)));
        this.mCell.put("G32", new Float(ttc.getValue()));

        this.mCell.put("A35", this.row.getObject("INFOS"));
    }
}
