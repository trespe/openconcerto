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
import java.util.Set;

public class EventProviders {

    private EventProviders() {
    }

    static final class Daily implements EventProvider {

        private final int dayIncrement;

        protected Daily(int dayIncrement) {
            super();
            this.dayIncrement = dayIncrement;
        }

        @Override
        public final void next(final Calendar c, final boolean initialValue) {
            // if initial value, any day is fine
            if (!initialValue) {
                c.add(Period.DAY.getCalendarField(), this.dayIncrement);
            }
        }
    }

    static final class Weekly implements EventProvider {

        private final int increment;
        private final Set<DayOfWeek> days;

        protected Weekly(final int increment, final Set<DayOfWeek> days) {
            super();
            this.increment = increment;
            if (days.isEmpty())
                throw new IllegalArgumentException("no days");
            this.days = days;
        }

        @Override
        public final void next(final Calendar c, final boolean initialValue) {
            final int currentWeekNumber = c.get(Calendar.WEEK_OF_YEAR);
            if (!initialValue) {
                // we want a different day
                c.add(Calendar.DAY_OF_WEEK, 1);
            }
            while (c.get(Calendar.WEEK_OF_YEAR) == currentWeekNumber) {
                if (this.days.contains(DayOfWeek.fromCalendar(c))) {
                    return;
                }
                c.add(Calendar.DAY_OF_WEEK, 1);
            }
            // come back to the (last day of) starting week
            c.add(Calendar.DAY_OF_WEEK, -1);
            assert c.get(Calendar.WEEK_OF_YEAR) == currentWeekNumber;

            c.add(Period.WEEK.getCalendarField(), this.increment);
            c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
            // we allow the passed day
            this.next(c, true);
        }
    }

    static private abstract class ConstantPeriodEventProvider implements EventProvider {

        private final Period period;
        private final int increment;

        protected ConstantPeriodEventProvider(final Period period, final int increment) {
            super();
            this.period = period;
            if (increment == 0)
                throw new IllegalArgumentException("Empty increment");
            this.increment = increment;
        }

        @Override
        public final void next(final Calendar c, final boolean initialValue) {
            if (!initialValue || currentPeriodBefore(c)) {
                // GregorianCalendar calls pinDayOfMonth() so that January the 31st + 1 month stays
                // in February
                c.add(this.period.getCalendarField(), this.increment);
            }
            setDate(c);
        }

        protected boolean currentPeriodBefore(final Calendar c) {
            final Calendar clone = (Calendar) c.clone();
            setDate(clone);
            return clone.compareTo(c) < 0;
        }

        protected abstract void setDate(final Calendar c);
    }

    static final class Monthly extends ConstantPeriodEventProvider {

        private final int dayOfMonth;

        protected Monthly(final int dayOfMonth, final int increment) {
            super(Period.MONTH, increment);
            this.dayOfMonth = dayOfMonth;
        }

        @Override
        protected boolean currentPeriodBefore(Calendar c) {
            return this.dayOfMonth < c.get(Calendar.DAY_OF_MONTH);
        }

        @Override
        protected void setDate(Calendar c) {
            if (c.get(Calendar.DAY_OF_MONTH) != this.dayOfMonth) {
                setDayOfMonth(c, this.dayOfMonth);
            }
        }

    }

    // set the day without changing month
    static protected void setDayOfMonth(final Calendar c, final int dayOfMonth) {
        c.set(Calendar.DAY_OF_MONTH, Math.min(c.getActualMaximum(Calendar.DAY_OF_MONTH), dayOfMonth));
    }

    static final class Yearly extends ConstantPeriodEventProvider {

        private final int dayOfMonth;
        private final int month;

        protected Yearly(final int dayOfMonth, final int month, final int increment) {
            super(Period.YEAR, increment);
            this.dayOfMonth = dayOfMonth;
            this.month = month;
        }

        @Override
        protected void setDate(Calendar c) {
            c.set(Calendar.MONTH, this.month);
            setDayOfMonth(c, this.dayOfMonth);
        }
    }

    static private abstract class WeekInMonth extends ConstantPeriodEventProvider {
        private final int ordinal;
        private final DayOfWeek day;

        protected WeekInMonth(final Period period, final int increment, final int ordinal, final DayOfWeek day) {
            super(period, increment);
            if (period.compareTo(Period.MONTH) < 0)
                throw new IllegalArgumentException("Period too short : " + period);
            this.ordinal = ordinal;
            this.day = day;
        }

        @Override
        protected void setDate(Calendar c) {
            // clear any fields that might interfere with time resolution
            c.clear(Calendar.DAY_OF_MONTH);
            c.clear(Calendar.DAY_OF_YEAR);
            c.clear(Calendar.WEEK_OF_YEAR);
            c.clear(Calendar.WEEK_OF_MONTH);

            c.set(Calendar.DAY_OF_WEEK_IN_MONTH, this.ordinal);
            c.set(Calendar.DAY_OF_WEEK, this.day.getCalendarField());
        }
    }

    static final class MonthlyDayOfWeek extends WeekInMonth {
        protected MonthlyDayOfWeek(final int ordinal, final DayOfWeek day, final int increment) {
            super(Period.MONTH, increment, ordinal, day);
        }
    }

    static final class YearlyDayOfWeekEventProvider extends WeekInMonth {

        private final int month;

        protected YearlyDayOfWeekEventProvider(final int ordinal, final DayOfWeek day, final int month, final int increment) {
            super(Period.YEAR, increment, ordinal, day);
            this.month = month;
        }

        @Override
        protected void setDate(final Calendar c) {
            super.setDate(c);
            c.set(Calendar.MONTH, this.month);
        }
    }

}
