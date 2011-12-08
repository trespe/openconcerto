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

import java.awt.IllegalComponentStateException;
import java.awt.KeyboardFocusManager;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class LightEventJTable extends JTable {

    private boolean blockRepaint = false;

    public LightEventJTable(TableModel model) {
        super(model);
    }

    public LightEventJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super();
        setLayout(null);

        /*
         * setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
         * JComponent.getManagingFocusForwardTraversalKeys());
         * setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
         * JComponent.getManagingFocusBackwardTraversalKeys());
         */
        if (cm == null) {
            cm = createDefaultColumnModel();
            autoCreateColumnsFromModel = true;
        }
        setColumnModel(cm);

        if (sm == null) {
            sm = createDefaultSelectionModel();
        }
        setSelectionModel(sm);

        // Set the model last, that way if the autoCreatColumnsFromModel has
        // been set above, we will automatically populate an empty columnModel
        // with suitable columns for the new model.
        if (dm == null) {
            dm = createDefaultDataModel();
        }
        setModel(dm);

        initializeLocalVars();
        updateUI();
    }

    public void createDefaultColumnsFromModel() {
        this.setBlockRepaint(true);
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
        this.setBlockRepaint(false);
    }

    public void setBlockEventOnColumn(boolean blockRepaint) {
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            EnhancedTableColumn newColumn = (EnhancedTableColumn) cm.getColumn(i);
            newColumn.setBlockEvent(blockRepaint);
        }
    }

    public void setBlockRepaint(boolean blockRepaint) {
        this.blockRepaint = blockRepaint;
    }

    public boolean isBlockRepaint() {
        return blockRepaint;
    }

    public void updateUI() {
        // TODO Auto-generated method stub
        this.setBlockRepaint(true);
        super.updateUI();
        this.setBlockRepaint(false);
    }

    public void doLayout() {
        this.setBlockRepaint(true);
        super.doLayout();
        this.setBlockRepaint(false);
    }

    protected void initializeLocalVars() {
        this.setBlockRepaint(true);
        super.initializeLocalVars();
        this.setBlockRepaint(false);
    }

    long lastRepaint = 0;

    public void repaint() {
        //System.err.println("repaint");
        //Thread.dumpStack();
        if (!blockRepaint) {
            super.repaint();
            /*
             * long t=System.currentTimeMillis(); if(t-lastRepaint<800){ System.err.println("Trop
             * de repaint sur cette table"); Thread.dumpStack(); } lastRepaint=t;
             */
            // System.out.println("LightEventJTable.repaint()");
            // Thread.dumpStack();
        }
        
    }

    public void removeSelectedRows() {
        final DefaultTableModel model = (DefaultTableModel) this.getModel();
        int index = getSelectedRow();
        while (index >= 0) {
            model.removeRow(index);
            index = getSelectedRow();
        }
    }
}
