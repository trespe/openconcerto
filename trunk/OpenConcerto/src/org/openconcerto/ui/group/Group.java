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

    private int order = 0;
    private List<Tuple2<Item, Integer>> list = new ArrayList<Tuple2<Item, Integer>>();

    public Group(String id) {
        super(id);
        this.localHint = new LayoutHints(LayoutHints.DEFAULT_GROUP_HINTS);
    }

    public Group(String id, LayoutHints hint) {
        super(id, new LayoutHints(hint));
    }

    public void addItem(String string) {
        this.add(new Item(string));
    }

    public void addItem(String string, LayoutHints hint) {
        this.add(new Item(string, hint));
    }

    public void add(Item item) {
        order += 100;
        add(item, order);
    }

    public void add(Item item, Integer order) {
        item.parent = this;
        list.add(new Tuple2<Item, Integer>(item, order));
    }

    public void insert(Item item, int order) {
        item.parent = this;
        list.add(new Tuple2<Item, Integer>(item, order));
    }

    public void dumpOneColumn() {
        final StringBuilder b = new StringBuilder();
        dumpOneColumn(b, 0, 1);
        System.out.println(b.toString());
    }

    @Override
    public void dumpOneColumn(StringBuilder builder, int localOrder, int level) {
        for (int i = 0; i < level - 1; i++) {
            builder.append("  ");
        }
        builder.append("+-+ ");
        builder.append(localOrder + " " + this.getId() + " [" + localHint + "]\n");
        sortSubGroup();
        for (Tuple2<Item, Integer> tuple : list) {
            ((Item) tuple.get0()).dumpOneColumn(builder, tuple.get1(), level + 1);
        }
    }

    public void sortSubGroup() {
        if (list.size() > 1) {
            Collections.sort(list, new Comparator<Tuple2<Item, Integer>>() {

                @Override
                public int compare(Tuple2<Item, Integer> o1, Tuple2<Item, Integer> o2) {
                    int c = o1.get1().compareTo(o2.get1());
                    if (c == 0) {
                        c = o1.get0().getId().compareTo(o2.get0().getId());
                    }
                    return c;
                }
            });
        }
    }

    public void dumpTwoColumn() {
        final StringBuilder b = new StringBuilder();
        System.out.println("==== Group " + this.getId() + " ====");
        dumpTwoColumn(b, 0, 0, 1);
        System.out.println(b.toString());
    }

    @Override
    public int dumpTwoColumn(StringBuilder builder, int x, int localOrder, int level) {
        if (localHint.isSeparated()) {
            x = 0;
            builder.append(" -------\n");
        }
        if (isEmpty()) {
            // print a leaf
            x = super.dumpTwoColumn(builder, x, localOrder, level);
        } else {
            // Subgroup
            sortSubGroup();
            for (Tuple2<Item, Integer> tuple : list) {
                final Item subGroup = tuple.get0();
                final Integer subGroupOrder = (Integer) tuple.get1();
                x = subGroup.dumpTwoColumn(builder, x, subGroupOrder, level + 1);
            }
        }
        return x;
    }

    public int getSize() {
        return this.list.size();
    }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public void sort() {
        sortSubGroup();
        for (Tuple2<Item, Integer> tuple : list) {
            final Item subGroup = tuple.get0();
            if (subGroup instanceof Group) {
                ((Group) subGroup).sort();
            }
        }
    }

    public Item getItem(int index) {
        return this.list.get(index).get0();
    }

    public void remove(int index) {
        this.list.remove(index);
    }

    public Integer getOrder(int index) {
        return this.list.get(index).get1();
    }

    @Override
    public String toString() {
        return "Group:" + this.getId();
    }

    public boolean contains(String id) {
        return getItemFromId(id) != null;
    }

    public Item getItemFromId(String id) {
        return getItemFromId(this, id);
    }

    private Item getItemFromId(Item item, String gId) {
        if (item.getId().equals(gId)) {
            return item;
        }
        if (item instanceof Group) {
            final Group group = (Group) item;
            final int size = group.getSize();
            for (int i = 0; i < size; i++) {
                final Item b = getItemFromId(group.getItem(i), gId);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    public void remove(String itemId) {
        this.remove(this, itemId);
    }

    private void remove(Group group, String id) {
        final int size = group.getSize();
        for (int i = 0; i < size; i++) {
            final Item b = group.getItem(i);
            if (b.getId().endsWith(id)) {
                remove(i);
                return;
            }
            if (b instanceof Group) {
                remove((Group) b, id);
            }
        }

    }

    public Integer getOrder(String id) {
        final int size = this.getSize();
        for (int i = 0; i < size; i++) {
            final Tuple2<Item, Integer> b = list.get(i);
            if (b.get0().getId().equals(id)) {
                return b.get1();
            }
        }
        return null;
    }

    public int getIndex(String id) {
        final int size = this.getSize();
        for (int i = 0; i < size; i++) {
            final Tuple2<Item, Integer> b = list.get(i);
            if (b.get0().getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

}
