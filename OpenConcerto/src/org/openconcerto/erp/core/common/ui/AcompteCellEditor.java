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

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class AcompteCellEditor extends AbstractCellEditor implements TableCellEditor {

    private AcompteField acompteField;

    private String fieldNamePercent, fieldNameMontant;

    public AcompteCellEditor(String fieldNamePercent, String fieldNameMontant) {
        super();

        this.acompteField = new AcompteField();
        this.fieldNamePercent = fieldNamePercent;
        this.fieldNameMontant = fieldNameMontant;
    }

    public void addKeyListener(KeyListener l) {
        this.acompteField.addKeyListener(l);
    }

    public boolean isCellEditable(EventObject e) {

        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        }
        return super.isCellEditable(e);
    }

    public Object getCellEditorValue() {

        return this.acompteField.getValue();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        RowValuesTable rowValuesTable = ((RowValuesTable) table);
        SQLRowValues rowVals = rowValuesTable.getRowValuesTableModel().getRowValuesAt(row);

        BigDecimal montant = rowVals.getBigDecimal(this.fieldNameMontant);
        BigDecimal percent = rowVals.getBigDecimal(this.fieldNamePercent);

        Acompte a = new Acompte(percent, montant);

        this.acompteField.setValue(a);
        this.acompteField.grabFocus();
        return this.acompteField;
    }

}
