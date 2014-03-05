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

import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListeDebiteursXmlSheet extends AbstractListeSheetXml {

    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

    public ListeDebiteursXmlSheet() {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
    }

    @Override
    protected String getStoragePathP() {
        return "Autres";
    }

    public String getDefaultTemplateId() {
        return "ListeDebiteur";
    }

    @Override
    public String getName() {
        return "ListeDebiteurs";
    }

    SQLElement eltEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT");
    SQLElement eltVf = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
    SQLElement eltMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT");
    SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
    SQLElement eltEncElt = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT_ELEMENT");

    protected void createListeValues() {

        // On récupére les échéances en cours
        SQLSelect sel = new SQLSelect();
        final SQLTable echTable = eltEch.getTable();
        final SQLTable clientTable = echTable.getForeignTable("ID_CLIENT");
        sel.addSelectStar(echTable);
        Where w = new Where(echTable.getField("REGLE"), "=", Boolean.FALSE);
        w = w.and(new Where(echTable.getField("REG_COMPTA"), "=", Boolean.FALSE));
        w = w.and(new Where(clientTable.getKey(), "=", echTable.getField("ID_CLIENT")));
        sel.setWhere(w);
        sel.addFieldOrder(clientTable.getField("NOM"));
        List<SQLRow> l = (List<SQLRow>) eltEch.getTable().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>();
        Map<Integer, String> styleValues = new HashMap<Integer, String>();
        for (SQLRow sqlRow : l) {
            Map<String, Object> mValues = new HashMap<String, Object>();
            int idMouvement = MouvementSQLElement.getSourceId(sqlRow.getInt("ID_MOUVEMENT"));
            SQLRow rowMvt = eltMvt.getTable().getRow(idMouvement);
            mValues.put("DATE_ECHEANCE", dateFormat.format(sqlRow.getDate("DATE").getTime()));
            if (rowMvt.getString("SOURCE").equalsIgnoreCase(eltVf.getTable().getName())) {
                SQLRow rowVf = eltVf.getTable().getRow(rowMvt.getInt("IDSOURCE"));
                mValues.put("NUMERO_FACTURE", rowVf.getString("NUMERO"));
                mValues.put("REFERENCE", rowVf.getString("NOM"));
                mValues.put("DATE", dateFormat.format(rowVf.getDate("DATE").getTime()));
                mValues.put("MODE_REGLEMENT", "");
                mValues.put("MONTANT", GestionDevise.currencyToString(rowVf.getLong("T_TTC")));
            } else {
                mValues.put("NUMERO_FACTURE", "");
                mValues.put("REFERENCE", "");
                mValues.put("DATE", "");
                mValues.put("MODE_REGLEMENT", "");
                mValues.put("MONTANT", "");
            }
            SQLRow rowClient = sqlRow.getForeignRow("ID_CLIENT");
            mValues.put("NOM_CLIENT", rowClient.getString("NOM"));
            mValues.put("CODE_CLIENT", rowClient.getString("CODE"));
            mValues.put("TELEPHONE", rowClient.getString("TEL"));
            styleValues.put(listValues.size(), "Normal");
            listValues.add(mValues);

            List<SQLRow> enc = sqlRow.getReferentRows(eltEncElt.getTable());

            for (SQLRow sqlRow2 : enc) {
                Map<String, Object> mValuesEnc = new HashMap<String, Object>();
                SQLRow rowEnc = sqlRow2.getForeignRow("ID_ENCAISSER_MONTANT");
                SQLRow rowMdr = rowEnc.getForeignRow("ID_MODE_REGLEMENT");
                mValuesEnc.put("NUMERO_FACTURE", "");
                mValuesEnc.put("REFERENCE", rowMdr.getString("NOM"));
                mValuesEnc.put("DATE", dateFormat.format(rowEnc.getDate("DATE").getTime()));
                mValuesEnc.put("NOM_CLIENT", "");
                mValuesEnc.put("CODE_CLIENT", "");
                mValuesEnc.put("TELEPHONE", "");
                mValuesEnc.put("MODE_REGLEMENT", rowMdr.getForeignRow("ID_TYPE_REGLEMENT").getString("NOM"));
                mValuesEnc.put("MONTANT", GestionDevise.currencyToString(sqlRow2.getLong("MONTANT_REGLE")));
                styleValues.put(listValues.size(), "Titre 1");
                listValues.add(mValuesEnc);

            }
            if (enc != null && enc.size() > 0) {
                Map<String, Object> mValuesEnc = new HashMap<String, Object>();
                mValuesEnc.put("DATE", dateFormat.format(sqlRow.getDate("DATE").getTime()));
                mValuesEnc.put("MODE_REGLEMENT", "Restant à régler");
                mValuesEnc.put("MONTANT", GestionDevise.currencyToString(sqlRow.getLong("MONTANT")));
                styleValues.put(listValues.size(), "Titre 1");
                listValues.add(mValuesEnc);
            }

        }

        this.listAllSheetValues.put(0, listValues);
        this.styleAllSheetValues.put(0, styleValues);

    }

}
