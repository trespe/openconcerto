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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class StatusBar extends JPanel {
    private Image bg;
    private Image bg_retour;
    private String title = "";

    private boolean previous;
    private int offsetX = 110;

    StatusBar() {
        this("toolbar.png", "toolbar_retour.png");
    }

    StatusBar(String fileBg, String fileFg) {
        bg = new ImageIcon(TicketPanel.class.getResource(fileBg)).getImage();
        bg_retour = new ImageIcon(TicketPanel.class.getResource(fileFg)).getImage();
        setFont(new Font("Arial", Font.BOLD, 24));
    }

    public void setPrevious(boolean b) {
        if (b != previous) {
            this.previous = b;
            repaint();
        }
    }

    public void setTitle(String t) {
        if (this.title == null || !this.title.equals(t)) {
            this.title = t;
            repaint();
        }
    }

    void setTitleLocation(int x) {
        offsetX = x;
    }

    @Override
    protected void paintComponent(Graphics g) {
        final int w = this.getWidth();
        int imWidth = bg.getWidth(null);
        for (int x = imWidth - 1; x <= w; x += imWidth) {
            g.drawImage(bg, x, 0, null);
        }
        if (previous)
            g.drawImage(bg_retour, 0, 0, null);
        else
            g.drawImage(bg, 0, 0, null);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(250, 250, 250));
        g.drawString(title, offsetX, 30);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(320, 44);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 44);
    }

}
