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
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CheckListRenderer extends JCheckBox implements ListCellRenderer {
    private Color color;

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
        setEnabled(list.isEnabled());
        setSelected(((CheckListItem) value).isSelected());
        setColorIndicator(((CheckListItem) value).getColor());
        setFont(list.getFont());
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        setText(value.toString());
        return this;
    }

    private void setColorIndicator(Color c) {
        this.color = c;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (color != null) {
            final int marginV = 4;
            final int marginH = 3;
            final int y = marginV;
            final int s = this.getHeight() - 2 * marginV;
            g.setColor(color);
            final int x = this.getWidth() - s - marginH;
            g.fillRect(x, y, s, s);
        }
    }
}
