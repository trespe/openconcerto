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
 
 package org.openconcerto.sql.changer.convert;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * This will convert every TEXT field to a varchar of appropriate size (if it fails, it won't erase
 * data, and will let you know). Now (5.0) that MySQL supports large varchars, they can be used
 * everywhere TEXT had to. TEXT fields have several weaknesses, like not supporting defaults.
 * 
 * @author Sylvain
 */
public class TextToVarChar extends Changer<SQLTable> {

    /** If set, change fields to NOT NULL DEFAULT '' */
    public static final String CHANGE_NULLABLE_AND_DEFAULT = "org.openconcerto.sql.changer.changeNullableAndDefault";

    private static final Pattern textType = Pattern.compile(".*text", Pattern.CASE_INSENSITIVE);

    private EnumSet<Properties> toChange;

    public TextToVarChar(DBSystemRoot b) {
        super(b);
        // by default change only type
        this.toChange = EnumSet.of(Properties.TYPE);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        // those are tested to not loose data when changing type.
        return EnumSet.of(SQLSystem.MYSQL, SQLSystem.POSTGRESQL, SQLSystem.H2);
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        if (Boolean.getBoolean(CHANGE_NULLABLE_AND_DEFAULT))
            this.toChange = EnumSet.allOf(Properties.class);
    }

    @Override
    protected void changeImpl(SQLTable t) throws SQLException {
        if (t.getServer().getSQLSystem() == SQLSystem.MYSQL) {
            final String modes = (String) this.getDS().executeScalar("SELECT @@global.sql_mode;");
            if (!modes.contains("STRICT_ALL_TABLES") && !modes.contains("STRICT_TRANS_TABLES"))
                throw new IllegalStateException("must run in STRICT mode (eg TRADITIONAL), current modes: " + modes);
        }
        // pg and h2 are always strict
        for (final SQLField f : t.getFields()) {
            if (f.getType().getTypeName() != null && (f.getType().getType() == Types.CLOB || textType.matcher(f.getType().getTypeName()).matches())) {
                int size = f.getType().getSize();
                // heuristic, but we're safe since STRICT mode is on
                if (size > 2048)
                    size = 2048;
                // '' default since MySQL don't support them for TEXT
                final AlterTable alterTable = new AlterTable(t);
                alterTable.alterColumn(f.getName(), this.toChange, " varchar(" + size + ")", "''", false);
                final String req = alterTable.asString();
                System.err.println(req);
                this.getDS().execute(req);
            }
        }
    }

}
