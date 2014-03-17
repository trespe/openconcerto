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

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class SwingThreadUtils {

    public static final void invoke(Runnable r) {
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }

    /**
     * Return the first ancestor of <code>component</code> (beginning with itself) that is an
     * {@link Class#isInstance(Object) instance of} <code>c</code>. If <code>component</code> is in
     * a popup, {@link JPopupMenu#getInvoker()} is used instead of {@link Component#getParent()}.
     * 
     * @param <C> type of class.
     * @param c the searched for class.
     * @param component the component.
     * @return the first ancestor or <code>null</code> if none match.
     */
    public static final <C> C getAncestorOrSelf(final Class<C> c, Component component) {
        while (component != null && !c.isInstance(component)) {
            if (component instanceof JPopupMenu) {
                // popups are in a JLayeredPane in the root pane
                component = ((JPopupMenu) component).getInvoker();
            } else {
                component = component.getParent();
            }
        }
        return c.cast(component);
    }

    /**
     * Tries to find an ID for the passed action from its {@link Action#getValue(String) properties}
     * . First use {@link Action#ACTION_COMMAND_KEY} then {@link Action#NAME}.
     * 
     * @param a an action.
     * @return the ID or <code>null</code> if none found.
     * @see #setActionID(Action, String)
     */
    public static final String getActionID(final Action a) {
        Object actionID = a.getValue(Action.ACTION_COMMAND_KEY);
        if (actionID == null)
            actionID = a.getValue(Action.NAME);
        return actionID == null ? null : actionID.toString();
    }

    /**
     * Make sure that {@link #getActionID(Action)} returns the passed string.
     * 
     * @param a an action.
     * @param id a string to be stored in {@link Action#ACTION_COMMAND_KEY}.
     */
    public static final void setActionID(final Action a, final String id) {
        a.putValue(Action.ACTION_COMMAND_KEY, id);
    }
}
