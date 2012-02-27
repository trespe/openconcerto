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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openconcerto.utils.Tuple3;

public class Group {

    private final String id;
    private int order = 0;
    private List<Tuple3<Group, LayoutHints, Integer>> list = new ArrayList<Tuple3<Group, LayoutHints, Integer>>();
    private Group parent;

    public Group(String id) {
        this.id = id.trim();
    }

    public String getId() {
        return id;
    }

    public Group getParent() {
        return parent;
    }

    public Group getRoot() {
        final Set<String> roots = new HashSet<String>();
        Group root = parent;
        while (root != null) {
            if (roots.contains(root)) {
                throw new IllegalStateException("Loop detected in group hierarchy at " + root.getId() + " for " + roots);
            }
            roots.add(root.getId());
            root = root.getParent();
        }
        return root;
    }

    public void add(Group group) {
        this.add(group, LayoutHints.DEFAULT_GROUP_HINTS);
    }

    public void add(Group group, LayoutHints hints) {
        group.parent = this;
        order += 100;
        list.add(new Tuple3<Group, LayoutHints, Integer>(group, hints, order));
    }

    public void insert(Group group, LayoutHints hints, int order) {
        group.parent = this;
        list.add(new Tuple3<Group, LayoutHints, Integer>(group, hints, order));
    }

    public void add(String string) {
        this.add(new Group(string), LayoutHints.DEFAULT_FIELD_HINTS);

    }

    public void add(String string, LayoutHints hints) {
        this.add(new Group(string), hints);
    }

    public void dumpOneColumn() {
        final StringBuilder b = new StringBuilder();
        dumpOneColumn(b, LayoutHints.DEFAULT_GROUP_HINTS, 0, 1);
        System.out.println(b.toString());
    }

    public void dumpOneColumn(StringBuilder builder, LayoutHints localHint, int localOrder, int level) {
        for (int i = 0; i < level - 1; i++) {
            builder.append("  ");
        }
        if (list.size() == 0)
            builder.append("+-- ");
        else
            builder.append("+-+ ");
        builder.append(localOrder + " " + this.id + " [" + localHint + "]\n");
        sortSubGroup();
        for (Tuple3<Group, LayoutHints, Integer> tuple : list) {
            ((Group) tuple.get0()).dumpOneColumn(builder, tuple.get1(), tuple.get2(), level + 1);
        }
    }

    private void sortSubGroup() {
        if (list.size() > 1) {
            Collections.sort(list, new Comparator<Tuple3<Group, LayoutHints, Integer>>() {

                @Override
                public int compare(Tuple3<Group, LayoutHints, Integer> o1, Tuple3<Group, LayoutHints, Integer> o2) {
                    int c = o1.get2().compareTo(o2.get2());
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
        dumpTwoColumn(b, 0, LayoutHints.DEFAULT_GROUP_HINTS, 0, 1);
        System.out.println(b.toString());
    }

    public int dumpTwoColumn(StringBuilder builder, int x, LayoutHints localHint, int localOrder, int level) {
        if (localHint.isSeparated()) {
            x = 0;
            builder.append("\n");
        }
        if (isEmpty()) {
            builder.append(" (" + x + ")");
            builder.append(localOrder + " " + this.id + "[" + localHint + "]");

            if ((x % 2) == 1) {
                builder.append("\n");
            }
        }
        sortSubGroup();
        for (Tuple3<Group, LayoutHints, Integer> tuple : list) {
            final Group subGroup = tuple.get0();
            final Integer subGroupOrder = (Integer) tuple.get2();
            x = subGroup.dumpTwoColumn(builder, x, tuple.get1(), subGroupOrder, level + 1);
        }
        if (isEmpty()) {
            x++;
        }
        if (!localHint.isSeparated() && list.size() != 0 && localHint.maximizeWidth()) {
            x = 0;
            builder.append("\n");
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
        for (Tuple3<Group, LayoutHints, Integer> tuple : list) {
            final Group subGroup = tuple.get0();
            subGroup.sort();
        }
    }

    public Group getGroup(int i) {
        return this.list.get(i).get0();
    }

    public LayoutHints getLayoutHints(int i) {
        return this.list.get(i).get1();
    }

    public Integer getOrder(int i) {
        return this.list.get(i).get2();
    }

}
