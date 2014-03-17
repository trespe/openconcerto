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
 
 package org.openconcerto.ui.list;

import org.openconcerto.ui.DefaultListModel;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.JList;

public class ReorderableJList extends JList implements DragSourceListener, DropTargetListener, DragGestureListener {

    private final DragSource dragSource;
    private final DropTarget dropTarget;

    private int draggedIndex = -1;

    public ReorderableJList() {
        super();
        setModel(new DefaultListModel());
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
        dropTarget = new DropTarget(this, this);
    }

    // DragGestureListener
    public void dragGestureRecognized(DragGestureEvent dge) {
        Point clickPoint = dge.getDragOrigin();
        int index = locationToIndex(clickPoint);
        if (index == -1) {
            return;
        }
        Object target = getModel().getElementAt(index);
        Transferable trans = new RJLTransferable(target);
        draggedIndex = index;
        dragSource.startDrag(dge, new Cursor(Cursor.HAND_CURSOR), trans, this);
    }

    // DragSourceListener events
    public void dragDropEnd(DragSourceDropEvent dsde) {
        draggedIndex = -1;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void dragEnter(DragSourceDragEvent dsde) {
    }

    public void dragExit(DragSourceEvent dse) {
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    // DropTargetListener events
    public void dragEnter(DropTargetDragEvent dtde) {
        if (dtde.getSource() != dropTarget) {
            dtde.rejectDrag();
        } else {
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        }
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void drop(DropTargetDropEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (dtde.getSource() != dropTarget) {
            dtde.rejectDrag();
        }
        Point dragPoint = dtde.getLocation();
        int index = locationToIndex(dragPoint);

        try {
            if (index != draggedIndex) {
                DefaultListModel mod = (DefaultListModel) getModel();
                Object from = mod.remove(draggedIndex);
                mod.insertElementAt(from, index);
                setSelectedIndex(index);
                draggedIndex = index;
                repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        repaint();
    }

    @Override
    public void dragOver(DragSourceDragEvent dsde) {

    }

}
