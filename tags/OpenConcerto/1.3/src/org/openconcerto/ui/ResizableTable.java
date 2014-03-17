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

import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class ResizableTable extends JTable {
    protected MouseInputAdapter rowResizer, columnResizer = null;

    public ResizableTable() {
    }

    public ResizableTable(TableModel dm) {
        super(dm);
    }

    // turn resizing on/of
    public void setResizable(boolean row, boolean column) {
        if (row) {
            if (rowResizer == null)
                rowResizer = new TableRowResizer(this);
        } else if (rowResizer != null) {
            removeMouseListener(rowResizer);
            removeMouseMotionListener(rowResizer);
            rowResizer = null;
        }
        if (column) {
            if (columnResizer == null)
                columnResizer = new TableColumnResizer(this);
        } else if (columnResizer != null) {
            removeMouseListener(columnResizer);
            removeMouseMotionListener(columnResizer);
            columnResizer = null;
        }
    }

    // mouse press intended for resize shouldn't change row/col/cell celection
    public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (getCursor() == TableColumnResizer.resizeCursor || getCursor() == TableRowResizer.resizeCursor)
            return;
        super.changeSelection(row, column, toggle, extend);
    }

    // Pour remplir tout l'espace
    public boolean getScrollableTracksViewportHeight() {
        return getPreferredSize().height < getParent().getHeight();
    }
    public static void main(String[] args) {
    	ResizableTable t=new ResizableTable(new DefaultTableModel(10,5));
    	t.setResizable(true,true);
    	//t.setCellEditor(new TextAreaTableCellEditor());
    	//t.setc(new TextAreaTableCellEditor());
    	TableColumnModel cmodel = t.getColumnModel();
    	cmodel.getColumn(1).setCellEditor(new TextAreaTableCellEditor(t));
    	cmodel.getColumn(1).setCellRenderer(new TextAreaRenderer());
    	
    	JFrame f=new JFrame();
    	f.getContentPane().setLayout(new GridLayout(1,1));
    	f.getContentPane().add(new JScrollPane(t));
    	f.setSize(500,300);
    	ToolTipManager.sharedInstance().unregisterComponent(t);
    	ToolTipManager.sharedInstance().unregisterComponent(t.getTableHeader());
    	f.show();
    	
    }
    public void columnMarginChanged(ChangeEvent e) {
       /* if (isEditing()) {
                removeEditor();
            }
        TableColumn resizingColumn = getResizingColumn();
        // Need to do this here, before the parent's
        // layout manager calls getPreferredSize().
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }*/
        resizeAndRepaint();
        }
    
    
}
