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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.map.ui.ITextComboVilleViewer;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class ITextComboVilleTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    private ITextComboVilleViewer comboBox;

    public ITextComboVilleTableCellEditor() {
        super();

        this.comboBox = new ITextComboVilleViewer();
        this.comboBox.setButtonVisible(false);
    }

    public void addKeyListener(KeyListener l) {
        this.comboBox.addKeyListener(l);
    }

    public boolean isCellEditable(EventObject e) {

        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {

        return this.comboBox.getValue();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // System.err.println("SQLTextComboEditor.getTableCellEditorComponent()");
        RowValuesTable rowValuesTable = ((RowValuesTable) table);
        SQLRowValues rowVals = rowValuesTable.getRowValuesTableModel().getRowValuesAt(row);
        String v = rowVals.getString("VILLE");
        String code = rowVals.getString("CODE_POSTAL");
        Ville ville = Ville.getVilleFromVilleEtCode(v + " (" + code + ")");
        this.comboBox.setValue(ville);
        this.comboBox.grabFocus();
        return this.comboBox;
    }

}
