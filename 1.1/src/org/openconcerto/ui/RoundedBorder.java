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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

public class RoundedBorder implements Border {
    private final int cornerlWidth, cornerlHeight;

    private final Color topColor, bottomColor;

    public RoundedBorder(int w, int h, Color topColor, Color bottomColor) {
        this.cornerlWidth = w;
        this.cornerlHeight = h;
        this.topColor = topColor;
        this.bottomColor = bottomColor;
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(cornerlHeight, cornerlWidth, cornerlHeight, cornerlWidth);
    }

    public boolean isBorderOpaque() {
        return true;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        width--;
        height--;

        g.setColor(topColor);
        g.drawLine(x, y + height - cornerlHeight, x, y + cornerlHeight);
        g.drawArc(x, y, 2 * cornerlWidth, 2 * cornerlHeight, 180, -90);
        g.drawLine(x + cornerlWidth, y, x + width - cornerlWidth, y);
        g.drawArc(x + width - 2 * cornerlWidth, y, 2 * cornerlWidth, 2 * cornerlHeight, 90, -90);

        g.setColor(bottomColor);
        g.drawLine(x + width, y + cornerlHeight, x + width, y + height - cornerlHeight);
        g.drawArc(x + width - 2 * cornerlWidth, y + height - 2 * cornerlHeight, 2 * cornerlWidth, 2 * cornerlHeight, 0, -90);
        g.drawLine(x + cornerlWidth, y + height, x + width - cornerlWidth, y + height);
        g.drawArc(x, y + height - 2 * cornerlHeight, 2 * cornerlWidth, 2 * cornerlHeight, -90, -90);
    }

}
