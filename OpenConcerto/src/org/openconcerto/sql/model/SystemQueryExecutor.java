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

import static java.util.Collections.singleton;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * To run queries on system tables from both {@link JDBCStructureSource} and {@link SQLTable}.
 * 
 * @author Sylvain
 */
abstract class SystemQueryExecutor {

    private final ITransformer<Tuple2<String, String>, SQLTable> tableFinder;

    public SystemQueryExecutor(final ITransformer<Tuple2<String, String>, SQLTable> tableFinder) {
        this.tableFinder = tableFinder;
    }

    /**
     * Return how to get the list of objects to apply.
     * 
     * @param b the base.
     * @param tables the tables by schema name.
     * @return a Throwable if there was an exception, a list to be returned, or a String to be
     *         executed.
     */
    protected abstract Object getQuery(final SQLBase b, TablesMap tables);

    protected abstract void apply(SQLTable t, Map o);

    /**
     * Execute this query on the passed schemas.
     * 
     * @param b the database.
     * @param newSchemas which schemas to use.
     * @throws QueryExn if {@link #getQuery(SQLBase, TablesMap)} returns a {@link Throwable}.
     */
    public final void apply(final SQLBase b, final Set<String> newSchemas) throws QueryExn {
        this.apply(b, TablesMap.createFromKeys(newSchemas));
    }

    public final void apply(final SQLBase b, final TablesMap tables) throws QueryExn {
        final Object sel = getQuery(b, tables);
        for (final Map m : exec(b, sel)) {
            final SQLTable newTable = getTable(m);
            // null means don't refresh
            if (newTable != null)
                apply(newTable, m);
        }
    }

    public final void apply(final SQLTable t) throws QueryExn {
        final String schemaName = t.getSchema().getName();
        final String tableName = t.getName();
        final Object sel = getQuery(t.getBase(), TablesMap.createFromTables(schemaName, singleton(tableName)));
        for (final Map m : exec(t.getBase(), sel)) {
            final Tuple2<String, String> newTable = createTuple(m);
            // only refresh t
            if (newTable.equals(Tuple2.create(schemaName, tableName)))
                apply(t, m);
        }
    }

    @SuppressWarnings("unchecked")
    private final List<Map> exec(final SQLBase b, Object sel) {
        if (sel instanceof Throwable)
            throw new QueryExn((Throwable) sel);
        else if (sel instanceof List)
            return (List<Map>) sel;
        else if (sel != null)
            // don't cache since we don't listen on system tables
            return (List<Map>) b.getDataSource().execute(sel.toString(), new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
        else
            return Collections.emptyList();
    }

    private final SQLTable getTable(final Map m) {
        return this.tableFinder.transformChecked(createTuple(m));
    }

    private Tuple2<String, String> createTuple(final Map m) {
        return Tuple2.create((String) m.get("TABLE_SCHEMA"), (String) m.get("TABLE_NAME"));
    }

    public class QueryExn extends RuntimeException {
        public QueryExn(Throwable cause) {
            super(cause);
        }
    }
}
