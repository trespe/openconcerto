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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.ui.list.JListMutable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

public class ITextWithCompletionPopUp extends JPopupMenu {
    static final int MAXROW = 10;

    JList list;

    private ListModel listModel;

    private int minWitdh = 150;

    private ITextWithCompletion text;

    ITextWithCompletionPopUp(final ListModel listModel, final ITextWithCompletion text) {
        this.text = text;
        this.list = new JList(listModel);
        this.listModel = listModel;

        this.list.setFocusable(false);
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.list.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                validateSelection();
            }
        });
        // scroller
        JScrollPane scroller = new JScrollPane(this.list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setFocusable(false);
        scroller.getVerticalScrollBar().setFocusable(false);
        scroller.setBorder(null);
        // Popup
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorderPainted(true);
        setBorder(BorderFactory.createLineBorder(Color.black));
        setOpaque(true);
        add(scroller);
        // setDoubleBuffered(true);
        setFocusable(false);

        this.list.addMouseMotionListener(new ListMouseMotionHandler());
    }

    public void setListEnabled(boolean b) {
        this.list.setEnabled(b);
        // this.list.setSelectionMode(ListSelectionModel.)
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int width = d.width;
        if (width > 500)
            width = 500;
        width = Math.max(width, this.minWitdh) + 2;
        int height = 2 + 17 * Math.min(listModel.getSize(), MAXROW);
        Dimension newD = new Dimension(width, height);
        return newD;
    }

    public void setMinWith(int i) {
        this.minWitdh = i;

    }

    protected class ListMouseMotionHandler extends MouseMotionAdapter {
        public void mouseMoved(MouseEvent anEvent) {
            Point location = anEvent.getPoint();
            // System.out.println(location);
            Rectangle r = new Rectangle();
            list.computeVisibleRect(r);
            if (r.contains(location)) {
                updateListBoxSelectionForEvent(anEvent, true);
            }
        }
    }

    protected void updateListBoxSelectionForEvent(MouseEvent anEvent, boolean shouldScroll) {
        // XXX - only seems to be called from this class. shouldScroll flag is
        // never true
        Point location = anEvent.getPoint();

        if (this.list == null)
            return;
        int index = this.list.locationToIndex(location);
        if (index == -1) {
            if (location.y < 0)
                index = 0;
            else
                index = listModel.getSize() - 1;
        }
        if (this.list.getSelectedIndex() != index) {
            this.list.setSelectedIndex(index);
            if (shouldScroll)
                this.list.ensureIndexIsVisible(index);
        }
    }

    public void selectNext() {

        int i = this.list.getSelectedIndex() + 1;
        if (i < this.listModel.getSize()) {
            this.list.setSelectedIndex(i);
            this.list.ensureIndexIsVisible(i);
        }
    }

    public void selectNextPage() {

        int i = Math.min(MAXROW + Math.max(this.list.getSelectedIndex(), 0), this.listModel.getSize() - 1);
        if (i < this.listModel.getSize()) {
            this.list.setSelectedIndex(i);
            this.list.ensureIndexIsVisible(i);
        }
    }

    public void selectPrevious() {

        int i = this.list.getSelectedIndex() - 1;
        if (i >= 0) {
            this.list.setSelectedIndex(i);
            this.list.ensureIndexIsVisible(i);
        } else {
            this.setVisible(false);
        }

    }

    public void selectPreviousPage() {
        int i = Math.max(0, this.list.getSelectedIndex() - MAXROW);
        this.list.setSelectedIndex(i);
        this.list.ensureIndexIsVisible(i);
    }

    public void validateSelection() {
        int sIndex = this.list.getSelectedIndex();
        if (sIndex >= 0) {
            IComboSelectionItem item = (IComboSelectionItem) listModel.getElementAt(sIndex);

            text.selectId(item.getId());
        }
    }
}
