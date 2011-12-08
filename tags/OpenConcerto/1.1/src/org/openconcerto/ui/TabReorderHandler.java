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

import java.awt.event.MouseEvent;

import javax.swing.JTabbedPane;
import javax.swing.event.MouseInputAdapter;

public class TabReorderHandler extends MouseInputAdapter {
    public static void enableReordering(JTabbedPane pane) {
        TabReorderHandler handler = new TabReorderHandler(pane);
        pane.addMouseListener(handler);
        pane.addMouseMotionListener(handler);
    }

    private JTabbedPane tabPane;
    private int draggedTabIndex;

    protected TabReorderHandler(JTabbedPane pane) {
        this.tabPane = pane;
        draggedTabIndex = -1;
    }

    public void mouseReleased(MouseEvent e) {
        draggedTabIndex = -1;
    }

    public void mouseDragged(MouseEvent e) {
        if (draggedTabIndex == -1) {
            return;
        }

        int targetTabIndex = tabPane.getUI().tabForCoordinate(tabPane, e.getX(), e.getY());
        if (targetTabIndex != -1 && targetTabIndex != draggedTabIndex) {
            boolean isForwardDrag = targetTabIndex > draggedTabIndex;
            tabPane.insertTab(tabPane.getTitleAt(draggedTabIndex), tabPane.getIconAt(draggedTabIndex), tabPane.getComponentAt(draggedTabIndex), tabPane.getToolTipTextAt(draggedTabIndex),
                    isForwardDrag ? targetTabIndex + 1 : targetTabIndex);
            draggedTabIndex = targetTabIndex;
            tabPane.setSelectedIndex(draggedTabIndex);
        }
    }

}
