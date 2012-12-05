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
 
 package org.openconcerto.sql.preferences;

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.replication.MemoryRep;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.RTInterruptedException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Preferences backed by SQL tables.
 * 
 * @author Sylvain CUAZ
 */
public class SQLPreferences extends AbstractPreferences {

    private static final String PREF_NODE_TABLENAME = "PREF_NODE";
    private static final String PREF_VALUE_TABLENAME = "PREF_VALUE";

    static public SQLTable getPrefTable(final DBRoot root) throws SQLException {
        if (!root.contains(PREF_VALUE_TABLENAME)) {
            final SQLCreateTable createNodeT = new SQLCreateTable(root, PREF_NODE_TABLENAME);
            // don't need ORDER and ARCHIVE
            createNodeT.setPlain(true);
            createNodeT.addColumn(SQLSyntax.ID_NAME, createNodeT.getSyntax().getPrimaryIDDefinition());
            // cannot use addForeignColumn() since it's a self-reference
            createNodeT.addColumn("ID_PARENT", createNodeT.getSyntax().getIDType() + " NULL");
            createNodeT.addVarCharColumn("NAME", Preferences.MAX_NAME_LENGTH);

            createNodeT.addForeignConstraint("ID_PARENT", new SQLName(createNodeT.getName()), SQLSyntax.ID_NAME);
            createNodeT.addUniqueConstraint("uniqNamePerParent", Arrays.asList("ID_PARENT", "NAME"));

            final SQLCreateTable createValueT = new SQLCreateTable(root, PREF_VALUE_TABLENAME);
            createValueT.setPlain(true);
            createValueT.addColumn("ID_NODE", createValueT.getSyntax().getIDType() + " NOT NULL");
            createValueT.addVarCharColumn("NAME", Preferences.MAX_KEY_LENGTH);
            createValueT.addVarCharColumn("VALUE", Preferences.MAX_VALUE_LENGTH);
            // unique name per node
            createValueT.setPrimaryKey("ID_NODE", "NAME");
            createValueT.addForeignConstraint("ID_NODE", new SQLName(createNodeT.getName()), SQLSyntax.ID_NAME);

            root.createTables(createNodeT, createValueT);
        }
        return root.getTable(PREF_VALUE_TABLENAME);
    }

    private static final Map<DBRoot, SQLPreferences> memCachedbyRoots = new IdentityHashMap<DBRoot, SQLPreferences>(8);

    static public SQLPreferences startMemCached(final DBRoot root) throws SQLException {
        return startMemCached(root, 8, TimeUnit.MINUTES, false);
    }

    static public SQLPreferences startMemCached(final DBRoot root, final long period, final TimeUnit unit, final boolean returnExisting) throws SQLException {
        synchronized (memCachedbyRoots) {
            if (!memCachedbyRoots.containsKey(root)) {
                final MemoryRep memoryRep = new MemoryRep(root.getDBSystemRoot(), TablesMap.createFromTables(root.getName(), Arrays.asList(PREF_NODE_TABLENAME, PREF_VALUE_TABLENAME)));
                try {
                    memoryRep.start(period, unit);
                } catch (InterruptedException e) {
                    throw new RTInterruptedException(e);
                } catch (Exception e) {
                    throw new SQLException(e);
                }
                final SQLPreferences res = new SQLPreferences(memoryRep);
                memCachedbyRoots.put(root, res);
                return res;
            } else if (!returnExisting) {
                throw new IllegalStateException("Preferences already created for " + root);
            } else {
                return memCachedbyRoots.get(root);
            }
        }
    }

    static public SQLPreferences getMemCached(final DBRoot root) {
        final SQLPreferences res;
        synchronized (memCachedbyRoots) {
            res = memCachedbyRoots.get(root);
        }
        if (res == null)
            throw new IllegalStateException("No preferences for " + root);
        return res;
    }

    private final SQLTable prefRT, prefWT;
    private final SQLTable nodeRT, nodeWT;
    private final MemoryRep rep;
    // values from the DB
    private Map<String, String> values;
    // values changed in-memory (not yet committed)
    private final Map<String, String> changedValues;
    // values removed in-memory (not yet committed)
    private final Set<String> removedKeys;
    private SQLRow node;

    // root node
    public SQLPreferences(DBRoot db) {
        this(null, db.getTable(PREF_VALUE_TABLENAME), db.getTable(PREF_VALUE_TABLENAME), db.getTable(PREF_NODE_TABLENAME), db.getTable(PREF_NODE_TABLENAME));
    }

    // root node
    private SQLPreferences(final MemoryRep rep) {
        this(rep, rep.getSlaveTable(PREF_VALUE_TABLENAME), rep.getMasterTable(PREF_VALUE_TABLENAME), rep.getSlaveTable(PREF_NODE_TABLENAME), rep.getMasterTable(PREF_NODE_TABLENAME));
    }

    // canonical root node
    private SQLPreferences(final MemoryRep rep, SQLTable prefRT, SQLTable prefWT, SQLTable nodeRT, SQLTable nodeWT) {
        this(null, "", rep, prefRT, prefWT, nodeRT, nodeWT);
    }

    // child node
    private SQLPreferences(SQLPreferences parent, String name) {
        this(parent, name, parent.rep, parent.prefRT, parent.prefWT, parent.nodeRT, parent.nodeWT);
    }

    // canonical constructor
    private SQLPreferences(SQLPreferences parent, String name, final MemoryRep rep, SQLTable prefRT, SQLTable prefWT, SQLTable nodeRT, SQLTable nodeWT) {
        super(parent, name);

        this.prefRT = prefRT;
        this.prefWT = prefWT;
        this.nodeRT = nodeRT;
        this.nodeWT = nodeWT;
        this.rep = rep;

        this.values = null;
        this.changedValues = new HashMap<String, String>();
        this.removedKeys = new HashSet<String>();
        this.node = null;
    }

    private final SQLTable getNodeRT() {
        return this.nodeRT;
    }

    private final SQLTable getPrefRT() {
        return this.prefRT;
    }

    private final SQLTable getNodeWT() {
        return this.nodeWT;
    }

    private final SQLTable getPrefWT() {
        return this.prefWT;
    }

    private final SQLDataSource getReadDS() {
        return this.getPrefRT().getDBSystemRoot().getDataSource();
    }

    private final SQLDataSource getWriteDS() {
        return this.getPrefWT().getDBSystemRoot().getDataSource();
    }

    private Object execute(final String sel, ResultSetHandler rsh) {
        if (this.rep != null) {
            try {
                this.rep.waitOnLastManualFuture();
            } catch (InterruptedException e) {
                throw new RTInterruptedException(e);
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }
        // don't use the cache, our superclass and us have already a cache system
        // plus this would require to fire when modifying the table
        return getReadDS().execute(sel, new IResultSetHandler(rsh, false));
    }

    // we changed the master or we want to be up to date
    private final void replicate() {
        if (this.rep != null) {
            this.rep.submitReplicate();
        }
    }

    private final LinkedList<SQLPreferences> getAncestors() {
        final LinkedList<SQLPreferences> res = new LinkedList<SQLPreferences>();
        res.add(this);
        SQLPreferences current = this;
        SQLPreferences p;
        while ((p = (SQLPreferences) current.parent()) != null) {
            res.addFirst(p);
            current = p;
        }
        return res;
    }

    public final SQLRow getNode() {
        if (this.node == null) {
            final SQLPreferences parent = (SQLPreferences) parent();
            final Where parentW;
            if (parent == null) {
                parentW = Where.isNull(this.getNodeRT().getField("ID_PARENT"));
            } else {
                final SQLRow parentNode = parent.getNode();
                parentW = parentNode == null ? null : new Where(this.getNodeRT().getField("ID_PARENT"), "=", parentNode.getID());
            }
            if (parentW == null) {
                // our parent is not in the DB, we can't
                this.node = null;
            } else {
                final SQLSelect sel = new SQLSelect().addSelectStar(this.getNodeRT());
                sel.setWhere(parentW.and(new Where(this.getNodeRT().getField("NAME"), "=", name())));

                @SuppressWarnings("unchecked")
                final Map<String, ?> m = (Map<String, ?>) execute(sel.asString(), SQLDataSource.MAP_HANDLER);
                this.node = m == null ? null : new SQLRow(this.getNodeRT(), m);
            }
        }
        return this.node;
    }

    private final SQLRow createThisNode() throws SQLException {
        final SQLPreferences parent = (SQLPreferences) parent();
        final SQLRowValues insVals = new SQLRowValues(this.getNodeWT());
        insVals.put("ID_PARENT", parent == null ? SQLRowValues.SQL_EMPTY_LINK : parent.node.getID());
        insVals.put("NAME", name());
        this.node = insVals.insert();
        return this.node;
    }

    private final boolean createNode() throws SQLException {
        if (this.node == null) {
            // to avoid deadlocks, do all the SELECT...
            final Iterator<SQLPreferences> iter = getAncestors().iterator();
            boolean rowExists = true;
            SQLPreferences ancestor = null;
            while (iter.hasNext() && rowExists) {
                ancestor = iter.next();
                rowExists = ancestor.getNode() != null;
            }
            // ... then all the INSERT
            // we used to insert the root node, which submitted a replicate, then select one of its
            // children, thus waiting on the replicate. But it itself was waiting on our transaction
            // since we inserted the root node before.
            if (!rowExists) {
                ancestor.createThisNode();
                while (iter.hasNext()) {
                    ancestor = iter.next();
                    ancestor.createThisNode();
                }
                return true;
            }
        }
        return false;
    }

    public final Map<String, String> getValues() {
        if (this.values == null) {
            this.values = new HashMap<String, String>();
            final SQLRow node = getNode();
            if (node != null) {
                final SQLSelect sel = new SQLSelect().addSelectStar(this.getPrefRT());
                sel.setWhere(new Where(this.getPrefRT().getField("ID_NODE"), "=", node.getID()));

                @SuppressWarnings("unchecked")
                final List<Map<String, Object>> l = (List<Map<String, Object>>) execute(sel.asString(), SQLDataSource.MAP_LIST_HANDLER);
                for (final Map<String, Object> r : l) {
                    this.values.put(r.get("NAME").toString(), r.get("VALUE").toString());
                }
            }
        }
        return this.values;
    }

    @Override
    protected void putSpi(String key, String value) {
        this.changedValues.put(key, value);
        this.removedKeys.remove(key);
    }

    @Override
    protected void removeSpi(String key) {
        this.removedKeys.add(key);
        this.changedValues.remove(key);
    }

    @Override
    protected String getSpi(String key) {
        if (this.removedKeys.contains(key))
            return null;
        else if (this.changedValues.containsKey(key))
            return this.changedValues.get(key);
        else
            return this.getValues().get(key);
    }

    // null means delete all
    private void deleteValues(final Set<String> keys) {
        final SQLRow node = this.getNode();
        if (node != null) {
            final String keysW;
            if (keys == null)
                keysW = "";
            else
                keysW = " and " + new Where(this.getPrefWT().getField("NAME"), keys).getClause();

            getWriteDS().execute("DELETE FROM " + this.getPrefWT().getSQLName().quote() + " where \"ID_NODE\" = " + node.getID() + keysW);
            // don't call replicate() it will be called by our caller
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        try {
            final SQLRow node = this.getNode();
            if (node != null) {
                SQLUtils.executeAtomic(getWriteDS(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                    @Override
                    public Object handle(SQLDataSource ds) throws SQLException {
                        deleteValues(null);
                        ds.execute("DELETE FROM " + getNodeWT().getSQLName().quote() + " where \"ID\" = " + node.getID());
                        return null;
                    }
                });
                replicate();
                this.node = null;
            }
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
        assert this.node == null;
        this.values = null;
        this.removedKeys.clear();
        this.changedValues.clear();
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        try {
            final Set<String> committedKeys = this.getValues().keySet();
            final Set<String> res;
            if (this.removedKeys.isEmpty() && this.changedValues.isEmpty()) {
                res = committedKeys;
            } else {
                res = new HashSet<String>(committedKeys);
                res.removeAll(this.removedKeys);
                res.addAll(this.changedValues.keySet());
            }
            return res.toArray(new String[res.size()]);
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        try {
            final SQLRow node = this.getNode();
            if (node == null) {
                // OK since "This method need not return the names of any nodes already cached"
                // so if we call pref.node("a/b/c") with no existing nodes this still works
                return new String[0];
            }

            final int nodeID = node.getID();
            final SQLSelect sel = new SQLSelect().addSelect(this.getNodeRT().getField("NAME"));
            final Where w = new Where(this.getNodeRT().getField("ID_PARENT"), "=", nodeID);
            sel.setWhere(w);
            @SuppressWarnings("unchecked")
            final List<String> names = (List<String>) execute(sel.asString(), SQLDataSource.COLUMN_LIST_HANDLER);
            return names.toArray(new String[names.size()]);
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    protected SQLPreferences childSpi(String name) {
        return new SQLPreferences(this, name);
    }

    @Override
    public void sync() throws BackingStoreException {
        this.replicate();
        super.sync();
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        this.flushSpi();
        // per our superclass documentation we must reflect the change of the persistent store :
        // - if some keys were changed, the next getValues() will fetch them ;
        // - if our node was deleted, we have to call removeNode() otherwise our parent will still
        // have us in kidCache and sync() will be called our cached kids.
        // Don't call getValues() or childrenNamesSpi() here so that when asked they'll be the most
        // up to date
        this.values = null;
        this.node = null;
        if (this.getNode() == null)
            this.removeNode();
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        if (!this.nodeExists(""))
            // values and node already removed in removeNodeSpi()
            return;
        try {
            SQLUtils.executeAtomic(getWriteDS(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException {
                    flushTxn();
                    return null;
                }
            });
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    protected final void flushTxn() throws SQLException {
        // create node even if there's no values nor any children
        boolean masterChanged = createNode();
        if (this.removedKeys.size() > 0 || this.changedValues.size() > 0) {
            // also delete changed, so we can insert afterwards
            this.deleteValues(CollectionUtils.union(this.removedKeys, this.changedValues.keySet()));
            this.removedKeys.clear();

            if (this.changedValues.size() > 0) {
                final int nodeID = getNode().getID();
                final List<String> insValues = new ArrayList<String>(this.changedValues.size());
                for (final Entry<String, String> e : this.changedValues.entrySet()) {
                    insValues.add("(" + nodeID + ", " + this.getPrefWT().getBase().quoteString(e.getKey()) + ", " + this.getPrefWT().getBase().quoteString(e.getValue()) + ")");
                }

                SQLRowValues.insertCount(this.getPrefWT(), "(\"ID_NODE\", \"NAME\", \"VALUE\") VALUES" + CollectionUtils.join(insValues, ", "));
                this.changedValues.clear();
            }
            masterChanged = true;
        }
        if (masterChanged)
            replicate();
    }
}
