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

import java.util.Calendar;
import java.util.EnumSet;

enum DayOfWeek {
    SUNDAY(Calendar.SUNDAY), MONDAY(Calendar.MONDAY), TUESDAY(Calendar.TUESDAY), WEDNESDAY(Calendar.WEDNESDAY), THURSDAY(Calendar.THURSDAY), FRIDAY(Calendar.FRIDAY), SATURDAY(Calendar.SATURDAY);

    public static final EnumSet<DayOfWeek> WORKING_DAYS = EnumSet.complementOf(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

    private final int calendarField;

    private DayOfWeek(int field) {
        this.calendarField = field;
    }

    public final int getCalendarField() {
        return this.calendarField;
    }

    /**
     * Return the instance from a Calendar constant.
     * 
     * @param field a {@link Calendar#DAY_OF_WEEK Calendar constant}, e.g. {@link Calendar#SUNDAY}.
     * @return the instance for the passed parameter.
     */
    static public DayOfWeek fromCalendarField(final int field) {
        for (final DayOfWeek d : values()) {
            if (d.getCalendarField() == field)
                return d;
        }
        throw new IllegalArgumentException("Unknown field : " + field);
    }

    static public DayOfWeek fromCalendar(final Calendar c) {
        return fromCalendarField(c.get(Calendar.DAY_OF_WEEK));
    }

    static public DayOfWeek[] valuesStartingAt(final DayOfWeek d) {
        final DayOfWeek[] all = values();
        if (d.ordinal() == 0)
            return all;

        final DayOfWeek[] res = new DayOfWeek[all.length];
        System.arraycopy(all, d.ordinal(), res, 0, all.length - d.ordinal());
        System.arraycopy(all, 0, res, all.length - d.ordinal(), d.ordinal());
        return res;
    }

    /**
     * The ordered days of a week.
     * 
     * @param c a calendar.
     * @return all days beginning by {@link Calendar#getFirstDayOfWeek()}.
     */
    static public DayOfWeek[] getWeek(final Calendar c) {
        return valuesStartingAt(fromCalendarField(c.getFirstDayOfWeek()));
    }
}
