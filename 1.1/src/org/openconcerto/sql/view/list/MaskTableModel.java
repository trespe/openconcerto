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
 
 package org.openconcerto.sql.view.list;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

public class MaskTableModel extends AbstractTableModel {

    // Map<Integer, Integer> hideColumnList;
    private Vector<Integer> showColumnList;
    private AbstractTableModel model;
    RowValuesTable table;

    public MaskTableModel(AbstractTableModel model, RowValuesTable table) {
        this.model = model;
        this.table = table;
        // this.hideColumnList = new HashMap<Integer, Integer>();
        this.showColumnList = new Vector<Integer>(this.model.getColumnCount());
        for (int i = 0; i < model.getColumnCount(); i++) {
            this.showColumnList.add(Integer.valueOf(i));
        }
    }

    public Vector<Integer> getShowColumnList() {
        return showColumnList;
    }

    /**
     * Nombre de colonne dans le model
     */
    public int getColumnCount() {

        return this.showColumnList.size();
    }

    public int getRowCount() {

        return model.getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        return model.getValueAt(rowIndex, this.showColumnList.get(columnIndex));
    }

    @Override
    public String getColumnName(int column) {

        return this.model.getColumnName(this.showColumnList.get(column));
    }

    public void hideColumn(int index) throws IllegalArgumentException {
        int r = getIndexFor(index);
        if (r >= 0) {
            this.showColumnList.remove(r);
            this.fireTableStructureChanged();
            this.model.fireTableStructureChanged();
            this.table.setEditorAndRendererDone(false);
            this.table.createDefaultColumnsFromModel();
            int width = this.table.getSize().width;
            int height = this.table.getTableHeader().getSize().height;
            this.table.getTableHeader().setSize(new Dimension(width, height));
            
            System.err.println("Table Width = " + this.table.getSize().width + " Header Width = " + this.table.getTableHeader().getSize().width);
            this.table.getTableHeader().invalidate();
            this.table.getTableHeader().resizeAndRepaint();
            this.table.invalidate();
            this.table.resizeAndRepaint();
        }
    }

    public void showColumn(int index) throws IllegalArgumentException {
        if (index >= 0) {
            Vector<Integer> v = new Vector<Integer>();
            for (int i = 0; i < this.model.getColumnCount(); i++) {
                if (this.showColumnList.contains(Integer.valueOf(i))) {
                    v.add(Integer.valueOf(i));
                } else {
                    if (i == index) {
                        v.add(Integer.valueOf(index));
                    }
                }
            }
            this.showColumnList = v;
            // this.showColumnList.add(index, index);
            this.fireTableStructureChanged();
            this.model.fireTableStructureChanged();
            this.table.createDefaultColumnsFromModel();
            this.table.setEditorAndRendererDone(false);

            int width = this.table.getSize().width;
            int height = this.table.getTableHeader().getSize().height;
            this.table.getTableHeader().setSize(new Dimension(width, height));
            System.err.println("Table Width = " + this.table.getSize().width + " Header Width = " + this.table.getTableHeader().getSize().width);
            this.table.getTableHeader().invalidate();
            this.table.getTableHeader().resizeAndRepaint();
            this.table.invalidate();
            this.table.resizeAndRepaint();

        }
    }

    public int getIndexFor(int index) {
        for (int i = 0; i < this.showColumnList.size(); i++) {
            int val = this.showColumnList.get(i);
            if (val == index) {
                return i;
            }
        }
        return -1;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        this.model.setValueAt(aValue, rowIndex, this.showColumnList.get(columnIndex));
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return this.model.isCellEditable(rowIndex, this.showColumnList.get(columnIndex));
    }

    public synchronized Class<?> getColumnClass(int columnIndex) {
        return this.model.getColumnClass(this.showColumnList.get(columnIndex));
    }

    public void addTableModelListener(TableModelListener l) {
        this.model.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        this.model.removeTableModelListener(l);
    }

}
