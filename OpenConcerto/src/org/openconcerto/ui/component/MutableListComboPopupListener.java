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
 
 package org.openconcerto.ui.component;

import static org.openconcerto.ui.component.ComboLockedMode.UNLOCKED;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MutableListComboPopupListener extends MouseAdapter {

    private final MutableListCombo combo;
    private JPopupMenu popup;

    public MutableListComboPopupListener(MutableListCombo combo) {
        this.combo = combo;
        this.popup = null;

    }

    public void listen() {
        this.combo.getPopupComp().addMouseListener(this);
    }

    public final void mouseReleased(MouseEvent ev) {
        // right-click only, respect enabled status
        if (ev.getButton() != MouseEvent.BUTTON3 || !this.combo.getPopupComp().isEnabled()) {
            return;
        }

        if (this.popup == null) {
            this.popup = new JPopupMenu();
            final JMenuItem menu1 = new JMenuItem("Ajouter à la liste");
            this.popup.add(menu1);
            menu1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MutableListComboPopupListener.this.combo.addCurrentText();
                }
            });

            final JMenuItem menu2 = new JMenuItem("Supprimer de la liste");
            this.popup.add(menu2);
            menu2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MutableListComboPopupListener.this.combo.removeCurrentText();
                }
            });
        }
        // Bouton droit en non locked
        // On debloque le lock en faisant CTRL + bouton droit
        if (this.combo.getMode() == UNLOCKED || (ev.isControlDown() && this.combo.getMode() == ComboLockedMode.ITEMS_LOCKED)) {
            this.popup.show(this.combo.getPopupComp(), ev.getX(), ev.getY());
        }
    }
}
