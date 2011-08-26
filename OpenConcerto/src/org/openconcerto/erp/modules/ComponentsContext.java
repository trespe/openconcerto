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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.view.DropManager;
import org.openconcerto.sql.view.FileDropHandler;
import org.openconcerto.sql.view.SQLMenuItemHelper;
import org.openconcerto.sql.view.SQLMenuItemHelper.SQLMenuItemAction;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.utils.CollectionMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import org.openconcerto.sql.view.list.RowActionFactory;

/**
 * Allow a module to add JComponent to edit fields.
 * 
 * @author Sylvain
 */

public final class ComponentsContext {

    private final SQLElementDirectory dir;
    private final DBRoot root;
    private final Set<String> createdTables;
    // only for non-created tables
    private final CollectionMap<String, String> createdFields;
    // * module items
    private final CollectionMap<SQLElement, String> fields;
    private final CollectionMap<SQLElement, RowAction> rowActions;
    private final List<JMenuItem> menuItems;

    ComponentsContext(SQLElementDirectory dir, DBRoot root, final Set<String> tables, final Set<SQLName> fields) {
        super();
        this.dir = dir;
        this.root = root;
        this.createdTables = tables;
        this.createdFields = new CollectionMap<String, String>();
        for (final SQLName f : fields) {
            assert f.getItemCount() == 2;
            final String tableName = f.getFirst();
            if (!this.createdTables.contains(tableName))
                this.createdFields.put(tableName, f.getItem(1));
        }
        this.fields = new CollectionMap<SQLElement, String>();
        this.rowActions = new CollectionMap<SQLElement, RowAction>();
        this.menuItems = new ArrayList<JMenuItem>();
    }

    private final DBRoot getRoot() {
        return this.root;
    }

    private final SQLElement checkField(final String tableName, final String name) {
        if (this.createdTables.contains(tableName))
            throw new IllegalArgumentException("The table " + tableName + " was created by this module");
        if (!this.createdFields.getNonNull(tableName).contains(name))
            throw new IllegalArgumentException("The field " + new SQLName(tableName, name).quote() + " wasn't created by this module");
        return getElement(tableName);
    }

    public final SQLElement getElement(final String tableName) {
        return this.dir.getElement(this.getRoot().getTable(tableName));
    }

    public final void putAdditionalField(final String tableName, final String name) {
        final SQLElement elem = checkField(tableName, name);
        if (elem.putAdditionalField(name)) {
            this.fields.put(elem, name);
        } else {
            throw new IllegalStateException("Already added " + name + " in " + elem);
        }
    }

    public final void putAdditionalField(final String tableName, final String name, final JTextComponent comp) {
        final SQLElement elem = checkField(tableName, name);
        if (elem.putAdditionalField(name, comp)) {
            this.fields.put(elem, name);
        } else {
            throw new IllegalStateException("Already added " + name + " in " + elem);
        }
    }

    public final void putAdditionalField(final String tableName, final String name, final SQLTextCombo comp) {
        final SQLElement elem = checkField(tableName, name);
        if (elem.putAdditionalField(name, comp)) {
            this.fields.put(elem, name);
        } else {
            throw new IllegalStateException("Already added " + name + " in " + elem);
        }
    }

    final CollectionMap<SQLElement, String> getFields() {
        return this.fields;
    }

    // * actions

    public final void addRowAction(final String tableName, final RowAction action) {
        final SQLElement elem = getElement(tableName);
        this.rowActions.put(elem, action);
        elem.getRowActions().add(action);
    }

    public final void addRowActionFactory(final RowActionFactory factory) {
        final SQLElement elem = getElement(factory.getTableName());
        elem.getRowActionFactories().add(factory);
    }

    final CollectionMap<SQLElement, RowAction> getRowActions() {
        return this.rowActions;
    }

    public final SQLMenuItemAction createEditAction(final String table) {
        return SQLMenuItemHelper.INSTANCE.createEditAction(getElement(table));
    }

    public final SQLMenuItemAction createListAction(final String table) {
        return SQLMenuItemHelper.INSTANCE.createListAction(getElement(table));
    }

    public final void addMenuItem(final Action action, final String menu) {
        this.addMenuItem(action, menu, null);
    }

    public final void addMenuItem(final Action action, final String menu, final String group) {
        this.addMenuItem(action, new String[] { menu, group });
    }

    public final void addMenuItem(final Action action, final String... path) {
        this.addMenuItem(action, Arrays.asList(path));
    }

    /**
     * Adds a menu item to the main frame.
     * 
     * @param action the action to perform.
     * @param path where to add the item.
     * @see MainFrame#addMenuItem(Action, List)
     */
    public final void addMenuItem(final Action action, final List<String> path) {
        final JMenuItem item = MainFrame.getInstance().addMenuItem(action, path);
        this.menuItems.add(item);
    }

    final Collection<JMenuItem> getMenuItems() {
        return this.menuItems;
    }

    public final void addFileDropHandler(SQLTable table, FileDropHandler handler) {
        DropManager.getInstance().add(table, handler);
    }
}
