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
import org.openconcerto.utils.change.CollectionChangeEvent;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * An item of the database structure as returned by JDBC.
 * 
 * @author Sylvain
 */
@ThreadSafe
public abstract class DBStructureItemJDBC extends DBStructureItem<DBStructureItemJDBC> {

    private static final String CHILDREN = "children";

    // linked to alterEgoCreated
    @GuardedBy("this")
    private DBStructureItemDB alterEgo;
    // alterEgo can be null, so we can't use that
    @GuardedBy("this")
    private boolean alterEgoCreated;
    @GuardedBy("this")
    private boolean dropped;
    @GuardedBy("supp")
    private final PropertyChangeSupport supp;

    protected DBStructureItemJDBC(final DBStructureItemJDBC parent, final String name) {
        super(parent, name);
        this.alterEgo = null;
        this.alterEgoCreated = false;
        this.dropped = false;
        this.supp = new PropertyChangeSupport(this);
    }

    final void dropped() {
        assert this.getParent() == null || this.getParent().getChild(getName()) != this : "Dropped child still in its parent : " + this;
        this.droppedRec();
    }

    private final void droppedRec() {
        for (final DBStructureItemJDBC child : this.getChildren()) {
            if (child != null)
                child.droppedRec();
        }
        final DBStructureItemDB alterEgo;
        synchronized (this) {
            if (this.dropped)
                throw new IllegalStateException("Already dropped : " + this);
            this.dropped = true;
            alterEgo = this.alterEgo;
        }
        // don't create it to drop it
        if (alterEgo != null)
            alterEgo.onDrop();
        this.onDrop();
    }

    @Override
    public synchronized final boolean isDropped() {
        return this.dropped;
    }

    protected final synchronized void checkDropped() {
        if (this.dropped)
            throw new IllegalStateException("Already dropped");
    }

    // ATTN called back with the lock on this
    @Override
    public final void addChildrenListener(final PropertyChangeListener l) {
        synchronized (this.supp) {
            this.supp.addPropertyChangeListener(CHILDREN, l);
        }
    }

    @Override
    public final void rmChildrenListener(final PropertyChangeListener l) {
        synchronized (this.supp) {
            this.supp.removePropertyChangeListener(CHILDREN, l);
        }
    }

    protected final CollectionChangeEventCreator createChildrenCreator() {
        return new AddAllCreator(this, CHILDREN, this.getChildrenNames());
    }

    protected final void fireChildrenChanged(final CollectionChangeEventCreator cc) {
        assert getDBSystemRoot() == null || Thread.holdsLock(getDBSystemRoot().getTreeMutex()) : "State might have already changed by the time Listeners are notified";
        if (!cc.getName().equals(CHILDREN))
            throw new IllegalArgumentException("wrong name: " + cc.getName() + " ; should use createChildrenCreator()");
        final CollectionChangeEvent event = cc.create(this.getChildrenNames());
        // can be removed when in java 7
        synchronized (this.supp) {
            this.supp.firePropertyChange(event);
        }
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

        final DBStructureItemJDBC child = this.getChild(null);
        return child != null ? child.getNonNullDBParent() : null;
    }

    protected final EnumOrderedSet<HierarchyLevel> getLevels() {
        return HierarchyLevel.getAll();
    }

    synchronized final DBStructureItemDB getRawAlterEgo() {
        if (!this.alterEgoCreated) {
            checkDropped();
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
