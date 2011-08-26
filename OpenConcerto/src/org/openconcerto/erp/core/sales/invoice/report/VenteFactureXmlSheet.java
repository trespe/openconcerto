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
import org.openconcerto.erp.generationDoc.AbstractSheetXml;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class VenteFactureXmlSheet extends AbstractSheetXml {

    private String startName;

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationFacture", "Factures");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    @Override
    public String getReference() {
        return this.row.getString("NOM");
    }

    @Override
    public SQLRow getRowLanguage() {
        SQLRow rowClient = this.row.getForeignRow("ID_CLIENT");
        if (rowClient.getTable().contains("ID_LANGUE")) {
            return rowClient.getForeignRow("ID_LANGUE");
        } else {
            return super.getRowLanguage();
        }
    }

    public VenteFactureXmlSheet(SQLRow row) {
        super(row);
        this.printer = PrinterNXProps.getInstance().getStringProperty("FacturePrinter");
        this.elt = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) row.getObject("DATE"));
        this.locationOO = SheetXml.getLocationForTuple(tuple, false) + File.separator + cal.get(Calendar.YEAR);
        this.locationPDF = SheetXml.getLocationForTuple(tuple, true) + File.separator + cal.get(Calendar.YEAR);
        if (row.getBoolean("COMPLEMENT")) {
            this.startName = "FactureComplement_";
            this.modele = "VenteFactureComplement";
        } else {
            if (row.getBoolean("ACOMPTE")) {
                this.startName = "FactureAcompte_";
                this.modele = "VenteFactureAcompte";
            } else {
                this.startName = "Facture_";
                this.modele = "VenteFacture";
            }
        }
    }

    public String getFileName() {

        return getValidFileName(this.startName + this.row.getString("NUMERO"));
    }
}
