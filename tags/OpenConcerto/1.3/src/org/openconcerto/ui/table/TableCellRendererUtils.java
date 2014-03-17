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
import java.awt.Component;

import javax.swing.JTable;

public final class TableCellRendererUtils {

    /**
     * Set the foreground and background to the table ones.
     * 
     * @param comp the renderer component to change.
     * @param table its table.
     * @param isSelected if the cell is selected.
     * @return <code>comp</code>.
     */
    public static Component setColors(final Component comp, final JTable table, boolean isSelected) {
        return setColors(comp, table, isSelected, null, null);
    }

    protected static Component setColors(final Component comp, final JTable table, boolean isSelected, Color bgColor, Color fgColor) {
        setBackgroundColor(comp, table, isSelected, bgColor);
        setForegroundColor(comp, table, isSelected, fgColor);
        return comp;
    }

    /**
     * Set the background colour from the table.
     * 
     * @param comp the renderer component to change.
     * @param table its table.
     * @param isSelected if the cell is selected.
     * @return <code>comp</code>.
     * @see JTable#getBackground()
     * @see JTable#getSelectionBackground()
     */
    public static Component setBackgroundColor(final Component comp, final JTable table, boolean isSelected) {
        return setBackgroundColor(comp, table, isSelected, null);
    }

    protected static Component setBackgroundColor(final Component comp, final JTable table, boolean isSelected, Color bgColor) {
        if (isSelected) {
            comp.setBackground(table.getSelectionBackground());
        } else {
            comp.setBackground(bgColor == null ? table.getBackground() : bgColor);
        }
        return comp;
    }

    public static Component setForegroundColor(final Component comp, final JTable table, boolean isSelected) {
        return setForegroundColor(comp, table, isSelected, null);
    }

    protected static Component setForegroundColor(final Component comp, final JTable table, boolean isSelected, Color fgColor) {
        if (isSelected) {
            comp.setForeground(table.getSelectionForeground());
        } else {
            comp.setForeground(fgColor == null ? table.getForeground() : fgColor);
        }
        return comp;
    }
}
