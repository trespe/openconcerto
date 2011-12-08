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
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixSerial extends Changer<SQLTable> {

    // nextVal('"SCHEMA"."seqName"'::regclass);
    private final static Pattern seqPattern = Pattern.compile("nextval\\('(.+)'.*\\)");

    public FixSerial(DBSystemRoot b) {
        super(b);
    }

    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.POSTGRESQL);
    }

    /**
     * Set the current value of the sequence for the primary key to the max of the primary key.
     * Useful after importing rows without using the default value for the primary key (thus not
     * updating the sequence).
     * 
     * @param t the table to fix.
     */
    protected void changeImpl(SQLTable t) {
        getStream().print(t + "... ");
        if (!t.isRowable()) {
            getStream().println("not rowable");
        } else {
            final String seqName = getPrimaryKeySeq(t);
            if (seqName != null) {
                final SQLSelect sel = new SQLSelect(t.getBase(), true);
                sel.addSelect(t.getKey(), "max");
                final Number maxID = (Number) this.getDS().executeScalar(sel.asString());
                // begin at 1 if table is empty
                final long nextID = maxID == null ? 1 : maxID.longValue() + 1;
                // for some reason this doesn't always work (maybe a cache pb ?):
                // final String s = "SELECT setval('" + seqName + "', " + maxID + ")";
                // while this does
                final String s = "ALTER SEQUENCE " + seqName + " RESTART " + nextID;
                this.getDS().executeScalar(s);
                getStream().println("done");
            } else
                getStream().println("no sequence: " + t.getKey().getDefaultValue());
        }
    }

    static public String getPrimaryKeySeq(SQLTable t) throws IllegalStateException {
        if (!t.isRowable()) {
            return null;
        } else {
            return getSeq(t.getKey());
        }
    }

    /**
     * Return the name of the sequence for the default value of <code>f</code>.
     * 
     * @param f a field.
     * @return the name of the sequence or <code>null</code> if the default is not a sequence, eg
     *         "schema"."table_id_seq".
     * @throws IllegalStateException if the sequence couldn't be parsed.
     */
    static public String getSeq(SQLField f) throws IllegalStateException {
        if (f == null) {
            return null;
        } else {
            final String def = ((String) f.getDefaultValue()).trim();
            if (def.startsWith("nextval")) {
                final Matcher matcher = seqPattern.matcher(def);
                if (matcher.matches()) {
                    return matcher.group(1);
                } else
                    throw new IllegalStateException("could not parse: " + def + " with " + seqPattern.pattern());
            } else
                return null;
        }
    }

}
