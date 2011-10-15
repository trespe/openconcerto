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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;

/**
 * A group inside a {@link VirtualMenu menu}.
 * 
 * @author Sylvain CUAZ
 */
class VirtualMenuGroup {

    /**
     * An item inside a group.
     */
    static abstract class Item {
        private VirtualMenuGroup parent;

        protected Item(final VirtualMenuGroup parent) {
            this.setParentGroup(parent);
        }

        final void setParentGroup(VirtualMenuGroup parent) {
            // parent can be null for the root menu, or when detached from our parent
            this.parent = parent;
        }

        final VirtualMenuGroup getParentGroup() {
            return this.parent;
        }

        protected abstract String getName();

        /**
         * The list of group and menu names from the root menu.
         * 
         * @return the path to this item.
         */
        abstract List<String> getPath();

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + getName();
        }
    }

    /**
     * A leaf item (i.e. a {@link JMenuItem}).
     */
    static class LeafItem extends Item {
        private final JMenuItem mi;

        LeafItem(final VirtualMenuGroup parent, final JMenuItem mi) {
            super(parent);
            this.mi = mi;
        }

        final JMenuItem getJMenuItem() {
            return this.mi;
        }

        @Override
        protected String getName() {
            return getJMenuItem().getText();
        }

        @Override
        final List<String> getPath() {
            return getParentGroup().getPath();
        }
    }

    private final VirtualMenu parent;
    private final String name;
    private final List<Item> items;
    private final Map<String, VirtualMenu> menus;

    VirtualMenuGroup(final VirtualMenu parent, final String name) {
        if (parent == null)
            throw new NullPointerException("Null parent");
        this.parent = parent;
        this.name = name;
        this.items = new ArrayList<Item>();
        this.menus = new LinkedHashMap<String, VirtualMenu>();
    }

    public final String getName() {
        return this.name;
    }

    public final VirtualMenu getParentMenu() {
        return this.parent;
    }

    public final List<String> getPath() {
        final List<String> res = this.getParentMenu().getPath();
        res.add(this.getName());
        return res;
    }

    public final int getItemCount() {
        return this.items.size();
    }

    public final List<Item> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public final void add(JMenuItem mi) {
        this.add(new LeafItem(this, mi));
    }

    public final void add(Item item) {
        this.items.add(item);
        item.setParentGroup(this);
        if (item instanceof VirtualMenu) {
            final VirtualMenu menu = (VirtualMenu) item;
            this.menus.put(menu.getName(), menu);
        }
    }

    public final void addAll(List<Item> items) {
        for (final Item item : items)
            this.add(item);
    }

    void merge(VirtualMenuGroup g) {
        for (final Item item : g.items)
            if (!(item instanceof VirtualMenu))
                this.add(item);
            else if (this.menus.containsKey(item.getName()))
                this.menus.get(item.getName()).merge((VirtualMenu) item);
            else
                // avoid creating empty menu just to merge (simplify minSize/maxSize handling)
                this.add(item);
    }

    public final void remove(Item item) {
        this.items.remove(item);
        item.setParentGroup(null);
        if (item instanceof VirtualMenu) {
            this.menus.remove(((VirtualMenu) item).getName());
        }
    }

    protected final VirtualMenu getMenu(final String name) {
        VirtualMenu res = this.menus.get(name);
        if (res == null) {
            res = VirtualMenu.createFromParentMenu(this, name);
            this.add(res);
        }
        return res;
    }

    public final Map<String, VirtualMenu> getMenus() {
        return Collections.unmodifiableMap(this.menus);
    }
}
