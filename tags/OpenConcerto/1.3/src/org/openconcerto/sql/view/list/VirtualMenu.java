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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.view.list.VirtualMenuGroup.Item;
import org.openconcerto.sql.view.list.VirtualMenuGroup.LeafItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenuItem;

/**
 * A menu containing grouped items and sub-menus.
 * 
 * @author Sylvain CUAZ
 * @see #addItem(JMenuItem, String)
 * @see #getSubmenu(String, String)
 */
public class VirtualMenu extends Item {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 50;

    static protected final VirtualMenu EMPTY = new VirtualMenu(null, null, null, 1, 1, Collections.<String, VirtualMenuGroup> emptyMap());

    private final String name;
    private final String defaultGroup;
    private int minSize, maxSize;
    private final Map<String, VirtualMenuGroup> groups;

    static VirtualMenu createRoot(final String defaultGroup) {
        return new VirtualMenu(null, null, defaultGroup, MIN_SIZE, MAX_SIZE);
    }

    static VirtualMenu createFromParentMenu(final VirtualMenuGroup parent, final String name) {
        final VirtualMenu parentMenu = parent.getParentMenu();
        return new VirtualMenu(parent, name, parentMenu.getDefaultGroupName(), parentMenu.minSize, parentMenu.maxSize);
    }

    private VirtualMenu(final VirtualMenuGroup parent, final String name, final String defaultGroup, int minSize, int maxSize) {
        this(parent, name, defaultGroup, minSize, maxSize, new LinkedHashMap<String, VirtualMenuGroup>());
    }

    private VirtualMenu(final VirtualMenuGroup parent, final String name, final String defaultGroup, int minSize, int maxSize, final Map<String, VirtualMenuGroup> groups) {
        super(parent);
        this.name = name;
        this.defaultGroup = defaultGroup;
        this.setMinSize(minSize);
        this.setMaxSize(maxSize);
        this.groups = groups;
    }

    /**
     * Set the minimum size of this menu. If this menu contains fewer items, they will be added to
     * the parent menu if they have the room. The default value is {@value #MIN_SIZE}.
     * 
     * @param minSize the new minimum size.
     * @return this.
     */
    public final VirtualMenu setMinSize(int minSize) {
        this.minSize = minSize;
        return this;
    }

    /**
     * Set the maximum size of this menu. If this menu contains more items, they will be added to a
     * new sub-menu. The default value is {@value #MAX_SIZE}.
     * 
     * @param maxSize the new maximum size.
     * @return this.
     */
    public final VirtualMenu setMaxSize(int maxSize) {
        if (maxSize < 1)
            throw new IllegalArgumentException("Max size should be at least 1");
        this.maxSize = maxSize;
        return this;
    }

    final VirtualMenu getParentMenu() {
        return this.getParentGroup().getParentMenu();
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    final List<String> getPath() {
        if (this.getParentGroup() == null) {
            // always return a new instance, since our callers add to the end
            return new ArrayList<String>();
        } else {
            final List<String> res = this.getParentGroup().getPath();
            res.add(getName());
            assert res.size() % 2 == 0;
            return res;
        }
    }

    public final String getDefaultGroupName() {
        return this.defaultGroup;
    }

    protected final VirtualMenuGroup getGroup(String name) {
        VirtualMenuGroup res = this.groups.get(name);
        if (res == null) {
            res = new VirtualMenuGroup(this, name);
            this.groups.put(name, res);
        }
        return res;
    }

    public final VirtualMenu addAction(Action a) {
        return this.addItem(new JMenuItem(a));
    }

    public final VirtualMenu addItem(JMenuItem mi) {
        return this.addItem(mi, getDefaultGroupName());
    }

    public final VirtualMenu addItem(JMenuItem mi, String group) {
        this.getGroup(group).add(mi);
        return this;
    }

    public final VirtualMenu getSubmenu(final String name) {
        return this.getSubmenu(name, getDefaultGroupName());
    }

    public final VirtualMenu getSubmenu(final String name, final String group) {
        return this.getGroup(group).getMenu(name);
    }

    final VirtualMenu getSubmenu(List<String> path) {
        if (path.size() % 2 != 0)
            throw new IllegalArgumentException("Path should be group/submenu/... :" + path);
        VirtualMenu res = this;
        for (int i = 0; i < path.size(); i += 2) {
            res = res.getSubmenu(path.get(i + 1), path.get(i));
        }
        return res;
    }

    final VirtualMenu addItem(JMenuItem mi, List<String> path) {
        return this.getSubmenu(path.subList(0, path.size() - 1)).addItem(mi, path.get(path.size() - 1));
    }

    final void merge(VirtualMenu m) {
        for (final VirtualMenuGroup g : m.groups.values()) {
            this.getGroup(g.getName()).merge(g);
        }
        // lessen size constraints
        this.minSize = Math.min(this.minSize, m.minSize);
        this.maxSize = Math.max(this.maxSize, m.maxSize);
    }

    final void applySizeConstraints() {
        int size = 0;
        // depth first
        for (final VirtualMenuGroup g : this.groups.values()) {
            for (final VirtualMenu submenu : g.getMenus().values())
                submenu.applySizeConstraints();
            size += g.getItemCount();
        }
        if (size < this.minSize) {
            // if we have a parent menu with enough room
            if (this.getParentGroup() != null && this.getParentMenu().hasRoomFor(size)) {
                // add all items in this menu to a new group in our parent menu
                final VirtualMenuGroup group = getParentMenu().getGroup(getParentGroup().getName());
                for (final VirtualMenuGroup g : this.groups.values()) {
                    group.addAll(g.getItems());
                }
                this.getParentGroup().remove(this);
            }
        } else if (size > this.maxSize) {
            final List<Item> items = new ArrayList<Item>();
            for (final VirtualMenuGroup g : this.groups.values()) {
                for (final Item item : g.getItems())
                    items.add(item);
            }
            assert size == items.size();

            VirtualMenu submenu = null;
            for (int i = this.maxSize; i < size; i++) {
                final Item item = items.get(i);
                if (i % this.maxSize == 0) {
                    if (this.getParentGroup() == null) {
                        submenu = this.getSubmenu("suite " + (i / this.maxSize), null);
                    } else {
                        submenu = this.getParentMenu().getSubmenu(getName() + " (suite " + (i / this.maxSize) + ")", getParentGroup().getName());
                    }
                }
                submenu.getGroup(item.getParentGroup().getName()).add(item);
            }
        }
    }

    private int getItemCount() {
        int size = 0;
        for (final VirtualMenuGroup g : this.groups.values()) {
            size += g.getItemCount();
        }
        return size;
    }

    private boolean hasRoomFor(int size) {
        return this.maxSize - this.getItemCount() >= size;
    }

    Map<JMenuItem, List<String>> getItemsAndPath(final boolean recurse) {
        final Map<JMenuItem, List<String>> res = new LinkedHashMap<JMenuItem, List<String>>();
        for (final VirtualMenuGroup g : this.groups.values()) {
            for (final Item i : g.getItems())
                if (i instanceof LeafItem)
                    res.put(((LeafItem) i).getJMenuItem(), i.getPath());
                else if (recurse)
                    res.putAll(((VirtualMenu) i).getItemsAndPath(recurse));
        }
        return res;
    }

    // * getter

    final Map<JMenuItem, List<String>> getContent() {
        this.applySizeConstraints();
        return this.getItemsAndPath(true);
    }
}
