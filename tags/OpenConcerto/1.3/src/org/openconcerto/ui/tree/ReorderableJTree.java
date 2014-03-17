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
 
 package org.openconcerto.ui.tree;

import org.openconcerto.ui.list.RJLTransferable;

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

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

public class ReorderableJTree extends JTree implements DragSourceListener, DropTargetListener, DragGestureListener {

    private final DragSource dragSource;

    private MutableTreeNode dropTargetNode = null;
    private MutableTreeNode draggedNode = null;

    public ReorderableJTree() {
        super();
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
        new DropTarget(this, this);
    }

    // DragGestureListener
    public void dragGestureRecognized(DragGestureEvent dge) {
        final Point clickPoint = dge.getDragOrigin();
        final TreePath path = getPathForLocation(clickPoint.x, clickPoint.y);
        if (path == null) {
            // Not on a node;
            return;
        }
        draggedNode = (MutableTreeNode) path.getLastPathComponent();
        final Transferable trans = new RJLTransferable(draggedNode);
        dragSource.startDrag(dge, new Cursor(Cursor.HAND_CURSOR), trans, this);
    }

    // DragSourceListener events
    public void dragDropEnd(DragSourceDropEvent dsde) {
        setCursor(Cursor.getDefaultCursor());
        dropTargetNode = null;
        draggedNode = null;
        repaint();
    }

    public void dragEnter(DragSourceDragEvent dsde) {
    }

    public void dragExit(DragSourceEvent dse) {
    }

    public void dragOver(DragSourceDragEvent dsde) {
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    // DropTargetListener events
    public void dragEnter(DropTargetDragEvent dtde) {
        dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {

        final Point dragPoint = dtde.getLocation();
        final TreePath path = getPathForLocation(dragPoint.x, dragPoint.y);
        if (path == null) {
            return;
        }

        dropTargetNode = (MutableTreeNode) path.getLastPathComponent();
        if (dropTargetNode == null || draggedNode.equals(dropTargetNode)) {
            return;
        }

        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTargetNode.getParent();
        if (parent == null || draggedNode.getParent() == null) {
            return;
        }
        // System.err.println("Drag from " + draggedNode + " to " + dropTargetNode);
        if (((DefaultMutableTreeNode) dropTargetNode).isNodeAncestor(draggedNode)) {
            // Prevent dropping parent in child
            return;
        }

        final DefaultTreeModel defaultTreeModel = (DefaultTreeModel) getModel();
        defaultTreeModel.removeNodeFromParent(draggedNode);
        if (dropTargetNode.getAllowsChildren()) {
            defaultTreeModel.insertNodeInto(draggedNode, dropTargetNode, 0);
        } else {
            final int index = parent.getIndex(dropTargetNode);
            defaultTreeModel.insertNodeInto(draggedNode, parent, index);
        }
        setSelectionPath(new TreePath(defaultTreeModel.getPathToRoot(draggedNode)));
        repaint();
    }

    public void drop(DropTargetDropEvent dtde) {
        dtde.dropComplete(true);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("TreeDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add content to the window.
        final ReorderableJTree tree = new ReorderableJTree();
        frame.add(tree);
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("The Java Series");
        for (int i = 0; i < 10; i++) {
            final DefaultMutableTreeNode r = new DefaultMutableTreeNode("Item" + i);
            root.add(r);
            if (i == 3) {
                for (int j = 0; j < 3; j++) {
                    r.add(new DefaultMutableTreeNode("Item" + i + ":" + j));
                }
            }

        }
        tree.setModel(new DefaultTreeModel(root));
        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
