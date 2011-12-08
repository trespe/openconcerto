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

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class MultiLineTableHeaderRenderer extends JTextArea implements TableCellRenderer {
    public MultiLineTableHeaderRenderer() {
        setOpaque(true);
        setForeground(UIManager.getColor("TableHeader.foreground"));
        setBackground(UIManager.getColor("TableHeader.background"));
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setFont(table.getFont());
        String str = (value == null) ? "" : value.toString();
        this.setText(str.trim());
        // this.setSize(10, 10);
        setSize(table.getColumnModel().getColumn(column).getWidth(), 165863);
        return this;
    }
}
