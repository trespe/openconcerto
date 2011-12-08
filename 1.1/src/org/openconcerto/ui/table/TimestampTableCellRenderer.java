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
 
 package org.openconcerto.ui.table;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class TimestampTableCellRenderer implements TableCellRenderer {

    private class TimeProp {
        long t;
        String label;
        Color bg, fg;

        public TimeProp(long time) {
            this.t = time;
        }
    }

    Vector<TimeProp> cache = new Vector<TimeProp>();
    private static final DateFormat formatterFull = DateFormat.getDateTimeInstance();
    private static final DateFormat formatterDate = new SimpleDateFormat("dd MMM yyyy");

    private Calendar calendar;
    private boolean highlight, showHour;
    private Calendar currentCalendar;
    private Date currentDate;
    private static SimpleDateFormat sf = new SimpleDateFormat("HH:mm");
    private int currentYear;
    private String currentYearS;
    private int currentDayOfYear;
    static final int WIDTH = 60;
    private JLabel jLabel = new JLabel("timestamp");

    public TimestampTableCellRenderer() {
        this(false);
    }

    public TimestampTableCellRenderer(boolean highlight) {
        this(highlight, true);
    }

    public TimestampTableCellRenderer(boolean highlight, boolean showHour) {

        this.jLabel.setOpaque(true);
        this.jLabel.setBorder(new EmptyBorder(1, 1, 1, 1));
        this.jLabel.setHorizontalAlignment(SwingConstants.LEFT);

        this.highlight = highlight;
        this.showHour = showHour;
        this.calendar = Calendar.getInstance();
        this.currentCalendar = Calendar.getInstance();
        this.currentCalendar.setTimeInMillis(System.currentTimeMillis());
        this.currentDate = new Date(System.currentTimeMillis());

        this.currentYear = this.currentCalendar.get(Calendar.YEAR);
        this.currentYearS = String.valueOf(this.currentYear);
        this.currentDayOfYear = this.currentCalendar.get(Calendar.DAY_OF_YEAR);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableCellRendererUtils.setColors(this.jLabel, table, isSelected);
        // if (isSelected) {
        // jLabel.setForeground(table.getSelectionForeground());
        // jLabel.setBackground(table.getSelectionBackground());
        // } else {
        // jLabel.setForeground(table.getForeground());
        // jLabel.setBackground(table.getBackground());
        // }
        if (value == null) {
            this.jLabel.setText("");
            return this.jLabel;
        }

        if (row >= this.cache.size()) {
            this.cache.setSize(row + 1);
        }
        TimeProp p = this.cache.get(row);

        // Depend de value
        Date time = (Date) value;
        this.calendar.setTime(time);
        // if (p != null && p.t != time.getTime()) {
        //
        // p = null;
        // }
        //
        // if (p == null) {
        p = new TimeProp(time.getTime());
        this.cache.set(row, p);

        String str;
        if (this.showHour) {
            str = formatterFull.format(time);
        } else {
            str = formatterDate.format(time);
        }
        // si c'est la meme annee, on ne mets pas l'année

        str = str.replaceAll(this.currentYearS, "");

        // si c'est le meme jour, on ne met que l'heure

        if (this.currentDayOfYear == this.calendar.get(Calendar.DAY_OF_YEAR) && this.currentYear == this.calendar.get(Calendar.YEAR)) {

            if (this.showHour) {
                str = "aujourd'hui à " + sf.format(time);

            } else {
                str = "aujourd'hui";
            }
            // label.setText(calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE));
        } else {
            // getField(Calendar.HOUR_OF_DAY);
            if (this.showHour) {
                str = str.substring(0, str.length() - 3);
            }
        }
        p.label = str;

        if (this.highlight) {

            // si c'est dans le passé, on met en rouge
            if (time.before(this.currentDate)) {
                p.fg = Color.RED.darker();
            }
            if ((this.currentDayOfYear == this.calendar.get(Calendar.DAY_OF_YEAR) || (this.currentDayOfYear + 1) == this.calendar.get(Calendar.DAY_OF_YEAR))
                    && this.currentYear == this.calendar.get(Calendar.YEAR)) {
                p.fg = Color.ORANGE.darker();
            }
        }
        if (!isSelected) {
            this.jLabel.setForeground(p.fg);
        }
        this.jLabel.setText(p.label);

        return this.jLabel;
    }

    public static void install(JTable jtable, int column) {
        jtable.setAutoCreateColumnsFromModel(false);
        TableCellRenderer renderer = new TimestampTableCellRenderer();
        TableColumnModel tcm = jtable.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellRenderer(renderer);
        tc.setPreferredWidth(WIDTH);
        tc.setMinWidth(WIDTH);
        tc.setMaxWidth(WIDTH);
    }

}
