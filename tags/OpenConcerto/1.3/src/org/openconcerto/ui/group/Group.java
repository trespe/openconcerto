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

import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Group extends Item {

    public static Group copy(final Group g, final Group newParent) {
        return new Group(g, newParent);
    }

    private static final Comparator<Tuple2<Item, Integer>> COMPARATOR = new Comparator<Tuple2<Item, Integer>>() {
        @Override
        public int compare(final Tuple2<Item, Integer> o1, final Tuple2<Item, Integer> o2) {
            int c = o1.get1().compareTo(o2.get1());
            if (c == 0) {
                c = o1.get0().getId().compareTo(o2.get0().getId());
            }
            return c;
        }
    };

    private int order;
    private final List<Tuple2<Item, Integer>> list;

    public Group(final String id) {
        this(id, LayoutHints.DEFAULT_GROUP_HINTS);
    }

    public Group(final String id, final LayoutHints hint) {
        super(id, hint);
        this.order = 100;
        this.list = new ArrayList<Tuple2<Item, Integer>>();
    }

    public Group(final Group g, final Group newParent) {
        super(g, newParent);
        this.order = g.order;
        this.list = new ArrayList<Tuple2<Item, Integer>>(g.list.size());
        for (final Tuple2<Item, Integer> t : g.list) {
            final Item copy = Item.copy(t.get0(), this);
            this.list.add(new Tuple2<Item, Integer>(copy, t.get1()));
        }
    }

    @Override
    public synchronized void freeze() {
        super.freeze();
        for (final Tuple2<Item, Integer> child : this.list) {
            child.get0().freeze();
        }
    }

    public Item addItem(final String string) {
        final Item res = new Item(string);
        this.add(res);
        return res;
    }

    public Item addItem(final String string, final LayoutHints hint) {
        final Item res = new Item(string, hint);
        this.add(res);
        return res;
    }

    public void add(final Item item) {
        add(item, this.order);
    }

    public void add(final Item item, final int order) {
        checkFrozen("add");
        item.setParent(this);
        this.list.add(new Tuple2<Item, Integer>(item, order));
        Collections.sort(this.list, COMPARATOR);
        if (this.order <= order) {
            this.order = (order / 100) * 100 + 100;
        }
    }

    public final int getSize() {
        return this.list.size();
    }

    public final boolean isEmpty() {
        return this.list.isEmpty();
    }

    public Item getItem(final int index) {
        return this.list.get(index).get0();
    }

    public Integer getOrder(final int index) {
        return this.list.get(index).get1();
    }

    public boolean contains(final String id) {
        return getDescFromID(id) != null;
    }

    @Override
    public Item getDescFromID(final String id, final int maxLevel) {
        final Item res = super.getDescFromID(id, maxLevel);
        if (res != null || maxLevel == 0)
            return res;
        final int size = this.getSize();
        final int nextLevel = maxLevel < 0 ? maxLevel : maxLevel - 1;
        for (int i = 0; i < size; i++) {
            final Item desc = this.getItem(i).getDescFromID(id, nextLevel);
            if (desc != null) {
                return desc;
            }
        }
        return null;
    }

    /**
     * Get a descendant group.
     * 
     * @param path a list of IDs.
     * @param create <code>true</code> if missing descendant should be created.
     * @return the descendant, or <code>null</code> if <code>create</code> is <code>false</code> and
     *         the descendant is missing.
     */
    public final Group followPath(final List<String> path, final boolean create) {
        final int size = path.size();
        Group g = this;
        for (int i = 0; i < size; i++) {
            final String id = path.get(i);
            final Item child = g.getChildFromID(id);
            if (child instanceof Group) {
                g = (Group) child;
            } else if (child != null) {
                throw new IllegalStateException("ID exists but isn't a group : " + child);
            } else if (create) {
                final Group ng = new Group(id);
                g.add(ng);
                g = ng;
            } else {
                return null;
            }
        }
        return g;
    }

    public void remove(final String itemId) {
        this.remove(this, itemId);
    }

    private void remove(final Group group, final String id) {
        checkFrozen("remove");
        final int size = group.getSize();
        for (int i = 0; i < size; i++) {
            final Item b = group.getItem(i);
            if (b.getId().equals(id)) {
                this.list.remove(i);
                return;
            }
            if (b instanceof Group) {
                remove((Group) b, id);
            }
        }

    }

    public Integer getOrder(final String id) {
        final int size = this.getSize();
        for (int i = 0; i < size; i++) {
            final Tuple2<Item, Integer> b = this.list.get(i);
            if (b.get0().getId().equals(id)) {
                return b.get1();
            }
        }
        return null;
    }

    public int getIndex(final String id) {
        final int size = this.getSize();
        for (int i = 0; i < size; i++) {
            final Tuple2<Item, Integer> b = this.list.get(i);
            if (b.get0().getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public String printTree() {
        final StringBuilder b = new StringBuilder();
        printTree(b, 0, 1);
        return b.toString();
    }

    @Override
    protected void printTree(final StringBuilder builder, final int localOrder, final int level) {
        for (int i = 0; i < level - 1; i++) {
            builder.append("  ");
        }
        builder.append("+-+ ");
        builder.append(localOrder + " " + this.getId() + " [" + this.getLocalHint() + "]\n");
        for (final Tuple2<Item, Integer> tuple : this.list) {
            tuple.get0().printTree(builder, tuple.get1(), level + 1);
        }
    }

    public String printTwoColumns() {
        final StringBuilder b = new StringBuilder("==== Group " + this.getId() + " ====\n");
        printColumns(b, 2, 0, 0, 1);
        return b.toString();
    }

    @Override
    protected int printColumns(final StringBuilder builder, final int width, int x, final int localOrder, final int level) {
        if (this.getLocalHint().isSeparated()) {
            x = 0;
            builder.append(" -------\n");
        }
        if (isEmpty()) {
            // print a leaf
            x = super.printColumns(builder, width, x, localOrder, level);
        } else {
            // Subgroup
            for (final Tuple2<Item, Integer> tuple : this.list) {
                final Item subGroup = tuple.get0();
                final Integer subGroupOrder = tuple.get1();
                x = subGroup.printColumns(builder, width, x, subGroupOrder, level + 1);
            }
        }
        return x;
    }

    @Override
    public boolean equalsDesc(Object obj) {
        if (this == obj)
            return true;
        if (!super.equalsDesc(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Group other = (Group) obj;
        final int size = this.list.size();
        if (size != other.list.size())
            return false;
        for (int i = 0; i < size; i++) {
            final Tuple2<Item, Integer> thisChild = this.list.get(i);
            final Tuple2<Item, Integer> oChild = other.list.get(i);
            if (!thisChild.get1().equals(oChild.get1()) || !thisChild.get0().equalsDesc(oChild.get0()))
                return false;
        }
        return true;
    }
}
