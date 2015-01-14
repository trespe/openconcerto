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
 
 package org.openconcerto.erp.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArrayTableModel {
    private List<List<Object>> dataVector;
    private List<Class<?>> columnTypes;

    public ArrayTableModel(List<List<Object>> data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Empty data");
        }

        int colCount = data.get(0).size();
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (int i = 0; i < colCount; i++) {
            types.add(Object.class);
        }
        init(data, types);
    }

    public ArrayTableModel(List<List<Object>> data, List<Class<?>> types) {
        init(data, types);

    }

    private void init(List<List<Object>> data, List<Class<?>> types) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Empty data");
        }
        int colCount = data.get(0).size();
        if (colCount != types.size()) {
            throw new IllegalArgumentException("Data raw count doesn't match types count");
        }
        this.dataVector = data;
        this.columnTypes = types;
    }

    /**
     * Returns the number of rows in the model.
     * 
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        return dataVector.size();
    }

    /**
     * Returns the number of columns in the model.
     * 
     * @return the number of columns in the model
     * @see #getRowCount
     */
    public int getColumnCount() {
        return columnTypes.size();
    }

    /**
     * Returns the most specific superclass for all the cell values in the column.
     * 
     * @param columnIndex the index of the column
     * @return the common ancestor class of the object values in the model.
     */
    public Class<?> getColumnClass(int columnIndex) {
        return this.columnTypes.get(columnIndex);
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and <code>rowIndex</code>.
     * 
     * @param rowIndex the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        final List<Object> row = dataVector.get(rowIndex);
        return row.get(columnIndex);
    }

    /**
     * Sets the value in the cell at <code>columnIndex</code> and <code>rowIndex</code> to
     * <code>aValue</code>.
     * 
     * @param aValue the new value
     * @param rowIndex the row whose value is to be changed
     * @param columnIndex the column whose value is to be changed
     * @see #getValueAt
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        final List<Object> rowVector = dataVector.get(rowIndex);
        if (aValue.getClass().equals(this.getColumnClass(columnIndex))) {
            rowVector.set(columnIndex, columnIndex);
        } else {
            throw new IllegalArgumentException(aValue + " should be an instance of " + this.getColumnClass(columnIndex) + " but is an instance of " + aValue.getClass());
        }

    }

    public void dump() {
        dump(0, this.getRowCount());
    }

    public void dump(int start, int stop) {
        final int rowCount = Math.min(this.getRowCount(), stop);
        final int colCount = this.getColumnCount();
        System.out.print("Types: ");
        for (int j = 0; j < colCount; j++) {
            Class c = this.columnTypes.get(j);
            System.out.print(j + ":" + c.getSimpleName());
            if (j < colCount - 1) {
                System.out.print(",");
            }
        }
        System.out.println();

        for (int i = start; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                Object v = getValueAt(i, j);
                if (v != null) {
                    System.out.print("[" + j + ":" + v.getClass().getSimpleName() + "]" + v);
                } else {
                    System.out.print("[" + j + "] null");
                }
                if (j < colCount - 1) {
                    System.out.print(",");
                }
            }
            System.out.println();
        }

    }

    public List<Object> getLineValuesAt(int rowIndex) {
        return Collections.unmodifiableList(this.dataVector.get(rowIndex));
    }

    public Set<Object> getValueSetForColumn(int column) {
        final Set<Object> result = new HashSet<Object>();
        final int rowCount = this.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final Object v = getValueAt(i, column);
            result.add(v);
        }
        return result;
    }
}
