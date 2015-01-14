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
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.cc.ITransformer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class VentilationAnalytiqueSheetXML extends AbstractListeSheetXml {

    private static int debutFill, endFill;
    protected final static SQLTable tableAssoc = base.getTable("ASSOCIATION_ANALYTIQUE");
    protected final static SQLTable tablePoste = base.getTable("POSTE_ANALYTIQUE");
    private final static SQLTable tableEcriture = base.getTable("ECRITURE");
    private final static SQLTable tableJournal = base.getTable("JOURNAL");
    private final static SQLTable tableMvt = base.getTable("MOUVEMENT");
    private final static SQLTable tableCompte = base.getTable("COMPTE_PCE");

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final static DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);
    private SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();

    private Date dateDu, dateAu;
    private String compteDeb, compteEnd;
    private SQLRow rowPoste;

    public static String TEMPLATE_ID = "VentilationAnalytique";
    public static String TEMPLATE_PROPERTY_NAME = "LocationJournaux";

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    Date date;

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "VentilationAnalytique" + date.getTime();
    }

    @Override
    protected String getStoragePathP() {
        return "Ventilation Analytique";
    }

    public VentilationAnalytiqueSheetXML(Date du, Date au, SQLRow rowPoste) {
        super();
        Calendar cal = Calendar.getInstance();
        cal.setTime(au);
        this.printer = PrinterNXProps.getInstance().getStringProperty("JournauxPrinter");
        this.dateAu = au;
        this.dateDu = du;
        this.rowPoste = rowPoste;
    }

    private String toDay = dateFormat.format(new Date());
    private int size;

    private void makeSousTotal(Map<String, Object> line, Map<Integer, String> style, int pos, long debit, long credit) {
        style.put(pos, "Titre 1");

        line.put("DATE", "");
        line.put("JOURNAL", "");
        line.put("MOUVEMENT", "");
        line.put("LIBELLE", "Sous total");
        line.put("DEBIT", Double.valueOf(GestionDevise.currencyToString(debit, false)));
        line.put("CREDIT", Double.valueOf(GestionDevise.currencyToString(credit, false)));
        line.put("SOLDE", Double.valueOf(GestionDevise.currencyToString(debit - credit, false)));
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

                Where w = (new Where(sel.getJoinFromField(tableAssoc.getField("ID_ECRITURE")).getJoinedTable().getField("DATE"), VentilationAnalytiqueSheetXML.this.dateDu,
                        VentilationAnalytiqueSheetXML.this.dateAu));

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
        size = list.size();

        long totalDebit, totalCredit, sousTotalDebit, sousTotalCredit, totalCreditAntC, totalDebitAntC, totalCreditAntF, totalDebitAntF;

        totalDebit = 0;
        totalCredit = 0;
        sousTotalCredit = 0;
        sousTotalDebit = 0;
        totalCreditAntC = 0;
        totalDebitAntC = 0;
        totalCreditAntF = 0;
        totalDebitAntF = 0;
        SQLRowAccessor rowFirstEcr = null;
        int idCptFirstEcr = 1;

        boolean setTitle = true;
        boolean setLine = false;
        boolean setCumuls = true;
        boolean firstEcrCentC = true;
        boolean firstEcrCentF = true;
        String numCptFirstEcr = "";

        final String titre3 = "Titre 3";
        // int j = 0;

        // Valeur de la liste
        // listAllSheetValues ;

        // Style des lignes
        // styleAllSheetValues;

        // Valeur à l'extérieur de la liste
        // mapAllSheetValues

        List<Map<String, Object>> tableauVals = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, tableauVals);

        Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);

        // Affiche le nom du compte
        setTitle = true;
        // ligne vide avant de mettre le setTitle
        setLine = false;
        for (int i = 0; i < size;) {
            // System.err.println(i);
            // // System.err.println("START NEW PAGE; POS : " + posLine);
            //
            // /***************************************************************************************
            // * ENTETE
            // **************************************************************************************/
            // // makeEntete(posLine);
            // // posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            final Double doubleZero = Double.valueOf("0");

            final SQLRowValues sqlRowValuesAnalytique = list.get(i);
            SQLRowAccessor rowEcr = sqlRowValuesAnalytique.getForeign("ID_ECRITURE");

            int idCpt = rowEcr.getInt("ID_COMPTE_PCE");
            String nomCpt = rowEcr.getString("COMPTE_NOM");
            String numCpt = rowEcr.getString("COMPTE_NUMERO");

            Map<String, Object> ooLine = new HashMap<String, Object>();
            tableauVals.add(ooLine);

            // Titre
            if (setTitle) {
                if (!setLine) {
                    style.put(tableauVals.size() - 1, "Titre 1");

                    ooLine.put("DATE", numCpt);
                    ooLine.put("CODE_JOURNAL", nomCpt);
                    ooLine.put("JOURNAL", "");
                    ooLine.put("NUMERO_COMPTE", "");
                    ooLine.put("LIBELLE_COMPTE", "");
                    ooLine.put("NUMERO_MOUVEMENT", "");
                    ooLine.put("LIBELLE", "");
                    ooLine.put("DEBIT", "");
                    ooLine.put("CREDIT", "");
                    ooLine.put("SOLDE", "");
                    setTitle = false;
                    setLine = true;

                    if (rowFirstEcr == null) {
                        rowFirstEcr = rowEcr;
                        idCptFirstEcr = rowEcr.getInt("ID_COMPTE_PCE");
                        numCptFirstEcr = rowEcr.getString("COMPTE_NUMERO");
                    }

                } else {
                    style.put(tableauVals.size() - 1, "Normal");
                    setLine = false;
                }
            } else {

                // si on change de compte alors on applique le style Titre 1
                if (rowFirstEcr != null && idCptFirstEcr != idCpt) {

                    rowFirstEcr = rowEcr;
                    idCptFirstEcr = rowFirstEcr.getInt("ID_COMPTE_PCE");
                    numCptFirstEcr = rowEcr.getString("COMPTE_NUMERO");
                    makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

                    sousTotalCredit = 0;
                    sousTotalDebit = 0;
                    setTitle = true;
                    setCumuls = true;
                } else {
                    long cred = rowEcr.getLong("CREDIT");
                    long deb = rowEcr.getLong("DEBIT");
                    // Centralisation fournisseur

                    ooLine.put("DATE", dateFormatEcr.format((Date) rowEcr.getObject("DATE")));

                    ooLine.put("CODE_JOURNAL", rowEcr.getString("JOURNAL_CODE"));
                    ooLine.put("JOURNAL", rowEcr.getString("JOURNAL_NOM"));
                    ooLine.put("NUMERO_MOUVEMENT", rowEcr.getForeign("ID_MOUVEMENT").getObject("NUMERO"));
                    ooLine.put("LIBELLE", rowEcr.getObject("NOM"));
                    ooLine.put("CODE_LETTRAGE", rowEcr.getObject("LETTRAGE"));
                    ooLine.put("CODE_POINTAGE", rowEcr.getObject("POINTEE"));
                    ooLine.put("DATE_LETTRAGE", rowEcr.getObject("DATE_LETTRAGE"));
                    ooLine.put("DATE_POINTAGE", rowEcr.getObject("DATE_LETTRAGE"));

                    totalCredit += cred;
                    totalDebit += deb;

                    sousTotalCredit += cred;
                    sousTotalDebit += deb;
                    long solde = sousTotalDebit - sousTotalCredit;

                    ooLine.put("DEBIT", (deb == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(deb, false)));
                    ooLine.put("CREDIT", (cred == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(cred, false)));
                    ooLine.put("SOLDE", (solde == 0) ? doubleZero : Double.valueOf(GestionDevise.currencyToString(solde, false)));

                    style.put(tableauVals.size() - 1, "Normal");
                    i++;
                }

            }

        }

        Map<String, Object> sheetVals = new HashMap<String, Object>();
        this.mapAllSheetValues.put(0, sheetVals);

        if (size > 0) {
            Map<String, Object> ooLine = new HashMap<String, Object>();
            tableauVals.add(ooLine);
            makeSousTotal(ooLine, style, tableauVals.size() - 1, sousTotalDebit, sousTotalCredit);

            sheetVals.put("TOTAL_DEBIT", (totalDebit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit, false)));
            sheetVals.put("TOTAL_CREDIT", (totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalCredit, false)));
            sheetVals.put("TOTAL_SOLDE", (totalDebit - totalCredit == 0) ? 0 : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));
        }

        sheetVals.put("TITRE_1", "Ventilation analytique " + this.rowSociete.getString("TYPE") + " " + this.rowSociete.getString("NOM"));
        sheetVals.put("DATE_EDITION", new Date());
        sheetVals.put("TITRE_2", "Poste analytique : " + this.rowPoste.getString("NOM") + ". Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu) + ".");

    }

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    public int getSize() {
        return size;
    }
}
