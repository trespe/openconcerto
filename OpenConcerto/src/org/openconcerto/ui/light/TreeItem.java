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
 
 package org.openconcerto.ui.light;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TreeItem implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 8087661728014786832L;
    private String id;
    private String label;
    private List<TreeItem> children;
    private boolean isSelected;
    private Color color;
    private boolean expanded;
    private String iconId;

    public TreeItem() {
    }

    public TreeItem(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public final String getId() {
        return id;
    }

    public final void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TreeItem getChild(int i) {
        return children.get(i);
    }

    public int getChildCount() {
        return children.size();
    }

    public final List<TreeItem> getChildren() {
        return children;
    }

    public final void addChild(TreeItem item) {
        if (this.children == null) {
            this.children = new ArrayList<TreeItem>();
        }
        this.children.add(item);
    }

    public final void addChildren(List<TreeItem> items) {
        if (this.children == null) {
            this.children = new ArrayList<TreeItem>();
        }
        this.children.addAll(items);
    }

    public final void setChildren(List<TreeItem> children) {
        this.children = children;
    }

    public final boolean hasChildren() {
        return this.children != null && !this.children.isEmpty();
    }

    public final boolean isSelected() {
        return isSelected;
    }

    public final void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public final Color getColor() {
        return color;
    }

    public final void setColor(Color color) {
        this.color = color;
    }

    public final boolean isExpanded() {
        return expanded;
    }

    public final void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public final String getIconId() {
        return iconId;
    }

    public final void setRightIconId(String iconId) {
        this.iconId = iconId;
    }

}
