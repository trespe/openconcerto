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
 
 package org.openconcerto.ui.touch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ScrollableList extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener, ListDataListener {
    /**
	 * 
	 */
    private static final long serialVersionUID = 8176007217030710406L;

    private ListModel model;

    int offsetY = 0;// >0 si list remontée

    private int cellHeight;

    private int mousePressedY;
    private int selectedIndex = -1;

    private ScrollAnimator a;

    private int lastMouseY;

    private boolean hasScrolled;

    private List<ListSelectionListener> selectionListeners = new ArrayList<ListSelectionListener>();

    public ScrollableList(ListModel model) {
        a = new ScrollAnimator(this);
        this.model = model;
        cellHeight = 60;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
        model.addListDataListener(this);

    }

    @Override
    public void paint(Graphics g) {
        if (isOpaque()) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        int minIndex = getMinVisibleIndex();

        int maxIndex = getMaxVisibleIndex();
        int drawY = getYFromIndex(minIndex);
        for (int i = minIndex; i <= maxIndex; i++) {
            paintCell(g, model.getElementAt(i), i, (i == selectedIndex), drawY);
            drawY += cellHeight;
        }

    }

    public int getMinVisibleIndex() {
        int minIndex = getIndexFromY(0);
        if (minIndex < 0) {
            minIndex = 0;
        }
        return minIndex;
    }

    public int getMinFullyVisibleIndex() {
        int minIndex = getIndexFromY(0);
        if (minIndex < 0) {
            minIndex = 0;
        }

        if ((minIndex) * cellHeight - offsetY < 0) {
            minIndex++;
        }
        return minIndex;
    }

    public int getMaxVisibleIndex() {
        int maxIndex = getIndexFromY(this.getHeight());
        if (maxIndex >= model.getSize()) {
            maxIndex = model.getSize() - 1;
        }
        return maxIndex;
    }

    public int getMaxFullyVisibleIndex() {
        int maxIndex = getIndexFromY(this.getHeight());
        if (maxIndex >= model.getSize()) {
            maxIndex = model.getSize() - 1;
        }
        if (this.getHeight() % cellHeight > 0) {
            maxIndex--;
        }
        return maxIndex;
    }

    public void paintCell(Graphics g, Object object, int index, boolean selected, int posY) {

        g.setColor(Color.WHITE);

        g.fillRect(0, posY, this.getWidth(), this.cellHeight);
        g.setColor(Color.GRAY);

        g.drawLine(0, posY + this.cellHeight - 1, this.getWidth(), posY + this.cellHeight - 1);

        if (selected) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.GRAY);
        }
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        g.drawString(object.toString(), 20, posY + 43);
    }

    private int getIndexFromY(int y) {
        return (offsetY + y) / cellHeight;
    }

    private int getYFromIndex(int index) {
        return (index * cellHeight) - offsetY;
    }

    public void setFixedCellHeight(int h) {
        this.cellHeight = h;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public void setOffsetY(int y) {
        if (offsetY != y) {
            this.offsetY = y;
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.mousePressedY = e.getY();
        this.lastMouseY = mousePressedY;
        hasScrolled = false;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        long targetOffset = cellHeight * Math.round((((double) this.offsetY)) / this.cellHeight);

        if (!hasScrolled) {
            setSelectedIndex(getIndexFromY(e.getY()));
        } else {
            scrollToOffset(targetOffset);
        }
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, true);
    }

    public void setSelectedIndex(int index, boolean scroll) {
        System.err.println("ScrollableList.setSelectedIndex()! "+index);
        if (index != selectedIndex) {
            this.selectedIndex = index;
           System.err.println("ScrollableList.setSelectedIndex():"+index);
            if (scroll) {
                scrollToSelectedIndex();
            }
            fireSelectionChanged();
        }
        repaint();
    }

    public Object getSelectedValue() {
        if (selectedIndex < 0 || selectedIndex > this.model.getSize() - 1) {
            return null;
        }
        return model.getElementAt(selectedIndex);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Pas besoin de scroller, il y a plus de place que de lignes
        if (model.getSize() <= (int) this.getHeight() / cellHeight) {
            return;
        }
        int dy = e.getY() - this.lastMouseY;
        this.setOffsetY(this.offsetY - dy);
        this.lastMouseY = e.getY();
        if (Math.abs(lastMouseY - this.mousePressedY) > 10) {
            hasScrolled = true;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        long targetOffset = cellHeight * Math.round((((double) this.offsetY)) / this.cellHeight);
        targetOffset += e.getWheelRotation() * cellHeight * 2;

        scrollToOffset(targetOffset);

    }

    public void scrollToOffset(long targetOffset) {

        int max = (model.getSize()) * cellHeight - this.getHeight();
        if (targetOffset > max) {
            targetOffset = max;
        }
        if (targetOffset < 0) {
            targetOffset = 0;
        }

        a.stop();
        a.setStart(this.offsetY);
        a.setStop(targetOffset);
        AnimatorManager.getInstance().start(a);
    }

    public void scrollToSelectedIndex() {
        if (selectedIndex < 0 || selectedIndex > this.model.getSize() - 1) {
            return;
        }
        if (this.selectedIndex < getMinFullyVisibleIndex()) {
            scrollToOffset(this.selectedIndex * cellHeight);
        } else if (this.selectedIndex > getMaxFullyVisibleIndex()) {
            int targetOffset = (this.selectedIndex + 1) * cellHeight - this.getHeight();

            scrollToOffset(targetOffset);
        } else {
            if (offsetY < 0) {
                scrollToOffset(0);
            }

        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                DefaultListModel model1 = new DefaultListModel();
                for (int i = 0; i < 20; i++) {
                    model1.addElement("Item" + i);
                }

                DefaultListModel model2 = new DefaultListModel();
                for (int i = 0; i < 3; i++) {
                    model2.addElement("Item" + i);
                }

                final ScrollableList l1 = new ScrollableList(model1);
                final ScrollableList l2 = new ScrollableList(model2);

                JFrame f = new JFrame();
                JPanel p = new JPanel();
                p.setLayout(new GridLayout(1, 3));
                p.add(l1);

                JButton comp = new JButton("ScrollToSelected");
                p.add(comp);
                p.add(l2);
                f.setContentPane(p);
                f.setSize(600, 400);
                f.setLocation(10, 80);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);
                comp.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        l1.scrollToSelectedIndex();
                        l2.scrollToSelectedIndex();
                    }
                });
            }
        });

    }

    public void clearSelection() {
        if (this.selectedIndex != -1) {
            this.selectedIndex = -1;
            fireSelectionChanged();
            repaint();
        }
    }

    public void setSelectedValue(Object articleSelected, boolean scroll) {
        if (articleSelected == null) {
            clearSelection();
            return;
        }

        int size = model.getSize();
        for (int i = 0; i < size; i++) {
            Object o = model.getElementAt(i);
            if (o.equals(articleSelected)) {
                this.setSelectedIndex(i);
                break;
            }
        }
    }

    public void addListSelectionListener(ListSelectionListener selectionListener) {
        this.selectionListeners.add(selectionListener);
    }

    void fireSelectionChanged() {
        int size = this.selectionListeners.size();
        for (int i = 0; i < size; i++) {
            this.selectionListeners.get(i).valueChanged(new ListSelectionEvent(this, this.selectedIndex, this.selectedIndex, false));
        }
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        this.offsetY = 0;
        repaint();
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        contentsChanged(e);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        contentsChanged(e);
    }

}
