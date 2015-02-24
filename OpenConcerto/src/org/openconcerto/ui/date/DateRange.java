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

import java.text.DateFormat;
import java.util.Date;

public class DateRange implements Comparable<DateRange> {
    private static final int MS_PER_HOUR = 3600 * 1000;
    private long start;
    private long stop;

    public DateRange() {
        this.start = System.currentTimeMillis();
        this.stop = System.currentTimeMillis() + MS_PER_HOUR;
    }

    public DateRange(final long start) {
        this.start = start;
        this.stop = start + MS_PER_HOUR;
    }

    public long getStart() {
        return this.start;
    }

    public long getStop() {
        return this.stop;
    }

    public void setStart(final long start) {
        this.start = start;
    }

    public void setStop(final long stop) {
        this.stop = stop;
    }

    @Override
    public int compareTo(final DateRange o) {
        return (int) (this.start - o.start);
    }

    @Override
    public String toString() {
        DateFormat f = DateFormat.getDateTimeInstance();
        return f.format(new Date(start)) + " -> " + f.format(new Date(stop));
    }
}
