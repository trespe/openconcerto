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
import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ListeFactureXmlSheet extends AbstractListeSheetXml {
    public static final String TEMPLATE_ID = "ListeFacture";
    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;

    private List<Map<String, Object>> listValues;
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    private List<Integer> listeIds;

    public ListeFactureXmlSheet(List<Integer> listeIds) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.listeIds = listeIds;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getName() {
        return "ListeFacture";
    }

    protected void createListeValues() {
        SQLElement eltFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        SQLElement eltModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        SQLElement eltTypeRegl = Configuration.getInstance().getDirectory().getElement("TYPE_REGLEMENT");

        if (this.listeIds == null) {
            return;
        }
        this.listValues = new ArrayList<Map<String, Object>>(this.listeIds.size());
        for (Iterator<Integer> i = this.listeIds.iterator(); i.hasNext();) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            SQLRow rowFacture = eltFacture.getTable().getRow(i.next());
            mValues.put("DATE", dateFormat.format((Date) rowFacture.getObject("DATE")));
            mValues.put("NUMERO", rowFacture.getObject("NUMERO"));
            mValues.put("MONTANT_HT", new Double(((Number) rowFacture.getObject("T_HT")).longValue() / 100.0));
            mValues.put("MONTANT_TTC", new Double(((Number) rowFacture.getObject("T_TTC")).longValue() / 100.0));
            mValues.put("INFOS", rowFacture.getString("INFOS"));

            // Client
            SQLRow rowCli;
                rowCli = rowFacture.getForeignRow("ID_CLIENT");
            String libClient = rowCli.getString("FORME_JURIDIQUE") + " " + rowCli.getString("NOM");
            mValues.put("CLIENT", libClient.trim());


            // Mode de reglement
            int idModeRegl = rowFacture.getInt("ID_MODE_REGLEMENT");
            if (idModeRegl > 1) {
                SQLRow rowModeRegl = eltModeRegl.getTable().getRow(idModeRegl);
                Date ech = ModeDeReglementSQLElement.calculDate(rowModeRegl.getInt("AJOURS"), rowModeRegl.getInt("LENJOUR"), (Date) rowFacture.getObject("DATE"));
                mValues.put("DATE_ECHEANCE", dateFormat.format(ech));

                int idTypeRegl = rowModeRegl.getInt("ID_TYPE_REGLEMENT");
                if (idTypeRegl > 1) {
                    SQLRow rowTypeRegl = eltTypeRegl.getTable().getRow(idTypeRegl);
                    mValues.put("TYPE_REGLEMENT", rowTypeRegl.getString("NOM"));
                }
            }

            this.listValues.add(mValues);
        }

        this.listAllSheetValues.put(0, this.listValues);
    }

}
