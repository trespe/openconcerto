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
 
 package org.openconcerto.utils;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * A table model using a delegate. If the indexes need to be translated you can use
 * {@link #adaptCol(int)} and {@link #adaptRow(int)} instead of overloading every method that use
 * indexes. Don't forget to override {@link #getColumnCount(int)} and {@link #getRowCount(int)} as
 * needed.
 * 
 * @author Sylvain
 */
public class TableModelAdapter implements TableModel {

    private final TableModel delegate;

    public TableModelAdapter(TableModel delegate) {
        super();
        this.delegate = delegate;
    }

    protected int adaptCol(int columnIndex) {
        return columnIndex;
    }

    protected int adaptRow(int rowIndex) {
        return rowIndex;
    }

    public void addTableModelListener(TableModelListener l) {
        this.delegate.addTableModelListener(l);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return this.delegate.getColumnClass(adaptCol(columnIndex));
    }

    public int getColumnCount() {
        return this.delegate.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return this.delegate.getColumnName(adaptCol(columnIndex));
    }

    public int getRowCount() {
        return this.delegate.getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return this.delegate.getValueAt(adaptRow(rowIndex), adaptCol(columnIndex));
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return this.delegate.isCellEditable(adaptRow(rowIndex), adaptCol(columnIndex));
    }

    public void removeTableModelListener(TableModelListener l) {
        this.delegate.removeTableModelListener(l);
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        this.delegate.setValueAt(value, adaptRow(rowIndex), adaptCol(columnIndex));
    }
}
