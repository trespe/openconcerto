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
import org.openconcerto.utils.change.CollectionChangeEvent;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

/**
 * An item of the database tree structure (server/base/schema/...). Each server has 2 trees, the
 * JDBC one : {@link DBStructureItemJDBC} and the DB specific one : {@link DBStructureItemDB}. This
 * class and its subclasses are thread-safe.
 * 
 * @author Sylvain
 * @param <D> the type of items of the tree.
 */
@ThreadSafe
public abstract class DBStructureItem<D extends DBStructureItem<D>> {

    // name is final and immutable : never need to synchronize its access
    private final String name;
    // parent is final but mutable (e.g. its children)
    private final D parent;

    protected DBStructureItem(final D parent, final String name) {
        this.name = name;
        this.parent = parent;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * Whether this item been dropped from the SQL System. Parent links are always keeped, but
     * children links are only keeped between items of the same dropped state, meaning if you drop a
     * table, it will keep its fields, but its schema won't list it.
     * 
     * @return <code>true</code> if this has been dropped.
     */
    public abstract boolean isDropped();

    /**
     * Called when this is removed from the tree. This instance must release any resources it had
     * acquired. This implementation does nothing.
     */
    protected void onDrop() {
    }

    // ** near

    @SuppressWarnings("unchecked")
    private D thisAsD() {
        return (D) this;
    }

    public final D getParent() {
        return this.parent;
    }

    // result must not be modified (e.g. immutable or a private copy)
    // may contain null values for children not loaded
    public abstract Map<String, ? extends D> getChildrenMap();

    public final D getChild(String name) {
        return this.getChildrenMap().get(name);
    }

    /**
     * Same as ({@link #getChild(String)} but checks that it exists.
     * 
     * @param name the name of the child.
     * @return the named child, never <code>null</code>.
     * @throws DBStructureItemNotFound if there's no child named <code>name</code>.
     */
    public final D getCheckedChild(String name) {
        final D res = this.getChild(name);
        if (res != null)
            return res;
        else
            throw new DBStructureItemNotFound(name + " is not a child of " + this);
    }

    public final Set<String> getChildrenNames() {
        return this.getChildrenMap().keySet();
    }

    protected final Set<D> getChildren() {
        final Set<D> res = new HashSet<D>();
        res.addAll(this.getChildrenMap().values());
        return res;
    }

    public final boolean contains(String childName) {
        return this.getChildrenNames().contains(childName);
    }

    /**
     * To be notified when our children change.
     * 
     * @param l a listener that will be passed a {@link CollectionChangeEvent} with
     *        {@link #getChildrenNames()}.
     */
    public abstract void addChildrenListener(final PropertyChangeListener l);

    public abstract void rmChildrenListener(final PropertyChangeListener l);

    // ** far

    public final D getDescendant(SQLName name) {
        final D child = this.getCheckedChild(name.getFirst());
        if (name.getItemCount() == 1)
            return child;
        else
            return child.getDescendant(name.getRest());
    }

    /**
     * All the descendants of this at the level of <code>clazz</code>.
     * 
     * @param <T> type of descendant
     * @param clazz the class of the descendants.
     * @return the descendants.
     * @throws IllegalArgumentException if <code>clazz</code> is not under this, eg this is a Field
     *         and clazz is Table.
     */
    public final <T extends D> Set<T> getDescendants(Class<T> clazz) {
        final int hops = this.getHopsTo(clazz);
        if (hops < 0)
            throw new IllegalArgumentException(clazz + " is not under " + this);

        if (hops == 0) {
            // getAncestor otherwise: Unchecked cast from DBStructureItem<D> to D
            return Collections.singleton(clazz.cast(this));
        } else {
            final Set<T> res = new HashSet<T>();
            for (final D child : this.getChildrenMap().values()) {
                if (child != null)
                    res.addAll(child.getDescendants(clazz));
            }
            return res;
        }
    }

    public final <T extends D> T getAncestor(Class<T> clazz) {
        if (clazz.isInstance(this))
            return clazz.cast(this);
        else if (this.getParent() == null)
            return null;
        else
            return this.getParent().getAncestor(clazz);
    }

    private final D getAncestor(int level) {
        if (level < 0)
            throw new IllegalArgumentException("negative level: " + level);
        else if (level == 0)
            return thisAsD();
        else if (this.getParent() == null)
            throw new IllegalArgumentException(this + " is the root, can't go up of " + level);
        else
            return this.getParent().getAncestor(level - 1);
    }

    /**
     * The youngest common ancestor.
     * 
     * @param other the other node, eg "CTech"."TENSION"."ID".
     * @return the youngest common ancestor, <code>null</code> if there exists none, eg "CTech" if
     *         this is "CTech"."CPI"."ID", or "CTech"."TENSION" if this is "CTech"."TENSION".
     */
    public final D getCommonAncestor(D other) {
        if (this == other)
            return thisAsD();
        final List<D> thisAncs = this.getAncestors();
        final List<D> oAncs = other.getAncestors();

        int current = 0;
        while (thisAncs.size() > current && oAncs.size() > current && thisAncs.get(current) == oAncs.get(current)) {
            current++;
        }
        return current == 0 ? null : thisAncs.get(current - 1);
    }

    /**
     * The list of its ancestors begining by the oldest.
     * 
     * @return the list of its ancestors, this included.
     */
    public final List<D> getAncestors() {
        final List<D> res = new ArrayList<D>();
        D anc = thisAsD();
        while (anc != null) {
            res.add(0, anc);
            anc = anc.getParent();
        }
        return res;
    }

    // ** roots

    public final SQLServer getServer() {
        return this.getAnc(SQLServer.class);
    }

    public final DBRoot getDBRoot() {
        return this.getDB().getAncestor(DBRoot.class);
    }

    public final DBSystemRoot getDBSystemRoot() {
        return this.getDB().getAncestor(DBSystemRoot.class);
    }

    // ** contextual

    public final D getContextualChild(String name) {
        return this.getContextualChild(SQLName.parse(name));
    }

    public final D getContextualChild(SQLName name) {
        return this.getContextualDescendant(name, 1);
    }

    /**
     * Get a descendant <code>level</code> levels beneath this. The <code>name</code> can be longer
     * than <code>level</code> to follow another branch of the tree. Eg if this is the table CLIENT,
     * getContextualDescendant("NAME", 1) will get you the field "NAME" of this table ;
     * getContextualDescendant("ADDRESS"."ZIP", 1) will return the field "ZIP" of the table
     * "ADDRESS" in the same schema.
     * 
     * @param name the name of the descendant, at least <code>level</code> long.
     * @param level the number of levels to descend.
     * @return the descendant
     * @throws IllegalArgumentException if <code>name</code> is too short.
     */
    public final D getContextualDescendant(SQLName name, final int level) {
        if (name.getItemCount() < level)
            throw new IllegalArgumentException(name + " is too short to go down of " + level);
        return this.getAncestor(name.getItemCount() - level).getDescendant(name);
    }

    /**
     * Like {@link #getContextualDescendant(SQLName, int)} but set the level using a
     * {@link DBStructureItemJDBC} class. This method does not return an object of class
     * <code>clazz</code> since it merely serves as a level indicator, ie if this is a
     * {@link DBStructureItemDB} the result will be at the same level than <code>clazz</code> ; see
     * {@link #getDesc(SQLName, Class)} .
     * 
     * @param name the name of the descendant.
     * @param clazz the number of levels to descend.
     * @return the descendant
     */
    public final D getContextualDescendant(SQLName name, Class<? extends DBStructureItemJDBC> clazz) {
        final int hops = this.getHopsTo(clazz);
        if (hops < 0)
            throw new IllegalArgumentException(clazz + "is not under " + this);
        return this.getContextualDescendant(name, hops);
    }

    @SuppressWarnings("unchecked")
    public final <T extends DBStructureItemJDBC> T getDesc(SQLName name, Class<T> clazz) {
        return (T) getJDBC(this.getDB().getContextualDescendant(name, clazz));
    }

    public final <T extends DBStructureItemJDBC> T getDesc(String name, Class<T> clazz) {
        return this.getDesc(SQLName.parse(name), clazz);
    }

    /**
     * Same as {@link #getDesc(String, Class)} but ignores {@link DBStructureItemNotFound} and
     * instead returns <code>null</code>.
     * 
     * @param <T> the type of descendant.
     * @param name the name of the descendant.
     * @param clazz the class of the desired descendant.
     * @return the corresponding descendant or <code>null</code> if not found.
     */
    public final <T extends DBStructureItemJDBC> T getDescLenient(String name, Class<T> clazz) {
        try {
            return this.getDesc(name, clazz);
        } catch (DBStructureItemNotFound e) {
            return null;
        }
    }

    public final <T extends DBStructureItemJDBC> T getDescLenient(SQLName name, Class<T> clazz) {
        try {
            return this.getDesc(name, clazz);
        } catch (DBStructureItemNotFound e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends DBStructureItem<?>> Set<T> getDescs(Class<T> clazz) {
        // getDescendants() stays in the same tree, so start from the desired one.
        if (DBStructureItemDB.class.isAssignableFrom(clazz))
            return (Set<T>) this.getDB().getDescendants(clazz.asSubclass(DBStructureItemDB.class));
        else
            return (Set<T>) this.getJDBC().getDescendants(clazz.asSubclass(DBStructureItemJDBC.class));
    }

    public final <T extends DBStructureItem<?>> T getAnc(Class<T> clazz) {
        final DBStructureItem<?> res;
        if (DBStructureItemDB.class.isAssignableFrom(clazz))
            res = this.getDB().getAncestor(clazz.asSubclass(DBStructureItemDB.class));
        else
            res = this.getJDBC().getAncestor(clazz.asSubclass(DBStructureItemJDBC.class));
        // safe since res is from .getAncestor(clazz) ie T
        // java doesn't know this since i'm forced to use clazz.asSubclass() which returns
        // <? extends DBStructureItemJDBC> although it's just clazz
        return clazz.cast(res);
    }

    public final int getHopsTo(Class<? extends DBStructureItem<?>> clazz) {
        return getLevels().getHops(this.getLevel(), this.getServer().getSQLSystem().getLevel(clazz));
    }

    // ** protected

    protected final HierarchyLevel getLevel() {
        return HierarchyLevel.get(this.getJDBC().getClass());
    }

    protected abstract EnumOrderedSet<HierarchyLevel> getLevels();

    protected abstract DBStructureItemJDBC getJDBC();

    protected abstract DBStructureItemDB getDB();

    protected final boolean isAlterEgoOf(DBStructureItem<?> o) {
        // getJDBC() since getDB() might change level
        return o != null && (this == o || this.getJDBC() == o.getJDBC());
    }

    // *** static

    static protected final DBStructureItemJDBC getJDBC(DBStructureItem<?> item) {
        return item == null ? null : item.getJDBC();
    }

    static protected final DBStructureItemDB getDB(DBStructureItem<?> item) {
        return item == null ? null : item.getDB();
    }

}
