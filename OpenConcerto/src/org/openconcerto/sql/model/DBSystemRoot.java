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
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IClosure;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    @GuardedBy("rootsToMap")
    private final Set<String> rootsToMap;

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

    DBSystemRoot(DBStructureItemJDBC delegate) {
        super(delegate);
        this.graph = null;
        this.rootsToMap = Collections.synchronizedSet(new HashSet<String>());
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

    public Set<String> getRootsToMap() {
        return this.rootsToMap;
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

    /**
     * The children to create for the passed parent.
     * 
     * @param parent the parent.
     * @return the children it can create or <code>null</code> for no restriction.
     */
    final Set<String> getNodesToCreate(DBStructureItemJDBC parent) {
        if (isSystemRoot(parent)) {
            final Set<String> rootsToMap = this.getRootsToMap();
            synchronized (rootsToMap) {
                return rootsToMap.size() == 0 ? null : rootsToMap;
            }
        } else {
            return null;
        }
    }

    final boolean createNode(DBStructureItemJDBC parent, String childName) {
        if (!isSystemRoot(parent))
            return true;
        final Set<String> s = new HashSet<String>();
        s.add(childName);
        this.filterNodes(parent, s);
        return !s.isEmpty();
    }

    private boolean shouldMap(String childName) {
        final Set<String> rootsToMap = this.getRootsToMap();
        synchronized (rootsToMap) {
            return rootsToMap.size() == 0 || rootsToMap.contains(childName);
        }
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

    private final void setGraph(DatabaseGraph graph) {
        if (graph != null)
            this.checkDropped();
        final DatabaseGraph oldValue;
        synchronized (this.graphMutex) {
            oldValue = this.graph;
            this.graph = graph;
        }
        synchronized (this.supp) {
            this.supp.firePropertyChange("graph", oldValue, graph);
        }
    }

    void descendantsChanged() {
        this.descendantsChanged(true);
    }

    void descendantsChanged(final boolean tableListChange) {
        assert Thread.holdsLock(getTreeMutex()) : "By definition descendants must be changed with the tree lock";
        this.clearGraph();
        // the dataSource must always have all tables, to listen to them for its cache
        if (tableListChange)
            this.getDataSource().setTables(getDescs(SQLTable.class));
    }

    private void clearGraph() {
        this.setGraph(null);
    }

    public DatabaseGraph getGraph() {
        assert Thread.holdsLock(this.getTreeMutex()) || !Thread.holdsLock(this.graphMutex) : "Global public lock, then private lock";
        synchronized (this.getTreeMutex()) {
            synchronized (this.graphMutex) {
                if (this.graph == null) {
                    try {
                        // keep new DatabaseGraph() inside the synchronized to prevent two
                        // concurrent expensive creations
                        this.setGraph(new DatabaseGraph(this));
                    } catch (SQLException e) {
                        throw new IllegalStateException("could not graph " + this, e);
                    }
                }
                return this.graph;
            }
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
        this.clearGraph();
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
        if (this.getJDBC() instanceof SQLBase)
            ((SQLBase) this.getJDBC()).fetchTables(childrenNames);
        else if (this.getJDBC() instanceof SQLServer) {
            ((SQLServer) this.getJDBC()).refetch(childrenNames);
        } else
            throw new IllegalStateException();
    }

    public final void reload(Set<String> childrenNames) throws SQLException {
        // only SQLBase has reload()
        if (this.getJDBC() instanceof SQLBase)
            ((SQLBase) this.getJDBC()).loadTables(childrenNames);
        else
            refetch(childrenNames);
    }

    /**
     * Add the passed roots to {@link #getRootsToMap()} and to root path and {@link #reload(Set)}.
     * 
     * @param roots the roots names to add.
     * @throws SQLException if problem while reloading.
     */
    public final void addRoots(final List<String> roots) throws SQLException {
        this.getRootsToMap().addAll(roots);
        this.reload(new HashSet<String>(roots));
        synchronized (this) {
            final List<String> newPath = new ArrayList<String>(this.getRootPath());
            newPath.addAll(roots);
            this.setRootPathWithPrivate(newPath);
        }
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

    public synchronized final void appendToRootPath(String schemaName) {
        final List<String> newPath = new ArrayList<String>(this.getRootPath());
        newPath.add(schemaName);
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
