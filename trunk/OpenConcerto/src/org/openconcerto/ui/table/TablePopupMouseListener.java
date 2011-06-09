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

import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPopupMenu;
import javax.swing.JTable;

/**
 * To easily add a popup on a {@link JTable}.
 * 
 * @author Sylvain
 */
public final class TablePopupMouseListener extends MouseAdapter {

    /**
     * Add a {@link MouseListener} on <code>t</code> to select on MousePress and display a popup.
     * 
     * @param t the table.
     * @param popup which popup to display.
     */
    public static void add(JTable t, final ITransformer<MouseEvent, JPopupMenu> popup) {
        t.addMouseListener(new TablePopupMouseListener(popup));
    }

    private final ITransformer<MouseEvent, JPopupMenu> popup;

    private TablePopupMouseListener(final ITransformer<MouseEvent, JPopupMenu> popup) {
        this.popup = popup;
    }

    public void mousePressed(MouseEvent e) {
        // do as other apps : select on MousePress
        // BUTTON1 is already handled by Swing
        if (e.getButton() != MouseEvent.BUTTON1) {
            adjustSelection(e);
        }
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // select the line where the mouse is now
            adjustSelection(e);
            // ne pas afficher un menu vide
            final JPopupMenu menu = this.popup.transformChecked(e);
            if (menu != null && menu.getSubElements().length > 0) {
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private void adjustSelection(final MouseEvent e) {
        final JTable table = (JTable) e.getSource();
        final int row = table.rowAtPoint(e.getPoint());
        // ne changer la sélection que si nécessaire
        // (pour permettre de cliquer droit sur plusieurs lignes)
        // ATTN if click on the 2nd line of a selection of 3, getSelectedRow() will
        // return the first one => MAYBE add lastClickedRow for the popup items to use
        if (!table.getSelectionModel().isSelectedIndex(row))
            table.getSelectionModel().setSelectionInterval(row, row);
    }
}
