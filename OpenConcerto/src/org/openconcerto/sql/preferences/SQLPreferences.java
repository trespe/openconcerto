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

import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.replication.MemoryRep;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.Value;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Preferences backed by SQL tables.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public class SQLPreferences extends AbstractPreferences {

    private static final boolean GET_NODE_JAVA_RECURSION = false;

    static private final String getJoinName(final int i) {
        return "j" + i;
    }

    private static final String PREF_NODE_TABLENAME = "PREF_NODE";
    private static final String PREF_VALUE_TABLENAME = "PREF_VALUE";

    static private SQLCreateTable[] getCreateTables(final DBRoot root) throws SQLException {
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
        createValueT.addVarCharColumn("VALUE", Preferences.MAX_VALUE_LENGTH, true);
        // unique name per node
        createValueT.setPrimaryKey("ID_NODE", "NAME");
        createValueT.addForeignConstraint("ID_NODE", new SQLName(createNodeT.getName()), SQLSyntax.ID_NAME);

        return new SQLCreateTable[] { createNodeT, createValueT };
    }

    static public SQLTable getPrefTable(final DBRoot root) throws SQLException {
        synchronized (root.getDBSystemRoot().getTreeMutex()) {
            if (!root.contains(PREF_VALUE_TABLENAME)) {
                root.createTables(getCreateTables(root));
            }
            return root.getTable(PREF_VALUE_TABLENAME);
        }
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

    // is called by methods holding this.lock, so it cannot try to acquire this.lock
    private final Object nodeLock = new Object();

    private final SQLTable prefRT, prefWT;
    private final SQLTable nodeRT, nodeWT;
    private final MemoryRep rep;
    // immutable
    private final List<SQLPreferences> ancestors;
    // values from the DB
    @GuardedBy("lock")
    private Map<String, String> values;
    // values changed in-memory (not yet committed)
    @GuardedBy("lock")
    private final Map<String, String> changedValues;
    // values removed in-memory (not yet committed)
    @GuardedBy("lock")
    private final Set<String> removedKeys;
    @GuardedBy("nodeLock")
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

        this.ancestors = Collections.unmodifiableList(this.findAncestors());

        this.values = null;
        this.changedValues = new HashMap<String, String>();
        this.removedKeys = new HashSet<String>();
        this.resetNode();
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

    private final boolean isRoot() {
        return this.absolutePath().equals("/");
    }

    private final LinkedList<SQLPreferences> findAncestors() {
        // parent() and node() take lock on root, so we have no way of finding our ancestor while we
        // hold this.lock
        assert !Thread.holdsLock(this.lock) : "Deadlock possible since we access parent()";
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

    // ATTN per the superclass documentation, if the current thread holds this.lock it cannot ask it
    // for an ancestor.
    private final List<SQLPreferences> getAncestors() {
        if (this.isRemoved())
            throw new IllegalStateException("Node has been removed.");
        // else our ancestors cannot be removed
        return this.ancestors;
    }

    // makes sure the next time getNode() is called, it will fetch up to date data.
    private final void resetNode() {
        synchronized (this.nodeLock) {
            this.node = null;
        }
    }

    private final void setNode(final SQLRow r) {
        synchronized (this.nodeLock) {
            this.node = r;
        }
    }

    public final SQLRow getNode() {
        synchronized (this.nodeLock) {
            if (this.node != null)
                return this.node;
            if (!GET_NODE_JAVA_RECURSION) {
                try {
                    final Value<SQLRow> res = this.getNodeFromRoot();
                    if (res.hasValue()) {
                        this.setNode(res.getValue());
                        return res.getValue();
                    }
                } catch (SQLException e) {
                    // we'll try the other way
                    e.printStackTrace();
                }
            }
        }
        // in contrast to getNodeFromRoot() : fill in ancestor's nodes at the expense of one request
        // per ancestor (note that we could change getNodeFromRoot() to return all rows from the
        // root in one request)
        final List<SQLPreferences> ancestors = getAncestors();
        SQLRow currentNode = null;
        for (final SQLPreferences ancestor : ancestors) {
            currentNode = ancestor.getNode(currentNode);
        }
        return currentNode;
    }

    // go through the path in SQL rather than in Java objects.
    // perhaps return all rows from the root to this
    private final Value<SQLRow> getNodeFromRoot() throws SQLException {
        final StringTokenizer tokenizer = new StringTokenizer(this.absolutePath(), "/");
        final List<String> path = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            path.add(tokenizer.nextToken());
        }

        final SQLBase base = getNodeRT().getSchema().getBase();
        // don't bother with recursive CTE for trivial requests (further it doesn't support empty
        // paths)
        if (path.size() > 2 && base.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            final int[] vers = base.getVersion();
            if (vers[0] >= 9 || vers[0] == 8 && vers[1] >= 4) {
                return Value.getSome(getNodeCTE(path, base));
            }
        }

        return Value.getSome(getNodeJoins(path));
    }

    // perhaps do more than one query if path is long.
    private SQLRow getNodeJoins(final List<String> path) {
        final int size = path.size();
        final SQLTable nodeT = getNodeRT();

        final SQLSelect sel = new SQLSelect();
        final AliasedTable rootT = new AliasedTable(nodeT, "root");
        sel.addFrom(rootT);
        String lastAlias = rootT.getAlias();
        for (int i = 0; i < size; i++) {
            final SQLSelectJoin join = sel.addBackwardJoin("INNER", getJoinName(i), nodeT.getField("ID_PARENT"), lastAlias);
            join.setWhere(new Where(join.getJoinedTable().getField("NAME"), "=", path.get(i)));
            lastAlias = join.getAlias();
        }
        sel.setWhere(Where.isNull(rootT.getField("ID_PARENT")));

        sel.addSelectStar(size == 0 ? rootT : new AliasedTable(nodeT, getJoinName(size - 1)));

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> res = (List<Map<String, Object>>) execute(sel.asString(), SQLDataSource.MAP_LIST_HANDLER);
        assert res.size() <= 1 : "Unique constraint not enforced";
        if (res.size() == 0)
            return null;
        return new SQLRow(nodeT, res.get(0));
    }

    private final SQLRow getNodeCTE(final List<String> path, final SQLBase base) {
        if (path.size() == 0)
            throw new IllegalArgumentException("Empty path : use getNodeJoins()");
        final List<List<String>> values = new ArrayList<List<String>>(path.size());
        for (final String token : path) {
            values.add(Arrays.asList(String.valueOf(values.size()), base.quoteString(token)));
        }

        final SQLTable nodeT = getNodeRT();
        final StringBuilder sb = new StringBuilder(1024);
        sb.append("with recursive path(idx, name) as (").append(SQLSyntax.get(base).getValues(values, 2)).append("),");
        sb.append("\nt as (");

        final SQLSelect selectRoot = new SQLSelect(true).addSelectStar(nodeT).addRawSelect("0", "depth").setWhere(Where.isNull(nodeT.getField("ID_PARENT")));
        sb.append(selectRoot.asString()).append("\nUNION ALL\n");
        sb.append(new SQLSelect(true).addSelectStar(nodeT).addRawSelect("\"depth\" + 1", "depth").asString());
        sb.append("\nINNER JOIN t on t." + nodeT.getKey().getQuotedName() + " = " + nodeT.getField("ID_PARENT").getFieldRef());
        sb.append("\nINNER JOIN path on path.idx = t.\"depth\" and path.name = ").append(nodeT.getField("NAME").getFieldRef());
        sb.append("\n)");
        sb.append("\nselect * from t where t.\"depth\" = (select max(idx)+1 from path)");

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) execute(sb.toString(), SQLDataSource.MAP_HANDLER);
        if (map == null)
            return null;

        map.keySet().retainAll(nodeT.getFieldsName());
        return new SQLRow(nodeT, map);
    }

    private final SQLRow getNode(final SQLRow parentNode) {
        synchronized (this.nodeLock) {
            if (this.node == null) {
                final Where parentW;
                if (isRoot()) {
                    parentW = Where.isNull(this.getNodeRT().getField("ID_PARENT"));
                } else {
                    parentW = parentNode == null ? null : new Where(this.getNodeRT().getField("ID_PARENT"), "=", parentNode.getID());
                }
                if (parentW == null) {
                    // our parent is not in the DB, we can't
                    this.setNode(null);
                } else {
                    final SQLSelect sel = new SQLSelect().addSelectStar(this.getNodeRT());
                    sel.setWhere(parentW.and(new Where(this.getNodeRT().getField("NAME"), "=", name())));

                    @SuppressWarnings("unchecked")
                    final Map<String, ?> m = (Map<String, ?>) execute(sel.asString(), SQLDataSource.MAP_HANDLER);
                    this.setNode(m == null ? null : new SQLRow(this.getNodeRT(), m));
                }
            }
            return this.node;
        }
    }

    private final boolean createThisNode(final SQLRow parentNode) throws SQLException {
        assert isRoot() == (parentNode == null) : "Either the root has a parent or a node has null parent (which shouldn't happen since we're inserting rows)";

        final SQLRowValues insVals = new SQLRowValues(this.getNodeWT());
        insVals.put("ID_PARENT", parentNode == null ? SQLRowValues.SQL_EMPTY_LINK : parentNode.getID());
        insVals.put("NAME", name());
        synchronized (this.nodeLock) {
            // need to be up to date to avoid the insert failing
            this.resetNode();
            final boolean res = this.getNode(parentNode) == null;
            if (res)
                this.setNode(insVals.insert());
            assert this.node != null;
            return res;
        }
    }

    private final boolean createNode() throws SQLException {
        final Iterator<SQLPreferences> iter = getAncestors().iterator();
        boolean created = false;
        SQLRow parentNode = null;
        while (iter.hasNext()) {
            final SQLPreferences ancestor = iter.next();
            final boolean ancestorCreated;
            synchronized (ancestor.nodeLock) {
                ancestorCreated = ancestor.createThisNode(parentNode);
                parentNode = ancestor.node;
            }
            // since we take and release the lock at each iteration, if more than one thread
            // executes this method then each ancestor node might get created by a different thread.
            // I.e. the results of createThisNode might be [false, true, false, false, true].
            created |= ancestorCreated;
        }
        return created;
    }

    public final Map<String, String> getValues() {
        synchronized (this.lock) {
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
    }

    @Override
    protected void putSpi(String key, String value) {
        synchronized (this.lock) {
            this.changedValues.put(key, value);
            this.removedKeys.remove(key);
        }
    }

    @Override
    protected void removeSpi(String key) {
        synchronized (this.lock) {
            this.removedKeys.add(key);
            this.changedValues.remove(key);
        }
    }

    @Override
    protected String getSpi(String key) {
        synchronized (this.lock) {
            if (this.removedKeys.contains(key))
                return null;
            else if (this.changedValues.containsKey(key))
                return this.changedValues.get(key);
            else
                return this.getValues().get(key);
        }
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
        synchronized (this.lock) {
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
                    this.resetNode();
                }
            } catch (Exception e) {
                throw new BackingStoreException(e);
            }
            this.values = null;
            this.removedKeys.clear();
            this.changedValues.clear();
        }
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        try {
            synchronized (this.lock) {
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
            }
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        try {
            synchronized (this.lock) {
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
            }
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

    // sync without flushing
    public void reset() throws BackingStoreException {
        this.replicate();
        this.resetRec();
    }

    private final void resetRec() throws BackingStoreException {
        synchronized (this.lock) {
            for (final AbstractPreferences kid : this.cachedChildren()) {
                ((SQLPreferences) kid).resetRec();
            }
            this.resetThis();
        }
    }

    // per our superclass documentation we must reflect the change of the persistent store :
    // - if some keys were changed, the next getValues() will fetch them ;
    // - if our node was deleted, we have to call removeNode() otherwise our parent will still
    // have us in kidCache and sync() will be called our cached kids.
    // Don't call getValues() or childrenNamesSpi() here so that when asked they'll be the most
    // up to date
    private final void resetThis() throws BackingStoreException {
        synchronized (this.lock) {
            this.values = null;
            this.removedKeys.clear();
            this.changedValues.clear();
            this.resetNode();
            if (this.getNode() == null)
                this.removeNode();
        }
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        synchronized (this.lock) {
            this.flushSpi();
            this.resetThis();
        }
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        synchronized (this.lock) {
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
    }

    protected final void flushTxn() throws SQLException {
        assert Thread.holdsLock(this.lock);
        // create node even if there's no values nor any children
        boolean masterChanged = createNode();
        if (this.removedKeys.size() > 0 || this.changedValues.size() > 0) {
            // also delete changed, so we can insert afterwards
            this.deleteValues(CollectionUtils.union(this.removedKeys, this.changedValues.keySet()));
            // MAYBE remove and clear after transaction commit
            if (this.values != null)
                this.values.keySet().removeAll(this.removedKeys);
            this.removedKeys.clear();

            if (this.changedValues.size() > 0) {
                final int nodeID = getNode().getID();
                final List<String> insValues = new ArrayList<String>(this.changedValues.size());
                for (final Entry<String, String> e : this.changedValues.entrySet()) {
                    insValues.add("(" + nodeID + ", " + this.getPrefWT().getBase().quoteString(e.getKey()) + ", " + this.getPrefWT().getBase().quoteString(e.getValue()) + ")");
                    // MAYBE put after transaction commit
                    if (this.values != null)
                        this.values.put(e.getKey(), e.getValue());
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
