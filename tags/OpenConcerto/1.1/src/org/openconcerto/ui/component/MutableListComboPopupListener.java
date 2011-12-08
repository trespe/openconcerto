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
import org.openconcerto.ui.PopupMouseListener;
import org.openconcerto.ui.SwingThreadUtils;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MutableListComboPopupListener {

    static private final Set<JPopupMenu> toClose = new HashSet<JPopupMenu>();
    static private final PropertyChangeListener propL = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final JPopupMenu src = (JPopupMenu) evt.getSource();
            if (!src.isVisible()) {
                removePopup(src);
            }
        }
    };
    /**
     * normally BasicPopupMenuUI.MouseGrabber.eventDispatched() closes popups, but BasicComboBoxUI
     * sets HIDE_POPUP_KEY to indicate that it will take care of all popups, including ours, so we
     * must mimic its behavior for JComboBox.
     */
    static private final AWTEventListener l = new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
            final MouseEvent me = (MouseEvent) event;
            if (me.getID() != MouseEvent.MOUSE_PRESSED && me.getID() != MouseEvent.MOUSE_WHEEL)
                return;

            final Component src = me.getComponent();
            final JPopupMenu menuClicked = SwingThreadUtils.getAncestorOrSelf(JPopupMenu.class, src);
            final Set<JPopupMenu> sansClicked = new HashSet<JPopupMenu>(toClose);
            if (menuClicked != null)
                sansClicked.remove(menuClicked);
            for (JPopupMenu menu : sansClicked) {
                menu.setVisible(false);
            }
        }
    };

    static private void addPopup(JPopupMenu p) {
        // ATTN the popup might not yet be visible
        if (toClose.add(p) && toClose.size() == 1) {
            Toolkit.getDefaultToolkit().addAWTEventListener(l, AWTEvent.MOUSE_EVENT_MASK);
        }
        // if the popup is closed for any reason (programmatically, menu item chosen), we no longer
        // need to handle it
        p.addPropertyChangeListener("visible", propL);
    }

    static private void removePopup(JPopupMenu p) {
        p.removePropertyChangeListener("visible", propL);
        if (toClose.remove(p) && toClose.size() == 0) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(l);
        }
    }

    private final MutableListCombo combo;
    private JPopupMenu popup;

    public MutableListComboPopupListener(MutableListCombo combo) {
        this.combo = combo;
        this.popup = null;

    }

    public void listen() {
        this.combo.getPopupComp().addMouseListener(new PopupMouseListener() {
            @Override
            protected JPopupMenu createPopup(MouseEvent e) {
                return getPopup(e);
            }
        });
    }

    protected final JPopupMenu getPopup(MouseEvent ev) {
        // respect enabled status
        if (!this.combo.getPopupComp().isEnabled())
            return null;
        // Bouton droit en non locked
        // On debloque le lock en faisant CTRL + bouton droit
        final boolean displayPopup = this.combo.getMode() == UNLOCKED || (ev.isControlDown() && this.combo.getMode() == ComboLockedMode.ITEMS_LOCKED);
        if (!displayPopup)
            return null;

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
        // popups are never closed in a JComboBox (except when choosing a menu item)
        if (SwingThreadUtils.getAncestorOrSelf(JComboBox.class, this.combo.getPopupComp()) != null)
            addPopup(this.popup);
        return this.popup;
    }
}
