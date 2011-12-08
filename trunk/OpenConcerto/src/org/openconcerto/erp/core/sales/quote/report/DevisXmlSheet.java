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
 
 package org.openconcerto.erp.core.sales.quote.report;

import org.openconcerto.erp.generationDoc.AbstractSheetXMLWithDate;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

public class DevisXmlSheet extends AbstractSheetXMLWithDate {

    public static final String TEMPLATE_ID = "Devis";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationDevis";

    @Override
    public String getReference() {
        return this.row.getString("OBJET");
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

    public DevisXmlSheet(SQLRow row) {
        super(row);
        this.printer = PrinterNXProps.getInstance().getStringProperty("DevisPrinter");
        this.elt = Configuration.getInstance().getDirectory().getElement("DEVIS");

    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getName() {
        return "Devis_" + this.row.getString("NUMERO");
    }
}
