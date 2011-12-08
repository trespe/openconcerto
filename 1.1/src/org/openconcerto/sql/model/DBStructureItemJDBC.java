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
import org.openconcerto.utils.change.AddAllCreator;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An item of the database structure as returned by JDBC.
 * 
 * @author Sylvain
 */
public abstract class DBStructureItemJDBC extends DBStructureItem<DBStructureItemJDBC> {

    private static final String CHILDREN = "children";

    private DBStructureItemDB alterEgo;
    // alterEgo can be null, so we can't use that
    private boolean alterEgoCreated;
    private boolean dropped;
    private final PropertyChangeSupport supp;

    protected DBStructureItemJDBC(final DBStructureItemJDBC parent, final String name) {
        super(parent, name);
        this.alterEgo = null;
        this.alterEgoCreated = false;
        this.dropped = false;
        this.supp = new PropertyChangeSupport(this);
    }

    final void dropped() {
        this.dropped = true;
        for (final DBStructureItemJDBC child : this.getChildren()) {
            child.dropped();
        }
        // don't create it to drop it
        if (this.alterEgo != null)
            this.alterEgo.onDrop();
        this.onDrop();
    }

    @Override
    public boolean isDropped() {
        return this.dropped;
    }

    @Override
    public final void addChildrenListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(CHILDREN, l);
    }

    @Override
    public final void rmChildrenListener(final PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(CHILDREN, l);
    }

    protected final CollectionChangeEventCreator createChildrenCreator() {
        return new AddAllCreator(this, CHILDREN, this.getChildrenNames());
    }

    protected final void fireChildrenChanged(final CollectionChangeEventCreator cc) {
        if (!cc.getName().equals(CHILDREN))
            throw new IllegalArgumentException("wrong name: " + cc.getName() + " ; should use createChildrenCreator()");
        this.supp.firePropertyChange(cc.create(this.getChildrenNames()));
    }

    private final HierarchyLevel getNextLevel() {
        return this.getLevels().getNext(this.getLevel());
    }

    /**
     * The parent of the first non-null db descendant.
     * 
     * @return the parent of the first non-null db descendant, or <code>null</code> if none can
     *         found (eg this has no children).
     */
    final DBStructureItemJDBC getNonNullDBParent() {
        final HierarchyLevel nextLevel = this.getNextLevel();
        if (nextLevel == null || this.getDB().getLevels().contains(nextLevel))
            return this;
        else if (this.getChildrenNames().contains(null))
            return this.getChild(null).getNonNullDBParent();
        else
            return null;
    }

    protected final EnumOrderedSet<HierarchyLevel> getLevels() {
        return HierarchyLevel.getAll();
    }

    final DBStructureItemDB getRawAlterEgo() {
        if (!this.alterEgoCreated) {
            this.alterEgo = DBStructureItemDB.create(this);
            this.alterEgoCreated = true;
        }
        return this.alterEgo;
    }

    public final DBStructureItemDB getDB() {
        if (this.getRawAlterEgo() != null)
            return this.getRawAlterEgo();
        else
            return this.getParent().getDB();
    }

    @Override
    protected DBStructureItemJDBC getJDBC() {
        return this;
    }

}
