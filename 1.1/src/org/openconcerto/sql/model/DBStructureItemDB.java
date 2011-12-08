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
import java.util.Set;

/**
 * An item of the database specific tree, ie it might leave out some JDBC level (for example MySQL
 * does not support schemas).
 * 
 * @author Sylvain
 */
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

    protected DBStructureItemDB(final DBStructureItemJDBC jdbc) {
        super(getDB(jdbc.getParent()), jdbc.getName());
        this.jdbc = jdbc;
    }

    @Override
    public boolean isDropped() {
        return this.getJDBC().isDropped();
    }

    public final DBStructureItemJDBC getJDBC() {
        return this.jdbc;
    }

    @Override
    protected DBStructureItemDB getDB() {
        return this;
    }

    @Override
    public DBStructureItemDB getChild(String name) {
        final DBStructureItemJDBC nonNullDBParent = this.getJDBC().getNonNullDBParent();
        if (nonNullDBParent == null)
            return null;
        else
            return getDB(nonNullDBParent.getChild(name));
    }

    @Override
    public Set<String> getChildrenNames() {
        final DBStructureItemJDBC nonNullDBParent = this.getJDBC().getNonNullDBParent();
        if (nonNullDBParent == null)
            return Collections.emptySet();
        else
            return nonNullDBParent.getChildrenNames();
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
