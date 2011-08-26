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
import java.util.Set;
import java.util.TreeSet;

/**
 * The root of a database system ie all its tables can reference each other. For example in mysql a
 * SQLServer, in postgresql a SQLBase.
 * 
 * @author Sylvain
 */
public final class DBSystemRoot extends DBStructureItemDB {

    private DatabaseGraph graph;
    private final Set<String> rootsToMap;

    private final PropertyChangeSupport supp;

    private SQLDataSource ds;
    private final List<String> schemaPath;
    // whether this.getTable("T") is the same as "SELECT FROM T"
    private boolean incoherentPath;
    private final PropertyChangeListener coherenceListener;

    DBSystemRoot(DBStructureItemJDBC delegate) {
        super(delegate);
        this.graph = null;
        this.rootsToMap = new HashSet<String>();
        this.ds = null;
        this.schemaPath = new ArrayList<String>();
        this.incoherentPath = false;

        this.supp = new PropertyChangeSupport(this);
        this.coherenceListener = new PropertyChangeListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (isIncoherentPath())
                    // our path is empty so nothing can be removed
                    setRootPathFromDS();
                else {
                    final Collection<String> newVal = (Collection<String>) evt.getNewValue();
                    final Collection<String> inexistant = CollectionUtils.substract(getRootPath(), newVal);
                    if (inexistant.size() > 0) {
                        // remove inexistant
                        DBSystemRoot.this.schemaPath.removeAll(inexistant);
                        // set ds path since inexistant might just mean hidden by rootsToMap not
                        // dropped from the db
                        if (DBSystemRoot.this.schemaPath.size() > 0)
                            setRootPath(DBSystemRoot.this.schemaPath);
                        else
                            unsetRootPath();
                    }
                }
            }
        };

        this.getServer().init(this);
    }

    public final DBRoot getRoot(final String name) {
        return (DBRoot) this.getCheckedChild(name);
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
        for (final String root : this.schemaPath) {
            if (this.contains(root) && this.getRoot(root).contains(name))
                return this.getRoot(root).getTable(name);
        }
        if (mustExist)
            throw new DBStructureItemNotFound("table " + name + " not found in " + this.schemaPath);
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
        if (isSystemRoot(parent))
            return this.getRootsToMap().size() == 0 ? null : this.getRootsToMap();
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

    private boolean shouldMap(String childName) {
        return this.getRootsToMap().size() == 0 || this.getRootsToMap().contains(childName);
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
        final DatabaseGraph oldValue = this.graph;
        this.graph = graph;
        this.supp.firePropertyChange("graph", oldValue, this.graph);
    }

    void descendantsChanged() {
        this.descendantsChanged(true);
    }

    void descendantsChanged(final boolean tableListChange) {
        this.clearGraph();
        // the dataSource must always have all tables, to listen to them for its cache
        if (tableListChange)
            this.getDataSource().setTables(getDescs(SQLTable.class));
    }

    private void clearGraph() {
        this.setGraph(null);
    }

    // synch otherwise multiple graph can be created
    public synchronized DatabaseGraph getGraph() {
        if (this.graph == null)
            try {
                this.setGraph(new DatabaseGraph(this));
            } catch (SQLException e) {
                throw new IllegalStateException("could not graph " + this, e);
            }
        return this.graph;
    }

    public final SQLDataSource getDataSource() {
        if (this.ds == null)
            throw new IllegalStateException("setDS() was not called");
        return this.ds;
    }

    public final boolean hasDataSource() {
        return this.ds != null;
    }

    @Override
    protected void onDrop() {
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
        final List<String> newPath = new ArrayList<String>(this.schemaPath);
        newPath.addAll(roots);
        this.setRootPath(newPath);
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
        this.supp.addPropertyChangeListener(l);
    }

    public final void rmListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    /**
     * A string with the content of this system root.
     * 
     * @return the content of this.
     */
    public String dump() {
        String res = "";
        for (final String rootName : new TreeSet<String>(this.getChildrenNames())) {
            final DBRoot root = this.getRoot(rootName);
            res += root + "\n\n";
            res += root.dump() + "\n\n\n";
        }
        return res;
    }

    final void setDS(String login, String pass, IClosure<SQLDataSource> dsInit) {
        if (this.ds != null)
            throw new IllegalStateException("already set: " + this.ds);
        // either base or above
        final String baseName = this.getLevel() == HierarchyLevel.SQLBASE ? this.getName() : "";
        this.ds = new SQLDataSource(this.getServer(), baseName, login, pass);
        if (dsInit != null)
            dsInit.executeChecked(this.ds);

        this.addChildrenListener(this.coherenceListener);

        setRootPathFromDS();
    }

    private void setRootPathFromDS() {
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
                this.setRootPath(Collections.<String> emptyList());
            } else {
                // we've got no schemas and the ds schema is set to something
                // (since isNoDefaultSchemaSupported() is false)
                this.schemaPath.clear();
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
    public final boolean isIncoherentPath() {
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
        this.setRootPath(Collections.singletonList(schemaName));
    }

    public final void appendToRootPath(String schemaName) {
        final List<String> newPath = new ArrayList<String>(this.schemaPath);
        newPath.add(schemaName);
        this.setRootPath(newPath);
    }

    public final void prependToRootPath(String schemaName) {
        final List<String> newPath = new ArrayList<String>(this.schemaPath);
        newPath.add(0, schemaName);
        this.setRootPath(newPath);
    }

    public final void clearRootPath() {
        this.setRootPath(Collections.<String> emptyList());
    }

    public final void unsetRootPath() {
        this.getDataSource().unsetInitialSchema();
        this.setRootPathFromDS();
    }

    public final void setRootPath(List<String> schemaNames) {
        if (!this.getChildrenNames().containsAll(schemaNames))
            throw new IllegalArgumentException(schemaNames + " are not all in " + this + ": " + this.getChildrenNames());

        // allow setRootPath(this.schemaPath);
        if (schemaNames != this.schemaPath) {
            this.schemaPath.clear();
            this.schemaPath.addAll(schemaNames);
        }

        // coherence with ds so that table.getRowCount() and
        // "select count(*) from table" mean the same
        final String dsSchema = schemaNames.size() > 0 ? schemaNames.get(0) : null;
        this.getDataSource().setInitialSchema(dsSchema);
        this.incoherentPath = false;
    }

    public final List<String> getRootPath() {
        return Collections.unmodifiableList(this.schemaPath);
    }

    public final DBRoot getDefaultRoot() {
        if (this.schemaPath.size() > 0) {
            return this.getRoot(this.schemaPath.get(0));
        } else
            return null;
    }
}
