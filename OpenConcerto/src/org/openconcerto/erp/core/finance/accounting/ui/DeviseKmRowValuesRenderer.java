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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class DeviseKmRowValuesRenderer extends DefaultTableCellRenderer {

    private static final Color red = new Color(255, 31, 52);
    private static final Color redGrey = new Color(224, 115, 137);
    private static final Color redLightGrey = new Color(240, 65, 85);
    private List<Integer> listRow = new ArrayList<Integer>();

    {
        AlternateTableCellRenderer.setBGColorMap(this, red, redLightGrey);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
            this.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        if (this.listRow.contains(row)) {
            if (isSelected) {
                this.setBackground(redGrey);
                this.setForeground(Color.WHITE);
            } else {
                this.setBackground(red);
                this.setForeground(Color.WHITE);
            }
        } else {
            TableCellRendererUtils.setColors(this, table, isSelected);
        }

        // TableCellEditor cellEditor = table.getColumnModel().getColumn(column).getCellEditor();
        // cellEditor.addCellEditorListener(this);
        //
        // jumpToNextEditCell(table, hasFocus, isSelected, row, column);

        return this;
    }

    public void setValid(boolean b, int index) {
        if (b) {
            if (this.listRow.contains(index)) {
                this.listRow.remove(Integer.valueOf(index));
            }
        } else {
            if (!this.listRow.contains(index)) {
                this.listRow.add(Integer.valueOf(index));
            }
        }
    }

    // @Override
    // public void editingCanceled(ChangeEvent e) {
    // // TODO Auto-generated method stub
    //
    // }
    //
    // @Override
    // public void editingStopped(ChangeEvent e) {
    // // TODO Auto-generated method stub
    // setEditingMode(true);
    // }
}
