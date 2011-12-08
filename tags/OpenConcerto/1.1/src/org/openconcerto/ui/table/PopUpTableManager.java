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
 
 package org.openconcerto.ui.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

public class PopUpTableManager extends MouseAdapter {

    private transient final JTable table;
    private transient final TablePopupMenuProvider provider;

    public PopUpTableManager(final JTable table, final TablePopupMenuProvider provider) {
        super();
        table.addMouseListener(this);
        this.table = table;
        this.provider = provider;
    }

    public void mousePressed(final MouseEvent event) {
        showPopup(event);
    }

    public void mouseReleased(final MouseEvent event) {
        showPopup(event);
    }

    private void showPopup(final MouseEvent event) {
        if (event.isPopupTrigger()) {
            final JPopupMenu popupMenu = new JPopupMenu();
            final int row = table.rowAtPoint(event.getPoint());
            final List<Action> actions = provider.getActions(table, row);
            final int size = actions.size();
            for (int i = 0; i < size; i++) {
                popupMenu.add(actions.get(i));
            }
            popupMenu.show(event.getComponent(), event.getX(), event.getY());
        }
    }
}
