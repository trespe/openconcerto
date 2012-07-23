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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.map.LazyMap;

/**
 * A class to help find fields by their fullname in resultset. Some jdbc drivers only accept the
 * short name of fields, eg you execute "select B.DESIGNATION from BATIMENT B" but you can only
 * access the designation with "DESIGNATION". With this class you can ask the index of
 * "BATIMENT.DESIGNATION".
 * 
 * @author Sylvain
 */
public final class ResultSetFullnameHelper {
    private final ResultSet delegate;
    private ResultSetMetaData rsMD;
    private final Map tablesMap;

    public ResultSetFullnameHelper(ResultSet rs) {
        this.delegate = rs;
        this.tablesMap = LazyMap.decorate(new HashMap(), new Factory() {
            public Object create() {
                return new HashMap();
            }
        });
        this.rsMD = null;
    }

    public final ResultSet getRS() {
        return this.delegate;
    }

    private final ResultSetMetaData getMetaData() throws SQLException {
        if (this.rsMD == null) {
            this.rsMD = this.delegate.getMetaData();
        }
        return this.rsMD;
    }

    public final int getIndex(SQLField f) throws SQLException {
        return this.getIndex(f.getTable().getName(), f.getName());
    }

    public final int getIndex(String fieldFullName) throws SQLException {
        final SQLName names = SQLName.parse(fieldFullName);
        return this.getIndex(names.getItem(-2), names.getName());
    }

    /**
     * Get the index of the specified field.
     * 
     * @param tableName the table of the field, eg "SITE".
     * @param fieldName the name of the field, eg "DESIGNATION".
     * @return the index of the field, or -1 if not found.
     * @throws SQLException if an error occur while retrieving metadata.
     */
    public final int getIndex(String tableName, String fieldName) throws SQLException {
        final Map m = (Map) this.tablesMap.get(tableName);
        if (!m.containsKey(fieldName)) {
            final int index = this.searchIndex(tableName, fieldName);
            m.put(fieldName, index < 1 ? null : new Integer(index));
        }
        final Integer val = (Integer) m.get(fieldName);
        return val == null ? -1 : val.intValue();
    }

    private final int searchIndex(String tableName, String fieldName) throws SQLException {
        for (int i = 1; i <= this.getMetaData().getColumnCount(); i++) {
            final String colName = this.getMetaData().getColumnName(i);
            if (colName.equals(fieldName) && this.getMetaData().getTableName(i).equals(tableName)) {
                return i;
            }
        }
        return -1;
    }

}
