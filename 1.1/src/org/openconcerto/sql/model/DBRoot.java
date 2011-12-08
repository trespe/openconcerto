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

import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.utils.SQLCreateRoot;
import org.openconcerto.sql.utils.SQL_URL;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * The root of a database, in mysql a SQLBase, in postgresql a SQLSchema.
 * 
 * @author Sylvain
 */
public final class DBRoot extends DBStructureItemDB {

    static DBRoot get(SQLBase b, String n) {
        final DBRoot ancestor = b.getDBRoot();
        final DBStructureItemDB parent = ancestor == null ? b.getDB() : ancestor.getParent();
        return (DBRoot) parent.getChild(n);
    }

    private DatabaseGraph graph;
    private final PropertyChangeListener l;

    DBRoot(DBStructureItemJDBC delegate) {
        super(delegate);
        this.graph = null;
        this.l = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("graph"))
                    clearGraph();
            }
        };
        this.getDBSystemRoot().addListener(this.l);
    }

    @Override
    protected void onDrop() {
        this.getDBSystemRoot().rmListener(this.l);
        this.clearGraph();
        super.onDrop();
    }

    public final SQLBase getBase() {
        return this.getJDBC().getAncestor(SQLBase.class);
    }

    public SQLTable getTable(String name) {
        return (SQLTable) getJDBC(this.getChild(name));
    }

    public SQLTable getTableDesc(String name) {
        return this.getDescLenient(name, SQLTable.class);
    }

    public SQLTable findTable(String name) {
        return this.findTable(name, false);
    }

    public SQLTable findTable(String name, final boolean mustExist) {
        if (this.contains(name))
            return this.getTable(name);
        else
            return this.getDBSystemRoot().findTable(name, mustExist);
    }

    /**
     * Return the tables of this root.
     * 
     * @return our tables.
     */
    public Set<SQLTable> getTables() {
        return getJDBC().getDescendants(SQLTable.class);
    }

    public SQLField getField(String name) {
        return this.getDesc(name, SQLField.class);
    }

    /**
     * Return the value of a metadata for this root.
     * 
     * @param name name of the metadata, eg "Customer".
     * @return value of the metadata or <code>null</code> if it doesn't exist, eg "ACME, inc".
     */
    public final String getMetadata(final String name) {
        return getSchema().getFwkMetadata(name);
    }

    // since by definition DBRoot is one level above SQLTable, there's only one schema below it
    public final SQLSchema getSchema() {
        return (SQLSchema) this.getJDBC().getNonNullDBParent();
    }

    /**
     * Set the value of a metadata.
     * 
     * @param name name of the metadata, eg "Customer".
     * @param value value of the metadata, eg "ACME, inc".
     * @return <code>true</code> if the value was set, <code>false</code> otherwise.
     * @throws SQLException if an error occurs while setting the value.
     */
    public final boolean setMetadata(final String name, final String value) throws SQLException {
        return getSchema().setFwkMetadata(name, value);
    }

    private synchronized void clearGraph() {
        this.graph = null;
    }

    public synchronized DatabaseGraph getGraph() {
        if (this.graph == null) {
            this.graph = new DatabaseGraph(this.getDBSystemRoot().getGraph(), this);
        }
        return this.graph;
    }

    /**
     * Refresh this from the database.
     * 
     * @throws SQLException if an error occurs.
     */
    public void refetch() throws SQLException {
        if (this.getJDBC() instanceof SQLBase)
            ((SQLBase) this.getJDBC()).fetchTables();
        else if (this.getJDBC() instanceof SQLSchema)
            this.getBase().fetchTables(Collections.singleton(this.getName()));
        else
            throw new IllegalStateException();
    }

    public final SQLCreateRoot getDefinitionSQL(final SQLSystem sys) {
        final SQLCreateRoot res = new SQLCreateRoot(sys.getSyntax(), this.getName());
        // order by name to be able to do diffs
        for (final String name : new TreeSet<String>(this.getChildrenNames())) {
            res.addTable(this.getTable(name).getCreateTable(sys));
        }
        return res;
    }

    public final String equalsDesc(final DBRoot o) {
        return this.equalsDesc(o, null);
    }

    public final String equalsDesc(final DBRoot o, final SQLSystem otherSystem) {
        if (this == o)
            return null;
        if (null == o)
            return "other is null";

        if (!this.getChildrenNames().equals(o.getChildrenNames()))
            return "unequal table names: " + this.getChildrenNames() + " != " + o.getChildrenNames();

        for (final SQLTable t : this.getDescs(SQLTable.class)) {
            final String eqDesc = t.equalsDesc(o.getTable(t.getName()), otherSystem, true);
            if (eqDesc != null)
                return "unequal " + t.getName() + ": " + eqDesc;
        }
        return null;
    }

    /**
     * Return the url pointing to this root.
     * 
     * @return the url or <code>null</code> if this cannot be represented as an {@link SQL_URL} (eg
     *         jdbc:h2:file:/a/b/c).
     */
    public final SQL_URL getURL() {
        final String hostname = this.getServer().getHostname();
        if (hostname == null)
            return null;
        final SQLSystem system = this.getServer().getSQLSystem();
        String url = system.name().toLowerCase() + "://" + this.getDBSystemRoot().getDataSource().getUsername() + "@" + hostname + "/";
        // handle systems w/o systemRoot
        if (system.getDBLevel(DBSystemRoot.class) != HierarchyLevel.SQLSERVER) {
            url += this.getDBSystemRoot().getName() + "/";
        }
        url += this.getName();
        try {
            return SQL_URL.create(url);
        } catch (URISyntaxException e) {
            // should not happen
            throw new IllegalStateException("could not produce url for " + this, e);
        }
    }

    /**
     * A string with the content of this root.
     * 
     * <pre>
     *   /TYPE_COURANT/
     *   ID_TYPE_COURANT t: 4 def: null
     *   LABEL t: 12 def: 
     *   ARCHIVE t: 4 def: 0
     *   ORDRE t: 4 def: 1
     * </pre>
     * 
     * @return the content of this.
     */
    public String dump() {
        String res = "";
        for (final String tableName : new TreeSet<String>(this.getChildrenNames())) {
            final SQLTable table = this.getTable(tableName);
            res += table + "\n";
            for (final String fieldName : new TreeSet<String>(table.getFieldsName())) {
                SQLField f = table.getField(fieldName);
                res += f.getName() + " t: " + f.getType() + " def: " + f.getDefaultValue() + "\n";
            }
            res += "\n";
        }
        return res;
    }
}
