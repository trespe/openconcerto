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

import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IncludeExclude;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allow to get the content of an SQLBase (schemas, tables, fields).
 * 
 * @author Sylvain
 * @see #init()
 * @see #fillTables()
 * @param <E> the type of exception fillTables() might throw.
 */
public abstract class StructureSource<E extends Exception> {

    private final TablesMap toRefresh;
    private final SQLBase base;
    private boolean preVerify;
    private Map<String, SQLSchema> newStructure;
    // schemas loaded by another StructureSource
    private final boolean hasExternalStruct;
    private final Map<String, SQLSchema> externalStruct;
    // each table has a version, thus externalStruct can contain a subset of toRefresh
    private final Set<String> externalOutOfDateSchemas;

    public StructureSource(SQLBase b, TablesMap toRefresh) {
        this(b, toRefresh, null, null);
    }

    public StructureSource(SQLBase b, TablesMap toRefresh, Map<String, SQLSchema> externalStruct, Set<String> externalOutOfDateSchemas) {
        super();
        this.base = b;
        this.toRefresh = TablesMap.create(toRefresh);
        if (this.toRefresh != null)
            this.toRefresh.removeAllEmptyCollections();
        this.preVerify = false;
        this.newStructure = null;
        this.hasExternalStruct = externalStruct != null;
        this.externalStruct = externalStruct == null ? Collections.<String, SQLSchema> emptyMap() : externalStruct;
        if (externalOutOfDateSchemas == null) {
            if (externalStruct != null)
                throw new NullPointerException("Null out of date");
            else
                externalOutOfDateSchemas = Collections.emptySet();
        }
        this.externalOutOfDateSchemas = externalOutOfDateSchemas;
    }

    final TablesMap getToRefresh() {
        return this.toRefresh;
    }

    final Set<String> getSchemasToRefresh() {
        // OK since we called removeAllEmptyCollections() in the constructor
        return this.getToRefresh() == null ? null : this.getToRefresh().keySet();
    }

    final Set<String> getTablesToRefresh(final String schema) {
        if (this.getToRefresh() == null) {
            return null;
        } else {
            // null result is null value (i.e. refresh all) not absence of key
            assert this.getToRefresh().containsKey(schema);
            return this.getToRefresh().get(schema);
        }
    }

    final boolean hasExternalStruct() {
        return this.hasExternalStruct;
    }

    /**
     * Should be called before any use of this instance. This loads the schema & table names.
     * Furthermore if this should be preverified, the whole source is read to construct an in-memory
     * representation.
     * 
     * @throws PrechangeException if an error occurs.
     * @see #fillTables()
     */
    public void init() throws PrechangeException {
        try {
            this.getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, E>() {
                @Override
                public Object handle(SQLDataSource ds) throws E {
                    getNames(ds.getConnection());
                    return null;
                }
            });
            if (this.isPreVerify()) {
                this.newStructure = this.createTables();
                this.fillTablesP();
            }
        } catch (Exception e) {
            throw new PrechangeException(e);
        }
    }

    final Map<String, SQLSchema> getNewStructure() {
        return this.newStructure;
    }

    // schemas in getNewStructure() that haven't all asked for tables
    abstract Set<String> getOutOfDateSchemas();

    private final Map<String, SQLSchema> createTables() {
        final Map<String, SQLSchema> res = new HashMap<String, SQLSchema>();
        final Set<String> newSchemas = this.getSchemas();
        for (final String schemaName : newSchemas) {
            final SQLSchema schema = new SQLSchema(this.getBase(), schemaName);
            res.put(schemaName, schema);
        }

        // refresh tables
        final Set<SQLName> newTableNames = this.getTablesNames();
        // create new table descendants (including empty tables)
        for (final SQLName tableName : newTableNames) {
            final SQLSchema s = res.get(tableName.getItemLenient(-2));
            s.addTableWithoutSysRootLock(tableName.getName());
        }
        return res;
    }

    protected abstract void getNames(final Connection c) throws E;

    public final SQLBase getBase() {
        return this.base;
    }

    // DatabaseMetaData param to avoid re-asking it
    protected final Set<String> getJDBCSchemas(final DatabaseMetaData metaData) throws SQLException {
        // getSchemas(this.getBase().getMDName(), null) not implemented by pg
        final Set<String> res = new HashSet<String>((List) SQLDataSource.COLUMN_LIST_HANDLER.handle(metaData.getSchemas()));
        // if db does not support schemas
        if (res.isEmpty() && !this.getBase().getServer().getSQLSystem().getLevels().contains(HierarchyLevel.SQLSCHEMA))
            res.add(null);
        return res;
    }

    /**
     * Whether to preverify the structure.
     * 
     * @param preVerify <code>true</code> if this source should be preverified.
     * @see #init()
     */
    public final void setPreVerify(boolean preVerify) {
        this.preVerify = preVerify;
    }

    public final boolean isPreVerify() {
        return this.preVerify;
    }

    // scope = toRefresh ∩ rootsToMap
    public final boolean isInTotalScope(String schemaName) {
        return this.getBase().getDBSystemRoot().createNode(this.getBase(), schemaName) && (this.toRefresh == null || this.getSchemasToRefresh().contains(schemaName));
    }

    // only keep schemas that we must load (ie exclude out of scope and already loaded in
    // externalStruct)
    protected final void filterOutOfScope(Collection<String> schemas) {
        final Iterator<String> iter = schemas.iterator();
        while (iter.hasNext()) {
            final String schema = iter.next();
            if (!this.isInScope(schema))
                iter.remove();
        }
    }

    protected final boolean isInScope(String schema) {
        return this.isInTotalScope(schema) && (!this.externalStruct.containsKey(schema) || this.externalOutOfDateSchemas.contains(schema));
    }

    final IncludeExclude<String> getTablesInScope(final String schema) {
        assert isInScope(schema) : "Schema " + schema + " not in scope and this method doesn't check it";
        final Set<String> includes = getTablesToRefresh(schema);
        final Set<String> excludes;
        if (this.hasExternalStruct()) {
            final SQLSchema externalSchema = this.externalStruct.get(schema);
            assert (externalSchema == null) == !this.externalStruct.containsKey(schema) : "externalStruct has a null value";
            excludes = externalSchema == null ? Collections.<String> emptySet() : externalSchema.getChildrenNames();
        } else {
            excludes = Collections.<String> emptySet();
        }
        return IncludeExclude.getNormalized(includes, excludes);
    }

    // existing schemas to refresh = toRefresh ∩ childrenNames
    public final Set<String> getExistingSchemasToRefresh() {
        return CollectionUtils.inter(this.getBase().getChildrenNames(), this.getSchemasToRefresh());
    }

    // existing tables to refresh
    public final Set<SQLName> getExistingTablesToRefresh() {
        if (this.toRefresh == null)
            return this.getBase().getAllTableNames();
        else {
            final Set<SQLName> res = new HashSet<SQLName>();
            for (final String schemaName : this.getExistingSchemasToRefresh()) {
                final Set<String> tablesToRefresh = this.getToRefresh().get(schemaName);
                final SQLSchema schema = this.getBase().getSchema(schemaName);
                for (final SQLTable t : schema.getTables()) {
                    if (tablesToRefresh == null || tablesToRefresh.contains(t.getName()))
                        res.add(t.getSQLName(schema));
                }
            }
            return res;
        }
    }

    // schemas of this source proper
    public abstract Set<String> getSchemas();

    public abstract Set<SQLName> getTablesNames();

    public final TablesMap getTablesMap() {
        return this.getTablesMap(false);
    }

    protected final TablesMap getTablesMap(final boolean includeEmptySchemas) {
        final TablesMap res = new TablesMap();
        if (includeEmptySchemas) {
            for (final String s : getSchemas()) {
                res.put(s, Collections.<String> emptySet());
            }
        }
        for (final SQLName table : getTablesNames()) {
            res.add(table.getItemLenient(-2), table.getName());
        }
        return res;
    }

    // schemas that will be filled by fillTables()
    // ie ours + externalStruct
    public final Set<String> getTotalSchemas() {
        return CollectionUtils.union(this.getSchemas(), this.externalStruct.keySet());
    }

    public final Set<SQLName> getTotalTablesNames() {
        final Set<SQLName> res = new HashSet<SQLName>(this.getTablesNames());
        for (final SQLSchema schema : this.externalStruct.values()) {
            for (final SQLTable t : schema.getTables()) {
                res.add(t.getSQLName(schema));
            }
        }
        return res;
    }

    /**
     * Fill the tables of our base. Our base's schemas and tables must match ours before this method
     * is called. If preverify is false, this read the source and change our base as it goes.
     * Otherwise, our base is mutated to our previous in-memory structure.
     * 
     * @throws E if an error occurs.
     */
    public final void fillTables() throws E {
        assert Thread.holdsLock(this.getBase().getDBSystemRoot().getTreeMutex());
        // externalStruct was created before this, so it must be applied first
        // (e.g. it might have an old schema version, while we have one up to date)
        mutateTo(this.externalStruct);
        if (!this.isPreVerify())
            this.fillTablesP();
        else {
            mutateTo(this.newStructure);
        }
    }

    private final void mutateTo(final Map<String, SQLSchema> m) {
        for (final String schemaName : m.keySet()) {
            final SQLSchema s = this.getBase().getSchema(schemaName);
            final SQLSchema newSchema = m.get(s.getName());
            s.mutateTo(newSchema);
        }
    }

    protected final SQLSchema getNewSchema(String name) {
        if (!this.isPreVerify())
            return this.getBase().getSchema(name);
        else {
            return this.newStructure.get(name);
        }
    }

    protected final SQLTable getNewTable(String schemaName, String name) {
        final SQLSchema newSchema = this.getNewSchema(schemaName);
        if (newSchema == null)
            return null;
        else
            return newSchema.getTable(name);
    }

    protected final void fillTablesP() throws E {
        // needs all schemas since even empty ones must be filled (e.g. schema version, procedures)
        this.fillTables(getTablesMap(true));
    }

    protected abstract void fillTables(TablesMap newSchemas) throws E;

    // save the new content if necessary
    public abstract void save();

    public static final class PrechangeException extends RuntimeException {
        public PrechangeException(Throwable cause) {
            super(cause);
        }

        public PrechangeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
