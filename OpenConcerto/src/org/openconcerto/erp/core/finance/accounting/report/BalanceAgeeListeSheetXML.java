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
 
 package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.core.finance.payment.element.ModeDeReglementSQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.generationDoc.SheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceAgeeListeSheetXML extends AbstractListeSheetXml {

    // private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
    private Date deb, fin;

    public static Tuple2<String, String> getTuple2Location() {
        return tupleDefault;
    }

    public BalanceAgeeListeSheetXML(Date deb, Date fin) {
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.deb = deb;
        this.fin = fin;
        this.modele = "BalanceAgee";

        this.locationOO = SheetXml.getLocationForTuple(tupleDefault, false);
        this.locationPDF = SheetXml.getLocationForTuple(tupleDefault, true);
    }

    public String getFileName() {
        return getValidFileName("BalanceAgee" + new Date().getTime());
    }

    protected void createListeValues() {

        SQLElement ecr = Configuration.getInstance().getDirectory().getElement("ECRITURE");
        SQLElement cpt = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE");
        SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");

        SQLSelect sel = new SQLSelect(ecr.getTable().getBase());
        sel.addSelectStar(ecr.getTable());
        Where w = new Where(cpt.getTable().getField("NUMERO"), "LIKE", "411%");
        if (this.deb != null) {
            w = w.and(new Where(ecr.getTable().getField("DATE"), ">=", this.deb));
        }
        if (this.fin != null) {
            w = w.and(new Where(ecr.getTable().getField("DATE"), "<=", this.fin));
        }
        // w = w.and(new Where(cpt.getTable().getField("NOM"), "LIKE", "%RIBEIRO%"));
        w = w.and(new Where(ecr.getTable().getField("ID_COMPTE_PCE"), "=", cpt.getTable().getKey()));
        w = w.and(new Where(ecr.getTable().getField("LETTRAGE"), "=", "").or(new Where(ecr.getTable().getField("LETTRAGE"), "=", (Object) null)));

        sel.setWhere(w);
        sel.addFieldOrder(ecr.getTable().getField("COMPTE_NUMERO"));

        System.err.println(sel.asString());
        List<Map<String, Object>> valuesTab = new ArrayList<Map<String, Object>>();

        List<SQLRow> l = (List<SQLRow>) ecr.getTable().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        Calendar c = Calendar.getInstance();
        Map<String, Map<String, Object>> vals = new HashMap<String, Map<String, Object>>();

        long total30 = 0;
        long total60 = 0;
        long total90 = 0;
        long totalPlus = 0;
        long totalFull = 0;

        for (SQLRow sqlRow : l) {
            long date = sqlRow.getDate("DATE").getTimeInMillis();
            SQLRow rowMvt = sqlRow.getForeignRow("ID_MOUVEMENT");
            if (rowMvt.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                SQLRow rowFact = fact.getTable().getRow(rowMvt.getInt("IDSOURCE"));
                date = ModeDeReglementSQLElement.calculDate(rowFact.getForeignRow("ID_MODE_REGLEMENT"), rowFact.getDate("DATE").getTime()).getTime();
            }

            long time = c.getTimeInMillis() - date;
            long day = time / 86400000;
            if (day < 0) {
                // System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                continue;
            }
            final SQLRow rowCpt = sqlRow.getForeignRow("ID_COMPTE_PCE");
            String num = rowCpt.getString("NUMERO");
            Map<String, Object> m;
            if (vals.get(num) == null) {
                m = new HashMap<String, Object>();
                vals.put(num, m);
            } else {
                m = vals.get(num);
            }

            final long value = sqlRow.getLong("DEBIT") - sqlRow.getLong("CREDIT");
            totalFull += value;
            String key = "+90";
            if (day <= 30) {
                key = "30";
                total30 += value;
            } else if (day <= 60) {
                key = "60";
                total60 += value;
            } else if (day <= 90) {
                key = "90";
                total90 += value;
            } else {
                totalPlus += value;
            }

            m.put("NUMERO", num);
            m.put("NOM", rowCpt.getString("NOM"));

            if (m.get(key) == null) {
                m.put(key, value);
            } else {
                long cred = (Long) m.get(key);
                m.put(key, cred + value);
            }

            long total = 0;
            if (m.get("TOTAL") != null) {
                total = (Long) m.get("TOTAL");
            }
            m.put("TOTAL", total + value);

        }

        for (String k : vals.keySet()) {
            final Map<String, Object> e = vals.get(k);
            Long l1 = (Long) e.get("30");
            Long l2 = (Long) e.get("60");
            Long l3 = (Long) e.get("90");
            Long l4 = (Long) e.get("+90");
            Long l5 = (Long) e.get("TOTAL");

            if ((l1 != null && l1 != 0) || (l2 != null && l2 != 0) || (l3 != null && l3 != 0) || (l4 != null && l4 != 0)) {
                if (l1 != null && l1 != 0) {
                    e.put("30", l1 / 100.0);
                }

                if (l2 != null && l2 != 0) {
                    e.put("60", l2 / 100.0);
                }

                if (l3 != null && l3 != 0) {
                    e.put("90", l3 / 100.0);
                }

                if (l4 != null && l4 != 0) {
                    e.put("+90", l4 / 100.0);
                }

                if (l5 != null && l5 != 0) {
                    e.put("TOTAL", l5 / 100.0);
                }

                valuesTab.add(e);
            }
        }
        Map<String, Object> totalMap = new HashMap<String, Object>();
        totalMap.put("NOM", "TOTAL");
        totalMap.put("30", total30 / 100.0);
        totalMap.put("60", total60 / 100.0);
        totalMap.put("90", total90 / 100.0);
        totalMap.put("+90", totalPlus / 100.0);
        totalMap.put("TOTAL", totalFull / 100.0);
        valuesTab.add(totalMap);

        // Map<String, Object> values = this.mapAllSheetValues.get(0);
        // if (values == null) {
        // values = new HashMap<String, Object>();
        // }
        // valuesHA.put("TOTAL", totalHA);
        // valuesE.put("TOTAL_HA", totalHA);
        // valuesE.put("TOTAL", totalE);
        // valuesE.put("TOTAL_VT", totalTPVTTC);
        // values.put("TOTAL", totalVC);
        // values.put("TOTAL_MARGE", totalTPVTTC - totalTPA);
        //
        // valuesE.put("TOTAL_GLOBAL", totalTPVTTC + totalHA);
        // values.put("TOTAL_PA", totalTPA);
        // values.put("TOTAL_PV_TTC", totalTPVTTC);
        //
        // String periode = "Période Du " + dateFormat.format(this.du) + " au " +
        // dateFormat.format(this.au);
        // values.put("DATE", periode);
        // valuesHA.put("DATE", periode);
        // valuesE.put("DATE", periode);

        this.listAllSheetValues.put(0, valuesTab);
        // this.mapAllSheetValues.put(0, values);

    }
}
