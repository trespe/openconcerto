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
 
 package org.openconcerto.sql.model;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.Immutable;

/**
 * A dotted SQL name, eg "table.field" or "schema.table".
 * 
 * @author Sylvain
 */
@Immutable
public final class SQLName {

    private static final Pattern unquoted = Pattern.compile("\\w+");
    private static final Pattern MS_END_QUOTE = Pattern.compile("]", Pattern.LITERAL);

    /**
     * Parse a possibly quoted string to an SQL name.
     * 
     * @param name a String, eg public."ta.ble seq".
     * @return the corresponding SQL name, eg "public"."ta.ble seq".
     */
    public static SQLName parse(String name) {
        return parse(name, '"', '"');
    }

    public static SQLName parseMS(String name) {
        // lucky for us, the rules are the same as for standard SQL
        return parse(name, '[', ']');
    }

    private static SQLName parse(String name, final char startQuote, final char endQuote) {
        name = name.trim();
        final List<String> res = new ArrayList<String>();
        int index = 0;
        while (index < name.length()) {
            final char c = name.charAt(index);
            final boolean inQuote = c == startQuote;
            if (inQuote) {
                // pass the opening quote
                index += 1;
                int index2 = findNextQuote(name, index, endQuote);
                // handle escaped "
                String part = "";
                // while the char after " is also "
                while ((index2 + 1) < name.length() && name.charAt(index2 + 1) == endQuote) {
                    // index2+1 to keep the first quote
                    part += name.substring(index, index2 + 1);
                    // pass ""
                    index = index2 + 2;
                    index2 = findNextQuote(name, index, endQuote);
                }
                part += name.substring(index, index2);
                res.add(part);
                // pass the closing quote
                index = index2 + 1;
            } else {
                final Matcher matcher = unquoted.matcher(name);
                if (!matcher.find(index))
                    throw new IllegalArgumentException("illegal unquoted name at " + index);
                final int index2 = matcher.end();
                res.add(name.substring(index, index2));
                index = index2;
            }
            if (index != name.length()) {
                if (name.charAt(index) != '.')
                    throw new IllegalArgumentException("no dot at " + index);
                if (index == name.length() - 1)
                    throw new IllegalArgumentException("trailing dot");
                // pass the dot
                index += 1;
            }
        }

        return new SQLName(res);
    }

    private static int findNextQuote(final String name, final int index, final char c) {
        final int res = name.indexOf(c, index);
        if (res < 0)
            throw new IllegalArgumentException("no corresponding quote " + index);
        return res;
    }

    private final List<String> items;

    public SQLName(String... items) {
        this(Arrays.asList(items));
    }

    /**
     * Create a new instance, ignoring null and empty items. Ignore <code>null</code> for systems
     * missing some JDBC level (e.g. MySQL has no schema), ignore "" for systems with private base
     * (e.g. in H2 the JDBC name is "", but cannot be used in SQL queries).
     * 
     * @param items the names.
     */
    public SQLName(List<String> items) {
        this(items, false);
    }

    private SQLName(List<String> items, final boolean safe) {
        super();
        if (safe) {
            this.items = items;
        } else {
            final List<String> tmp = new ArrayList<String>(items.size());
            for (final String item : items) {
                if (item != null && item.length() > 0)
                    tmp.add(item);
            }
            this.items = Collections.unmodifiableList(tmp);
        }
    }

    /**
     * Return the quoted form, eg for table.field : "table"."field".
     * 
     * @return the quoted form of this name.
     */
    public String quote() {
        return CollectionUtils.join(this.items, ".", new ITransformer<String, String>() {
            public String transformChecked(String input) {
                return SQLBase.quoteIdentifier(input);
            }
        });
    }

    public String quoteMS() {
        return CollectionUtils.join(this.items, ".", new ITransformer<String, String>() {
            public String transformChecked(String input) {
                return '[' + MS_END_QUOTE.matcher(input).replaceAll("]]") + ']';
            }
        });
    }

    /**
     * Return the item at the given index. You can use negatives to count backwards (ie -1 is the
     * last item).
     * 
     * @param index an int between 0 and count - 1, or between -count and -1.
     * @return the corresponding item.
     */
    public String getItem(int index) {
        if (index < 0)
            index = this.getItemCount() + index;
        return this.items.get(index);
    }

    /**
     * Same as getItem, but will return <code>null</code> if index is out of bound.
     * 
     * @param index an int.
     * @return the corresponding item, or <code>null</code>.
     */
    public String getItemLenient(int index) {
        try {
            return this.getItem(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getItemCount() {
        return this.items.size();
    }

    /**
     * The last part, eg "field" for table.field.
     * 
     * @return the name.
     */
    public String getName() {
        return this.getItem(this.items.size() - 1);
    }

    public String getFirst() {
        return this.getItem(0);
    }

    public SQLName getRest() {
        return new SQLName(this.items.subList(1, this.items.size()), true);
    }

    /**
     * Resolve the passed name from this.
     * 
     * @param to the name to resolve, e.g. "t2".
     * @return the resolved name, e.g. if this is "root"."t1", "root"."t2".
     */
    public final SQLName resolve(final SQLName to) {
        final SQLName from = this;
        final int fromCount = from.getItemCount();
        final int toCount = to.getItemCount();
        if (fromCount <= toCount) {
            return to;
        } else {
            final List<String> l = new ArrayList<String>(fromCount);
            l.addAll(from.asList().subList(0, fromCount - toCount));
            l.addAll(to.asList());
            return new SQLName(Collections.unmodifiableList(l), true);
        }
    }

    /**
     * The shortest SQLName to identify <code>to</code> from this.
     * 
     * @param to the name to shorten, e.g. "root"."t2".
     * @return the shortest name identifying <code>to</code>, e.g. if this is "root"."t1", "t2".
     */
    public final SQLName getContextualName(final SQLName to) {
        final SQLName from = this;
        final int fromCount = from.getItemCount();
        final int toCount = to.getItemCount();
        if (fromCount < toCount) {
            return to;
        } else if (fromCount > toCount) {
            final SQLName resolved = from.resolve(to);
            assert resolved.getItemCount() == fromCount;
            return from.getContextualName(resolved);
        }
        assert fromCount == toCount;
        final int common = CollectionUtils.equalsFromStart(from.asList(), to.asList());
        if (common == 0) {
            return to;
        } else {
            return new SQLName(to.asList().subList(common, toCount), true);
        }
    }

    public final List<String> asList() {
        return this.items;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SQLName) {
            final SQLName o = (SQLName) obj;
            return this.items.equals(o.items);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.items.hashCode();
    }

    public String toString() {
        return this.quote();
    }
}
