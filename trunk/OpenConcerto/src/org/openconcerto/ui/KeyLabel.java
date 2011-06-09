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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class KeyLabel extends JPanel {

    private Image im;
    final JLabel label = new JLabel("");

    public KeyLabel(String string) {
        this.setOpaque(false);
        this.setLayout(null);
        im = new ImageIcon(this.getClass().getResource("f.png")).getImage();
        setText(string);
        label.setLocation(9, 3);
        label.setSize(30, 10);
        label.setOpaque(false);
        this.add(label);
        this.setMinimumSize(new Dimension(im.getWidth(null), im.getHeight(null)));
        this.setPreferredSize(new Dimension(im.getWidth(null), im.getHeight(null)));

    }

    public void setFont(Font font) {
        super.setFont(font);
        if (label != null)
            label.setFont(font);
    }

    public void setText(String txt) {
        this.label.setText(txt);
    }

    public void paint(Graphics g) {
        g.drawImage(im, 0, 0, null);
        super.paint(g);
    }
}
