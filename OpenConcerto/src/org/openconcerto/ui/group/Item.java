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
 
 package org.openconcerto.ui.group;

import java.util.Collection;
import java.util.Collections;

import net.jcip.annotations.GuardedBy;

public class Item {

    public static Item copy(final Item item, final Group newParent) {
        return item instanceof Group ? Group.copy((Group) item, newParent) : new Item(item, newParent);
    }

    private String id;
    @GuardedBy("this")
    private boolean frozen;
    private Group parent;
    private LayoutHints localHint;

    public Item(final String id) {
        this(id, null);
    }

    public Item(final String id, final LayoutHints hint) {
        this.id = id.trim();
        this.frozen = false;
        this.setLocalHint(hint);
    }

    public Item(final Item item, final Group newParent) {
        this.id = item.id;
        // no need to copy if frozen
        this.frozen = false;
        this.parent = newParent;
        this.setLocalHint(item.localHint);
    }

    public final String getId() {
        return this.id;
    }

    public final void setId(final String id) {
        checkFrozen("setId");
        this.id = id;
    }

    public final synchronized boolean isFrozen() {
        return this.frozen;
    }

    protected final void checkFrozen(String op) {
        if (this.isFrozen())
            throw new IllegalStateException("Frozen cannot " + op);
    }

    public synchronized void freeze() {
        this.frozen = true;
    }

    public final Item getChildFromID(final String id) {
        return this.getDescFromID(id, 1);
    }

    /**
     * Get all descendant leaves, including this.
     * 
     * @return the descendant non-group.
     */
    public Collection<Item> getDescendantItems() {
        return Collections.singletonList(this);
    }

    protected void getDescendantItems(final Collection<Item> res) {
        res.add(this);
    }

    public final Item getDescFromID(final String id) {
        return this.getDescFromID(id, -1);
    }

    public Item getDescFromID(final String id, final int maxLevel) {
        return this.getId().equals(id) ? this : null;
    }

    public Group getParent() {
        return this.parent;
    }

    final void setParent(Group parent) {
        checkFrozen("setParent");
        this.parent = parent;
    }

    public LayoutHints getLocalHint() {
        return this.localHint;
    }

    public void setLocalHint(final LayoutHints localHint) {
        checkFrozen("setLocalHint");
        this.localHint = new LayoutHints(localHint == null ? LayoutHints.DEFAULT_FIELD_HINTS : localHint);
    }

    public final Group getRoot() {
        Item current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current instanceof Group ? (Group) current : null;
    }

    protected void printTree(final StringBuilder builder, final int localOrder, final int level) {
        for (int i = 0; i < level - 1; i++) {
            builder.append("  ");
        }

        builder.append("+-- ");
        builder.append(localOrder + " " + this.getId() + " [" + this.localHint + "]\n");
    }

    protected int printColumns(final StringBuilder builder, final int width, int x, final int localOrder, final int level) {
        if (this.localHint.largeWidth() && x > 0) {
            builder.append("\n");
            x = 0;
        }
        // print a leaf
        builder.append(" (" + x + ")");
        builder.append(localOrder + " " + this.getId() + "[" + this.localHint + "]");

        x++;
        if (x >= width || this.localHint.largeWidth()) {
            builder.append("\n");
            x = 0;
        }
        return x;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " '" + this.getId() + "'";
    }

    public boolean equalsDesc(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Item other = (Item) obj;
        return this.id.equals(other.id) && this.localHint.equals(other.localHint);
    }
}
