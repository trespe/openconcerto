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
 
 package org.openconcerto.sql.model.graph;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBStructureItemJDBC;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.SetMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TablesMap extends SetMap<String, String> {

    static public TablesMap createFromTables(final String rootName, final Collection<String> tables) {
        return create(Collections.singletonMap(rootName, tables));
    }

    /**
     * Create an instance with all tables for each key.
     * 
     * @param keys the keys.
     * @return the new instance.
     */
    static public TablesMap createFromKeys(Collection<String> keys) {
        return create(keys == null ? null : CollectionUtils.<String, Set<String>> createMap(keys));
    }

    static public TablesMap create(Map<? extends String, ? extends Collection<String>> m) {
        if (m == null) {
            return null;
        } else {
            final TablesMap res = new TablesMap();
            res.putAllCollections(m);
            return res;
        }
    }

    static public final TablesMap createByRootFromTable(final SQLTable t) {
        return createFromTables(t.getDBRoot().getName(), Collections.singleton(t.getName()));
    }

    static public final TablesMap createByRootFromTables(final SQLTable... tables) {
        return createByRootFromTables(Arrays.asList(tables));
    }

    static public final TablesMap createByRootFromTables(final Collection<SQLTable> tables) {
        final int size = tables.size();
        if (size == 0) {
            return new TablesMap(0);
        } else if (size == 1) {
            return createByRootFromTable(tables.iterator().next());
        } else {
            final TablesMap res = new TablesMap();
            DBSystemRoot sysRoot = null;
            for (final SQLTable t : tables) {
                final DBRoot root = t.getDBRoot();
                final DBSystemRoot tSystemRoot = root.getDBSystemRoot();
                // otherwise algorithm doesn't work
                assert tSystemRoot != null;
                if (sysRoot == null)
                    sysRoot = tSystemRoot;
                else if (tSystemRoot != sysRoot)
                    throw new IllegalArgumentException("Tables are not all from the same system root : " + sysRoot + " != " + tSystemRoot);
                res.add(root.getName(), t.getName());
            }
            return res;
        }
    }

    static public final TablesMap createBySchemaFromTable(final SQLTable t) {
        return createFromTables(t.getSchema().getName(), Collections.singleton(t.getName()));
    }

    /**
     * Create an instance whose keys are {@link DBRoot} representing the passed parameters.
     * 
     * @param parent a structure item, not <code>null</code>, e.g. a base.
     * @param children its children, may be <code>null</code>, e.g. <code>null</code>.
     * @return the map of tables by root, e.g. either <code>null</code> (for postgreSQL) or
     *         <code>{ parent.getName() -&gt; null } (for MySQL)</code>.
     */
    static public final TablesMap createByRootFromChildren(final DBStructureItemJDBC parent, final Set<String> children) {
        return createFromChildren(parent, children, DBRoot.class);
    }

    static public final TablesMap createBySchemaFromChildren(final DBStructureItemJDBC parent, final Set<String> children) {
        return createFromChildren(parent, children, SQLSchema.class);
    }

    static private final <AT extends DBStructureItem<?>> TablesMap createFromChildren(final DBStructureItemJDBC parent, final Set<String> children, final Class<AT> aboveTable) {
        assert parent.getServer().getSQLSystem().getHops(aboveTable, SQLTable.class) == 1;
        if (parent == null)
            throw new NullPointerException("Null parent");
        final TablesMap res;
        final int hopsToRoot = parent.getHopsTo(aboveTable);
        // parent and children are above DBRoot
        if (hopsToRoot > 1) {
            if (children != null && children.size() == 1)
                return null;
            else
                throw new IllegalArgumentException("Parent is too high, " + hopsToRoot + " levels above " + aboveTable + " : " + parent);
            // parent is above DBRoot, children are at DBRoot
        } else if (hopsToRoot == 1) {
            if (children == null) {
                res = null;
            } else {
                res = new TablesMap(children.size());
                for (final String s : children)
                    res.put(s, null);
            }
        } else {
            final AT root = parent.getAnc(aboveTable);
            // since hopsToRoot <= 0;
            assert root != null;
            final Set<String> tables;
            final int hopsToTable = parent.getHopsTo(SQLTable.class);
            if (hopsToTable <= 0) {
                tables = Collections.singleton(parent.getAncestor(SQLTable.class).getName());
            } else if (hopsToTable == 1) {
                tables = children;
            } else {
                tables = null;
            }
            res = TablesMap.createFromTables(root.getName(), tables);
        }
        return res;
    }

    public TablesMap() {
        this(3);
    }

    public TablesMap(int initialCapacity) {
        super(initialCapacity, Mode.NULL_MEANS_ALL, true);
    }

    @Override
    public Set<String> createCollection(Collection<? extends String> v) {
        final Set<String> res = new HashSet<String>(Math.max(8, v.size()));
        res.addAll(v);
        return res;
    }
}
