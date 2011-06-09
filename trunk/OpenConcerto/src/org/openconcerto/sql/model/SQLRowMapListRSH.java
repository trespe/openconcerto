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
 
 /**
 * 
 */
package org.openconcerto.sql.model;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Creates a list of Map. This is obviously limited since it cannot be used with requests that
 * reference the same table more than once.
 * 
 * @author Sylvain CUAZ
 * @deprecated use {@link SQLRowValuesListFetcher}
 */
public final class SQLRowMapListRSH implements ResultSetHandler {

    /**
     * Renvoie une liste de Map.
     * 
     * @param tables les tables qui nous intéressent.
     * @param rs le resultSet à parcourir.
     * @return une List de Map de SQLRow, dont les clefs sont tables.
     * @throws SQLException si pb SQL.
     */
    static final List<Map<SQLTable, SQLRow>> createListFromRS(Set<SQLTable> tables, ResultSet rs) throws SQLException {
        final ResultSetMetaData rsmd = rs.getMetaData();
        final List<Map<SQLTable, SQLRow>> res = new ArrayList<Map<SQLTable, SQLRow>>();
        while (rs.next()) {
            final Map<SQLTable, SQLRow> rows = new HashMap<SQLTable, SQLRow>();
            for (final SQLTable table : tables) {
                // we cannot know if rs only contains 1 table since we didn't create it
                rows.put(table, SQLRow.createFromRS(table, rs, rsmd, false));
            }
            res.add(rows);
        }
        return res;
    }

    private final Set<SQLTable> t;

    public SQLRowMapListRSH(Set<SQLTable> tables) {
        super();
        this.t = tables;
    }

    public Object handle(ResultSet rs) throws SQLException {
        return createListFromRS(this.t, rs);
    }
}
