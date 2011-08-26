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
 
 package org.openconcerto.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class EnhancedTable extends JTable {
    private boolean blockRepaint = false;
    protected HashMap<Integer, Integer> rowHeights = new HashMap<Integer, Integer>();
    private HashMap<Long, Integer> prefferedRowHeights = new HashMap<Long, Integer>();

    public EnhancedTable() {
        this(null, null, null);
    }

    public EnhancedTable(TableModel dm) {
        this(dm, null, null);
    }

    public EnhancedTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    public EnhancedTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initUI();
    }

    public EnhancedTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
    }

    public EnhancedTable(final Vector rowData, final Vector columnNames) {
        super(rowData, columnNames);
        initUI();
    }

    public EnhancedTable(final Object[][] rowData, final Object[] columnNames) {
        super(rowData, columnNames);
        initUI();
    }

    protected void initializeLocalVars() {
        this.setBlockRepaint(true);
        super.initializeLocalVars();
        this.setBlockRepaint(false);
    }

    public void setUI(TableUI ui) {
        this.setBlockRepaint(true);
        super.setUI(ui);

        this.setBlockRepaint(false);
    }

    /**
     * 
     */
    private void initUI() {
        setUI(new EnhancedTableUI());
        // Fix for Nimbus L&F http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6594663
        this.setIntercellSpacing(new Dimension(1, 1));
    };

    public int rowAtPoint(Point point) {
        int y = point.y;
        if (y < 0)
            return -1;

        int rowSpacing = 0;// getIntercellSpacing().height;
        int rowCount = getRowCount();
        int aRowHeight = 0;
        for (int i = 0; i < rowCount; i++) {
            aRowHeight += getRowHeight(i) + rowSpacing;
            if (y < aRowHeight)
                return i;
        }
        return -1;
    }

    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        // System.out.println("EnhancedTable.getCellRect() row:"+row+" col:"+column);
        Rectangle r = new Rectangle();
        boolean valid = true;
        if (row < 0) {
            // y = height = 0;
            valid = false;
        }

        // Difference par rapport a Sun:
        // else if (row >= getRowCount()) { r.y = getHeight(); valid = false; }

        else {
            r.height = getRowHeight(row);
            // int rowSpacing = getIntercellSpacing().height;
            int y = 0;
            for (int i = 0; i < row; i++) {
                y += getRowHeight(i);// + rowSpacing;
            }
            r.y = y;
            // au lieu de: r.y = (rowModel == null) ? row * r.height : rowModel.getPosition(row);
        }

        if (column < 0) {
            if (!getComponentOrientation().isLeftToRight()) {
                r.x = getWidth();
            }
            // otherwise, x = width = 0;
            valid = false;
        } else if (column >= getColumnCount()) {
            if (getComponentOrientation().isLeftToRight()) {
                r.x = getWidth();
            }
            // otherwise, x = width = 0;
            valid = false;
        } else {
            TableColumnModel cm = getColumnModel();
            if (getComponentOrientation().isLeftToRight()) {
                for (int i = 0; i < column; i++) {
                    r.x += cm.getColumn(i).getWidth();
                }
            } else {
                for (int i = cm.getColumnCount() - 1; i > column; i--) {
                    r.x += cm.getColumn(i).getWidth();
                }
            }
            r.width = cm.getColumn(column).getWidth();
        }

        if (valid && !includeSpacing) {
            int rm = getRowMargin();
            int cm = getColumnModel().getColumnMargin();
            // This is not the same as grow(), it rounds differently.
            r.setBounds(r.x + cm / 2, r.y + rm / 2, r.width - cm, r.height - rm);
        }

        // System.out.println("EnhancedTable.getCellRect() row:" + row + " col:" + column + " ->" +
        // r);
        return r;
    }

    public void tableChanged(TableModelEvent e) {
        // System.out.println("EnhancedTable.tableChanged()");

        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            // The whole thing changed
            clearSelection();

            if (getAutoCreateColumnsFromModel())
                createDefaultColumnsFromModel();

            resizeAndRepaint();

            return;
        }

        if (e.getType() == TableModelEvent.INSERT) {
            tableRowsInserted(e);
            repaint();// En plus
            return;
        }

        if (e.getType() == TableModelEvent.DELETE) {
            tableRowsDeleted(e);
            repaint();// En plus
            return;
        }

        int modelColumn = e.getColumn();
        int start = e.getFirstRow();
        int end = e.getLastRow();

        if (start == TableModelEvent.HEADER_ROW) {
            start = 0;
            end = Integer.MAX_VALUE;
        }

        // int rowHeight = getRowHeight() + rowMargin;
        Rectangle dirtyRegion;
        if (modelColumn == TableModelEvent.ALL_COLUMNS) {
            // 1 or more rows changed
            // dirtyRegion = new Rectangle(0, start * rowHeight,
            dirtyRegion = new Rectangle(0, getCellRect(start, 0, false).y,

            getColumnModel().getTotalColumnWidth(), 0);
        } else {
            // A cell or column of cells has changed.
            // Unlike the rest of the methods in the JTable, the TableModelEvent
            // uses the co-ordinate system of the model instead of the view.
            // This is the only place in the JTable where this "reverse mapping"
            // is used.
            int column = convertColumnIndexToView(modelColumn);
            dirtyRegion = getCellRect(start, column, false);
        }

        // Now adjust the height of the dirty region according to the value of "end".
        // Check for Integer.MAX_VALUE as this will cause an overflow.
        if (end != Integer.MAX_VALUE) {
            // dirtyRegion.height = (end-start+1)*rowHeight;
            dirtyRegion.height = getCellRect(end + 1, 0, false).y - dirtyRegion.y;
            repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
        }
        // In fact, if the end is Integer.MAX_VALUE we need to revalidate anyway
        // because the scrollbar may need repainting.
        else {
            resizeAndRepaint();
        }
    }

    private void tableRowsInserted(TableModelEvent e) {
        // System.out.println("EnhancedTable.tableRowsInserted()");
        int start = e.getFirstRow();
        int end = e.getLastRow();
        if (start < 0)
            start = 0;

        // Move down row height info - for rows below the first inserted row
        int rowCount = getRowCount();
        int rowsInserted = end - start + 1;
        for (int r = start; r < rowCount; r++) {
            Integer height = rowHeights.get(Integer.valueOf(r));
            if (height == null)
                continue;
            rowHeights.put(Integer.valueOf(r + rowsInserted), height);
        }

        // 1 or more rows added, so we have to repaint from the first
        // new row to the end of the table. (Everything shifts down)
        // int rowHeight = getRowHeight() + rowMargin;
        Rectangle drawRect = new Rectangle(0, getCellRect(start, 0, false).y,

        getColumnModel().getTotalColumnWidth(), 0);
        // (getRowCount()-start) * rowHeight);
        drawRect.height = getCellRect(rowCount, 0, false).y - drawRect.y;

        // Adjust the selection to account for the new rows
        if (selectionModel != null) {
            if (end < 0)
                end = getRowCount() - 1;
            int length = end - start + 1;

            selectionModel.insertIndexInterval(start, length, true);
        }
        revalidate();
        // PENDING(philip) Find a way to stop revalidate calling repaint
        repaint(drawRect);
        // System.out.println("EnhancedTable.tableRowsInserted() done");
    }

    /*
     * Invoked when rows have been removed from the table.
     * 
     * @param e the TableModelEvent encapsulating the deletion
     */
    private void tableRowsDeleted(TableModelEvent e) {
        int start = e.getFirstRow();
        int end = e.getLastRow();
        if (start < 0)
            start = 0;

        int deletedCount = end - start + 1;
        int previousRowCount = getRowCount() + deletedCount;

        // Remove any height information for deleted rows
        for (int i = start; i <= end; i++)
            resetRowHeight(i);
        // Move up row height info - for rows below the last deleted row
        for (int r = end + 1; r < previousRowCount; r++) {
            Integer height = rowHeights.get(Integer.valueOf(r));
            if (height == null)
                continue;
            rowHeights.put(Integer.valueOf(r - deletedCount), height);
        }

        // 1 or more rows added, so we have to repaint from the first
        // new row to the end of the table. (Everything shifts up)
        // int rowHeight = getRowHeight() + rowMargin;
        Rectangle drawRect = new Rectangle(0, getCellRect(start, 0, false).y, getColumnModel().getTotalColumnWidth(), 0);
        // (previousRowCount - start) * rowHeight);
        drawRect.height = getCellRect(previousRowCount, 0, false).y - drawRect.y;

        // Adjust the selection to account for the new rows
        if (selectionModel != null) {
            if (end < 0)
                end = getRowCount() - 1;

            if (end < 0)
                end = 0;

            if (start < 0)
                start = 0;
            selectionModel.removeIndexInterval(start, end);
        }
        revalidate();

    }

    public int getRowHeight(int row) {
        int result = 0;
        if (rowHeights == null) {

            result = super.getRowHeight() + 3;
        } else {

            Integer o = rowHeights.get(Integer.valueOf(row));
            if (o == null) {
                result = getRowHeight() + 3;
            } else {
                // System.out.println("EnhancedTable.getRowHeight() heights retrieved");
                result = o.intValue();
            }
        }
        // System.out.println("EnhancedTable.getRowHeight() of row: " + row + " ->" + result);
        return result;
    }

    public void setPreferredRowHeight(int row, int column, int preferredHeight) {
        prefferedRowHeights.put(Long.valueOf(column + row * 1024), Integer.valueOf(preferredHeight));
    }

    public int getPreferredRowHeightAt(int row, int column) {
        Integer o = prefferedRowHeights.get(Long.valueOf(column + row * 1024));
        if (o == null)
            return -1;
        return o.intValue();
    }

    public int getMaxRowHeight(int row) {
        int max = 0;
        for (int i = 0; i < this.getColumnCount(); i++) {
            Integer o = prefferedRowHeights.get(Long.valueOf(i + row * 1024));
            if (o != null) {
                int v = o.intValue();
                if (v > max) {
                    max = v;
                }
            }

        }
        return max;
    }

    public void setRowHeight(int row, int height) {
        // System.out.println("EnhancedTable.setRowHeight(): row:" + row + " to height:" + height);
        if (getRowHeight(row) != height) {
            rowHeights.put(Integer.valueOf(row), Integer.valueOf(height + 3));
        }
        // rowModel.setSize(row, rowHeight);
        // / super.setRowHeight(row, rowHeight);

        // int rowCount = getRowCount();
        /*
         * Rectangle drawRect = new Rectangle(0, getCellRect(row, 0, false).y,
         * 
         * getColumnModel().getTotalColumnWidth(), 0); // (getRowCount()-start) * rowHeight);
         * drawRect.height = getCellRect(rowCount, 0, false).y - drawRect.y; // revalidate();
         * repaint(drawRect);
         */
    }

    void resetRowHeight(int row) {
        rowHeights.remove(Integer.valueOf(row));
        // revalidate();
    }

    void resetRowHeight() {
        rowHeights.clear();
        // revalidate();
    }

    public void setBlockRepaint(boolean blockRepaint) {
        // this.blockRepaint = blockRepaint;
    }

    public boolean isBlockRepaint() {
        return blockRepaint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // If call resizeAndRepaint(); infinite repaints on OpenSuse...
    }

    @Override
    public void paintComponents(Graphics g) {
        super.paintComponents(g);
        resizeAndRepaint();
    }

    public void repaint() {
        if (!blockRepaint) {
            // System.out.println("--------------- EnhancedTable.repaint()");
            // Thread.dumpStack();
            super.repaint();
        }
        // else
        // System.err.println("Blocked repaint");
    }

    /**
     * Sert a mettre la bonne taille quand ce que l'on tape fait appairaitre les slider du
     * Scrollpane
     */

    public void columnMarginChanged(ChangeEvent e) {
        /*
         * Code de Sun: if (isEditing()) { removeEditor(); } TableColumn resizingColumn =
         * getResizingColumn(); // Need to do this here, before the parent's // layout manager calls
         * getPreferredSize(). if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
         * resizingColumn.setPreferredWidth(resizingColumn.getWidth()); }
         */
        // Vu! Si on ne le fait pas, les colonnes sortent de la largeur

        super.columnMarginChanged(e);
        resizeAndRepaint();
    }

    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m != null) {
            // Remove any current columns
            TableColumnModel cm = getColumnModel();
            while (cm.getColumnCount() > 0) {
                cm.removeColumn(cm.getColumn(0));
            }

            // Create new columns from the data model info
            for (int i = 0; i < m.getColumnCount(); i++) {
                TableColumn newColumn = new EnhancedTableColumn(i);
                addColumn(newColumn);
            }
        }
    }

    public void setBlockEventOnColumn(boolean blockRepaint) {
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            EnhancedTableColumn newColumn = (EnhancedTableColumn) cm.getColumn(i);
            newColumn.setBlockEvent(blockRepaint);
        }
    }

    protected void resizeAndRepaint() {
        // System.out.println("*- EnhancedTable.resizeAndRepaint()" + this.getSize());
        // Update the UI of the table header
        if (this.tableHeader != null) {

            this.tableHeader.resizeAndRepaint();
        }

        super.resizeAndRepaint();
        // System.out.println("*- EnhancedTable.resizeAndRepaint()" + this.getSize() + " done");
    }

    @Override
    public boolean getShowHorizontalLines() {
        // Fix for Nimbus L&F http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6594663
        return true;
    }

    @Override
    public boolean getShowVerticalLines() {
        // Fix for Nimbus L&F http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6594663
        return true;
    }
}
