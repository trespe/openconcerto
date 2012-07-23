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

    private final Set<String> toRefresh;
    private final SQLBase base;
    private boolean preVerify;
    private Map<String, SQLSchema> newStructure;
    // schemas loaded by another StructureSource
    private final boolean hasExternalStruct;
    private final Map<String, SQLSchema> externalStruct;

    public StructureSource(SQLBase b, Set<String> toRefresh) {
        this(b, toRefresh, null);
    }

    public StructureSource(SQLBase b, Set<String> toRefresh, Map<String, SQLSchema> externalStruct) {
        super();
        this.base = b;
        this.toRefresh = toRefresh;
        this.preVerify = false;
        this.newStructure = null;
        this.hasExternalStruct = externalStruct != null;
        this.externalStruct = externalStruct == null ? Collections.<String, SQLSchema> emptyMap() : externalStruct;
    }

    final Set<String> getToRefresh() {
        return this.toRefresh;
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
            if (this.preVerify) {
                this.newStructure = this.createTables();
                this.fillTables(this.newStructure.keySet());
            }
        } catch (Exception e) {
            throw new PrechangeException(e);
        }
    }

    final Map<String, SQLSchema> getNewStructure() {
        return this.newStructure;
    }

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

    // scope = toRefresh ∩ rootsToMap
    public final boolean isInTotalScope(String schemaName) {
        return this.getBase().getDBSystemRoot().createNode(this.getBase(), schemaName) && (this.toRefresh == null || this.toRefresh.contains(schemaName));
    }

    // only keep schemas that we must load (ie exclude out of scope and already loaded in
    // externalStruct)
    protected final void filterOutOfScope(Collection<String> schemas) {
        final Iterator<String> iter = schemas.iterator();
        while (iter.hasNext()) {
            final String schema = iter.next();
            if (!this.isInTotalScope(schema) || this.externalStruct.containsKey(schema))
                iter.remove();
        }
    }

    // existing schemas to refresh = toRefresh ∩ childrenNames
    public final Set<String> getSchemasToRefresh() {
        return CollectionUtils.inter(this.getBase().getChildrenNames(), this.toRefresh);
    }

    // existing tables to refresh = tables of getSchemasToRefresh()
    public final Set<SQLName> getTablesToRefresh() {
        if (this.toRefresh == null)
            return this.getBase().getAllTableNames();
        else {
            final Set<SQLName> res = new HashSet<SQLName>();
            for (final String schemaName : this.getSchemasToRefresh()) {
                final SQLSchema schema = this.getBase().getSchema(schemaName);
                for (final SQLTable t : schema.getTables()) {
                    res.add(t.getSQLName(schema));
                }
            }
            return res;
        }
    }

    // schemas of this source proper
    public abstract Set<String> getSchemas();

    public abstract Set<SQLName> getTablesNames();

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
        if (!this.preVerify)
            this.fillTables(this.getSchemas());
        else {
            mutateTo(this.newStructure);
        }
        mutateTo(this.externalStruct);
    }

    private final void mutateTo(final Map<String, SQLSchema> m) {
        for (final String schemaName : m.keySet()) {
            final SQLSchema s = this.getBase().getSchema(schemaName);
            final SQLSchema newSchema = m.get(s.getName());
            s.mutateTo(newSchema);
        }
    }

    protected final SQLSchema getNewSchema(String name) {
        if (!this.preVerify)
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

    protected abstract void fillTables(Set<String> newSchemas) throws E;

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
