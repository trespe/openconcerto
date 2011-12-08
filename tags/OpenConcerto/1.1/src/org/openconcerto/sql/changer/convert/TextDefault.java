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
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Set '' as the default for non nullable String fields with a null default. ATTN mysql 5.0 doesn't
 * support TEXT defaults, use textToVarChar first.
 * 
 * @author Sylvain
 */
public class TextDefault extends Changer<SQLTable> {

    public TextDefault(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.MYSQL);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void changeImpl(SQLTable t) throws SQLException {
        final String infoSchema = SQLSelect.quote("SELECT TABLE_NAME, COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE from information_schema.COLUMNS where TABLE_SCHEMA=%s and TABLE_NAME=%s", t.getBase()
                .getName(), t.getName());
        final Map<String, Object> defaults = (Map<String, Object>) this.getDS().execute(infoSchema, new ResultSetHandler() {
            public Object handle(ResultSet rs) throws SQLException {
                final Map<String, Object> res = new HashMap<String, Object>();
                while (rs.next()) {
                    res.put(rs.getString("COLUMN_NAME"), rs.getObject("COLUMN_DEFAULT"));
                }
                return res;
            }
        });
        for (final SQLField f : t.getFields()) {
            if (f.getType().getJavaType().equals(String.class) && Boolean.FALSE.equals(f.isNullable()) && defaults.get(f.getName()) == null) {
                final String req = "ALTER TABLE " + SQLBase.quoteIdentifier(t.getName()) + " MODIFY COLUMN " + SQLBase.quoteIdentifier(f.getName()) + " " + f.getType().getTypeName() + "("
                        + f.getType().getSize() + ") NOT NULL DEFAULT ''";
                System.err.println(req);
                this.getDS().execute(req);
            }
        }
    }

}
