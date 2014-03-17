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

import org.openconcerto.erp.config.MenuAndActions;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.view.SQLElementEditAction;
import org.openconcerto.sql.view.SQLElementListAction;
import org.openconcerto.ui.SwingThreadUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

/**
 * Allow a module to modify the main menu.
 * 
 * @author Sylvain
 * @see #getMenuAndActions()
 */
public final class MenuContext extends ElementContext {

    private final String moduleID;
    private final MenuAndActions menuAndActions;

    MenuContext(final MenuAndActions menuAndActions, final String moduleID, final SQLElementDirectory dir, final DBRoot root) {
        super(dir, root);
        this.moduleID = moduleID;
        this.menuAndActions = menuAndActions;
    }

    public final SQLElementEditAction createEditAction(final String table) {
        return new SQLElementEditAction(getElement(table));
    }

    public final SQLElementListAction createListAction(final String table) {
        return new SQLElementListAction(getElement(table));
    }

    public final void addMenuItem(final Action action, final String menu) {
        this.addMenuItem(action, Collections.singletonList(menu));
    }

    public final void addMenuItem(final Action action, final String menu, final String group) {
        this.addMenuItem(action, new String[] { menu, group });
    }

    public final void addMenuItem(final Action action, final String... path) {
        this.addMenuItem(action, Arrays.asList(path));
    }

    /**
     * Adds a menu item to the main frame. The menu item must not exist. To find the ID needed by
     * {@link MenuAndActions} this method prefixes (if necessary) the module ID to the
     * {@link SwingThreadUtils#getActionID(Action) action ID}.
     * 
     * @param action the action to perform.
     * @param path where to add the item.
     * @throws IllegalStateException if the action already exists.
     * @see MenuAndActions#addMenuItem(Action, String, List, boolean)
     */
    public final void addMenuItem(final Action action, final List<String> path) throws IllegalStateException {
        final String actionID = SwingThreadUtils.getActionID(action);
        final String uniqID;
        if (actionID == null || actionID.trim().length() == 0) {
            uniqID = this.moduleID + '/' + action.getClass().getName();
        } else if (!actionID.startsWith(this.moduleID)) {
            uniqID = this.moduleID + '/' + actionID;
        } else {
            uniqID = actionID;
        }
        assert uniqID.startsWith(this.moduleID);
        getMenuAndActions().addMenuItem(action, uniqID, path, false);
    }

    /**
     * The main menu and its actions.
     * 
     * @return the menu.
     * @see #addMenuItem(Action, List) for simple needs.
     */
    public final MenuAndActions getMenuAndActions() {
        return this.menuAndActions;
    }
}
