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
 
 package org.openconcerto.openoffice.spreadsheet;

import org.openconcerto.openoffice.ODDocument;

import javax.swing.table.AbstractTableModel;

public class SheetTableModel<D extends ODDocument> extends AbstractTableModel {

    protected final Table<D> table;
    protected final int row;
    protected final int column;
    protected final int lastRow;
    protected final int lastCol;

    SheetTableModel(final Table<D> table, final int row, final int column) {
        this(table, row, column, table.getRowCount(), table.getColumnCount());
    }

    /**
     * Creates a new instance.
     * 
     * @param table parent table.
     * @param row the first row, inclusive.
     * @param column the first column, inclusive.
     * @param lastRow the last row, exclusive.
     * @param lastCol the last column, exclusive.
     */
    SheetTableModel(final Table<D> table, final int row, final int column, final int lastRow, final int lastCol) {
        super();
        this.table = table;
        this.row = row;
        this.column = column;
        this.lastRow = lastRow;
        this.lastCol = lastCol;
    }

    @Override
    public int getColumnCount() {
        return this.lastCol - this.column;
    }

    @Override
    public int getRowCount() {
        return this.lastRow - this.row;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        check(rowIndex, columnIndex);
        return this.table.getValueAt(this.column + columnIndex, this.row + rowIndex);
    }

    public Cell<D> getImmutableCellAt(int rowIndex, int columnIndex) {
        check(rowIndex, columnIndex);
        return this.table.getImmutableCellAt(this.column + columnIndex, this.row + rowIndex);
    }

    // protect cells outside our range
    protected final void check(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= this.getRowCount())
            throw new IndexOutOfBoundsException("row :" + rowIndex + " not between 0 and " + (this.getRowCount() - 1));
        if (columnIndex < 0 || columnIndex >= this.getColumnCount())
            throw new IndexOutOfBoundsException("column: " + columnIndex + " not between 0 and " + (this.getColumnCount() - 1));
    }

    static public final class MutableTableModel<D extends ODDocument> extends SheetTableModel<D> {

        MutableTableModel(final Table<D> table, final int row, final int column) {
            super(table, row, column);
        }

        MutableTableModel(final Table<D> table, final int row, final int column, final int lastRow, final int lastCol) {
            super(table, row, column, lastRow, lastCol);
        }

        @Override
        public void setValueAt(final Object obj, final int rowIndex, final int columnIndex) {
            check(rowIndex, columnIndex);
            this.table.setValueAt(obj, this.column + columnIndex, this.row + rowIndex);
        }

        public MutableCell<D> getCellAt(int rowIndex, int columnIndex) {
            check(rowIndex, columnIndex);
            return this.table.getCellAt(this.column + columnIndex, this.row + rowIndex);
        }
    }
}
