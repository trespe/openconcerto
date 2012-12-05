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

import org.openconcerto.utils.EnumOrderedSet;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.jcip.annotations.ThreadSafe;

/**
 * An item of the database specific tree, ie it might leave out some JDBC level (for example MySQL
 * does not support schemas).
 * 
 * @author Sylvain
 */
@ThreadSafe
public class DBStructureItemDB extends DBStructureItem<DBStructureItemDB> {

    static DBStructureItemDB create(final DBStructureItemJDBC jdbc) {
        if (isOfLevel(jdbc, DBSystemRoot.class))
            return new DBSystemRoot(jdbc);
        else if (isOfLevel(jdbc, DBRoot.class))
            return new DBRoot(jdbc);
        else if (jdbc.getServer().getSQLSystem().getLevels().contains(jdbc.getLevel()))
            return new DBStructureItemDB(jdbc);
        else
            return null;
    }

    private static boolean isOfLevel(final DBStructureItemJDBC jdbc, Class<? extends DBStructureItemDB> c) {
        return jdbc.getServer().getSQLSystem().getDBLevel(c).equals(jdbc.getLevel());
    }

    private final DBStructureItemJDBC jdbc;
    private Map<String, ? extends DBStructureItemJDBC> childrenJDBC;
    private Map<String, DBStructureItemDB> children;

    protected DBStructureItemDB(final DBStructureItemJDBC jdbc) {
        super(getDB(jdbc.getParent()), jdbc.getName());
        this.jdbc = jdbc;
    }

    @Override
    public boolean isDropped() {
        return this.getJDBC().isDropped();
    }

    protected final void checkDropped() {
        this.getJDBC().checkDropped();
    }

    public final DBStructureItemJDBC getJDBC() {
        return this.jdbc;
    }

    @Override
    protected DBStructureItemDB getDB() {
        return this;
    }

    @Override
    public Map<String, ? extends DBStructureItemDB> getChildrenMap() {
        final DBStructureItemJDBC nonNullDBParent = this.getJDBC().getNonNullDBParent();
        if (nonNullDBParent == null) {
            return Collections.emptyMap();
        } else {
            final Map<String, ? extends DBStructureItemJDBC> jdbcChildren = nonNullDBParent.getChildrenMap();
            assert jdbcChildren != null : "Cannot be null for the first comparison (when this.childrenJDBC is null)";
            Map<String, DBStructureItemDB> res = null;
            synchronized (this) {
                // OK to test with reference equality since jdbcChildren doesn't change
                if (this.childrenJDBC == jdbcChildren) {
                    res = this.children;
                    assert res != null : "Cannot be null for the comparison below";
                }
            }
            if (res == null) {
                res = new HashMap<String, DBStructureItemDB>(jdbcChildren.size());
                for (final Entry<String, ? extends DBStructureItemJDBC> e : jdbcChildren.entrySet()) {
                    res.put(e.getKey(), getDB(e.getValue()));
                }
                res = Collections.unmodifiableMap(res);
                synchronized (this) {
                    this.childrenJDBC = jdbcChildren;
                    this.children = res;
                }
            }
            return res;
        }
    }

    @Override
    public final void addChildrenListener(PropertyChangeListener l) {
        final DBStructureItemJDBC nonNullDBParent = this.getJDBC().getNonNullDBParent();
        if (nonNullDBParent != null)
            nonNullDBParent.addChildrenListener(l);
    }

    @Override
    public final void rmChildrenListener(PropertyChangeListener l) {
        final DBStructureItemJDBC nonNullDBParent = this.getJDBC().getNonNullDBParent();
        if (nonNullDBParent != null)
            nonNullDBParent.rmChildrenListener(l);
    }

    protected final EnumOrderedSet<HierarchyLevel> getLevels() {
        return this.getServer().getSQLSystem().getLevels();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getJDBC();
    }

}
