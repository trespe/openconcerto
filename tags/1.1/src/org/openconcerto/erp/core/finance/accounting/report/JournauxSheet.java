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
import org.openconcerto.erp.generationDoc.SheetInterface;
import org.openconcerto.erp.generationDoc.SheetXml;
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
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class JournauxSheet extends SheetInterface {

    protected static int debutFill, endFill;

    private final static SQLTable tableEcriture = base.getTable("ECRITURE");
    protected final static SQLTable tableJournal = base.getTable("JOURNAL");
    private final static SQLTable tableMvt = base.getTable("MOUVEMENT");
    protected final static SQLTable tableCompte = base.getTable("COMPTE_PCE");
    public final static int MODEALL = 1;
    public final static int MODELETTREE = 2;
    public final static int MODENONLETTREE = 3;

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final static DateFormat dateFormatEcr = DateFormat.getDateInstance(DateFormat.SHORT);
    protected Date dateDu, dateAu;
    protected int[] idS;
    protected int lettrage;
    private String compteDeb, compteEnd;

    public static void setSize(int debut, int fin) {
        debutFill = debut;
        endFill = fin;
    }

    static {
        setSize(7, 68);
    }

    private static final Tuple2<String, String> tuple = Tuple2.create("LocationJournaux", "Journaux");

    public static Tuple2<String, String> getTuple2Location() {
        return tuple;
    }

    public JournauxSheet(int[] id, Date du, Date au, int lettrage, String compteDeb, String compteEnd) {
        super();
        Calendar cal = Calendar.getInstance();
        cal.setTime(au);
        this.printer = PrinterNXProps.getInstance().getStringProperty("JournauxPrinter");
        this.modele = "Journaux.ods";
        this.locationOO = SheetXml.getLocationForTuple(tuple, false) + File.separator + cal.get(Calendar.YEAR);
        this.locationPDF = SheetXml.getLocationForTuple(tuple, true) + File.separator + cal.get(Calendar.YEAR);
        this.dateAu = au;
        this.dateDu = du;
        this.idS = id;
        this.lettrage = lettrage;
        this.nbRowsPerPage = 71;
        this.compteDeb = compteDeb;
        this.compteEnd = compteEnd;

        System.err.println("Init ids with values ");
        for (int i = 0; i < id.length; i++) {
            System.err.println(id[i]);
        }
        createMap();
    }

    protected void makeEntete(int row, String nomJournal) {
        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        this.mCell.put("A" + row, rowSociete.getObject("NOM"));
        this.mCell.put("F" + row, "Edition du " + dateFormat.format(new Date()));
        // this.mCell.put("D" + (row + 2), "Impression Journaux");
        System.err.println("MAKE ENTETE");
    }

    protected void makeBasPage(int row, String nomJournal) {
        this.mCell.put("A" + row, "Journal : " + nomJournal);
        this.mCell.put("E" + row, "Période du " + dateFormatEcr.format(this.dateDu) + " au " + dateFormatEcr.format(this.dateAu));
    }

    protected void createMap() {

        this.mapReplace = new HashMap();
        this.mCell = new HashMap();
        this.mapStyleRow = new HashMap();

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

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                Where w = (new Where(tableEcriture.getField("DATE"), JournauxSheet.this.dateDu, JournauxSheet.this.dateAu));

                Where w2 = null;
                for (int i = 0; i < JournauxSheet.this.idS.length; i++) {
                    if (w2 == null) {
                        w2 = new Where(tableEcriture.getField("ID_JOURNAL"), "=", JournauxSheet.this.idS[i]);
                    } else {
                        w2 = w2.or(new Where(tableEcriture.getField("ID_JOURNAL"), "=", JournauxSheet.this.idS[i]));
                    }
                }

                // w.and(new Where(tableEcriture.getField("ID_MOUVEMENT"), "=",
                // tableMvt.getField("ID")));

                if (JournauxSheet.this.lettrage == MODELETTREE) {
                    Object o = null;
                    w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "<>", o));
                    w = w.and(new Where(tableEcriture.getField("LETTRAGE"), "!=", ""));
                } else {
                    if (JournauxSheet.this.lettrage == MODENONLETTREE) {
                        Object o = null;
                        Where w3 = new Where(tableEcriture.getField("LETTRAGE"), "=", o);
                        w = w.and(w3.or(new Where(tableEcriture.getField("LETTRAGE"), "=", "")));
                    }
                }

                if (JournauxSheet.this.compteDeb.equals(JournauxSheet.this.compteEnd)) {
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), "=", JournauxSheet.this.compteDeb));
                } else {
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), (Object) JournauxSheet.this.compteDeb, (Object) JournauxSheet.this.compteEnd));
                }

                if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
                    // TODO Show Restricted acces in UI
                    w = w.and(new Where(tableEcriture.getField("COMPTE_NUMERO"), "LIKE", "411%"));
                }

                sel.setWhere(w.and(w2));
                sel.addFieldOrder(sel.getAlias(tableEcriture.getField("ID_JOURNAL")));
                sel.addFieldOrder(sel.getAlias(tableEcriture.getField("DATE")));
                sel.addFieldOrder(sel.getAlias(tableMvt.getField("NUMERO")));

                return sel;
            }
        });

        List<SQLRowValues> list = fetcher.fetch();

        int posLine = 1;
        int firstLine = 1;
        System.err.println("START CREATE JOURNAUX, NB ecritures  " + list.size());
        this.nbPage = 0;
        long totalDebit, totalCredit;

        totalDebit = 0;
        totalCredit = 0;
        int prevIdMvt = 0;
        SQLRowValues rowFirstEcr = null;
        String firstJournal = null;

        for (int i = 0; i < list.size();) {

            rowFirstEcr = list.get(i);

            if (firstJournal == null || !firstJournal.equalsIgnoreCase(rowFirstEcr.getString("JOURNAL_NOM"))) {
                totalDebit = 0;
                totalCredit = 0;
            }

            firstJournal = rowFirstEcr.getString("JOURNAL_NOM");
            System.err.println("START NEW PAGE --> Journal : " + firstJournal + "; POS : " + posLine);

            /***************************************************************************************
             * ENTETE
             **************************************************************************************/
            makeEntete(posLine, firstJournal);
            posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            // && (posLine % endFill !=0)
            for (int j = 0; (j < endFill - debutFill + 1) && i < list.size(); j++) {

                SQLRowValues rowEcr = list.get(i);
                String journal = rowEcr.getString("JOURNAL_NOM");

                if (journal.equalsIgnoreCase(firstJournal)) {

                    SQLRowAccessor rowMvt = rowEcr.getForeign("ID_MOUVEMENT");

                    // si on change de mouvement alors on applique le style Titre 1
                    if (prevIdMvt != rowMvt.getID()) {
                        prevIdMvt = rowMvt.getID();
                        this.mapStyleRow.put(new Integer(posLine), "Titre 1");
                    } else {
                        this.mapStyleRow.put(new Integer(posLine), "Normal");
                    }
                    this.mCell.put("A" + posLine, dateFormatEcr.format(rowEcr.getDate("DATE").getTime()));

                    this.mCell.put("B" + posLine, rowEcr.getString("COMPTE_NUMERO"));

                    this.mCell.put("C" + posLine, rowMvt.getObject("NUMERO"));
                    this.mCell.put("D" + posLine, rowEcr.getObject("NOM"));
                    long deb = ((Long) rowEcr.getObject("DEBIT")).longValue();
                    long cred = ((Long) rowEcr.getObject("CREDIT")).longValue();

                    long solde = deb - cred;

                    totalCredit += cred;
                    totalDebit += deb;

                    this.mCell.put("E" + posLine, (deb == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(deb, false)));
                    this.mCell.put("F" + posLine, (cred == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(cred, false)));
                    this.mCell.put("G" + posLine, (solde == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(solde, false)));

                } else {
                    break;
                }

                i++;
                posLine++;
            }

            posLine = firstLine + endFill;
            /*
             * if (this.mapStyleRow.get(new Integer(posLine - 1)) != null) {
             * this.mapStyleRow.put(new Integer(posLine - 1), "Titre 2"); }
             */

            // Bas de page
            this.mCell.put("E" + posLine, (totalDebit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit, false)));
            this.mCell.put("F" + posLine, (totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalCredit, false)));
            this.mCell.put("G" + posLine, (totalDebit - totalCredit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(totalDebit - totalCredit, false)));

            posLine += 2;

            makeBasPage(posLine, firstJournal);

            posLine++;
            firstLine = posLine;
            this.nbPage++;
        }

        // on conserve la page d'origine du model
        if (this.nbPage > 0) {
            this.nbPage--;
        }
    }
}
