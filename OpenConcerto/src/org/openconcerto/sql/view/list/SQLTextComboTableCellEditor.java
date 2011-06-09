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
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class SQLTextComboTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private SQLRequestComboBox comboBox;
    // Stock Value of Combo to fix problem with undefined
    int val = 1;

    boolean addUndefined;

    public SQLTextComboTableCellEditor(final SQLElement elt, final boolean addUndefined) {
        super();

        this.addUndefined = addUndefined;

        this.comboBox = new SQLRequestComboBox(addUndefined);
        ComboSQLRequest c = new ComboSQLRequest(elt.getComboRequest());
        this.comboBox.uiInit(c);
    }

    public SQLRequestComboBox getCombo() {
        return this.comboBox;
    }

    public void addAction(Action a) {
        this.comboBox.getActions().add(a);
    }

    public boolean isCellEditable(EventObject e) {

        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {
        return this.val;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value != null) {
            this.val = (Integer) value;
        }
        this.comboBox.setValue(Integer.valueOf(value.toString()));
        this.comboBox.grabFocus();

        this.comboBox.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!SQLTextComboTableCellEditor.this.comboBox.isUpdating()) {
                    SQLTextComboTableCellEditor.this.val = SQLTextComboTableCellEditor.this.comboBox.getSelectedId();
                }
            }
        });
        return this.comboBox;
    }

    public int getComboSelectedId() {
        return SQLTextComboTableCellEditor.this.comboBox.getSelectedId();
    }

    public void setWhere(Where w) {
        this.comboBox.getRequest().setWhere(w);
    }

    public void addSelectionListener(PropertyChangeListener l) {
        this.comboBox.addValueListener(l);
    }
}
