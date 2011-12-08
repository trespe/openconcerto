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
 
 package org.openconcerto.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.MenuElement;

public class MenuUtils {

    private static final String GROUPNAME_PROPNAME = "GROUPNAME";
    private static final String DEFAULT_GROUPNAME = "defaultGroupName";

    static public <C extends JComponent & MenuElement> JMenuItem addMenuItem(final Action action, final C topLevelMenu) throws IllegalArgumentException {
        return addMenuItem(action, topLevelMenu, (String) null);
    }

    static public <C extends JComponent & MenuElement> JMenuItem addMenuItem(final Action action, final C topLevelMenu, final String... path) throws IllegalArgumentException {
        return addMenuItem(action, topLevelMenu, Arrays.asList(path));
    }

    /**
     * Adds a menu item to the passed menu. The path should be an alternation of group and menu
     * within that menu. All items within the same group will be grouped together inside separators.
     * Menus will be created as needed and groups' names can be <code>null</code>. NOTE: items added
     * with this method (even with null groups) will always be separated from items added directly.
     * 
     * @param action the action to perform.
     * @param topLevelMenu where to add the menu item.
     * @param path where to add the menu item.
     * @return the newly created item.
     * @throws IllegalArgumentException if path length is not odd.
     */
    static public <C extends JComponent & MenuElement> JMenuItem addMenuItem(final Action action, final C topLevelMenu, final List<String> path) throws IllegalArgumentException {
        return addMenuItem(new JMenuItem(action), topLevelMenu, path);
    }

    static public <C extends JComponent & MenuElement> JMenuItem addMenuItem(final JMenuItem mi, final C topLevelMenu, final List<String> path) throws IllegalArgumentException {
        if (path.size() == 0 || path.size() % 2 == 0)
            throw new IllegalArgumentException("Path should be of the form group/menu/group/... : " + path);
        JComponent menu = topLevelMenu;
        for (int i = 0; i < path.size() - 1; i += 2) {
            final String groupName = path.get(i);
            final String menuName = path.get(i + 1);
            menu = addChild(menu, groupName, new JMenu(menuName), JMenu.class, false);
        }
        final String actionGroupName = path.get(path.size() - 1);
        return addChild(menu, actionGroupName, mi, JMenuItem.class, true);
    }

    static private Component[] getChildren(final Container c) {
        return c instanceof JMenu ? ((JMenu) c).getMenuComponents() : c.getComponents();
    }

    // finds a child like created in the passed group or adds created to the passed group in menu
    static private <T extends JMenuItem> T addChild(final Container c, String groupName, final T created, final Class<T> clazz, final boolean alwaysAdd) {
        if (groupName == null)
            groupName = DEFAULT_GROUPNAME;
        final Component[] children = getChildren(c);
        final int[] groupRange = getRange(children, groupName);
        final T res;
        if (groupRange == null) {
            if (children.length > 0)
                c.add(new JSeparator());
            res = created;
            c.add(res);
        } else {
            final T existingChild = alwaysAdd ? null : findChild(children, groupRange, created.getText(), clazz);
            if (existingChild != null) {
                res = existingChild;
            } else {
                res = created;
                // add after the last of the group (groupRange[1] is exclusive)
                // from Container.addImpl() index can be equal to count() to add at the end
                c.add(res, groupRange[1]);
            }
        }
        res.putClientProperty(GROUPNAME_PROPNAME, groupName);
        return res;
    }

    // search for the range (inclusive, exclusive) of children that are in the passed group
    static private int[] getRange(final Component[] children, String groupName) {
        int min = -1;
        int max = -1;
        for (int i = 0; i < children.length; i++) {
            if (groupName.equals(((JComponent) children[i]).getClientProperty(GROUPNAME_PROPNAME))) {
                if (min < 0)
                    min = i;
                if (max < i)
                    max = i;
            }
        }
        // inclusive, exclusive
        return min == -1 ? null : new int[] { min, max + 1 };
    }

    /**
     * Find a child with the passed class and text.
     * 
     * @param <T> type of child
     * @param c the parent.
     * @param name the text of the child.
     * @param clazz the class of the child.
     * @return the child found or <code>null</code>.
     */
    static public <T extends JMenuItem> T findChild(final Container c, final String name, Class<T> clazz) {
        return findChild(getChildren(c), null, name, clazz);
    }

    static private <T extends JMenuItem> T findChild(final Component[] children, int[] range, final String name, Class<T> clazz) {
        if (range == null)
            range = new int[] { 0, children.length };
        for (int groupIndex = range[0]; groupIndex < range[1]; groupIndex++) {
            final Component child = children[groupIndex];
            if (clazz == child.getClass()) {
                final T casted = clazz.cast(child);
                if (name.equals(casted.getText())) {
                    return casted;
                }
            }
        }
        return null;
    }

    /**
     * Remove the passed item from its menu. This method handles the cleanup of separator and empty
     * menus.
     * 
     * @param item the item to remove.
     */
    static public void removeMenuItem(final JMenuItem item) {
        Container parent = detachItem(item);
        // have to use getAncestorOrSelf() since there are JPopupMenu everywhere
        JMenu menu = SwingThreadUtils.getAncestorOrSelf(JMenu.class, parent);
        while (menu != null && menu.getMenuComponentCount() == 0) {
            parent = detachItem(menu);
            menu = SwingThreadUtils.getAncestorOrSelf(JMenu.class, parent);
        }
    }

    // remove item from its parent and cleanup separator
    static private Container detachItem(final JComponent item) {
        Container parent = item.getParent();
        final Component[] siblings = parent.getComponents();
        final int index = Arrays.asList(siblings).indexOf(item);
        final boolean sepBefore = index > 0 && siblings[index - 1] instanceof JSeparator;
        final boolean sepAfter = index < siblings.length - 1 && siblings[index + 1] instanceof JSeparator;
        parent.remove(index);
        // ATTN siblings.length is before removing item
        if (siblings.length > 1) {
            if ((sepBefore && sepAfter) || (sepAfter && index == 0)) {
                parent.remove(index);
            } else if (sepBefore && index == siblings.length - 1) {
                parent.remove(index - 1);
            }
        }
        ((JComponent) parent).revalidate();
        // needed at least when removing the last menu of a menu bar
        parent.repaint();
        return parent;
    }
}
