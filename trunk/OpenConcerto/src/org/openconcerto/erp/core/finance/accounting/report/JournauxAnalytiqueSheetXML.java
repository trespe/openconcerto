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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.cc.ITransformer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournauxAnalytiqueSheetXML extends AbstractListeSheetXml {

    private final static SQLTable tableEcriture = base.getTable("ECRITURE");
    protected final static SQLTable tableJournal = base.getTable("JOURNAL");
    protected final static SQLTable tableAssoc = base.getTable("ASSOCIATION_ANALYTIQUE");
    protected final static SQLTable tablePoste = base.getTable("POSTE_ANALYTIQUE");
    private final static SQLTable tableMvt = base.getTable("MOUVEMENT");
    protected final static SQLTable tableCompte = base.getTable("COMPTE_PCE");
    public final static int MODEALL = 1;
    public final static int MODELETTREE = 2;
    public final static int MODENONLETTREE = 3;

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final static DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);
    protected Date dateDu, dateAu;
    private SQLRow rowPoste;

    public static String TEMPLATE_ID = "JournauxAnalytique";
    public static String TEMPLATE_PROPERTY_NAME = "LocationJournaux";

    private SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getStoragePathP() {
        return "Journaux";
    }

    Date date;

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "JournalAnalytique" + date.getTime();
    }

    public JournauxAnalytiqueSheetXML(Date du, Date au, SQLRow rowPoste) {
        super();
        Calendar cal = Calendar.getInstance();
        cal.setTime(au);
        this.printer = PrinterNXProps.getInstance().getStringProperty("JournauxPrinter");
        this.dateAu = au;
        this.dateDu = du;
        this.rowPoste = rowPoste;
    }

    protected void makeEntete(Map<String, Object> line, String nomJournal) {
        line.put("TITRE_1", "JournalAnalytique " + nomJournal + " - " + rowSociete.getObject("TYPE") + " " + rowSociete.getObject("NOM"));
        line.put("TITRE_2", "Edition du " + dateFormat.format(new Date()) + " Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu));
    }

    protected void createListeValues() {

        final SQLRowValues vals = new SQLRowValues(tableEcriture);

        vals.put("ID_JOURNAL", null);
        vals.put("ID_COMPTE_PCE", null);
        vals.put("COMPTE_NUMERO", null);
        vals.put("COMPTE_NOM", null);
        vals.put("JOURNAL_CODE", null);
        vals.put("JOURNAL_NOM", null);
        vals.putRowValues("ID_MOUVEMENT").put("NUMERO", null);

        vals.put("CREDIT", null);
        vals.put("DEBIT", null);
        vals.put("DATE", null);
        vals.put("NOM", null);

        final SQLRowValues valsAnalytique = new SQLRowValues(tableAssoc);
        valsAnalytique.put("ID_ECRITURE", vals);
        valsAnalytique.putRowValues("ID_POSTE_ANALYTIQUE").put("NOM", null);
        valsAnalytique.put("POURCENT", null);
        valsAnalytique.put("MONTANT", null);
        valsAnalytique.put("ID_ECRITURE", vals);

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(valsAnalytique);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                Where w = (new Where(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("DATE"), JournauxAnalytiqueSheetXML.this.dateDu,
                        JournauxAnalytiqueSheetXML.this.dateAu));

                // if (rowPoste != null && !rowPoste.isUndefined()) {

                w = w.and(new Where(tableAssoc.getField("ID_POSTE_ANALYTIQUE"), "=", rowPoste.getID()));
                // }

                sel.setWhere(w);
                sel.addFieldOrder(tableAssoc.getField("ID_POSTE_ANALYTIQUE"));
                sel.addFieldOrder(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("COMPTE_NUMERO"));

                return sel;
            }
        });

        List<SQLRowValues> list = fetcher.fetch();

        System.err.println("START CREATE JOURNAUX, NB ecritures  " + list.size());

        long totalDebit, totalCredit;

        totalDebit = 0;
        totalCredit = 0;
        int prevIdMvt = 0;

        String firstJournal = rowPoste.getString("NOM");

        List<Map<String, Object>> tableauVals = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, tableauVals);

        Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);

        for (int i = 0; i < list.size(); i++) {

            Map<String, Object> values = new HashMap<String, Object>();

            SQLRowValues rowValsAnalytique = list.get(i);
            SQLRowAccessor rowEcr = rowValsAnalytique.getForeign("ID_ECRITURE");

            SQLRowAccessor rowMvt = rowEcr.getForeign("ID_MOUVEMENT");

            // si on change de mouvement alors on applique le style Titre 1
            if (prevIdMvt != rowMvt.getID()) {
                prevIdMvt = rowMvt.getID();
                style.put(tableauVals.size(), "Titre 1");
            } else {
                style.put(tableauVals.size(), "Normal");
            }
            values.put("DATE", dateFormatEcr.format(rowEcr.getDate("DATE").getTime()));

            values.put("NUMERO_COMPTE", rowEcr.getString("COMPTE_NUMERO"));

            values.put("NUMERO_MOUVEMENT", rowMvt.getObject("NUMERO"));
            Object libelle = rowEcr.getObject("NOM");
            values.put("LIBELLE", libelle);

            long l = rowValsAnalytique.getLong("MONTANT");

            long deb = (l > 0 ? l : 0);
            long cred = (l < 0 ? -l : 0);

            long solde = deb - cred;

            totalCredit += cred;
            totalDebit += deb;

            values.put("DEBIT", (deb == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(deb, false)));
            values.put("CREDIT", (cred == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(cred, false)));
            values.put("SOLDE", (solde == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(solde, false)));

            tableauVals.add(values);

        }

        Map<String, Object> sheetVals = new HashMap<String, Object>();
        this.mapAllSheetValues.put(0, sheetVals);

        makeEntete(sheetVals, firstJournal);

        sheetVals.put("TOTAL_DEBIT", (totalDebit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit, false)));
        sheetVals.put("TOTAL_CREDIT", (totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalCredit, false)));
        sheetVals.put("TOTAL_SOLDE", (totalDebit - totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));

    }
}
