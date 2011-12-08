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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DateFormat;
import java.util.Calendar;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class RangeSlider extends JPanel implements ComponentListener, MouseListener, MouseMotionListener {

    private boolean isResized;
    private Image backgroundImage;
    private Image cursor1;
    private Image cursor2;
    private int cursor_size;
    private int width;
    private int height;
    private int margin;

    private int c1_pos; // en pixel
    private int c2_pos;

    private double tk;
    private int old_pos;
    private int scaleY;
    private int last_dragged;
    private int precision;
    private long min;
    private long max;
    private double cur_min;
    private double cur_max;
    private boolean isCurMinSet;
    private boolean isCurMaxSet;

    protected boolean draggable1;
    protected boolean draggable2;

    // Vignettes de date
    private String leftHintText;
    private Rectangle leftHintRect;
    private String rightHintText;
    private Rectangle rightHintRect;

    private Font font;
    private FontMetrics fm;
    private static Calendar cal = Calendar.getInstance();
    private int year;
    long COEF = (24 * 3600 * 1000); // nb de millis par jour

    public RangeSlider(int year) {
        // si c'est 2005, on va de 2004 a 2005
        this.year = year;
        isResized = true;
        cursor_size = 20;
        margin = 5;
        width = 2 * margin + getNbDayOfYear(year - 1) + getNbDayOfYear(year);
        height = 50;

        // msize = 5;
        last_dragged = 1;
        isCurMinSet = false;
        isCurMaxSet = false;
        // isPrecSet = false;
        draggable1 = false;
        draggable2 = false;

        addComponentListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        cursor1 = new ImageIcon(this.getClass().getResource("cursor.png")).getImage();
        cursor2 = new ImageIcon(this.getClass().getResource("cursor.png")).getImage();
        min = timeOfFirstDayOfYear(year - 1);
        // System.out.println("min:" + min);
        max = timeOfLastDayOfYear(year);
        // System.out.println("max:" + max);
        // setBackground(new Color(153, 153, 153));
        this.c1_pos = 0;
        this.c2_pos = 365;

    }

    private static long timeOfFirstDayOfYear(int ayear) {
        cal.set(ayear, Calendar.JANUARY, 1);
        return cal.getTimeInMillis();
    }

    private static long timeOfLastDayOfYear(int ayear) {
        cal.set(ayear, Calendar.DECEMBER, 31);
        return cal.getTimeInMillis();
    }

    /**
     * @param ayear
     * @return
     */
    private static int getNbDayOfYear(int ayear) {
        cal.set(ayear, Calendar.DECEMBER, 31);
        int nbday = cal.get(Calendar.DAY_OF_YEAR);
        return nbday;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        font = this.getFont();

        if (isResized) {
            Dimension dimension = getSize();
            width = dimension.width;
            height = dimension.height;
            scaleY = height / 2;
            createScale(g);
            if (isCurMinSet)
                setValue(0, cur_min);
            else
                c1_pos = margin;
            if (isCurMaxSet)
                setValue(1, cur_max);
            else
                c2_pos = dimension.width - margin;
            isResized = false;
        }
        if (fm == null) {

            fm = g.getFontMetrics(font);
            c1_pos = margin;
            c2_pos = width - margin;

            computeLeftRect(c1_pos);
            computeRightRect(c2_pos);
        }
        g.drawImage(backgroundImage, 0, 0, this);
        if (c1_pos == c2_pos && last_dragged == 1 || c1_pos != c2_pos)
            g.drawImage(cursor1, 2 + c1_pos - cursor_size / 2, 9 + scaleY - cursor_size / 2, this);
        if (c1_pos == c2_pos && last_dragged == 2 || c1_pos != c2_pos)
            g.drawImage(cursor2, 2 + c2_pos - cursor_size / 2, 9 + scaleY - cursor_size / 2, this);

        g.setFont(font);
        // System.out.println("-------");
        if (leftHintRect != null) {

            g.setColor(Color.WHITE);
            g.fillRect(leftHintRect.x, leftHintRect.y, leftHintRect.width, leftHintRect.height);
            g.setColor(Color.black);
            g.drawString(leftHintText, leftHintRect.x, (leftHintRect.y + fm.getHeight()) - 3);
        }
        if (rightHintRect != null) {

            g.setColor(Color.WHITE);
            g.fillRect(rightHintRect.x, rightHintRect.y, rightHintRect.width, rightHintRect.height);
            g.setColor(Color.black);
            g.drawString(rightHintText, rightHintRect.x, (rightHintRect.y + fm.getHeight()) - 3);
        }
    }

    public long getValue(int i) {
        long d = 0;

        if (i == 0) {
            if (c1_pos == margin)
                d = min;
            else if (c1_pos == width - margin)
                d = max;
            else
                d = ((c1_pos - margin) * COEF) + min;
        } else {
            if (c2_pos == margin)
                d = min;
            else if (c2_pos == width - margin)
                d = max;
            else
                d = (c2_pos - margin) * COEF + min;
        }
        // System.out.println("GetValue:" + c2_pos + " ->" + d);
        return d;
    }

    public void setValue(int i, double d) {
        int j = margin + (int) ((d - min) * tk);
        if (i == 0)
            c1_pos = j;
        else
            c2_pos = j;
        repaint();
    }

    private void createScale(Graphics g) {
        Dimension dimension = getSize();
        backgroundImage = createImage(dimension.width, dimension.height);
        Graphics g1 = backgroundImage.getGraphics();
        // g1.setColor(new Color(153, 153, 153));
        g1.setColor(this.getBackground());
        g1.fillRect(0, 0, dimension.width, dimension.height);
        g1.setFont(font);
        // Reglette
        g1.setColor(Color.BLACK);
        g1.drawLine(margin, scaleY, width - margin, scaleY);// bas

        g1.drawLine(margin, scaleY - 4, width - margin, scaleY - 4);// haut
        g1.setColor(Color.WHITE);
        g1.drawLine(margin, scaleY - 3, width - margin, scaleY - 3);
        g1.setColor(Color.LIGHT_GRAY);
        g1.drawLine(margin, scaleY - 2, width - margin, scaleY - 2);
        g1.drawLine(margin, scaleY - 1, width - margin, scaleY - 1);

        g1.setColor(new Color(200, 200, 200));
        g1.drawLine(margin, scaleY - 3, margin + getNbDayOfYear(this.year - 1), scaleY - 3);

        g1.setColor(new Color(160, 160, 160));
        g1.drawLine(margin, scaleY - 2, margin + getNbDayOfYear(this.year - 1), scaleY - 2);
        g1.setColor(new Color(120, 120, 120));
        g1.drawLine(margin, scaleY - 1, margin + getNbDayOfYear(this.year - 1), scaleY - 1);
        // reperes
        int x = margin;

        for (int i = 1; i >= 0; i--) {
            for (int month = 0; month < 12; month++) {
                if (month % 2 == 0) {
                    g1.setColor(Color.WHITE);

                } else {
                    g1.setColor(new Color(252, 252, 252));
                }
                cal.set(this.year - i, month, 1);
                int nbday = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                // g1.fillRect(x,5,x+nbday,50);
                g1.setColor(Color.DARK_GRAY);
                g1.drawLine(x, scaleY - 3, x, scaleY - 2);
                x += nbday;
                if (i == 1 && month == 11) {
                    g1.drawLine(x, scaleY - 7, x, scaleY - 0);

                }
            }
        }

        // ligne de fin
        g1.drawLine(x, scaleY - 7, x, scaleY);

        long currenttime = System.currentTimeMillis();
        long delta = currenttime - this.min;
        int nbpixel = margin + (int) (delta / COEF);
        if (nbpixel > 0) {
            g1.setColor(Color.RED);
            g1.drawLine(nbpixel, scaleY + 2, nbpixel, scaleY - 7);

        }

        // affichage de l'année

        g1.setColor(Color.DARK_GRAY);
        g1.drawString(String.valueOf(year - 1), 176, scaleY + 12);
        g1.setColor(Color.BLACK);
        g1.drawString(String.valueOf(year), 540, scaleY + 12);

    }

    public String getDateFromMillis(long millis) {
        cal.setTimeInMillis(millis);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        String r = df.format(cal.getTime());
        return r;
    }

    public int getPrecision() {
        return precision;
    }

    public static int getOrder(double d) {
        int i = 0;
        if (d == 0.0D)
            return i;
        if (Math.abs(d) >= 1.0D) {
            do {
                d /= 10D;
                i++;
            } while (d >= 1.0D);
            return --i;
        }
        if (Math.abs(d) < 1.0D) {
            while (d < 1.0D) {
                d *= 10D;
                i++;
            }
            return -i;
        }
        return i;

    }

    public void mouseReleased(MouseEvent mouseevent) {

        draggable1 = false;
        draggable2 = false;
    }

    private String getLeftCursorText() {
        return " Du " + getDateFromMillis(getValue(0)) + " ";
    }

    private String getRightCursorText() {
        return " au " + getDateFromMillis(getValue(1)) + " ";
    }

    public void mousePressed(MouseEvent mouseevent) {
        int x = mouseevent.getX();
        int y = mouseevent.getY();
        boolean flag = false;

        // Rectangle rectangle = leftHintRect;

        if (isOnLeftCursor(x, y)) {
            // System.out.println("cas1");
            draggable1 = true;
            old_pos = x;
            flag = true;
            leftHintText = getLeftCursorText();
            leftHintRect = new Rectangle(x, scaleY - fm.getHeight() - 6, fm.stringWidth(leftHintText), fm.getHeight());

        } else if (isOnRightCursor(x, y)) {
            // System.out.println("cas2");
            draggable2 = true;
            old_pos = x;
            flag = true;
            rightHintText = " au " + getRightCursorText() + " ";
            rightHintRect = new Rectangle(x, scaleY - fm.getHeight() - 6, fm.stringWidth(rightHintText), fm.getHeight());

        } else {
            return;

        }

        // if (rectangle != null)
        // repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);

        /*
         * TODO: ... if (hintRect != null) { if (hintRect.x + hintRect.width > width) hintRect.x =
         * width - hintRect.width - 1; repaint(hintRect.x, hintRect.y, hintRect.width,
         * hintRect.height); }
         */

        if (c1_pos == c2_pos && flag) {
            if (last_dragged == 1) {
                draggable1 = true;
                draggable2 = false;
            }
            if (last_dragged == 2) {
                draggable1 = false;
                draggable2 = true;
            }
        }
        mouseDragged(mouseevent);
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private boolean isOnRightCursor(int x, int y) {
        return x >= c2_pos - cursor_size / 2 && x <= c2_pos + cursor_size / 2 && y >= scaleY - cursor_size / 2 - 1 && y <= scaleY + cursor_size / 2 + 1;
    }

    /**
     * @param x
     * @param y
     * @return
     */
    private boolean isOnLeftCursor(int x, int y) {
        return x >= c1_pos - cursor_size / 2 && x <= c1_pos + cursor_size / 2 && y >= scaleY - cursor_size / 2 - 1 && y <= scaleY + cursor_size / 2 + 1;
    }

    public void mouseDragged(MouseEvent mouseevent) {
        int x = mouseevent.getX();
        mouseevent.getY();
        int j = 0;
        // System.out.println("draggable1:"+draggable1+" draggable2:"+draggable2);
        if (draggable1) {
            if (x > c2_pos) {
                c1_pos = c2_pos;
            } else if (x < margin) {
                c1_pos = margin;
            } else {
                c1_pos = x;
            }

            if (c1_pos > old_pos)
                repaint(old_pos - cursor_size, scaleY - cursor_size / 2, c1_pos + cursor_size, scaleY + cursor_size / 2);
            else
                repaint(c1_pos - cursor_size, scaleY - cursor_size / 2, old_pos + cursor_size, scaleY + cursor_size / 2);
            old_pos = x;
            last_dragged = 1;

            j = c1_pos;

        }
        if (draggable2) {
            if (x < c1_pos) {
                // on butte sur le curseur de gauche
                c2_pos = c1_pos;
            } else if (x > getSize().width - margin) {
                // on butte sur la limite à droite
                c2_pos = getSize().width - margin;
            } else {
                c2_pos = x;
            }

            if (c2_pos > old_pos) {
                // on a bougé à droite, on repaint ou était l'ancien curseur
                repaint(old_pos - cursor_size, scaleY - cursor_size / 2, c2_pos + cursor_size, scaleY + cursor_size / 2);
            } else
                repaint(c2_pos - cursor_size, scaleY - cursor_size / 2, old_pos + cursor_size, scaleY + cursor_size / 2);
            old_pos = x;
            last_dragged = 2;

            j = c2_pos;

        }

        if (draggable1) {

            computeLeftRect(j);

            // repaint(leftHintRect.x, leftHintRect.y, leftHintRect.width, leftHintRect.height);
            // repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }
        if (draggable2) {

            computeRightRect(j);
        }
        repaint();

    }

    /**
     * @param j
     */
    private void computeRightRect(int j) {
        rightHintText = getRightCursorText();
        rightHintRect = new Rectangle(j, scaleY - fm.getHeight() - 6, fm.stringWidth(rightHintText), fm.getHeight());

        if (leftHintRect != null && c2_pos - (leftHintRect.x/* +leftHintRect.width */) < fm.stringWidth(leftHintText))
            rightHintRect.x = leftHintRect.x + fm.stringWidth(leftHintText);

        if (rightHintRect.x + rightHintRect.width > width)
            rightHintRect.x = width - rightHintRect.width - 1;
        // repaint(rightHintRect.x, rightHintRect.y, rightHintRect.width, rightHintRect.height);
        // repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    /**
     * @param j
     */
    private void computeLeftRect(int j) {
        leftHintText = getLeftCursorText();
        leftHintRect = new Rectangle(j, scaleY - fm.getHeight() - 6, fm.stringWidth(leftHintText), fm.getHeight());
        // System.out.println(rightHintRect);
        // System.out.println(c1_pos);
        // System.out.println(fm.stringWidth(leftHintText));

        if (rightHintRect != null && rightHintRect.x - c1_pos < fm.stringWidth(leftHintText))
            leftHintRect.x = rightHintRect.x - fm.stringWidth(leftHintText);

        if (leftHintRect.x + leftHintRect.width > width)
            leftHintRect.x = width - leftHintRect.width - 1;
    }

    public void componentResized(ComponentEvent componentevent) {
        // isResized = true;
    }

    public void componentMoved(ComponentEvent componentevent) {
    }

    public void componentShown(ComponentEvent componentevent) {
    }

    public void componentHidden(ComponentEvent componentevent) {
    }

    public void mouseMoved(MouseEvent mouseevent) {
        int x = mouseevent.getX();
        int y = mouseevent.getY();
        if (isOnLeftCursor(x, y) || isOnRightCursor(x, y)) {
            if (this.getCursor() != Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        } else {
            if (this.getCursor() != Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void mouseClicked(MouseEvent mouseevent) {
    }

    public void mouseEntered(MouseEvent mouseevent) {
    }

    public void mouseExited(MouseEvent mouseevent) {
    }

    public boolean imageUpdate(Image image, int i, int j, int k, int l, int i1) {
        boolean flag = super.imageUpdate(image, i, j, k, l, i1);
        if (flag)
            repaint();
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#getPreferredSize()
     */
    public Dimension getPreferredSize() {
        return new Dimension(this.width, this.height);
    }

    public Dimension getMinimumSize() {
        return new Dimension(this.width, this.height);
    }

    public Dimension getMaximumSize() {
        return new Dimension(this.width, this.height);
    }
}
