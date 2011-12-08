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

import org.openconcerto.utils.cc.ConstantFactory;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * To easily add a popup.
 * 
 * @author Sylvain
 */
public class PopupMouseListener extends MouseAdapter {

    private final ITransformer<MouseEvent, JPopupMenu> popup;

    public PopupMouseListener() {
        this((JPopupMenu) null);
    }

    public PopupMouseListener(final JPopupMenu menu) {
        this(ConstantFactory.<MouseEvent, JPopupMenu> createTransformer(menu));
    }

    public PopupMouseListener(final ITransformer<MouseEvent, JPopupMenu> popup) {
        this.popup = popup;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // ne pas afficher un menu vide
            final JPopupMenu menu = createPopup(e);
            if (menu != null && menu.getSubElements().length > 0) {
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    protected JPopupMenu createPopup(MouseEvent e) {
        return this.popup.transformChecked(e);
    }
}
