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
 
 package org.openconcerto.ui.date;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class DateRangeTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 5076344567233395335L;
    private final List<DateRange> ranges = new ArrayList<DateRange>();

    @Override
    public int getRowCount() {
        return this.ranges.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    public DateRange getRange(int index) {
        return this.ranges.get(index);
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final Calendar c = Calendar.getInstance();
        if (columnIndex == 0) {
            c.setTimeInMillis(this.ranges.get(rowIndex).getStart());
        } else {
            c.setTimeInMillis(this.ranges.get(rowIndex).getStop());
        }
        return c.getTime();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null || !(aValue instanceof Date)) {
            throw new IllegalArgumentException("Cannot set " + aValue + " at " + rowIndex + ":" + columnIndex);
        }
        long time = ((Date) aValue).getTime();

        if (columnIndex == 0) {
            this.ranges.get(rowIndex).setStart(time);
        } else {
            this.ranges.get(rowIndex).setStop(time);
        }
    }

    public void addNewLine() {
        long start = System.currentTimeMillis();
        for (final DateRange r : this.ranges) {
            if (r.getStart() + 3600 * 1000 > start) {
                start = r.getStart() + 24 * 3600 * 1000;
            }
        }
        this.ranges.add(new DateRange(start));
        fireTableDataChanged();
    }

    public void fillFrom(final List<DateRange> list) {
        this.ranges.clear();
        this.ranges.addAll(list);
        Collections.sort(this.ranges);
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(final int column) {
        if (column == 0) {
            return "Début";
        } else {
            return "Fin";
        }
    }

    public void remove(final int[] selection) {
        for (int i = selection.length - 1; i >= 0; i--) {
            this.ranges.remove(selection[i]);
        }
        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
}
