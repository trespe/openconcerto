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

import java.util.HashSet;
import java.util.Set;

public class Item {
    private String id;
    protected Group parent;
    protected LayoutHints localHint;

    public Item(String id) {
        this.id = id.trim();
        this.localHint = new LayoutHints(LayoutHints.DEFAULT_FIELD_HINTS);
    }

    public Item(String id, LayoutHints hint) {
        this.id = id.trim();
        this.localHint = new LayoutHints(hint);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Group getParent() {
        return parent;
    }

    public LayoutHints getLocalHint() {
        return localHint;
    }

    public void setLocalHint(LayoutHints localHint) {
        this.localHint = localHint;
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

    public void dumpOneColumn(StringBuilder builder, int localOrder, int level) {
        for (int i = 0; i < level - 1; i++) {
            builder.append("  ");
        }

        builder.append("+-- ");
        builder.append(localOrder + " " + this.getId() + " [" + localHint + "]\n");
    }

    public int dumpTwoColumn(StringBuilder builder, int x, int localOrder, int level) {
        if (localHint.largeWidth() && x > 0) {
            builder.append("\n");
            x = 0;
        }
        // print a leaf
        builder.append(" (" + x + ")");
        builder.append(localOrder + " " + this.getId() + "[" + localHint + "]");

        if (localHint.largeWidth()) {
            x += 2;
        } else {
            x++;
        }

        if (x > 1) {
            builder.append("\n");
            x = 0;
        }
        return x;
    }

    @Override
    public String toString() {
        return "Item:" + this.getId();
    }
}
