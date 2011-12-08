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

import org.openconcerto.utils.FormatGroup;

import java.text.Format;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Search a text in an object (first in toString() then using formats).
 * 
 * @author Sylvain
 */
public class TextSearchSpec implements SearchSpec {

    public static enum Mode {
        CONTAINS, CONTAINS_STRICT, LESS_THAN, EQUALS, EQUALS_STRICT, GREATER_THAN
    }

    // cannot use Collator : it doesn't works for CONTAINS
    static private final Pattern thoroughPattern = Pattern.compile("(\\p{Punct}|\\p{InCombiningDiacriticalMarks})+");
    static private final Pattern multipleSpacesPattern = Pattern.compile("\\p{Space}+");

    private final Mode mode;
    private final String filterString, normalizedFilterString;
    private final Map<Class<?>, FormatGroup> formats;
    // parsing of filterString for each format
    private final Map<Format, Object> parsedFilter;
    private Double parsedFilterD;
    private boolean parsedFilterD_tried = false;

    public TextSearchSpec(String filterString) {
        this(filterString, Mode.CONTAINS);
    }

    public TextSearchSpec(String filterString, final Mode mode) {
        this.mode = mode;
        this.filterString = filterString;
        this.normalizedFilterString = normalize(filterString);
        this.formats = new HashMap<Class<?>, FormatGroup>();
        this.parsedFilter = new HashMap<Format, Object>();
    }

    private String normalize(String s) {
        if (this.mode == Mode.CONTAINS_STRICT || this.mode == Mode.EQUALS_STRICT) {
            return s.trim();
        } else {
            final String sansAccents = thoroughPattern.matcher(Normalizer.normalize(s.trim(), Form.NFD)).replaceAll("");
            return multipleSpacesPattern.matcher(sansAccents).replaceAll(" ").toLowerCase();
        }
    }

    private final Object getParsed(final Format fmt) {
        Object res;
        if (this.parsedFilter.containsKey(fmt)) {
            res = this.parsedFilter.get(fmt);
        } else {
            try {
                res = fmt.parseObject(this.filterString);
                assert res != null : "Cannot tell apart parsing failed from parsed to null";
            } catch (ParseException e) {
                res = null;
            }
            this.parsedFilter.put(fmt, res);
        }
        return res;
    }

    private final Double getDouble() {
        if (!this.parsedFilterD_tried) {
            try {
                this.parsedFilterD = Double.valueOf(this.filterString);
            } catch (NumberFormatException e) {
                this.parsedFilterD = null;
            }
            this.parsedFilterD_tried = true;
        }
        return this.parsedFilterD;
    }

    private boolean matchWithFormats(Object cell) {
        if (cell == null)
            return false;

        // return now since only the toString() of strings can be sorted (it makes no sense to sort
        // 12/25/2010)
        if (cell.getClass() == String.class)
            return test(cell.toString());

        final boolean containsOrEquals = isContainsOrEquals();
        final boolean isContains = isContains();

        // first an inexpensive comparison
        if (containsOrEquals && containsOrEquals(cell.toString()))
            return true;

        // then try to format the cell
        final FormatGroup fg = getFormat(cell);
        if (fg != null) {
            final List<? extends Format> fmts = fg.getFormats();
            final int stop = fmts.size();
            for (int i = 0; i < stop; i++) {
                final Format fmt = fmts.get(i);
                // e.g. test if "2006" is contained in "25 déc. 2010"
                if (containsOrEquals && containsOrEquals(fmt.format(cell)))
                    return true;
                // e.g. test if "01/01/2006" is before "25 déc. 2010"
                else if (!isContains && test(getParsed(fmt), cell))
                    return true;
            }
        } else if (!isContains && cell instanceof Number) {
            final Number n = (Number) cell;
            if (test(this.getDouble(), n.doubleValue()))
                return true;
        }
        return false;
    }

    private boolean test(final String searched) {
        final String normalized = normalize(searched);
        if (isContains())
            return normalized.indexOf(this.normalizedFilterString) >= 0;
        else if (this.mode == Mode.EQUALS || this.mode == Mode.EQUALS_STRICT)
            return normalized.equals(this.normalizedFilterString);
        else if (this.mode == Mode.LESS_THAN)
            return normalized.compareTo(this.normalizedFilterString) <= 0;
        else if (this.mode == Mode.GREATER_THAN)
            return normalized.compareTo(this.normalizedFilterString) >= 0;
        else
            throw new IllegalArgumentException("unknown mode " + this.mode);
    }

    private boolean isContainsOrEquals() {
        // only real Strings can be sorted (it makes no sense to sort 12/25/2010)
        return this.mode != Mode.LESS_THAN && this.mode != Mode.GREATER_THAN;
    }

    private boolean isContains() {
        return this.mode == Mode.CONTAINS || this.mode == Mode.CONTAINS_STRICT;
    }

    private boolean containsOrEquals(final String searched) {
        // don't normalize otherwise 2005-06-20 matches 2006 :
        // searched is first formatted to 20/06/2005 then normalized to 20062005
        if (isContains()) {
            return searched.indexOf(this.filterString) >= 0;
        } else {
            assert this.mode == Mode.EQUALS || this.mode == Mode.EQUALS_STRICT : "Only call contains() if isContainsOrEquals()";
            return searched.equals(this.filterString);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean test(Object search, final Object cell) {
        assert !(this.mode == Mode.CONTAINS || this.mode == Mode.CONTAINS_STRICT) : "Only call test() if not isContains()";
        if (search == null)
            return false;
        if (this.mode == Mode.EQUALS || this.mode == Mode.EQUALS_STRICT)
            return cell.equals(search);

        if (cell instanceof Comparable) {
            final Comparable c = (Comparable<?>) cell;
            if (this.mode == Mode.LESS_THAN) {
                return c.compareTo(search) <= 0;
            } else {
                assert this.mode == Mode.GREATER_THAN;
                return c.compareTo(search) >= 0;
            }
        } else {
            return false;
        }
    }

    private FormatGroup getFormat(Object cell) {
        final Class<?> clazz = cell.getClass();
        if (!this.formats.containsKey(clazz)) {
            // cache the findings (eg sql.Date can be formatted like util.Date)
            this.formats.put(clazz, findFormat(clazz));
        }
        return this.formats.get(clazz);
    }

    // find if there's a format for cell
    // 1st tries its class, then goes up the hierarchy
    private FormatGroup findFormat(final Class<?> clazz) {
        Class<?> c = clazz;
        FormatGroup res = null;
        while (res == null && c != Object.class) {
            res = this.formats.get(c);
            c = c.getSuperclass();
        }
        return res;
    }

    @Override
    public boolean match(Object line) {
        return this.isEmpty() || matchWithFormats(line);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + this.filterString;
    }

    @Override
    public boolean isEmpty() {
        return this.filterString == null || this.filterString.length() == 0;
    }

    public void setFormats(Map<Class<?>, FormatGroup> formats) {
        this.formats.clear();
        this.formats.putAll(formats);
    }
}
