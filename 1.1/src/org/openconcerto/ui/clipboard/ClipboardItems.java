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
 
 package org.openconcerto.ui.clipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

public class ClipboardItems {

    public static void addJPopupMenu(final JTextComponent txt) {
        final JPopupMenu popup = new JPopupMenu();
        final List<JMenuItem> items = getMenuItems(txt);
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            popup.add(items.get(i));
        }

        txt.addMouseListener(new MouseAdapter() {
            private void maybeShowPopup(final MouseEvent e) {
                if (e.isPopupTrigger() && ((JComponent) e.getSource()).contains(e.getX(), e.getY())) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    // hide if we click elsewhere in the same component or if we click outside of it
                    popup.setVisible(false);
                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                this.maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                this.maybeShowPopup(e);
            }
        });
    }

    public static List<JMenuItem> getMenuItems(final JTextComponent txt) {
        final JMenuItem cut = new JMenuItem(new AbstractAction("Couper") {
            /**
             * 
             */
            private static final long serialVersionUID = -3130393168610349906L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                txt.cut();
            }
        });
        final JMenuItem copy = new JMenuItem(new AbstractAction("Copier") {
            /**
             * 
             */
            private static final long serialVersionUID = -2356236723988987848L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                txt.copy();
            }
        });
        final JMenuItem paste = new JMenuItem(new AbstractAction("Coller") {
            /**
             * 
             */
            private static final long serialVersionUID = -7908878173661917911L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                txt.paste();
            }
        });

        // disable when no selection
        txt.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                final boolean emptySel = e.getDot() == e.getMark();
                cut.setEnabled(!emptySel);
                copy.setEnabled(!emptySel);
            }
        });
        // when nothing to paste
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        paste.setEnabled(hasString(clipboard));
        clipboard.addFlavorListener(new FlavorListener() {
            @Override
            public void flavorsChanged(final FlavorEvent e) {
                final Clipboard cb = (Clipboard) e.getSource();
                paste.setEnabled(hasString(cb));
            }
        });

        return Arrays.asList(cut, copy, paste);
    }

    private static boolean hasString(final Clipboard cb) {
        boolean containsString;
        try {
            containsString = Arrays.asList(cb.getAvailableDataFlavors()).contains(DataFlavor.stringFlavor);
        } catch (final Exception e) {
            containsString = false;
        }
        return containsString;
    }
}
