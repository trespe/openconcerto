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
 
 package org.openconcerto.ui.list;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JLabel;

public class HighLightableJLabel extends JLabel {
    public static Color DEFAULT_COLOR = new Color(252, 252, 180);
    private String text;
    private Color highLightColor = DEFAULT_COLOR;

    public void setHightlight(String text) {
        this.text = text;
    }

    public void setHighLightColor(Color highLightColor) {
        this.highLightColor = highLightColor;
    }

    @Override
    public void paint(Graphics g) {
        if (highLightColor != null && text != null) {
            String currentText = this.getText();
            int offset = currentText.indexOf(text);
            if (offset >= 0) {
                FontMetrics metrics = g.getFontMetrics();
                String start = getText().substring(0, offset);
                // You may also need to account for some offsets here:
                int startX = metrics.stringWidth(start);
                int startY = 0; // You probably have some vertical offset to add here.
                int length = metrics.stringWidth(text);
                int height = metrics.getHeight();
                // Now you can draw the highlighted region before drawing the rest of the label:
                g.setColor(highLightColor);
                g.fillRect(startX, startY, length, height);
            }
        }
        super.paint(g);
    }
}
