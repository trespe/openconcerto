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
 
 package org.openconcerto.sql.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openconcerto.utils.Tuple3;

public class Group {

    private String id;
    private int order = 0;
    private List<Tuple3<Group, LayoutHints, Integer>> list = new ArrayList<Tuple3<Group, LayoutHints, Integer>>();

    public Group(String id) {
        this.id = id.trim();

    }

    public String getId() {
        return id;
    }

    public void add(Group group) {
        order += 100;
        list.add(new Tuple3<Group, LayoutHints, Integer>(group, LayoutHints.DEFAULT_GROUP_HINTS, order));
    }

    public void add(Group group, LayoutHints hints) {
        order += 100;
        list.add(new Tuple3<Group, LayoutHints, Integer>(group, hints, order));
    }

    public void insert(Group group, LayoutHints hints, int order) {
        list.add(new Tuple3<Group, LayoutHints, Integer>(group, hints, order));
    }

    public void add(String string) {
        this.add(new Group(string), LayoutHints.DEFAULT_FIELD_HINTS);

    }

    public void add(String string, LayoutHints hints) {
        this.add(new Group(string), hints);

    }

    public void dumpOneColumn() {
        dumpOneColumn(LayoutHints.DEFAULT_GROUP_HINTS, 0, 1);
    }

    public void dumpOneColumn(LayoutHints localHint, int localOrder, int level) {
        for (int i = 0; i < level - 1; i++) {
            System.out.print("  ");
        }
        if (list.size() == 0)
            System.out.print("+-- ");
        else
            System.out.print("+-+ ");
        System.out.println(localOrder + " " + this.id + " [" + localHint + "]");
        sortSubGroup();
        for (Tuple3<Group, LayoutHints, Integer> tuple : list) {

            ((Group) tuple.get0()).dumpOneColumn(tuple.get1(), tuple.get2(), level + 1);
        }
        // System.out.println("== end" + this.id);
    }

    private void sortSubGroup() {
        if (list.size() > 1) {
            Collections.sort(list, new Comparator<Tuple3<Group, LayoutHints, Integer>>() {

                @Override
                public int compare(Tuple3<Group, LayoutHints, Integer> o1, Tuple3<Group, LayoutHints, Integer> o2) {
                    return o1.get2().compareTo(o2.get2());
                }
            });
        }
    }

    public void dumpTwoColumn() {
        dumpTwoColumn(0, LayoutHints.DEFAULT_GROUP_HINTS, 0, 1);
    }

    public int dumpTwoColumn(int x, LayoutHints localHint, int localOrder, int level) {

        if (isEmpty()) {
            System.out.print(" (" + x + ")");

            System.out.print(localOrder + " " + this.id + "[" + localHint + "]");

            if ((x % 2) == 1) {
                System.out.println();
            }
        }
        sortSubGroup();
        for (Tuple3<Group, LayoutHints, Integer> tuple : list) {
            final Group subGroup = tuple.get0();
            final Integer subGroupOrder = (Integer) tuple.get2();

            x = subGroup.dumpTwoColumn(x, tuple.get1(), subGroupOrder, level + 1);

        }
        if (isEmpty()) {
            x++;
        }
        if (list.size() != 0 && localHint.maximizeWidth()) {
            x = 0;
            System.out.println();
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
