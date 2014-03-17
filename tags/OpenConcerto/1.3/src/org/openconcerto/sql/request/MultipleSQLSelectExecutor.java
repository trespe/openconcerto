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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.utils.SQLUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

public class MultipleSQLSelectExecutor {
    private DBSystemRoot root;

    private List<ResultSetHandler> handlers;
    private List<SQLSelect> queries;

    public MultipleSQLSelectExecutor(DBSystemRoot root, List<SQLSelect> queries) {

        this.queries = queries;
        this.root = root;
        final int size = queries.size();
        handlers = new ArrayList<ResultSetHandler>(size);

        for (int i = 0; i < size; i++) {
            this.handlers.add(SQLRowListRSH.createFromSelect(queries.get(i)));
        }

    }

    public List<List<SQLRow>> execute() throws SQLException {
        final List<String> req = new ArrayList<String>(queries.size());
        for (SQLSelect query : queries) {
            req.add(query.asString());
        }

        @SuppressWarnings("unchecked")
        final List<List<SQLRow>> result = (List<List<SQLRow>>) SQLUtils.executeMultiple(root, req, handlers);
        return result;
    }
}
