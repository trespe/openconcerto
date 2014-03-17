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
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BalanceAgeeListeSheetXML extends AbstractListeSheetXml {

    private Date deb, fin;
    public static String TEMPLATE_ID = "Balance agée";
    private boolean excludeCloture;

    public BalanceAgeeListeSheetXML(Date deb, Date fin, boolean excludeClotureEcr) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.deb = deb;
        this.fin = fin;
        this.excludeCloture = excludeClotureEcr;
    }

    @Override
    public String getDefaultTemplateId() {
        return "BalanceAgee";
    }

    Date d;

    @Override
    public String getName() {
        if (d == null) {
            d = new Date();
        }
        return "BalanceAgee" + d.getTime();
    }

    @Override
    protected String getStoragePathP() {
        return "Balance";
    }

    protected void createListeValues() {
        final SQLElement ecr = Configuration.getInstance().getDirectory().getElement("ECRITURE");
        // SQLElement cpt = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE");
        final SQLElement fact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");

        SQLRowValues rowValsEcr = new SQLRowValues(ecr.getTable());
        rowValsEcr.put("COMPTE_NUMERO", null);
        rowValsEcr.put("COMPTE_NOM", null);
        rowValsEcr.put("DATE_LETTRAGE", null);
        rowValsEcr.put("NOM", null);
        rowValsEcr.put("DEBIT", null);
        rowValsEcr.put("DATE", null);
        rowValsEcr.put("CREDIT", null);

        SQLRowValues rowValsMvt = new SQLRowValues(ecr.getTable().getForeignTable("ID_MOUVEMENT"));
        rowValsMvt.put("IDSOURCE", null);
        rowValsMvt.put("SOURCE", null);
        rowValsEcr.put("ID_MOUVEMENT", rowValsMvt);

        // Liste des codes de lettrage hors période
        SQLSelect sel = new SQLSelect();
        sel.addSelect(ecr.getTable().getField("LETTRAGE"));
        Where w = new Where(ecr.getTable().getField("LETTRAGE"), "IS NOT", (Object) null);
        w = w.and(new Where(ecr.getTable().getField("LETTRAGE"), "!=", ""));

        if (fin != null) {
            w = w.and(new Where(ecr.getTable().getField("DATE"), "<=", fin));
        }
        sel.setWhere(w);

        final List<String> lettrageList = (List<String>) Configuration.getInstance().getBase().getDataSource().executeCol(sel.asString());

        // Liste des codes de lettrage hors période
        SQLSelect sel2 = new SQLSelect();
        sel2.addSelect(ecr.getTable().getField("LETTRAGE"));
        Where w2 = new Where(ecr.getTable().getField("LETTRAGE"), "IS NOT", (Object) null);
        w2 = w2.and(new Where(ecr.getTable().getField("LETTRAGE"), "!=", ""));
        if (deb != null) {
            w2 = w2.and(new Where(ecr.getTable().getField("DATE"), ">=", deb));
        }
        if (fin != null) {
            w2 = w2.and(new Where(ecr.getTable().getField("DATE"), "<=", fin));
        }
        sel2.addGroupBy(ecr.getTable().getField("LETTRAGE"));
        sel2.setHaving(Where.createRaw("SUM(\"DEBIT\") != SUM(\"CREDIT\")", Arrays.asList(ecr.getTable().getField("DEBIT"), ecr.getTable().getField("CREDIT"))));
        sel2.setWhere(w2);
        System.err.println(sel2.asString());
        lettrageList.addAll((List<String>) Configuration.getInstance().getBase().getDataSource().executeCol(sel2.asString()));

        final HashSet<String> lettrageToExclude = new HashSet<String>();
        lettrageToExclude.addAll(lettrageList);

        SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(rowValsEcr);

        // SQLSelect sel = new SQLSelect();
        // sel.addSelectStar(ecr.getTable());
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                final SQLTable tableEcriture = ecr.getTable();
                Where w = new Where(tableEcriture.getField("COMPTE_NUMERO"), "LIKE", "411%");
                if (deb != null) {
                    w = w.and(new Where(tableEcriture.getField("DATE"), ">=", deb));
                }
                if (fin != null) {
                    w = w.and(new Where(tableEcriture.getField("DATE"), "<=", fin));
                }
                // w = w.and(new Where(cpt.getTable().getField("NOM"), "LIKE", "%RIBEIRO%"));
                // w = w.and(new Where(ecr.getTable().getField("ID_COMPTE_PCE"), "=",
                // cpt.getTable().getKey()));
                // Where whereLettrage = new Where(ecr.getTable().getField("LETTRAGE"), "=",
                // "").or(new
                // Where(ecr.getTable().getField("LETTRAGE"), "=", (Object) null));
                // whereLettrage = whereLettrage.or(new Where(ecr.getTable().getField("LETTRAGE"),
                // "!=",
                // "").and(new Where(ecr.getTable().getField("LETTRAGE"), "!=", (Object) null)).and(
                // new Where(ecr.getTable().getField("DATE_LETTRAGE"), ">=", this.fin)));

                // Where whereLettrage = new Where(tableEcriture.getField("DATE_LETTRAGE"), "IS",
                // (Object) null).or(new Where(tableEcriture.getField("DATE_LETTRAGE"), ">", fin));
                // w = w.and(whereLettrage);
                if (excludeCloture) {
                    w = w.and(new Where(tableEcriture.getField("NOM"), "NOT LIKE", "Fermeture du compte %"));
                    w = w.and(new Where(ecr.getTable().getField("NOM"), "!=", "A nouveaux"));
                }

                Where wLettrage = new Where(tableEcriture.getField("LETTRAGE"), "IS", (Object) null);
                wLettrage = wLettrage.or(new Where(tableEcriture.getField("LETTRAGE"), "=", ""));
                String aliasEcr = input.getAlias(tableEcriture.getField("LETTRAGE")).getAlias();
                // DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                // wLettrage = wLettrage.or(Where.createRaw("(SELECT COUNT(*) FROM " +
                // tableEcriture.getSQLName().quote() + " e WHERE e.\"LETTRAGE\"=\"" + aliasEcr +
                // "\".\"LETTRAGE\" AND e.\"DATE\"<='"
                // + format.format(fin) + "' HAVING SUM(e.\"DEBIT\") != SUM (e.\"CREDIT\") )>0",
                // tableEcriture.getField("LETTRAGE")));

                wLettrage = wLettrage.or(new Where(tableEcriture.getField("LETTRAGE"), true, lettrageToExclude));
                input.setWhere(w.and(wLettrage));

                input.addFieldOrder(tableEcriture.getField("COMPTE_NUMERO"));

                System.err.println(input.asString());
                return input;
            }
        });

        List<Map<String, Object>> valuesTab = new ArrayList<Map<String, Object>>();

        // List<SQLRow> l = (List<SQLRow>)
        // ecr.getTable().getBase().getDataSource().execute(sel.asString(),
        // SQLRowListRSH.createFromSelect(sel));

        List<SQLRowValues> l = fetcher.fetch();

        Map<String, Map<String, Object>> vals = new LinkedHashMap<String, Map<String, Object>>();

        long total0 = 0;
        long total30 = 0;
        long total60 = 0;
        long total90 = 0;
        long totalPlus = 0;
        long totalFull = 0;
        long totalEchue = 0;

        // Calendar c = Calendar.getInstance();
        // final long timeInMillis = c.getTimeInMillis();
        final long timeInMillis = this.fin.getTime();
        for (SQLRowValues sqlRow : l) {
            long date = sqlRow.getDate("DATE").getTimeInMillis();
            SQLRowAccessor rowMvt = sqlRow.getForeign("ID_MOUVEMENT");
            if (rowMvt.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                SQLRow rowFact = fact.getTable().getRow(rowMvt.getInt("IDSOURCE"));
                date = ModeDeReglementSQLElement.calculDate(rowFact.getForeignRow("ID_MODE_REGLEMENT"), rowFact.getDate("DATE").getTime()).getTime();
            }

            long time = timeInMillis - date;
            long day = time / 86400000;
            // if (day < 0) {
            // continue;
            // }
            // final SQLRow rowCpt = sqlRow.getForeignRow("ID_COMPTE_PCE");
            // String num = rowCpt.getString("NUMERO");
            String num = sqlRow.getString("COMPTE_NUMERO");
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
            if (day < 0) {
                key = "0";
                total0 += value;
            } else {
                totalEchue += value;
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
            }
            m.put("NUMERO", num);
            m.put("NOM", sqlRow.getString("COMPTE_NOM"));

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
        System.err.println("INTERMED BALANCE");
        for (String k : vals.keySet()) {
            final Map<String, Object> e = vals.get(k);
            Long l0 = (Long) e.get("0");
            Long l1 = (Long) e.get("30");
            Long l2 = (Long) e.get("60");
            Long l3 = (Long) e.get("90");
            Long l4 = (Long) e.get("+90");
            Long l5 = (Long) e.get("TOTAL");

            if ((l0 != null && l0 != 0) || (l1 != null && l1 != 0) || (l2 != null && l2 != 0) || (l3 != null && l3 != 0) || (l4 != null && l4 != 0)) {

                if (l0 != null && l0 != 0) {
                    e.put("0", l0 / 100.0);
                }

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
        final Map<String, Object> totalMap = new HashMap<String, Object>();
        totalMap.put("NOM", "TOTAL");
        totalMap.put("0", total0 / 100.0);
        totalMap.put("30", total30 / 100.0);
        totalMap.put("60", total60 / 100.0);
        totalMap.put("90", total90 / 100.0);
        totalMap.put("+90", totalPlus / 100.0);
        totalMap.put("TOTAL", totalFull / 100.0);
        totalMap.put("TOTAL_ECHUE", totalEchue / 100.0);
        valuesTab.add(totalMap);
        System.err.println("FIN BALANCE");
        this.listAllSheetValues.put(0, valuesTab);

    }
}
