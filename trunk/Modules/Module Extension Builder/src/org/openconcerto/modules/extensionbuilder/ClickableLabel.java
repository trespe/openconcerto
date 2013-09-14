package org.openconcerto.modules.extensionbuilder;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;

public class ClickableLabel extends JLabel implements MouseListener, MouseMotionListener {

    private Runnable runnable;
    private boolean mouseOver;

    public ClickableLabel(String text, final Runnable r) {
        super(text);
        this.runnable = r;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setForeground(Color.BLUE);
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (mouseOver) {
            Rectangle r = g.getClipBounds();
            final int y1 = r.height - getFontMetrics(getFont()).getDescent() + 1;
            g.drawLine(0, y1, getFontMetrics(getFont()).stringWidth(getText()), y1);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (this.runnable != null) {
            this.runnable.run();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        this.mouseOver = true;
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.mouseOver = false;
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
