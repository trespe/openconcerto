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

/**
 * A dotted SQL name, eg "table.field" or "schema.table".
 * 
 * @author Sylvain
 */
public final class SQLName {

    private static final Pattern unquoted = Pattern.compile("\\w+");

    /**
     * Parse a possibly quoted string to an SQL name.
     * 
     * @param name a String, eg public."ta.ble seq".
     * @return the corresponding SQL name, eg "public"."ta.ble seq".
     */
    public static SQLName parse(String name) {
        name = name.trim();
        final List<String> res = new ArrayList<String>();
        int index = 0;
        while (index < name.length()) {
            final char c = name.charAt(index);
            final boolean inQuote = c == '"';
            if (inQuote) {
                // pass the opening quote
                index += 1;
                int index2 = findNextQuote(name, index);
                // handle escaped "
                String part = "";
                // while the char after " is also "
                while ((index2 + 1) < name.length() && name.charAt(index2 + 1) == '"') {
                    // index2+1 to keep the first quote
                    part += name.substring(index, index2 + 1);
                    // pass ""
                    index = index2 + 2;
                    index2 = findNextQuote(name, index);
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

    private static int findNextQuote(final String name, final int index) {
        final int res = name.indexOf('"', index);
        if (res < 0)
            throw new IllegalArgumentException("no corresponding quote " + index);
        return res;
    }

    final List<String> items;

    public SQLName(String... items) {
        this(Arrays.asList(items));
    }

    /**
     * Create a new instance, ignoring null items.
     * 
     * @param items the names.
     */
    public SQLName(List<String> items) {
        super();
        this.items = new ArrayList<String>(items.size());
        for (final String item : items) {
            if (item != null)
                this.items.add(item);
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
        return new SQLName(this.items.subList(1, this.items.size()));
    }

    public final List<String> asList() {
        return Collections.unmodifiableList(this.items);
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
