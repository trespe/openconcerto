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

import javax.swing.table.TableModel;

/**
 * A table model exposing only a selection of its delegate data.
 * 
 * @author Sylvain
 */
public class TableModelSelectionAdapter extends TableModelAdapter {

    private final int[] selectedRows;
    private final int[] selectedCols;

    public TableModelSelectionAdapter(TableModel delegate, final int[] selectedRows) {
        this(delegate, selectedRows, null);
    }

    /**
     * Create a new instance.
     * 
     * @param delegate the table model to use for data.
     * @param selectedRows a list of row indexes of <code>delegate</code> to expose, or
     *        <code>null</code> to use all.
     * @param selectedCols a list of column indexes of <code>delegate</code> to expose, or
     *        <code>null</code> to use all.
     */
    public TableModelSelectionAdapter(TableModel delegate, final int[] selectedRows, final int[] selectedCols) {
        super(delegate);
        this.selectedRows = selectedRows;
        this.selectedCols = selectedCols;
    }

    @Override
    protected int adaptRow(int rowIndex) {
        return this.selectedRows == null ? rowIndex : this.selectedRows[rowIndex];
    }

    @Override
    public int getRowCount() {
        return this.selectedRows == null ? super.getRowCount() : this.selectedRows.length;
    }

    @Override
    protected int adaptCol(int columnIndex) {
        return this.selectedCols == null ? columnIndex : this.selectedCols[columnIndex];
    }

    @Override
    public int getColumnCount() {
        return this.selectedCols == null ? super.getColumnCount() : this.selectedCols.length;
    }
}
