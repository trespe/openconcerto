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

import org.openconcerto.ui.Log;
import org.openconcerto.utils.Tuple2;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Allow to maintain columns' width so that the widest cell fits. Maintain meaning that after a
 * table change event or even after a table model change the column will be automatically resized.
 * 
 * @author Sylvain CUAZ
 */
public class ColumnSizeAdjustor {

    private final JTable table;
    // keep a reference to be able to remove our listener
    private TableModel tableModel;
    private final TableModelListener tableL;
    private int origAutoResizeMode;
    // true if we can create columns ourselves
    private boolean createColumns;
    private final PropertyChangeListener autoResizeL;
    private final List<Integer> maxWidths;

    private boolean installed;

    public ColumnSizeAdjustor(final JTable table) {
        this.table = table;

        this.autoResizeL = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("autoResizeMode") || evt.getPropertyName().equals("autoCreateColumnsFromModel")) {
                    Log.get().warning(evt.getPropertyName() + " changed so uninstalling " + ColumnSizeAdjustor.this);
                    uninstall();
                } else if (evt.getPropertyName().equals("model")) {
                    uninstall();
                    install();
                }
            }
        };
        this.tableL = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    // structure change, the columns and the data model can be out of sync since the
                    // columns are also created by e
                    if (ColumnSizeAdjustor.this.createColumns) {
                        // if we can, update columModel so we can compute new widths immediately,
                        // avoiding flicker (since otherwise new columns will be resized in an
                        // invokeLater())
                        // following lines pasted from JTable.createDefaultColumnsFromModel()
                        TableModel m = (TableModel) e.getSource();
                        // Remove any current columns
                        TableColumnModel cm = table.getColumnModel();
                        while (cm.getColumnCount() > 0) {
                            cm.removeColumn(cm.getColumn(0));
                        }
                        if (m != null) {
                            // Create new columns from the data model info
                            for (int i = 0; i < m.getColumnCount(); i++) {
                                TableColumn newColumn = new TableColumn(i);
                                table.addColumn(newColumn);
                            }
                        }
                        packColumns();
                    } else
                        // if we don't create the columns ourselves we can't know when they will be,
                        // thus invokeLater() so that ColumModel and TableModel are coherent again
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                packColumns();
                            }
                        });
                } else {
                    // only row change
                    final boolean allCols = e.getColumn() == TableModelEvent.ALL_COLUMNS;
                    final int firstCol = allCols ? 0 : e.getColumn();
                    // +1 since we want lastCol exclusive
                    final int lastCol = allCols ? ((TableModel) e.getSource()).getColumnCount() : e.getColumn() + 1;
                    for (int i = firstCol; i < lastCol; i++) {
                        packColumn(i, e.getType(), e.getFirstRow(), e.getLastRow());
                    }
                }
            }
        };
        this.maxWidths = new ArrayList<Integer>(this.table.getColumnCount());
        this.installed = false;
        this.tableModel = null;
        this.install();
    }

    public final boolean isInstalled() {
        return this.installed;
    }

    public void setInstalled(boolean b) {
        if (b != this.installed) {
            if (b) {
                // Disable auto resizing
                this.origAutoResizeMode = this.table.getAutoResizeMode();
                this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                // if the table was creating the columns, take over, so as to keep the column model
                // and the data model in sync
                this.createColumns = this.table.getAutoCreateColumnsFromModel();
                if (this.createColumns)
                    this.table.setAutoCreateColumnsFromModel(false);

                this.table.addPropertyChangeListener(this.autoResizeL);
                this.tableModel = this.table.getModel();
                this.tableModel.addTableModelListener(this.tableL);
                this.packColumns();
            } else {
                this.tableModel.removeTableModelListener(this.tableL);
                this.tableModel = null;
                this.table.removePropertyChangeListener(this.autoResizeL);
                if (this.table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF)
                    this.table.setAutoResizeMode(this.origAutoResizeMode);
                if (this.createColumns)
                    this.table.setAutoCreateColumnsFromModel(true);
            }
            this.installed = b;
        }
    }

    public void install() {
        this.setInstalled(true);
    }

    public void uninstall() {
        this.setInstalled(false);
    }

    public void packColumns() {
        final int columnCount = this.table.getColumnCount();
        this.maxWidths.clear();
        this.maxWidths.addAll(Collections.<Integer> nCopies(columnCount, null));
        for (int c = 0; c < columnCount; c++) {
            packColumn(c, TableModelEvent.UPDATE, 0, -1);
        }
    }

    private final void packColumn(int colModelIndex, final int type, final int firstModelRow, final int lastModelRow) {
        // MAYBE compute anyway if rowCount is low, or keep the 100 largest cells per column to
        // shrink
        if (type == TableModelEvent.DELETE)
            return;

        final int initialWidth = this.maxWidths.get(colModelIndex) == null ? 0 : this.maxWidths.get(colModelIndex).intValue();
        final Tuple2<TableColumn, Integer> colAndWidth = computeMaxWidth(colModelIndex, initialWidth, firstModelRow, lastModelRow);
        final Integer width = colAndWidth.get1();
        this.maxWidths.set(colModelIndex, width);
        // Set the width
        if (width != null) {
            final int margin = 2;
            colAndWidth.get0().setPreferredWidth(width + 2 * margin);
        }
    }

    /**
     * Compute the max width for all cells of column <code>colModelIndex</code> from row
     * <code>firstModelRow</code> inclusive to row <code>last</code> inclusive.
     * 
     * @param colModelIndex the column index.
     * @param initialWidth the initial value to give to the result, ie useful when inserting new
     *        rows.
     * @param firstModelRow the first line to compute.
     * @param last the last inclusive line to compute.
     * @return the column and its width, both <code>null</code> if <code>colModelIndex</code> is not
     *         displayed.
     */
    private final Tuple2<TableColumn, Integer> computeMaxWidth(final int colModelIndex, final int initialWidth, final int firstModelRow, final int last) {
        final int viewIndex = this.table.convertColumnIndexToView(colModelIndex);
        if (viewIndex < 0)
            // not displayed
            return new Tuple2<TableColumn, Integer>(null, null);
        final TableColumn col = this.table.getColumnModel().getColumn(viewIndex);

        final int rowCount = this.table.getRowCount();
        // TableModelEvent use Integer.MAX_VALUE
        // +1 since last is inclusive and we want lastModelRow to be exclusive
        final int lastModelRow = last < 0 || last >= rowCount ? rowCount : last + 1;

        // don't need initialWidth if we're computing the whole column
        int width = firstModelRow == 0 && lastModelRow == rowCount ? 0 : initialWidth;
        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = this.table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(this.table, col.getHeaderValue(), false, false, 0, 0);
        width = Math.max(width, comp.getPreferredSize().width);

        // Get maximum width of column data
        // <= since last is inclusive
        for (int r = firstModelRow; r < lastModelRow; r++) {
            final int viewRow = this.table.convertRowIndexToView(r);
            renderer = this.table.getCellRenderer(viewRow, viewIndex);
            comp = renderer.getTableCellRendererComponent(this.table, this.table.getModel().getValueAt(r, colModelIndex), false, false, viewRow, viewIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        return Tuple2.create(col, width);
    }
}
