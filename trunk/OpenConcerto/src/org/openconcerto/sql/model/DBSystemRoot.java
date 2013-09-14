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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.LoadingListener.LoadingChangeSupport;
import org.openconcerto.sql.model.LoadingListener.LoadingEvent;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.sql.utils.SQLCreateRoot;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IClosure;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * The root of a database system ie all its tables can reference each other. For example in mysql a
 * SQLServer, in postgresql a SQLBase.
 * 
 * @author Sylvain
 */
@ThreadSafe
public final class DBSystemRoot extends DBStructureItemDB {

    private final Object treeMutex = new String("Tree mutex");
    @GuardedBy("graphMutex")
    private DatabaseGraph graph;
    private final Object graphMutex = new String("Graph mutex");
    @GuardedBy("this")
    private Set<String> rootsToMap;
    @GuardedBy("this")
    private boolean useCache;

    @GuardedBy("supp")
    private final PropertyChangeSupport supp;

    // linked to schemaPath and incoherentPath
    @GuardedBy("this")
    private SQLDataSource ds;
    // immutable
    @GuardedBy("this")
    private List<String> schemaPath;
    // whether this.getTable("T") is the same as "SELECT FROM T"
    @GuardedBy("this")
    private boolean incoherentPath;
    private final PropertyChangeListener coherenceListener;

    private final LoadingChangeSupport loadingListenersSupp;

    DBSystemRoot(DBStructureItemJDBC delegate) {
        super(delegate);
        this.graph = null;
        // initial state since mapAllRoots() can cause exceptions to happen later on
        mapNoRoots();
        this.useCache = Boolean.getBoolean(SQLBase.STRUCTURE_USE_XML);
        this.ds = null;
        this.schemaPath = Collections.emptyList();
        this.incoherentPath = false;

        this.supp = new PropertyChangeSupport(this);
        this.coherenceListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                rootsChanged(evt);
            }
        };
        this.loadingListenersSupp = new LoadingChangeSupport(this);

        this.getServer().init(this);
    }

    private synchronized void rootsChanged(PropertyChangeEvent evt) {
        if (isIncoherentPath()) {
            // our path is empty so nothing can be removed
            setRootPathFromDS();
        } else {
            @SuppressWarnings("unchecked")
            final Collection<String> newVal = (Collection<String>) evt.getNewValue();
            final List<String> rootPath = getRootPath();
            final Collection<String> inexistant = CollectionUtils.substract(rootPath, newVal);
            if (inexistant.size() > 0) {
                // remove inexistant
                final List<String> copy = new ArrayList<String>(rootPath);
                copy.removeAll(inexistant);
                // set ds path since inexistant might just mean hidden by rootsToMap not
                // dropped from the db
                if (copy.size() > 0)
                    setRootPathWithPrivate(copy);
                else
                    unsetRootPath();
            }
        }
    }

    /**
     * Lock that is held when the tree rooted here is modified.
     * 
     * @return the tree lock.
     */
    public final Object getTreeMutex() {
        return this.treeMutex;
    }

    public final DBRoot getRoot(final String name) {
        return (DBRoot) this.getCheckedChild(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, DBRoot> getChildrenMap() {
        return (Map<String, DBRoot>) super.getChildrenMap();
    }

    /**
     * Map all available roots. NOTE: once this method is called you cannot use
     * {@link #removeExplicitRootToMap(String)}.
     */
    public final void mapAllRoots() {
        this.setRootsToMap(null);
    }

    public final void mapNoRoots() {
        this.setRootsToMap(Collections.<String> emptySet(), false, true);
    }

    public final void setRootToMap(final String rootName) {
        this.setRootsToMap(Collections.singleton(rootName), false, true);
    }

    public final void setRootsToMap(final Collection<String> rootsNames) {
        this.setRootsToMap(rootsNames, true, false);
    }

    /**
     * The roots that will be mapped.
     * 
     * @return the immutable set of names of the roots, <code>null</code> meaning map all.
     */
    private final Set<String> getRootsToMap() {
        // OK since immutable
        synchronized (this) {
            return this.rootsToMap;
        }
    }

    private final void setRootsToMap(final Collection<String> rootsNames, final boolean copy, final boolean immutable) {
        Set<String> s;
        if (rootsNames == null) {
            s = null;
        } else {
            final boolean needsCopy = copy || !(rootsNames instanceof Set);
            s = needsCopy ? new HashSet<String>(rootsNames) : (Set<String>) rootsNames;
            if (needsCopy || !immutable)
                s = Collections.unmodifiableSet(s);
        }
        synchronized (this) {
            this.rootsToMap = s;
        }
    }

    public final void addRootToMap(final String rootName) {
        this.addRootsToMap(Collections.singleton(rootName));
    }

    public final void addRootsToMap(final Collection<String> rootsNames) {
        synchronized (this) {
            // otherwise already included
            if (this.rootsToMap != null) {
                final Set<String> newSet = new HashSet<String>(this.rootsToMap);
                if (newSet.addAll(rootsNames))
                    this.rootsToMap = Collections.unmodifiableSet(newSet);
            }
        }
    }

    /**
     * Remove a root to map.
     * 
     * @param rootName the root to remove.
     * @throws IllegalStateException if {@link #mapAllRoots()} was called.
     */
    public final void removeExplicitRootToMap(final String rootName) throws IllegalStateException {
        synchronized (this) {
            // MAYBE rootsNotToMap
            if (this.rootsToMap == null)
                throw new IllegalStateException("Mapping all roots");

            final Set<String> newSet = new HashSet<String>(this.rootsToMap);
            if (newSet.remove(rootName))
                this.rootsToMap = Collections.unmodifiableSet(newSet);
        }
    }

    /**
     * Set whether this instance uses a cache, can only be used before this has a data source. I.e.
     * in the <code>systemRootInit</code> parameter of
     * {@link SQLServer#SQLServer(SQLSystem, String, String, String, String, IClosure, IClosure)
     * server}.
     * 
     * @param useCache <code>true</code> to use a cache.
     * @see #hasDataSource()
     */
    public synchronized final void initUseCache(boolean useCache) {
        if (this.hasDataSource())
            throw new IllegalStateException("Instance already inited");
        this.useCache = useCache;
    }

    /**
     * Set whether this instance uses a cache. This creates the meta data table if needed.
     * 
     * @param useCache <code>true</code> to use a cache.
     * @throws SQLException if an error occurs or if the cache cannot be used (e.g.
     *         {@value SQLSchema#NOAUTO_CREATE_METADATA} is <code>true</code>).
     */
    public synchronized final void setUseCache(final boolean useCache) throws SQLException {
        if (this.hasDataSource() && useCache) {
            // null if we shouldn't alter the base
            final SQLCreateMoveableTable createMetadata = SQLSchema.getCreateMetadata(this.getServer().getSQLSystem().getSyntax());
            final TablesMap m = new TablesMap();
            for (final DBRoot r : this.getChildrenMap().values()) {
                // works because when created a root is always fully loaded (we don't allow roots
                // with a subset of tables)
                if (!r.contains(SQLSchema.METADATA_TABLENAME)) {
                    if (createMetadata != null) {
                        getDataSource().execute(createMetadata.asString(r.getName()));
                        m.add(r.getName(), createMetadata.getName());
                    } else {
                        throw new SQLException(JDBCStructureSource.getCacheError(r.getName()));
                    }
                }
            }
            this.refresh(m, false);
        }
        this.useCache = useCache;
    }

    /**
     * Whether this instance uses a cache for its structure and graph. This initial value is set to
     * the property {@link SQLBase#STRUCTURE_USE_XML}.
     * 
     * @return <code>true</code> if this uses a cache.
     */
    public synchronized final boolean useCache() {
        return this.useCache;
    }

    public final SQLTable findTable(String name) {
        return findTable(name, false);
    }

    /**
     * Search for a table named <code>name</code> in our path.
     * 
     * @param name a table name, eg "TENSION".
     * @param mustExist if <code>true</code> <code>null</code> will never be returned.
     * @return the first table named <code>name</code>, or <code>null</code>.
     * @throws DBStructureItemNotFound if <code>mustExist</code> and no table was found.
     */
    public final SQLTable findTable(String name, final boolean mustExist) {
        final Map<String, DBRoot> children = this.getChildrenMap();
        final List<String> path = this.getRootPath();
        for (final String root : path) {
            final DBRoot child = children.get(root);
            final SQLTable res = child == null ? null : child.getTable(name);
            if (res != null)
                return res;
        }
        if (mustExist)
            throw new DBStructureItemNotFound("table " + name + " not found in " + path);
        else
            return null;
    }

    final boolean createNode(DBStructureItemJDBC parent, String childName) {
        if (!isSystemRoot(parent))
            return true;
        final Set<String> s = new HashSet<String>();
        s.add(childName);
        this.filterNodes(parent, s);
        return !s.isEmpty();
    }

    public final boolean shouldMap(String childName) {
        final Set<String> rootsToMap = this.getRootsToMap();
        return rootsToMap == null || rootsToMap.contains(childName);
    }

    private boolean isSystemRoot(DBStructureItemJDBC parent) {
        return parent.getDBSystemRoot() == parent.getRawAlterEgo();
    }

    final void filterNodes(DBStructureItemJDBC parent, Set<String> childrenNames) {
        if (isSystemRoot(parent)) {
            parent.getServer().getSQLSystem().removeRootsToIgnore(childrenNames);
            final Iterator<String> iter = childrenNames.iterator();
            while (iter.hasNext()) {
                final String childName = iter.next();
                if (!shouldMap(childName))
                    iter.remove();
            }
        }
    }

    void descendantsChanged(DBStructureItemJDBC parent, Set<String> childrenRefreshed, final boolean readCache) {
        this.descendantsChanged(TablesMap.createByRootFromChildren(parent, childrenRefreshed), readCache, true);
    }

    void descendantsChanged(TablesMap tablesRefreshed, final boolean readCache, final boolean tableListChange) {
        assert Thread.holdsLock(getTreeMutex()) : "By definition descendants must be changed with the tree lock";
        try {
            // don't fire GraphLoadingEvent here since we might be in an atomicRefresh
            this.getGraph().refresh(tablesRefreshed, readCache);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't refresh the graph", e);
        }
        // the dataSource must always have all tables, to listen to them for its cache
        if (tableListChange)
            this.getDataSource().setTables(getDescs(SQLTable.class));
    }

    public DatabaseGraph getGraph() {
        synchronized (this.graphMutex) {
            return this.graph;
        }
    }

    public synchronized final SQLDataSource getDataSource() {
        if (this.ds == null)
            throw new IllegalStateException("setDS() was not called");
        return this.ds;
    }

    public synchronized final boolean hasDataSource() {
        return this.ds != null;
    }

    @Override
    protected synchronized void onDrop() {
        this.rmChildrenListener(this.coherenceListener);
        // if setDS() was never called
        if (this.ds != null) {
            try {
                this.ds.close();
            } catch (SQLException e) {
                // we've tried
                e.printStackTrace();
            }
        }
        super.onDrop();
    }

    public void refetch() throws SQLException {
        this.refetch(null);
    }

    /**
     * Refresh some children of this from the database.
     * 
     * @param childrenNames what to refresh, <code>null</code> meaning everything.
     * @throws SQLException if an error occurs.
     */
    public void refetch(Set<String> childrenNames) throws SQLException {
        this.refresh(childrenNames, false);
    }

    public void reload() throws SQLException {
        this.reload(null);
    }

    public void reload(Set<String> childrenNames) throws SQLException {
        this.refresh(childrenNames, true);
    }

    public final TablesMap refresh(Set<String> childrenNames, final boolean readCache) throws SQLException {
        return this.refresh(TablesMap.createFromKeys(childrenNames), readCache);
    }

    /**
     * Refresh some tables.
     * 
     * @param tables which root/tables to refresh.
     * @param readCache <code>false</code> to use JDBC.
     * @return tables loaded with JDBC.
     * @throws SQLException if an error occurs.
     */
    public final TablesMap refresh(TablesMap tables, final boolean readCache) throws SQLException {
        if (this.getJDBC() instanceof SQLBase) {
            return ((SQLBase) this.getJDBC()).refresh(tables, readCache);
        } else if (this.getJDBC() instanceof SQLServer) {
            final Map<String, TablesMap> toRefresh;
            if (tables == null) {
                toRefresh = null;
            } else {
                final int size = tables.size();
                if (size == 0)
                    return tables;
                // translate from root to schema : sysRoot is server, meaning root is base and it
                // has a unique null schema
                toRefresh = new HashMap<String, TablesMap>(size);
                for (final Entry<String, Set<String>> e : tables.entrySet()) {
                    toRefresh.put(e.getKey(), TablesMap.createFromTables(null, e.getValue()));
                }
            }
            final Map<String, TablesMap> refreshed = ((SQLServer) this.getJDBC()).refresh(toRefresh, readCache);
            // translate from schema to root
            final TablesMap res = new TablesMap(refreshed.size());
            for (final Entry<String, TablesMap> e : refreshed.entrySet()) {
                assert e.getValue().keySet().equals(Collections.singleton(null));
                res.addAll(e.getKey(), e.getValue().get(null));
            }
            return res;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Adds a listener for this and its children.
     * 
     * @param l the listener.
     */
    public final void addLoadingListener(LoadingListener l) {
        this.loadingListenersSupp.addLoadingListener(l);
    }

    final void fireLoading(LoadingEvent evt) {
        this.loadingListenersSupp.fireLoading(evt);
    }

    public final void removeLoadingListener(LoadingListener l) {
        this.loadingListenersSupp.removeLoadingListener(l);
    }

    /**
     * Add the passed roots to the roots to map and to root path then {@link #reload(Set)}.
     * 
     * @param roots the roots names to add.
     * @throws SQLException if problem while reloading.
     */
    public final void addRoots(final List<String> roots) throws SQLException {
        this.addRoots(roots, true, true);
    }

    public final DBRoot addRoot(final String root) throws SQLException {
        return this.addRoot(root, true);
    }

    /**
     * Add the passed root to the roots to map then refresh.
     * 
     * @param root the roots name to add (must exist).
     * @param readCache <code>false</code> to use JDBC.
     * @return the newly added root, not <code>null</code>.
     * @throws SQLException if problem while reloading.
     */
    public final DBRoot addRoot(final String root, final boolean readCache) throws SQLException {
        this.addRoots(Collections.singletonList(root), readCache);
        return this.getRoot(root);
    }

    public final void addRoots(final List<String> roots, final boolean readCache) throws SQLException {
        this.addRoots(roots, readCache, false);
    }

    public final void addRoots(final List<String> roots, final boolean readCache, final boolean addToPath) throws SQLException {
        this.addRootsToMap(roots);
        this.refresh(new HashSet<String>(roots), readCache);
        if (addToPath)
            this.appendToRootPath(roots);
    }

    /**
     * Create a root, add it to roots to map and refresh.
     * 
     * @param rootName the root to create (mustn't exist).
     * @return the new root.
     * @throws SQLException if problem while reloading.
     * @see #addRoot(String)
     */
    public final DBRoot createRoot(final String rootName) throws SQLException {
        for (final String s : new SQLCreateRoot(SQLSyntax.get(this), rootName).asList(rootName, false, true))
            getDataSource().execute(s);
        return this.addRoot(rootName);
    }

    /**
     * Whether this root's structure is cached.
     * 
     * @param rootName the name of the root.
     * @return <code>true</code> if the passed root is saved.
     */
    boolean isSaved(String rootName) {
        if (this.getJDBC() instanceof SQLBase)
            return ((SQLBase) this.getJDBC()).isSaved(rootName);
        else if (this.getJDBC() instanceof SQLServer) {
            return SQLBase.isSaved((SQLServer) this.getJDBC(), rootName, null);
        } else
            throw new IllegalStateException();
    }

    public final void addListener(PropertyChangeListener l) {
        synchronized (this.supp) {
            this.supp.addPropertyChangeListener(l);
        }
    }

    public final void rmListener(PropertyChangeListener l) {
        synchronized (this.supp) {
            this.supp.removePropertyChangeListener(l);
        }
    }

    /**
     * A string with the content of this system root.
     * 
     * @return the content of this.
     */
    public String dump() {
        String res = "";
        for (final DBRoot root : new TreeMap<String, DBRoot>(this.getChildrenMap()).values()) {
            res += root + "\n\n";
            res += root.dump() + "\n\n\n";
        }
        return res;
    }

    synchronized final void setDS(String login, String pass, IClosure<SQLDataSource> dsInit) {
        if (this.ds != null)
            throw new IllegalStateException("already set: " + this.ds);
        this.checkDropped();
        // either base or above
        final String baseName = this.getLevel() == HierarchyLevel.SQLBASE ? this.getName() : "";
        this.ds = new SQLDataSource(this.getServer(), baseName, login, pass);
        if (dsInit != null)
            dsInit.executeChecked(this.ds);

        synchronized (this.graphMutex) {
            this.graph = new DatabaseGraph(this);
        }

        this.addChildrenListener(this.coherenceListener);

        setRootPathFromDS();
    }

    private synchronized void setRootPathFromDS() {
        final String dsSchema = this.ds.getSchema();
        final Set<String> childrenNames = getChildrenNames();
        if (dsSchema != null && childrenNames.contains(dsSchema)) {
            this.setDefaultRoot(dsSchema);
        } else {
            // ds has no valid default schema
            if (childrenNames.size() == 1) {
                // if we have only 1, use it as the default
                this.setDefaultRoot(childrenNames.iterator().next());
            } else if (this.getServer().getSQLSystem().isNoDefaultSchemaSupported()) {
                // prefer not to choose so unset default schema
                this.clearRootPath();
            } else {
                // we've got no schemas and the ds schema is set to something
                // (since isNoDefaultSchemaSupported() is false)
                this.schemaPath = Collections.emptyList();
                this.incoherentPath = true;
                Log.get().warning("db default schema is " + dsSchema + " and the schemas of " + this + " are empty ; the first created schema will become the default one");
            }
        }
    }

    /**
     * Whether {@link #getRootPath()} is coherent with the database path.
     * 
     * @return <code>true</code> if both paths are not coherent.
     * @see #setDefaultRoot(String)
     */
    public synchronized final boolean isIncoherentPath() {
        return this.incoherentPath;
    }

    /**
     * This method changes the default root, and ensures that getTable("tableName") and "tableName"
     * in getDataSource().execute() mean the same. The change only takes effect for future
     * connections, but does not affect existing ones.
     * 
     * @param schemaName the default schema, can be <code>null</code>
     * @throws IllegalArgumentException if <code>schemaName</code> is not in this.
     * @see SQLDataSource#setSchema(String)
     */
    public final void setDefaultRoot(String schemaName) {
        this.setRootPathWithImmutable(Collections.singletonList(schemaName));
    }

    public final void appendToRootPath(String schemaName) {
        this.appendToRootPath(Collections.singletonList(schemaName));
    }

    public synchronized final void appendToRootPath(List<String> schemasNames) {
        final List<String> newPath = new ArrayList<String>(this.getRootPath());
        newPath.addAll(schemasNames);
        this.setRootPathWithPrivate(newPath);
    }

    public synchronized final void prependToRootPath(String schemaName) {
        final List<String> newPath = new ArrayList<String>(this.getRootPath());
        newPath.add(0, schemaName);
        this.setRootPathWithPrivate(newPath);
    }

    public final void clearRootPath() {
        this.setRootPathWithImmutable(Collections.<String> emptyList());
    }

    public synchronized final void unsetRootPath() {
        this.getDataSource().unsetInitialSchema();
        this.setRootPathFromDS();
    }

    public final void setRootPath(List<String> schemaNames) {
        this.setRootPathWithPrivate(new ArrayList<String>(schemaNames));
    }

    private final void setRootPathWithPrivate(List<String> schemaNames) {
        this.setRootPathWithImmutable(Collections.unmodifiableList(schemaNames));
    }

    private synchronized final void setRootPathWithImmutable(List<String> schemaNames) {
        if (!this.getChildrenNames().containsAll(schemaNames))
            throw new IllegalArgumentException(schemaNames + " are not all in " + this + ": " + this.getChildrenNames());

        this.schemaPath = schemaNames;

        // coherence with ds so that table.getRowCount() and
        // "select count(*) from table" mean the same
        final String dsSchema = schemaNames.size() > 0 ? schemaNames.get(0) : null;
        this.getDataSource().setInitialSchema(dsSchema);
        this.incoherentPath = false;
    }

    public synchronized final List<String> getRootPath() {
        return this.schemaPath;
    }

    public final DBRoot getDefaultRoot() {
        final List<String> path = this.getRootPath();
        if (path.size() > 0) {
            return this.getRoot(path.get(0));
        } else
            return null;
    }
}
