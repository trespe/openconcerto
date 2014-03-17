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
 
 package org.openconcerto.ui.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.VerticalLayout;

public class PercentRenderer extends JPanel {
    private int v = 0;
    private JLabel l1 = new JLabel("100 %", SwingConstants.CENTER);
    private JLabel l2 = new JLabel("100 %", SwingConstants.CENTER);

    public PercentRenderer() {
        l1.setForeground(Color.WHITE);
        l2.setForeground(Color.BLACK);
    }

    public void setValue(Number value) {
        v = value.intValue();
        final String text = v + " %";
        l1.setText(text);
        l2.setText(text);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        l1.setBounds(x, y, width, height);
        l2.setBounds(x, y, width, height);
        super.setBounds(x, y, width, height);
    }

    @Override
    public Dimension getPreferredSize() {
        return l1.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return l1.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return l1.getMaximumSize();
    }

    @Override
    public void paint(Graphics g) {
        int width = (int) (this.getWidth() * v) / 100;
        int height = this.getHeight();
        int y = 0;
        if (g.getClipBounds() != null) {
            height = g.getClipBounds().height;
            y = g.getClipBounds().y;
        }
        if (v < 100) {
            g.setColor(Color.WHITE);
            g.fillRect(0, y, this.getWidth(), height);
        }
        if (width > 0) {
            if (v < 10) {
                g.setColor(Color.ORANGE);
            } else if (v >= 100) {
                g.setColor(new Color(25, 169, 4));
            } else {
                g.setColor(new Color(70, 130, 180));
            }
            g.fillRect(0, y, width, height);
            g.setClip(0, y, width, height);
            l1.paint(g);
        }
        g.setClip(width, y, this.getWidth() - width, height);
        l2.paint(g);

    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        JPanel p = new JPanel();
        p.setLayout(new VerticalLayout());
        for (int i = 0; i <= 100; i += 4) {
            PercentRenderer r1 = new PercentRenderer();
            r1.setValue(i);
            r1.setSize(100, 16);
            p.add(r1);

        }
        f.setContentPane(p);
        f.setSize(100, 500);
        f.setVisible(true);
    }
}
