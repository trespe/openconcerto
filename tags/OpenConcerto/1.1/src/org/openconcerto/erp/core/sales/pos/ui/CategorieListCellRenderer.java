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



import org.openconcerto.erp.core.sales.pos.model.Categorie;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CategorieListCellRenderer implements ListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel l = new JLabel(" " + ((Categorie) value).getName()) {
            @Override
            public void paint(Graphics g) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                super.paint(g);
                g.setColor(Color.LIGHT_GRAY);
                //g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        l.setOpaque(true);
        if (isSelected) {
            l.setBackground(new Color(232, 242, 254));
            l.setForeground(Color.BLACK);
        } else {
            l.setBackground(Color.WHITE);
            l.setForeground(Color.GRAY);
        }
        l.setFont(l.getFont().deriveFont(24f));
        return l;
    }
}
