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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;

public class TouchScrollPane extends JScrollPane implements MouseListener, MouseMotionListener {

    private JComponent child;
    int dy = 0;
    int olddy = 0;
    private int mousePressedY;
    private double viewY;
    private int lastLocationOnscreen;

    public TouchScrollPane(JComponent l) {
        super(l);
        this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        if (!(l instanceof Scrollable)) {
            throw new IllegalArgumentException("Argument is not implementing Scrollable");
        }
        this.child = l;
        child.addMouseListener(this);
        child.addMouseMotionListener(this);

    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        super.paint(g);
    }

    /*
     * @Override public void paint(Graphics g, int x, int y, int width, int height) { int oldOffsetY
     * = g.getOffsetY(); g.setColor(Color.RED); g.fillRect(0, 0, this.getWidth(), this.getHeight());
     * g.setOffset(g.getOffsetX(), oldOffsetY + dy); child.paint(g, x, y, width, height);
     * g.setOffset(g.getOffsetX(), oldOffsetY); }
     */

    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame f = new JFrame();
        Vector<String> v = new Vector<String>();
        for (int i = 0; i < 100; i++) {
            v.add("Item " + i);
        }

        JList l = new JList(v);
        l.putClientProperty("List.isFileList", Boolean.TRUE);
        l.setFixedCellHeight(50);
        TouchScrollPane t = new TouchScrollPane(l);
        f.setContentPane(t);
        f.setSize(200, 400);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.show();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("TouchScrollPane.mouseClicked()");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("TouchScrollPane.mouseEntered()");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("TouchScrollPane.mouseExited()");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.mousePressedY = e.getLocationOnScreen().y;
        System.out.println("TouchScrollPane.mousePressed():" + e.getY());

        viewY = this.getViewport().getViewPosition().getY();
        e.consume();

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("TouchScrollPane.mouseReleased()");
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        e.consume();
        if (e.getLocationOnScreen().y == lastLocationOnscreen) {
            return;
        }

        lastLocationOnscreen = e.getLocationOnScreen().y;
        System.out.println("TouchScrollPane.mouseDragged()");
        // final int newDy = olddy + (e.getY() - this.mousePressedY);
        //
        // dy = newDy;
        // if (dy >= 0) {
        // dy = 0;
        // }
        // if (dy <= -(child.getHeight() - this.getHeight())) {
        // dy = -(child.getHeight() - this.getHeight());
        // }
        //
        // this.getViewport().setViewPosition(new Point(0,dy));
        int xDiff = 0;
        int yDiff = mousePressedY;
        System.out.println("DY:" + yDiff);
        JViewport jv = this.getViewport();
        Point p = jv.getViewPosition();
        int newX = 0;// p.x - (e.getX() - xDiff);
        int deplacementY = e.getLocationOnScreen().y - yDiff;
        int newY = (int) viewY - deplacementY;
        System.out.println("View Position:" + p.y);
        System.out.println("Decallage depuis mouse pressed:" + deplacementY + "(" + e.getLocationOnScreen().y + " - " + yDiff + ")");
        int maxX = child.getWidth() - jv.getWidth();
        int maxY = child.getHeight() - jv.getHeight();
        /*
         * if (newX < 0) newX = 0; if (newX > maxX) newX = maxX;
         */
        if (newY < 0)
            newY = 0;/*
                      * if (newY > maxY) newY = maxY;
                      */

        jv.setViewPosition(new Point(newX, newY));
        System.out.println(newY);
        e.consume();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("TouchScrollPane.mouseMoved()");
    }

}
