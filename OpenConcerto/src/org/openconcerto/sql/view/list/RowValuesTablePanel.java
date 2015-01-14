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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public abstract class RowValuesTablePanel extends JPanel {
    protected RowValuesTableModel model;
    protected RowValuesTable table;
    protected SQLRowValues defaultRowVals;

    /**
     * TableModel and Table initialization
     * */
    protected abstract void init();

    public abstract SQLElement getSQLElement();

    /**
     * User interface initialization
     */
    protected void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(new JScrollPane(this.table), c);
        this.table.setDefaultRenderer(Long.class, new RowValuesTableRenderer());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());
    }

    public void updateField(String field, int id) {
        this.table.updateField(field, id);
    }

    public RowValuesTable getRowValuesTable() {
        return this.table;
    }

    public void insertFrom(String field, int id) {
        this.table.insertFrom(field, id);
    }

    public RowValuesTableModel getModel() {
        return this.table.getRowValuesTableModel();
    }

    public void refreshTable() {
        this.table.repaint();
    }

    public SQLRowValues getDefaultRowValues() {
        return this.defaultRowVals;
    }

    public void removeSelectedRow() {
        int index = this.table.getSelectedRow();
        if (index >= 0 && index < model.getRowCount()) {
            this.model.removeRowAt(index);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.table.setEnabled(enabled);
    }

}
