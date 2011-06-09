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
 
 package org.openconcerto.sql.view.search;

import java.util.Date;
import java.util.List;

public class DateSearchSpec implements SearchSpec {
    // include or exclude filterString
    private boolean excludeFilterString;
    private int columnIndex;
    private Date fromDate;
    private Date toDate;

    public DateSearchSpec(Date from, Date to, int columnIndex) {
        this(false, from, to, columnIndex);
    }

    public DateSearchSpec(boolean excludeFilterString, Date from, Date to, int columnIndex) {
        this.excludeFilterString = excludeFilterString;
        this.fromDate = new Date(from.getTime());
        this.toDate = new Date(to.getTime());
        this.columnIndex = columnIndex;
    }

    /**
     * Est-ce que line contient une date entre fromDate et toDate dans la colonne index.
     * 
     * @param line la ligne dans laquelle chercher.
     * @param fromDate la date de debut.
     * @param toDate la date de fin.
     * @param index l'index de la colonne, -1 pour toute la ligne.
     * @return <code>true</code> si la ligne contient.
     */
    static private boolean contains(Object line, Date startDate, Date stopDate, int index) {

        List list = (List) line;
        final int start;
        final int stop;
        if (index < 0) {
            // Cas ou on cherche sur tout
            start = 0;
            stop = list.size();// this.getColumnCount();
        } else {
            // Cas ou on cherche sur 1 colonne
            start = index;
            stop = index + 1;
        }

        for (int i = start; i < stop; i++) {
            final Object cell = list.get(i);
            if (cell != null) {
                if (cell instanceof Date) {
                    Date date = (Date) cell;
                    if (date.after(startDate) && date.before(stopDate)) {
                        return true;
                    }
                } else {
                    throw new IllegalArgumentException("The value is not a Date:" + cell + " index:" + index);
                }

            }
        }
        return false;
    }

    public boolean match(Object line) {
        return this.excludeFilterString ^ contains(line, this.fromDate, this.toDate, this.columnIndex);
    }

    public void dump() {
        System.out.println(this.excludeFilterString + ":" + this.fromDate + "->" + this.toDate + " col:" + this.columnIndex);
    }

    public boolean isEmpty() {
        return true;
    }

}
