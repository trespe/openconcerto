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
 
 package org.openconcerto.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeConstants.Field;

import net.jcip.annotations.Immutable;

public class TimeUtils {
    static private DatatypeFactory typeFactory = null;
    static private List<Field> FIELDS_LIST = Arrays.asList(DatatypeConstants.YEARS, DatatypeConstants.MONTHS, DatatypeConstants.DAYS, DatatypeConstants.HOURS, DatatypeConstants.MINUTES,
            DatatypeConstants.SECONDS);
    static private List<Field> DATE_FIELDS, TIME_FIELDS;

    static {
        final int dayIndex = FIELDS_LIST.indexOf(DatatypeConstants.DAYS);
        DATE_FIELDS = Collections.unmodifiableList(FIELDS_LIST.subList(0, dayIndex + 1));
        TIME_FIELDS = Collections.unmodifiableList(FIELDS_LIST.subList(dayIndex + 1, FIELDS_LIST.size()));
    }

    public static List<Field> getAllFields() {
        return FIELDS_LIST;
    }

    /**
     * Get the fields for the date part.
     * 
     * @return fields until {@link DatatypeConstants#DAYS} included.
     */
    public static List<Field> getDateFields() {
        return DATE_FIELDS;
    }

    /**
     * Get the fields for the time part.
     * 
     * @return fields from {@link DatatypeConstants#HOURS}.
     */
    public static List<Field> getTimeFields() {
        return TIME_FIELDS;
    }

    private static Class<? extends Number> getFieldClass(final Field f) {
        return f == DatatypeConstants.SECONDS ? BigDecimal.class : BigInteger.class;
    }

    static public final DatatypeFactory getTypeFactory() {
        if (typeFactory == null)
            try {
                typeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException(e);
            }
        return typeFactory;
    }

    static private final <N extends Number> N getZeroIfNull(final Number n, final Class<N> clazz) {
        final Number res;
        if (n != null)
            res = n;
        else if (clazz == BigInteger.class)
            res = BigInteger.ZERO;
        else if (clazz == BigDecimal.class)
            res = BigDecimal.ZERO;
        else
            throw new IllegalArgumentException("Unknown class : " + n);
        return clazz.cast(res);
    }

    static private final <N extends Number> N getNullIfZero(final N n) {
        if (n == null)
            return null;
        final boolean isZero;
        if (n instanceof BigInteger)
            isZero = n.intValue() == 0;
        else
            isZero = ((BigDecimal) n).compareTo(BigDecimal.ZERO) == 0;
        return isZero ? null : n;
    }

    /**
     * Get non-null seconds with the the correct class.
     * 
     * @param d a duration.
     * @return the seconds, never <code>null</code>.
     * @see Duration#getField(javax.xml.datatype.DatatypeConstants.Field)
     * @see Duration#getMinutes()
     */
    static public final BigDecimal getSeconds(final Duration d) {
        return getZeroIfNull(d.getField(DatatypeConstants.SECONDS), BigDecimal.class);
    }

    /**
     * Convert the time part of a calendar to a duration.
     * 
     * @param cal a calendar, e.g. 23/12/2011 11:55:33.066 GMT+02.
     * @return a duration, e.g. P0Y0M0DT11H55M33.066S.
     */
    public final static Duration timePartToDuration(final Calendar cal) {
        final BigDecimal seconds = BigDecimal.valueOf(cal.get(Calendar.SECOND)).add(BigDecimal.valueOf(cal.get(Calendar.MILLISECOND)).movePointLeft(3));
        return getTypeFactory().newDuration(true, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.valueOf(cal.get(Calendar.HOUR_OF_DAY)), BigInteger.valueOf(cal.get(Calendar.MINUTE)),
                seconds);
    }

    // removes explicit 0
    public final static Duration trimDuration(final Duration dur) {
        return DurationNullsChanger.ALL_TO_NULL.apply(dur);
    }

    // replace null by 0
    public final static Duration removeNulls(final Duration dur) {
        return DurationNullsChanger.NONE_TO_NULL.apply(dur);
    }

    public static enum EmptyFieldPolicy {
        AS_IS, SET_TO_NULL, SET_TO_ZERO
    }

    public final static class DurationNullsBuilder {

        private final Map<Field, EmptyFieldPolicy> policy;

        public DurationNullsBuilder() {
            this(EmptyFieldPolicy.AS_IS);
        }

        public DurationNullsBuilder(final EmptyFieldPolicy initialPolicy) {
            this.policy = new HashMap<Field, EmptyFieldPolicy>();
            this.setPolicy(FIELDS_LIST, initialPolicy);
        }

        public final void setPolicy(Collection<Field> fields, final EmptyFieldPolicy to) {
            for (final Field f : fields)
                this.policy.put(f, to);
        }

        public final DurationNullsBuilder setToNull(Collection<Field> fields) {
            setPolicy(fields, EmptyFieldPolicy.SET_TO_NULL);
            return this;
        }

        public final DurationNullsBuilder setToZero(Collection<Field> fields) {
            setPolicy(fields, EmptyFieldPolicy.SET_TO_ZERO);
            return this;
        }

        public final DurationNullsBuilder dontChange(Collection<Field> fields) {
            setPolicy(fields, EmptyFieldPolicy.AS_IS);
            return this;
        }

        public final DurationNullsChanger build() {
            return new DurationNullsChanger(this.policy);
        }
    }

    /**
     * Allow to change empty fields between two equivalent state. In a {@link Duration} an empty
     * field can be set to <code>null</code> and it won't be output or it can be set to 0 and it
     * will be explicitly output.
     * 
     * @author Sylvain
     * @see DurationNullsBuilder
     */
    @Immutable
    public final static class DurationNullsChanger {

        public final static DurationNullsChanger ALL_TO_NULL = new DurationNullsBuilder(EmptyFieldPolicy.SET_TO_NULL).build();
        public final static DurationNullsChanger NONE_TO_NULL = new DurationNullsBuilder(EmptyFieldPolicy.SET_TO_ZERO).build();

        private final Map<Field, EmptyFieldPolicy> policy;

        private DurationNullsChanger(final Map<Field, EmptyFieldPolicy> policy) {
            this.policy = Collections.unmodifiableMap(new HashMap<Field, EmptyFieldPolicy>(policy));
        }

        // doesn't change the duration value, just nulls and 0s
        public final Duration apply(final Duration dur) {
            boolean changed = false;
            final Map<Field, Number> newValues = new HashMap<Field, Number>();
            for (final Field f : FIELDS_LIST) {
                final Number oldVal = dur.getField(f);
                final EmptyFieldPolicy pol = this.policy.get(f);
                final Number newVal;
                if (pol == EmptyFieldPolicy.SET_TO_NULL) {
                    newVal = getNullIfZero(oldVal);
                } else if (pol == EmptyFieldPolicy.SET_TO_ZERO) {
                    newVal = getZeroIfNull(oldVal, getFieldClass(f));
                } else {
                    assert pol == EmptyFieldPolicy.AS_IS;
                    newVal = oldVal;
                }
                newValues.put(f, newVal);
                changed |= !CompareUtils.equals(newVal, oldVal);
            }

            if (!changed) {
                // Duration is immutable
                return dur;
            } else {
                return getTypeFactory().newDuration(dur.getSign() >= 0, (BigInteger) newValues.get(DatatypeConstants.YEARS), (BigInteger) newValues.get(DatatypeConstants.MONTHS),
                        (BigInteger) newValues.get(DatatypeConstants.DAYS), (BigInteger) newValues.get(DatatypeConstants.HOURS), (BigInteger) newValues.get(DatatypeConstants.MINUTES),
                        (BigDecimal) newValues.get(DatatypeConstants.SECONDS));
            }
        }
    }

    /**
     * Normalize <code>cal</code> so that any Calendar with the same local time have the same
     * result. If you don't need a Calendar this is faster than
     * {@link #copyLocalTime(Calendar, Calendar)}.
     * 
     * @param cal a calendar, e.g. 0:00 CEST.
     * @return the time in millisecond of the UTC calendar with the same local time, e.g. 0:00 UTC.
     */
    public final static long normalizeLocalTime(final Calendar cal) {
        return cal.getTimeInMillis() + cal.getTimeZone().getOffset(cal.getTimeInMillis());
    }

    /**
     * Copy the local time from one calendar to another. Except if both calendars have the same time
     * zone, from.getTimeInMillis() will be different from to.getTimeInMillis().
     * <p>
     * NOTE : In case the two calendars are not from the same class but one of them is a
     * {@link GregorianCalendar} then this method will use a GregorianCalendar with the time zone
     * and absolute time of the other.
     * </p>
     * 
     * @param from the source calendar, e.g. 23/12/2011 11:55:33.066 GMT-12.
     * @param to the destination calendar, e.g. 01/01/2000 0:00 GMT+13.
     * @return the modified destination calendar, e.g. 23/12/2011 11:55:33.066 GMT+13.
     * @throws IllegalArgumentException if both calendars aren't from the same class and none of
     *         them are Gregorian.
     */
    public final static Calendar copyLocalTime(final Calendar from, final Calendar to) throws IllegalArgumentException {
        final boolean sameClass = from.getClass() == to.getClass();
        final boolean createGregSource = !sameClass && to.getClass() == GregorianCalendar.class;
        final boolean createGregDest = !sameClass && from.getClass() == GregorianCalendar.class;
        if (!sameClass && !createGregSource && !createGregDest)
            throw new IllegalArgumentException("Calendars mismatch " + from.getClass() + " != " + to.getClass());

        final Calendar source = createGregSource ? new GregorianCalendar(from.getTimeZone()) : from;
        if (createGregSource) {
            source.setTime(from.getTime());
        }
        final Calendar dest = createGregDest ? new GregorianCalendar(to.getTimeZone()) : to;
        assert source.getClass() == dest.getClass();
        if (source.getTimeZone().equals(dest.getTimeZone())) {
            dest.setTimeInMillis(source.getTimeInMillis());
        } else {
            dest.clear();
            for (final int field : new int[] { Calendar.ERA, Calendar.YEAR, Calendar.DAY_OF_YEAR, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND }) {
                dest.set(field, source.get(field));
            }
        }
        if (createGregDest) {
            to.setTime(dest.getTime());
        }
        return to;
    }
}
