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
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.GestionDevise;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class JournauxMoisSheet extends JournauxSheet {

    public JournauxMoisSheet(int[] id, Date du, Date au, int lettrage) {

        super(id, du, au, lettrage, "1", "8");
        this.modele = "JournauxMois.ods";
    }

    private static DateFormat dateFormatMonth = new SimpleDateFormat("MMMM");
    private static DateFormat dateFormatYear = new SimpleDateFormat("yyyy");
    private static DateFormat dateFormatPG = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void createMap() {
        this.mapReplace = new HashMap();
        this.mCell = new HashMap();
        this.mapStyleRow = new HashMap();
        String schema = ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName();

        String select = "SELECT SUM(\"DEBIT\"), SUM(\"CREDIT\"), date_part('month', \"DATE\"), date_part('year', \"DATE\"),\"JOURNAL\".\"ID\" FROM \"" + schema + "\".\"ECRITURE\" , \"" + schema
                + "\".\"JOURNAL\" ";

        String groupBy = " GROUP BY date_part('year', \"DATE\"), date_part('month', \"DATE\"),\"JOURNAL\".\"ID\"";
        String orderBy = " ORDER BY \"JOURNAL\".\"ID\",date_part('year', \"DATE\"), date_part('month', \"DATE\")";
        if (this.idS != null && this.idS.length > 0) {
            select += " WHERE";
            for (int i = 0; i < this.idS.length; i++) {
                if (i == 0) {
                    select += "( \"" + schema + "\".\"JOURNAL\".\"ID\" = " + this.idS[i];
                } else {
                    select += " OR \"" + schema + "\".\"JOURNAL\".\"ID\" = " + this.idS[i];
                }
            }
            select += ")";
        }

        if (this.lettrage == MODELETTREE) {
            if (this.idS != null && this.idS.length > 0) {
                select += " AND  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" <> NULL";
                select += " AND  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" != ''";

            } else {
                select += " WHERE  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" <> NULL";
                select += " AND  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" != ''";
            }
        } else {
            if (this.lettrage == MODENONLETTREE) {
                if (this.idS != null && this.idS.length > 0) {
                    select += " AND  (\"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" = NULL";
                    select += " OR \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" = '')";

                } else {
                    select += " WHERE  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" = NULL";
                    select += " OR  \"" + schema + "\".\"ECRITURE\".\"LETTRAGE\" = ''";
                }
            }
        }
        // ARCHIVE, ID 1,
        select += " AND \"" + schema + "\".\"JOURNAL\".\"ID\"!=1";
        select += " AND \"" + schema + "\".\"ECRITURE\".\"ID\"!=1";
        select += " AND \"" + schema + "\".\"JOURNAL\".\"ARCHIVE\"!=1";
        select += " AND \"" + schema + "\".\"ECRITURE\".\"ARCHIVE\"!=1";

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            // TODO Show Restricted acces in UI
            select += " AND \"" + schema + "\".\"ECRITURE\".\"COMPTE_NUMERO\" LIKE '411%'";
        }

        // DATE
        select += " AND \"" + schema + "\".\"ECRITURE\".\"DATE\" BETWEEN '" + dateFormatPG.format(this.dateDu) + "' AND '" + dateFormatPG.format(this.dateAu) + "'";
        select += " AND \"" + schema + "\".\"JOURNAL\".\"ID\"=\"" + schema + "\".\"ECRITURE\".\"ID_JOURNAL\"" + groupBy + orderBy;
        System.err.println(select);

        List l = (List) base.getDataSource().execute(select, new ArrayListHandler());

        int posLine = 1;
        int firstLine = 1;
        System.err.println("START CREATE JOURNAUX, NB ecritures  " + l.size());
        this.nbPage = 0;
        long totalDebit, totalCredit;

        totalDebit = 0;
        totalCredit = 0;
        SQLRow rowFirstJournal = null;

        for (int i = 0; i < l.size();) {

            Object[] tmp = (Object[]) l.get(i);
            int idJrnl = Integer.valueOf(tmp[4].toString());
            int year = Double.valueOf(tmp[3].toString()).intValue();
            int month = Double.valueOf(tmp[2].toString()).intValue();
            long credit = Integer.valueOf(tmp[1].toString());
            long debit = Integer.valueOf(tmp[0].toString());

            if (rowFirstJournal == null || rowFirstJournal.getID() != idJrnl) {
                totalDebit = 0;
                totalCredit = 0;
            }

            rowFirstJournal = tableJournal.getRow(idJrnl);
            System.err.println("START NEW PAGE --> Journal : " + rowFirstJournal.getString("NOM") + "; POS : " + posLine);

            /***************************************************************************************
             * ENTETE
             **************************************************************************************/
            makeEntete(posLine, rowFirstJournal.getString("NOM"));
            posLine += debutFill - 1;

            /***************************************************************************************
             * CONTENU
             **************************************************************************************/
            Calendar cal = Calendar.getInstance();
            // && (posLine % endFill !=0)
            for (int j = 0; (j < endFill - debutFill + 1) && i < l.size(); j++) {
                tmp = (Object[]) l.get(i);
                idJrnl = Integer.valueOf(tmp[4].toString());
                year = Double.valueOf(tmp[3].toString()).intValue();
                month = Double.valueOf(tmp[2].toString()).intValue();
                credit = Integer.valueOf(tmp[1].toString());
                debit = Integer.valueOf(tmp[0].toString());

                SQLRow rowJournal = tableJournal.getRow(idJrnl);

                if (rowJournal.getID() == rowFirstJournal.getID()) {

                    this.mapStyleRow.put(new Integer(posLine), "Titre 1");
                    cal.set(Calendar.MONTH, month - 1);
                    cal.set(Calendar.YEAR, year);
                    this.mCell.put("A" + posLine, dateFormatMonth.format(cal.getTime()));
                    this.mCell.put("B" + posLine, dateFormatYear.format(cal.getTime()));
                    this.mCell.put("C" + posLine, "");
                    this.mCell.put("D" + posLine, "");

                    long solde = debit - credit;

                    totalCredit += credit;
                    totalDebit += debit;

                    this.mCell.put("E" + posLine, (debit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(debit, false)));
                    this.mCell.put("F" + posLine, (credit == 0) ? new Double(0) : new Double(GestionDevise.currencyToString(credit, false)));
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

            makeBasPage(posLine, rowFirstJournal.getString("NOM"));

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
