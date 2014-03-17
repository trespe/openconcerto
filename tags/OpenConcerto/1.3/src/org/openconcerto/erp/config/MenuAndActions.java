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
 
 package org.openconcerto.erp.config;

import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public final class MenuAndActions {

    @GuardedBy("this")
    private final Group group;
    @GuardedBy("this")
    private final Map<String, Action> actions;

    public MenuAndActions() {
        this(new Group("menu.main"), Collections.<String, Action> emptyMap());
    }

    public MenuAndActions(final Group group, final Map<String, Action> actions) {
        this.group = Group.copy(group, null);
        this.actions = new HashMap<String, Action>(actions);
    }

    public final synchronized MenuAndActions copy() {
        return new MenuAndActions(this.getGroup(), this.actions);
    }

    public final void putAction(final Action a) {
        this.putAction(a, null);
    }

    @Deprecated
    public final void registerAction(String id, Action a) {
        this.putAction(a, id);
    }

    public final void putAction(final Action a, final String id) {
        this.putAction(a, id, false);
    }

    /**
     * Add an action.
     * 
     * @param a the action to add.
     * @param id the ID, if <code>null</code> taken from the
     *        {@link SwingThreadUtils#getActionID(Action) action}.
     * @param canReplace <code>true</code> if the passed action can replace an existing one.
     * @return the used ID.
     */
    public final String putAction(final Action a, final String id, final boolean canReplace) {
        return this.putAction(a, id, canReplace, false);
    }

    private final synchronized String putAction(final Action a, String id, final boolean canReplace, final boolean dryRun) {
        if (a == null)
            throw new NullPointerException("Null action");
        if (id == null)
            id = SwingThreadUtils.getActionID(a);
        if (id == null)
            throw new NullPointerException("Null ID");
        if (!canReplace && this.actions.containsKey(id))
            throw new IllegalStateException("ID exists : " + this.actions.get(id));
        if (!dryRun)
            this.actions.put(id, a);
        return id;
    }

    public final synchronized Action getAction(final String id) {
        return this.actions.get(id);
    }

    /**
     * Return the group modeling the menu.
     * 
     * @return a frozen group.
     */
    public final synchronized Group getGroup() {
        return this.group;
    }

    public void addMenuItem(final Action action, final List<String> path) {
        this.addMenuItem(action, null, path);
    }

    public void addMenuItem(final Action action, final String actionID, final List<String> path) {
        this.addMenuItem(action, actionID, path, false);
    }

    /**
     * Adds a menu item to this menu. The path should be an alternation of menu and group within
     * that menu. All items within the same group will be grouped together inside separators. Menus
     * will be created as needed.
     * 
     * @param action the action to perform.
     * @param actionID ID of the action, see {@link #putAction(Action, String, boolean)}.
     * @param path where to add the menu item.
     * @param canReplace <code>true</code> if this method can replace an existing action and menu
     *        item.
     * @return the menu item.
     * @throws IllegalArgumentException if path is empty.
     * @throws IllegalStateException if <code>actionID</code> already exists in either the menu or
     *         the actions.
     */
    public Item addMenuItem(final Action action, String actionID, final List<String> path, final boolean canReplace) throws IllegalStateException {
        if (path.size() == 0)
            throw new IllegalArgumentException("Empty path");

        final Item res;
        synchronized (this) {
            // check actionID
            actionID = this.putAction(action, actionID, canReplace, true);

            // check and modify group
            final Group groupDesc = this.getGroup().followPath(path, true);
            final Item child = groupDesc.getChildFromID(actionID);
            if (!canReplace && child != null) {
                throw new IllegalStateException("ID exists : " + child);
            }
            if (child == null) {
                res = new Item(actionID);
                groupDesc.add(res);
            } else {
                res = child;
            }

            // modify action
            this.putAction(action, actionID, canReplace, false);
        }
        assert res != null;
        return res;
    }

    public void setMenuItemVisible(final String actionID, final boolean v) {
        synchronized (this) {
            final Item mi = this.getGroup().getDescFromID(actionID);
            mi.setLocalHint(mi.getLocalHint().getBuilder().setVisible(v).build());
        }
    }
}
