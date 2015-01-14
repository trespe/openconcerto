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
import org.openconcerto.erp.core.common.element.BanqueSQLElement;
import org.openconcerto.erp.generationDoc.AbstractSheetXMLWithDate;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

public class VenteFactureXmlSheet extends AbstractSheetXMLWithDate {

    public static final String TEMPLATE_ID = "VenteFacture";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationFacture";

    @Override
    public String getReference() {
        return this.row.getString("NOM");
    }

    @Override
    public String getName() {
        final String startName;
        if (row.getBoolean("COMPLEMENT")) {
            startName = "FactureComplement_";
        } else if (row.getBoolean("ACOMPTE")) {
            startName = "FactureAcompte_";
        } else {
            startName = "Facture_";
        }
        return startName + this.row.getString("NUMERO");
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
        getDefaultTemplateId();
    }

    @Override
    public String getType() {
        String type;
        if (row.getBoolean("COMPLEMENT")) {
            type = "Complement";
        } else if (row.getBoolean("ACOMPTE")) {
            type = "Acompte";
        } else if (row.getBoolean("PARTIAL") || row.getBoolean("SOLDE")) {
            type = "Situation";
        } else {
            type = null;

        }

        return type;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

}
