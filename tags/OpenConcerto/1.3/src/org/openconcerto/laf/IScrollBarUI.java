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
 
 /*
 * Created on 26 mai 2005
 * 
 * To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */
package org.openconcerto.laf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class IScrollBarUI extends BasicScrollBarUI {

    private static final Color COLOR_DARK = new Color(140, 141, 121);

    /**
     * The scrollbar's highlight color.
     */
    private static Color highlightColor;

    /**
     * The scrollbar's dark shadow color.
     */
    private static Color darkShadowColor;

    /**
     * The thumb's shadow color.
     */
    private static Color thumbShadow;

    /**
     * The thumb's highlight color.
     */
    private static Color thumbHighlightColor;

    /** true if thumb is in rollover state */
    protected boolean isRollover = false;

    /** true if thumb was in rollover state */
    protected boolean wasRollover = false;

    /**
     * The free standing property of this scrollbar UI delegate.
     */
    private boolean freeStanding = false;

    int scrollBarWidth;

    Image h, b;

    public IScrollBarUI() {
    }

    /**
     * Installs some default values. Initializes the metouia dots used for the thumb.
     */
    protected void installDefaults() {
        scrollBarWidth = 13;
        super.installDefaults();
        scrollbar.setBorder(null);
        this.h = new ImageIcon(IScrollBarUI.class.getResource("scrollHaut.png")).getImage();
        this.b = new ImageIcon(IScrollBarUI.class.getResource("scrollBas.png")).getImage();

    }

    /**
     * Creates the UI delegate for the given component.
     * 
     * @param c The component to create its UI delegate.
     * @return The UI delegate for the given component.
     */
    public static ComponentUI createUI(JComponent c) {
        return new IScrollBarUI();
    }

    JButton decreaseButton, increaseButton;

    /**
     * Creates the decrease button of the scrollbar.
     * 
     * @param orientation The button's orientation.
     * @return The created button.
     */
    protected JButton createDecreaseButton(int orientation) {
      
        if (orientation == NORTH) {
            this.decreaseButton = new JButton(new ImageIcon(getClass().getResource("up.png")));
        } else {
            this.decreaseButton = new JButton(new ImageIcon(getClass().getResource("left.png")));
        }
        this.decreaseButton.setBorder(null);
        // decreaseButton = new IScrollBarUI(orientation, scrollBarWidth,
        // freeStanding);
        return this.decreaseButton;
    }

    /**
     * Creates the increase button of the scrollbar.
     * 
     * @param orientation The button's orientation.
     * @return The created button.
     */
    protected JButton createIncreaseButton(int orientation) {
       
        if (orientation == SOUTH) {
            this.increaseButton = new JButton(new ImageIcon(getClass().getResource("down.png")));
        } else {
            this.increaseButton = new JButton(new ImageIcon(getClass().getResource("right.png")));
        }

        this.increaseButton.setBorder(null);
        // increaseButton = new XPScrollButton(orientation, scrollBarWidth,
        // freeStanding);
        return this.increaseButton;
    }

    // / From MetalUI

    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, scrollBarWidth * 3 + 10);
        } else // Horizontal
        {
            return new Dimension(scrollBarWidth * 3 + 10, scrollBarWidth);
        }

    }

    public void paint(Graphics g, JComponent c) {
        Rectangle trackBounds = getTrackBounds();
        g.setColor(new Color(255, 0, 0));
        drawFond(g, 0, trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

        Rectangle thumbBounds = getThumbBounds();

        // int index = skinThumbIndexModel.getIndexForState(c.isEnabled(),
        // isRollover, isDragging);
        /*
         * getSkinThumb().draw(g, index, thumbBounds.x, thumbBounds.y, thumbBounds.width,
         * thumbBounds.height);
         * 
         * getSkinGripper().drawCentered(g, index, thumbBounds.x, thumbBounds.y, thumbBounds.width,
         * thumbBounds.height);
         */
        drawThumb(g, thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
    }

    private void drawThumb(Graphics g, int x, int y, int width, int height) {

        x++;
        y++;
        width -= 3;
        height -= 2;

        g.setColor(new Color(244, 244, 240));
        g.fillRect(x, y, width, height);
        // Tour
        g.setColor(COLOR_DARK);
        g.drawRect(x, y, width + 1, height);
        // gauche
        g.setColor(new Color(252, 252, 251));
        g.drawLine(x + 1, y + 1, x + 1, y + height - 5);
        // droite 225,225,214
        g.setColor(new Color(225, 225, 214));
        g.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 5);
        // droite-1237,237,231
        g.setColor(new Color(237, 237, 231));
        g.drawLine(x + width - 2, y + 1, x + width - 2, y + height - 5);

        // milieu 244,244,240
        //haut
        g.drawImage(h, x  + width -8, y + 1, null);
        //bas
        g.drawImage(b, x  + width - 8, y + height - 5, null);

        g.setColor(COLOR_DARK);
        
        // Scroll vertical
        if(height>width) {
            int yindex = y + height / 2;
            g.drawLine(x + 4, yindex - 3, x + width - 4, yindex - 3);
            g.drawLine(x + 4, yindex, x + width - 4, yindex);
            g.drawLine(x + 4, yindex + 3, x + width - 4, yindex + 3);
        }else {
            int xindex = x + width / 2;
           
            g.drawLine(xindex-3,y+4, xindex-3, y+height-4);
            g.drawLine(xindex,y+4, xindex, y+height-4);
            g.drawLine(xindex+3,y+4, xindex+3, y+height-4);
           
        
        }
    }

    private void drawFond(Graphics g, int i, int x, int y, int width, int height) {
        g.setColor(new Color(239, 235, 231));
        g.fillRect(x, y, width, height);

    }

    public boolean isThumbVisible() {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            if (getThumbBounds().height == 0)
                return false;
            else
                return true;
        } else {
            if (getThumbBounds().width == 0)
                return false;
            else
                return true;
        }
    }

    // From BasicUI
    protected TrackListener createTrackListener() {
        return new MyTrackListener();
    }

    /**
     * Basically does BasicScrollBarUI.TrackListener the right job, it just needs an additional
     * repaint and rollover management
     */
    protected class MyTrackListener extends BasicScrollBarUI.TrackListener {
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            scrollbar.repaint();
        }

        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            scrollbar.repaint();
        }

        public void mouseEntered(MouseEvent e) {
            isRollover = false;
            wasRollover = false;
            if (getThumbBounds().contains(e.getX(), e.getY())) {
                isRollover = true;
            }
        }

        public void mouseExited(MouseEvent e) {
            isRollover = false;
            if (isRollover != wasRollover) {
                scrollbar.repaint();
                wasRollover = isRollover;
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (getThumbBounds().contains(e.getX(), e.getY())) {
                isRollover = true;
            }
            super.mouseDragged(e);
        }

        public void mouseMoved(MouseEvent e) {
            if (getThumbBounds().contains(e.getX(), e.getY())) {
                isRollover = true;
                if (isRollover != wasRollover) {
                    scrollbar.repaint();
                    wasRollover = isRollover;
                }
            } else {
                isRollover = false;
                if (isRollover != wasRollover) {
                    scrollbar.repaint();
                    wasRollover = isRollover;
                }
            }
        }
    }

}
