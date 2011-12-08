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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

public class TableRowResizer extends MouseInputAdapter{ 
    public static Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR); 
 
    private int mouseYOffset, resizingRow; 
    private Cursor otherCursor = resizeCursor; 
    private JTable table; 
 
    public TableRowResizer(JTable table){ 
        this.table = table; 
        table.addMouseListener(this); 
        table.addMouseMotionListener(this); 
    } 
 
    private int getResizingRow(Point p){ 
        return getResizingRow(p, table.rowAtPoint(p)); 
    } 
 
    private int getResizingRow(Point p, int row){ 
        if(row == -1){ 
            return -1; 
        } 
        int col = table.columnAtPoint(p); 
        if(col==-1) 
            return -1; 
        Rectangle r = table.getCellRect(row, col, true); 
        r.grow(0, -3); 
        if(r.contains(p)) 
            return -1; 
 
        int midPoint = r.y + r.height / 2; 
        int rowIndex = (p.y < midPoint) ? row - 1 : row; 
 
        return rowIndex; 
    } 
 
    public void mousePressed(MouseEvent e){ 
        Point p = e.getPoint(); 
 
        resizingRow = getResizingRow(p); 
        mouseYOffset = p.y - table.getRowHeight(resizingRow); 
    } 
 
    private void swapCursor(){ 
        Cursor tmp = table.getCursor(); 
        table.setCursor(otherCursor); 
        otherCursor = tmp; 
    } 
 
    public void mouseMoved(MouseEvent e){ 
        if((getResizingRow(e.getPoint())>=0) 
           != (table.getCursor() == resizeCursor)){ 
            swapCursor(); 
        } 
    } 
 
    public void mouseDragged(MouseEvent e){ 
        int mouseY = e.getY(); 
 
        if(resizingRow >= 0){ 
            int newHeight = mouseY - mouseYOffset; 
            if(newHeight > 0)
                table.setRowHeight(resizingRow, newHeight); 
        } 
    } 
} 
