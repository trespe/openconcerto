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

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

public class JComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

    private JComboBox comboBox;

    public JComboBoxCellEditor(final JComboBox comboBox) {
        this.comboBox = comboBox;
        JComponent editorComponent = (JComponent) comboBox.getEditor().getEditorComponent();
        editorComponent.setBorder(null);
    }

    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }

    public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }

}
