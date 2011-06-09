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

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public interface IAlternateTableCellRenderer extends TableCellRenderer {
    /**
     * Returns the component used for drawing cells of alternate rows.
     * 
     * @param table the <code>JTable</code> that is asking the renderer to draw; can be
     *        <code>null</code>
     * @param value the value of the cell to be rendered. It is up to the specific renderer to
     *        interpret and draw the value. For example, if <code>value</code> is the string "true",
     *        it could be rendered as a string or it could be rendered as a check box that is
     *        checked. <code>null</code> is a valid value
     * @param isSelected true if the cell is to be rendered with the selection highlighted;
     *        otherwise false
     * @param hasFocus if true, render cell appropriately. For example, put a special border on the
     *        cell, if the cell can be edited, render in the color used to indicate editing
     * @param row the row index of the cell being drawn. When drawing the header, the value of
     *        <code>row</code> is -1
     * @param column the column index of the cell being drawn
     * @return the component used for drawing the cell.
     * @see #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
     */
    Component getAlternateTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);
}
