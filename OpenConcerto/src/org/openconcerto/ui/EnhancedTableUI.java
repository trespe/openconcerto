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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class EnhancedTableUI extends BasicTableUI {

    /**
     * Return the minimum size of the table. The minimum height is the row height (plus inter-cell
     * spacing) times the number of rows. The minimum width is the sum of the minimum widths of each
     * column (plus inter-cell spacing).
     */
    public Dimension getMinimumSize(JComponent c) {
        long width = 0;
        Enumeration enumeration = table.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            TableColumn aColumn = (TableColumn) enumeration.nextElement();
            width = width + aColumn.getMinWidth();
        }
        return createTableSize(width);
    }

    /**
     * Return the preferred size of the table. The preferred height is the row height (plus
     * inter-cell spacing) times the number of rows. The preferred width is the sum of the preferred
     * widths of each column (plus inter-cell spacing).
     */
    public Dimension getPreferredSize(JComponent c) {
        long width = 0;
        Enumeration enumeration = table.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            TableColumn aColumn = (TableColumn) enumeration.nextElement();
            width = width + aColumn.getPreferredWidth();
        }
        return createTableSize(width);
    }

    /**
     * Return the maximum size of the table. The maximum height is the row height (plus inter-cell
     * spacing) times the number of rows. The maximum width is the sum of the maximum widths of each
     * column (plus inter-cell spacing).
     */
    public Dimension getMaximumSize(JComponent c) {
        long width = 0;
        Enumeration enumeration = table.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            TableColumn aColumn = (TableColumn) enumeration.nextElement();
            width = width + aColumn.getMaxWidth();
        }
        return createTableSize(width);
    }

    private Dimension createTableSize(long width) {
        final int row = table.getRowCount() - 1;
        final EnhancedTable enhancedTable = (EnhancedTable) table;
        int height = enhancedTable.getCellRect(row, 0, true).y + enhancedTable.getRowHeight(row);
        // int totalMarginWidth = table.getColumnModel().getColumnMargin() * table.getColumnCount();
        long widthWithMargin = Math.abs(width);// + totalMarginWidth;
        if (widthWithMargin > Integer.MAX_VALUE) {
            widthWithMargin = Integer.MAX_VALUE;
        }

        return new Dimension((int) widthWithMargin, height);
    }

    /**
     * Paint a representation of the <code>table</code> instance that was set in installUI().
     */
    public void paint(Graphics g, JComponent c) {
        if (table.getRowCount() <= 0 || table.getColumnCount() <= 0) {
            return;
        }
        // g.setColor(Color.GREEN);
        // g.fillRect(0, 0, 600, 600);
        Rectangle clip = g.getClipBounds();
        Point upperLeft = clip.getLocation();
        Point lowerRight = new Point(clip.x + clip.width - 1, clip.y + clip.height - 1);
        int rMin = 0;// table.rowAtPoint(upperLeft);
        int rMax = table.getRowCount() - 1;// /table.rowAtPoint(lowerRight);
        // This should never happen.
        if (rMin == -1) {
            rMin = 0;
        }
        // If the table does not have enough rows to fill the view we'll get -1.
        // Replace this with the index of the last row.
        if (rMax == -1) {
            rMax = table.getRowCount() - 1;
        }

        boolean ltr = table.getComponentOrientation().isLeftToRight();
        int cMin = table.columnAtPoint(ltr ? upperLeft : lowerRight);
        int cMax = table.columnAtPoint(ltr ? lowerRight : upperLeft);
        // This should never happen.
        if (cMin == -1) {
            cMin = 0;
        }
        // If the table does not have enough columns to fill the view we'll get -1.
        // Replace this with the index of the last column.
        if (cMax == -1) {
            cMax = table.getColumnCount() - 1;
        }
       // System.out.println("paint:" + rMin + " /" + rMax + " - col:" + cMin + " / " + cMax);
        for (int i = rMin; i <= rMax; i++) {
            for (int j = cMin; j <= cMax; j++) {
                if (table.isEditing() && table.getEditingRow() == i && table.getEditingColumn() == j) {
                    Component component = table.getEditorComponent();

                    component.validate();
                } else {
                    TableCellRenderer renderer = table.getCellRenderer(i, j);
                    if (renderer == null) {
                        throw new IllegalStateException("Renderer null for " + i + "," + j);
                    }
                    table.prepareRenderer(renderer, i, j);
                    // System.out.println("Paint: "+row+" / "+column +" rect: "+cellRect);
                    // rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                    // cellRect.width, cellRect.height, true);
                }

                // TableCellRenderer renderer = table.getCellRenderer(i, j);
                // if(renderer instanceof TextAreaRenderer)
                // renderer.getTableCellRendererComponent(table, table.getValueAt(i, j), false,
                // false, i, j);
                // System.out.println("update renderer: "+i+","+j);
            }
        }
        // Paint the grid.
        paintGrid(g, rMin, rMax, cMin, cMax);

        // Paint the cells.
        paintCells(g, rMin, rMax, cMin, cMax);
    }

    public void paintold(Graphics g, JComponent c) {
        System.out.println("\nEnhancedTableUI.paint()---------------------------- Line count:" + table.getRowCount());
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 600, 600);
        /*
         * Rectangle oldClipBounds = g.getClipBounds(); Rectangle clipBounds = new
         * Rectangle(oldClipBounds); int tableWidth = table.getColumnModel().getTotalColumnWidth();
         * clipBounds.width = Math.min(clipBounds.width, tableWidth); g.setClip(clipBounds);
         */
        // Paint the grid
        // paintGrid(g);
        System.out.println("Update Cells");
        // updateCellsSize(g, 0, table.getRowCount() - 1, 0, table.getColumnCount() - 1);
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                TableCellRenderer renderer = table.getCellRenderer(i, j);
                System.out.println(renderer.getTableCellRendererComponent(table, table.getValueAt(i, j), false, false, i, j));
            }
        }

        System.out.println("Paint Grids");
        paintGrid(g, 0, table.getRowCount() - 1, 0, table.getColumnCount() - 1);
        // Paint the rows
        // paintCells(g, clipBounds, tableWidth);
        System.out.println("Paint Cells");
        paintCells(g, 0, table.getRowCount() - 1, 0, table.getColumnCount() - 1);
        // g.setClip(oldClipBounds);
        System.out.println("EnhancedTableUI.paint()---------------------------- done");
    }

    // From Sun
    /*
     * Paints the grid lines within <I>aRect</I>, using the grid color set with <I>setGridColor</I>.
     * Paints vertical lines if <code>getShowVerticalLines()</code> returns true and paints
     * horizontal lines if <code>getShowHorizontalLines()</code> returns true.
     */
    private void paintGrid(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        g.setColor(table.getGridColor());

        Rectangle minCell = table.getCellRect(rMin, cMin, true);
        Rectangle maxCell = table.getCellRect(rMax, cMax, true);
        Rectangle damagedArea = minCell.union(maxCell);

        if (table.getShowHorizontalLines()) {
            int tableWidth = damagedArea.x + damagedArea.width;
            int y = damagedArea.y;
            for (int row = rMin; row <= rMax; row++) {
                y += table.getRowHeight(row);
                // System.out.println("paintGrid at y=:"+(y-1));
                g.drawLine(damagedArea.x, y - 1, tableWidth - 1, y - 1);
            }
        }
        if (table.getShowVerticalLines()) {
            TableColumnModel cm = table.getColumnModel();
            int tableHeight = damagedArea.y + damagedArea.height;
            int x;
            if (table.getComponentOrientation().isLeftToRight()) {
                x = damagedArea.x;
                for (int column = cMin; column <= cMax; column++) {
                    int w = cm.getColumn(column).getWidth();
                    x += w;
                    g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
                }
            } else {
                x = damagedArea.x + damagedArea.width;
                for (int column = cMin; column < cMax; column++) {
                    int w = cm.getColumn(column).getWidth();
                    x -= w;
                    g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
                }
                x -= cm.getColumn(cMax).getWidth();
                g.drawLine(x, 0, x, tableHeight - 1);
            }
        }
    }

    // From Sun + fix
    private void paintCells(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        JTableHeader header = table.getTableHeader();
        TableColumn draggedColumn = (header == null) ? null : header.getDraggedColumn();

        TableColumnModel cm = table.getColumnModel();
        int columnMargin = cm.getColumnMargin();

        Rectangle cellRect;
        TableColumn aColumn;
        int columnWidth;
        if (table.getComponentOrientation().isLeftToRight()) {
            for (int row = rMin; row <= rMax; row++) {
                cellRect = table.getCellRect(row, cMin, false);
                // Mon fix
                // cellRect.height = ((EnhancedTable) table).getRowHeight(row);
                for (int column = cMin; column <= cMax; column++) {
                    aColumn = cm.getColumn(column);
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    if (aColumn != draggedColumn) {
                        paintCell(g, cellRect, row, column);
                    }
                    cellRect.x += columnWidth;
                }
            }
        } else {
            for (int row = rMin; row <= rMax; row++) {
                cellRect = table.getCellRect(row, cMin, false);
                aColumn = cm.getColumn(cMin);
                if (aColumn != draggedColumn) {
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    paintCell(g, cellRect, row, cMin);
                }
                for (int column = cMin + 1; column <= cMax; column++) {
                    aColumn = cm.getColumn(column);
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    cellRect.x -= columnWidth;
                    if (aColumn != draggedColumn) {
                        paintCell(g, cellRect, row, column);
                    }
                }
            }
        }

        // Paint the dragged column if we are dragging.
        if (draggedColumn != null) {
            paintDraggedArea(g, rMin, rMax, draggedColumn, header.getDraggedDistance());
        }

        // Remove any renderers that may be left in the rendererPane.
        rendererPane.removeAll();
    }

    // From Sun 100%
    private int viewIndexForColumn(TableColumn aColumn) {
        TableColumnModel cm = table.getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }

    // From Sun 100%
    private void paintDraggedArea(Graphics g, int rMin, int rMax, TableColumn draggedColumn, int distance) {
        int draggedColumnIndex = viewIndexForColumn(draggedColumn);

        Rectangle minCell = table.getCellRect(rMin, draggedColumnIndex, true);
        Rectangle maxCell = table.getCellRect(rMax, draggedColumnIndex, true);

        Rectangle vacatedColumnRect = minCell.union(maxCell);

        // Paint a gray well in place of the moving column.
        g.setColor(table.getParent().getBackground());
        g.fillRect(vacatedColumnRect.x, vacatedColumnRect.y, vacatedColumnRect.width, vacatedColumnRect.height);

        // Move to the where the cell has been dragged.
        vacatedColumnRect.x += distance;

        // Fill the background.
        g.setColor(table.getBackground());
        g.fillRect(vacatedColumnRect.x, vacatedColumnRect.y, vacatedColumnRect.width, vacatedColumnRect.height);

        // Paint the vertical grid lines if necessary.
        if (table.getShowVerticalLines()) {
            g.setColor(table.getGridColor());
            int x1 = vacatedColumnRect.x;
            int y1 = vacatedColumnRect.y;
            int x2 = x1 + vacatedColumnRect.width - 1;
            int y2 = y1 + vacatedColumnRect.height - 1;
            // Left
            g.drawLine(x1 - 1, y1, x1 - 1, y2);
            // Right
            g.drawLine(x2, y1, x2, y2);
        }

        for (int row = rMin; row <= rMax; row++) {
            // Render the cell value
            Rectangle r = table.getCellRect(row, draggedColumnIndex, false);
            r.x += distance;
            paintCell(g, r, row, draggedColumnIndex);

            // Paint the (lower) horizontal grid line if necessary.
            if (table.getShowHorizontalLines()) {
                g.setColor(table.getGridColor());
                Rectangle rcr = table.getCellRect(row, draggedColumnIndex, true);
                rcr.x += distance;
                int x1 = rcr.x;
                int y1 = rcr.y;
                int x2 = x1 + rcr.width - 1;
                int y2 = y1 + rcr.height - 1;
                g.drawLine(x1, y2, x2, y2);
            }
        }
    }

    // From Sun 100%
    private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
        if (table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column) {
            Component component = table.getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
        } else {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component component = table.prepareRenderer(renderer, row, column);
            // System.out.println("Paint: "+row+" / "+column +" rect: "+cellRect);
            rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
        }
    }

}
