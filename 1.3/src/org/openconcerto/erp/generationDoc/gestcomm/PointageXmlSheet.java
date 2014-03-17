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
 
 package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PointageXmlSheet extends AbstractListeSheetXml {

    public static final String TEMPLATE_ID = "Pointage";
    public static final String TEMPLATE_PROPERTY_NAME = DEFAULT_PROPERTY_NAME;
    private Calendar c = Calendar.getInstance();
    private Date date = new Date();
    private final long MILLIS_IN_HOUR = 3600000;

    public PointageXmlSheet(int mois, int year) {
        super();
        this.printer = PrinterNXProps.getInstance().getStringProperty("BonPrinter");
        this.mapAllSheetValues = new HashMap<Integer, Map<String, Object>>();
        this.c.set(Calendar.DAY_OF_MONTH, 1);
        this.c.set(Calendar.YEAR, year);
        this.c.set(Calendar.MONTH, mois);
        this.c.set(Calendar.HOUR_OF_DAY, 0);
        this.c.set(Calendar.MINUTE, 0);
        this.c.set(Calendar.SECOND, 0);
        this.c.set(Calendar.MILLISECOND, 1);

    }

    @Override
    protected String getStoragePathP() {
        return "Pointage";
    }

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "Pointage" + this.date;
    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    protected void createListeValues() {
        SQLElement eltPointage = Configuration.getInstance().getDirectory().getElement("POINTAGE");
        SQLElement eltUser = Configuration.getInstance().getDirectory().getElement("USER_COMMON");
        int user = 0;

        final int actualMaximum = this.c.getActualMaximum(Calendar.DAY_OF_MONTH);
        this.listAllSheetValues = new HashMap<Integer, List<Map<String, Object>>>();
        this.styleAllSheetValues = new HashMap<Integer, Map<Integer, String>>();

        DateFormat format = new SimpleDateFormat("EEEE dd MMMM");
        DateFormat formatMonth = new SimpleDateFormat("MMMM");
        DateFormat formatMonthYear = new SimpleDateFormat("MMMM yyyy");
        DateFormat formatHour = new SimpleDateFormat("HH:mm");

        Date d1 = this.c.getTime();
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.setTime(d1);
        endOfMonth.set(Calendar.DAY_OF_MONTH, this.c.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 59);
        Date d2 = endOfMonth.getTime();

        // On recupere la liste des utilisateurs ayant pointé dans le mois
        // SELECT u."ID" FROM "KD_Common"."USER_COMMON" u, "KD_Common"."POINTAGE" p WHERE
        // p."ID_USER_COMMON"=u."ID" AND p."DATE" BETWEEN '2010-02-23 01:01' AND '2010-02-23 23:59'
        // GROUP BY u."ID" HAVING COUNT(p."ID_USER_COMMON")>0
        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        final SQLTable tableUser = eltUser.getTable();
        AliasedTable tablePointage = new AliasedTable(eltPointage.getTable(), "POINTAGE");

        sel.addSelect(tableUser.getKey());
        sel.addSelect(tableUser.getField("NOM"));
        sel.addSelect(tableUser.getField("PRENOM"));
        sel.addSelect(tableUser.getField("HEURE_MATIN_A"));
        sel.addSelect(tableUser.getField("HEURE_MATIN_D"));
        sel.addSelect(tableUser.getField("MINUTE_MATIN_A"));
        sel.addSelect(tableUser.getField("MINUTE_MATIN_D"));
        sel.addSelect(tableUser.getField("HEURE_MIDI_A"));
        sel.addSelect(tableUser.getField("HEURE_MIDI_D"));
        sel.addSelect(tableUser.getField("MINUTE_MIDI_A"));
        sel.addSelect(tableUser.getField("MINUTE_MIDI_D"));
        Where w = new Where(tableUser.getField("ID"), "=", tablePointage.getField("ID_USER_COMMON"));
        w = w.and(new Where(tablePointage.getField("DATE"), d1, d2));

        sel.setWhere(w);
        sel.addGroupBy(tableUser.getKey());
        sel.addGroupBy(tableUser.getField("NOM"));
        sel.addGroupBy(tableUser.getField("PRENOM"));
        sel.addGroupBy(tableUser.getField("HEURE_MATIN_A"));
        sel.addGroupBy(tableUser.getField("HEURE_MATIN_D"));
        sel.addGroupBy(tableUser.getField("MINUTE_MATIN_A"));
        sel.addGroupBy(tableUser.getField("MINUTE_MATIN_D"));
        sel.addGroupBy(tableUser.getField("HEURE_MIDI_A"));
        sel.addGroupBy(tableUser.getField("HEURE_MIDI_D"));
        sel.addGroupBy(tableUser.getField("MINUTE_MIDI_A"));
        sel.addGroupBy(tableUser.getField("MINUTE_MIDI_D"));
        sel.setHaving(Where.createRaw("COUNT (\"POINTAGE\".\"ID_USER_COMMON\") > 0", tablePointage.getField("ID_USER_COMMON")));
        sel.addFieldOrder(tableUser.getField("NOM"));
        System.err.println(sel.asString());
        @SuppressWarnings("unchecked")
        List<SQLRow> listUser = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, tableUser));

        String entete = "Horaires de travail du mois de " + formatMonth.format(d1);
        String pied = "Total " + formatMonthYear.format(d1);

        // Pour chacun des utilisateurs
        for (SQLRow row : listUser) {

            int semaine = -1;
            final String userName = row.getString("NOM") + " " + row.getString("PRENOM");
            this.sheetNames.add(userName);
            System.err.println(userName);

            // Entete
            Map<String, Object> mapSheetValue = new HashMap<String, Object>();
            mapSheetValue.put("A1", userName);
            mapSheetValue.put("F1", entete);

            long tempsDePause = this.MILLIS_IN_HOUR;

            // calcul du temps de pause si possible
            if (row.getObject("HEURE_MATIN_D") != null && row.getObject("MINUTE_MATIN_D") != null && row.getObject("HEURE_MIDI_A") != null && row.getObject("MINUTE_MIDI_A") != null) {

                int heureD = row.getInt("HEURE_MATIN_D") * 60;
                int minuteD = row.getInt("MINUTE_MATIN_D");
                int heureA = row.getInt("HEURE_MIDI_A") * 60;
                int minuteA = row.getInt("MINUTE_MIDI_A");

                tempsDePause = (heureA + minuteA - heureD - minuteD) * 60000;
            }
            System.err.println("Temps de pause " + tempsDePause);

            // heure totale travaillees dans la semaine
            long heureTotalSemaine = 0;
            long heureTotalMois = 0;
            List<Map<String, Object>> listValues = new ArrayList<Map<String, Object>>();
            Map<Integer, String> styleValues = new HashMap<Integer, String>();
            int error = 0;

            // On parcourt les jours du mois
            for (int i = 1; i <= actualMaximum; i++) {

                // Valeur pour les cellules de la ligne
                Map<String, Object> mValues = new HashMap<String, Object>();
                this.c.set(Calendar.DAY_OF_MONTH, i);

                // Temps travaillé dans la semaine
                if (semaine > 0 && semaine != this.c.get(Calendar.WEEK_OF_YEAR)) {
                    Map<String, Object> mValues2 = new HashMap<String, Object>();
                    long hour = heureTotalSemaine / this.MILLIS_IN_HOUR;
                    long minute = (heureTotalSemaine % this.MILLIS_IN_HOUR) / 60000;
                    mValues2.put("HEURE_TOTAL", hour + "h" + minute);
                    heureTotalSemaine = 0;
                    listValues.add(mValues2);
                    styleValues.put(listValues.size() - 1, "Titre 2");
                }

                // On met à jour le numéro de semaine
                semaine = this.c.get(Calendar.WEEK_OF_YEAR);

                // Jour
                mValues.put("JOUR", format.format(this.c.getTime()));

                this.c.set(Calendar.HOUR_OF_DAY, 0);
                this.c.set(Calendar.MINUTE, 0);
                this.c.set(Calendar.SECOND, 0);
                this.c.set(Calendar.MILLISECOND, 1);

                Date debutJournee = this.c.getTime();
                this.c.set(Calendar.HOUR_OF_DAY, 23);
                this.c.set(Calendar.MINUTE, 59);
                this.c.set(Calendar.SECOND, 59);
                this.c.set(Calendar.MILLISECOND, 59);

                Date finJournee = this.c.getTime();
                SQLSelect sel2 = new SQLSelect(Configuration.getInstance().getBase());
                final SQLTable table2 = eltPointage.getTable();
                sel2.addSelectStar(table2);
                Where wDay = new Where(table2.getField("ID_USER_COMMON"), "=", row.getID());
                wDay = wDay.and(new Where(table2.getField("DATE"), debutJournee, finJournee));
                sel2.setWhere(wDay);
                sel2.addFieldOrder(table2.getField("DATE"));

                @SuppressWarnings("unchecked")
                List<SQLRow> list2 = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel2.asString(), SQLRowListRSH.createFromSelect(sel2, table2));

                if (list2.size() > 2) {
                    boolean dehors = true;
                    Calendar dateSortie = null;
                    long timeOut = 0;
                    String heurePointe = "";
                    for (SQLRow rowPointage : list2) {
                        final Calendar date = rowPointage.getDate("DATE");
                        date.set(Calendar.SECOND, 0);
                        date.set(Calendar.MILLISECOND, 0);
                        if (dehors) {
                            if (dateSortie != null) {
                                timeOut += date.getTimeInMillis() - dateSortie.getTimeInMillis();
                                System.err.println(row.getString("PRENOM") + "  " + timeOut);
                            }
                            dehors = false;
                        } else {
                            dehors = true;
                            dateSortie = date;
                        }

                        heurePointe += formatHour.format(date.getTime()) + ", ";
                    }

                    if (list2.size() % 2 == 1) {
                        heurePointe += "Erreur de pointage";
                        error++;
                    }

                    mValues.put("HEURE_POINTE", heurePointe);
                    timeOut = Math.max(timeOut, tempsDePause);

                    final Calendar dateOutDay = list2.get(list2.size() - 1).getDate("DATE");
                    dateOutDay.set(Calendar.SECOND, 0);
                    dateOutDay.set(Calendar.MILLISECOND, 0);
                    final Calendar dateInDay = list2.get(0).getDate("DATE");
                    dateInDay.set(Calendar.SECOND, 0);
                    dateInDay.set(Calendar.MILLISECOND, 0);
                    System.err.println("Date Out " + formatHour.format(dateOutDay.getTime()));
                    System.err.println("Date In " + formatHour.format(dateInDay.getTime()));
                    long time = dateOutDay.getTimeInMillis() - dateInDay.getTimeInMillis();

                    long timeWorked = (time - timeOut);
                    if (time < timeOut) {
                        timeWorked = time;
                    }

                    System.err.println("time " + time + " Worked :" + timeWorked);
                    heureTotalSemaine += timeWorked;
                    heureTotalMois += timeWorked;
                    long hour = timeWorked / this.MILLIS_IN_HOUR;
                    long minute = (timeWorked % this.MILLIS_IN_HOUR) / 60000;
                    mValues.put("HEURE_TOTAL", hour + "h" + minute);

                } else {
                    mValues.put("HEURE_TOTAL", "0h0");
                    if (list2.size() == 1) {
                        error++;
                        SQLRow rowPointage = list2.get(0);
                        final Calendar date = rowPointage.getDate("DATE");
                        mValues.put("HEURE_POINTE", formatHour.format(date.getTime()) + ", erreur de pointage");
                    }
                }

                listValues.add(mValues);
                styleValues.put(listValues.size() - 1, "Titre 1");
            }

            // Heure de la derniere semaine
            Map<String, Object> mValues2 = new HashMap<String, Object>();
            long hour = heureTotalSemaine / this.MILLIS_IN_HOUR;
            long minute = (heureTotalSemaine % this.MILLIS_IN_HOUR) / 60000;
            mValues2.put("HEURE_TOTAL", hour + "h" + minute);
            heureTotalSemaine = 0;
            listValues.add(mValues2);
            styleValues.put(listValues.size() - 1, "Titre 2");
            this.listAllSheetValues.put(user, listValues);
            this.styleAllSheetValues.put(user, styleValues);

            // Heure total du mois
            Map<String, Object> mValuesMois = new HashMap<String, Object>();
            hour = heureTotalMois / this.MILLIS_IN_HOUR;
            minute = (heureTotalMois % this.MILLIS_IN_HOUR) / 60000;
            mValuesMois.put("HEURE_TOTAL", hour + "h" + minute);
            heureTotalMois = 0;

            if (error > 0) {
                if (error == 1) {
                    mValuesMois.put("JOUR", pied + " dont une erreur de pointage");
                } else {
                    mValuesMois.put("JOUR", pied + " dont " + error + " erreurs de pointage");
                }
            } else {
                mValuesMois.put("JOUR", pied);
            }
            listValues.add(mValuesMois);
            styleValues.put(listValues.size() - 1, "Titre 3");

            this.listAllSheetValues.put(user, listValues);
            this.styleAllSheetValues.put(user, styleValues);
            this.mapAllSheetValues.put(user, mapSheetValue);

            user++;
        }

    }

}
