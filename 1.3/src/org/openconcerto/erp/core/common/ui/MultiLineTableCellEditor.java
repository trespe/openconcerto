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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.TableCellEditor;

public class MultiLineTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private JDialog frame;
    private JTextField text;
    private boolean popupOpen = false;
    private RowValuesMultiLineEditTable tableItem;
    private JPanel panel;

    public MultiLineTableCellEditor(RowValuesMultiLineEditTable rowValuesTable, JPanel panel) {
        this.tableItem = rowValuesTable;
        this.panel = panel;
    }

    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {

        this.text = new JTextField();

        createJFrame(table, row, column);

        this.text.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                showPopup();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }
        });
        return this.text;
    }

    private void createJFrame(JTable table, int row, int column) {
        if (table instanceof RowValuesTable) {
            RowValuesTable rowValuesTable = (RowValuesTable) table;
            SQLRowValues rowVals = rowValuesTable.getRowValuesTableModel().getRowValuesAt(row);
            this.tableItem.getRowValuesTableModel().clearRows();
            this.tableItem.setRoots(rowValuesTable, row, rowVals);
        }

        Rectangle rect = table.getCellRect(row, column, true);
        Point p = new Point(rect.x, rect.y + table.getRowHeight(row));
        SwingUtilities.convertPointToScreen(p, table);

        JFrame rootFrame = (JFrame) SwingUtilities.getRoot(table);
        if (this.frame != null) {
            this.frame.dispose();
            this.frame = null;
        }

        this.frame = new JDialog(rootFrame, true);

        this.frame.setUndecorated(true);
        this.frame.getContentPane().add(this.panel);
        this.frame.setLocation(p.x, p.y);
        this.frame.pack();

        // frame.setAlwaysOnTop(true);

        this.panel.grabFocus();
    }

    public void showPopup() {

        this.popupOpen = true;
        this.frame.setVisible(true);
        this.frame.requestFocusInWindow();
        this.frame.requestFocus();

        // frame.setAlwaysOnTop(true);
        // frame.toFront();
    }

    public Object getCellEditorValue() {

        return this.tableItem.getStringValue();
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public void refreshText() {

        this.text.setText(this.tableItem.getStringValue());
    }

    public void setTable(RowValuesMultiLineEditTable table) {
        this.tableItem = table;
    }
}
