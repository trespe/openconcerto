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
 
 package org.openconcerto.erp.utils.correct;

import org.openconcerto.sql.changer.convert.MergeRows;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Merge KD clients.
 * 
 * @author Sylvain CUAZ
 */
public class MergeClients extends MergeRows {

    static private final Pattern punctAndSpacePattern = Pattern.compile("(\\p{Punct}|\\p{Space})+");
    static private final Pattern diacriticalPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static private final Pattern spacePattern = Pattern.compile("\\p{Blank}+");

    // if full is true "L'érable" = "Le rablé", otherwise "l.erable" != "le.rable"
    static private String normalize(String s, boolean full) {
        if (s == null)
            return null;

        s = diacriticalPattern.matcher(Normalizer.normalize(s.trim(), Form.NFD)).replaceAll("");
        return punctAndSpacePattern.matcher(s).replaceAll(full ? "" : ".").toLowerCase();
    }

    // replace tabs and multiple spaces by one space
    static private String removeBlanks(String s) {
        return spacePattern.matcher(s).replaceAll(" ").trim();
    }

    public MergeClients(DBSystemRoot b) {
        super(b);
        this.setFieldsToCompare(Arrays.asList("SIRET"));
        if (!b.isMappingAllRoots()) {
            b.mapAllRoots();
            try {
                b.reload();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected SQLRowValues createGraph(SQLTable t) {
        final SQLRowValues res = super.createGraph(t);
        // needed for mergeFields()
        res.setAllToNull();
        res.putRowValues("ID_ADRESSE").putNulls("VILLE", "RUE");
        return res;
    }

    @Override
    protected SQLRowValuesListFetcher createFetcher(final SQLTable t) {
        final SQLRowValuesListFetcher res = super.createFetcher(t);
        res.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                return input.andWhere(Where.createRaw("length(trim( both from " + t.getField("SIRET").getFieldRef() + ")) > 0"));
            }
        });
        return res;
    }

    @Override
    protected Object normalize(String fieldName, Object o) {
        return normalize((String) o, fieldName.equals("SIRET"));
    }

    @Override
    protected boolean shouldMerge(SQLRowValues r1, SQLRowValues r2) {
        // login can be empty but it must match
        final boolean superRes = super.shouldMerge(r1, r2) && isEqualValue(r1, r2, "EXTRANET_LOGIN", false);
        if (!superRes)
            return superRes;
        // same non empty SIRET

        if (isEqualNonEmptyValue(r1, r2, "TEL"))
            return true;

        final SQLRowAccessor addr1 = r1.getForeign("ID_ADRESSE");
        final SQLRowAccessor addr2 = r2.getForeign("ID_ADRESSE");
        return isEqualNonEmptyValue(addr1, addr2, "VILLE") && isEqualValue(addr1, addr2, "RUE", false);
    }

    @Override
    protected void mergeFields(SQLRowValues emptyUpdateRow, List<SQLRow> rows) {
        final SQLRow dest = rows.get(0);
        final SQLTable t = emptyUpdateRow.getTable();
        // getField() to make sure field exists
        final SQLField dontMerge = t.getField("CODE");
        final Set<SQLField> fks = t.getForeignKeys();
        for (final SQLField f : t.getContentFields()) {
            if (f.equals(dontMerge))
                continue;
            final String fname = f.getName();
            Object mergedValue = null;
            boolean toUpdate = false;

            if (String.class.isAssignableFrom(f.getType().getJavaType())) {
                // longest value by normalized value (e.g. if one row contains "ABC" and "AB,C" use
                // the second one)
                final Map<String, String> m = new HashMap<String, String>();
                for (final SQLRow r : rows) {
                    String val = r.getString(fname);
                    if (val != null) {
                        val = removeBlanks(val);
                        final String normalized = (String) this.normalize(fname, val);
                        if (normalized.length() > 0) {
                            final String existing = m.get(normalized);
                            if (existing == null || val.length() > existing.length()) {
                                m.put(normalized, val);
                            }
                        }
                    }
                }
                // concatenate values
                mergedValue = CollectionUtils.join(m.values(), " ; ");
                // since we put val.trim() the row must be updated even if contained val
                toUpdate = !CompareUtils.equals(dest.getObject(fname), mergedValue);
            } else if (fks.contains(f)) {
                // use first non-empty foreign row
                for (final SQLRow r : rows) {
                    if (!r.isForeignEmpty(fname)) {
                        mergedValue = r.getObject(fname);
                        toUpdate = r != dest;
                        break;
                    }
                }
            } else {
                // use first non-null value
                for (final SQLRow r : rows) {
                    final Object fValue = r.getObject(fname);
                    if (fValue != null) {
                        mergedValue = fValue;
                        toUpdate = r != dest;
                        break;
                    }
                }
            }

            if (toUpdate)
                emptyUpdateRow.put(fname, mergedValue);
        }
    }
}
