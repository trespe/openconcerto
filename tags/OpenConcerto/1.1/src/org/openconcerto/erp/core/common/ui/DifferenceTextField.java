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

import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;

import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;



public class DifferenceTextField extends JTextField implements TableModelListener {
    private RowValuesTableModel model;
    private int columnIndex1,columnIndex2;
 
    public DifferenceTextField(RowValuesTableModel model, SQLTableElement e1, SQLTableElement e2) {
        super();
        this.model = model;
        this.columnIndex1 = model.getColumnIndexForElement(e1);
        this.columnIndex2 = model.getColumnIndexForElement(e2);
        if (columnIndex1 < 0 ||  columnIndex2 < 0) {
            throw new IllegalArgumentException("Impossible de trouver la colonne de " + e1 + " / "+e2);
        }
        updateTotal();
        model.addTableModelListener(this);
    }

    public void tableChanged(TableModelEvent e) {
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() == columnIndex1 || e.getColumn() == columnIndex2) {
            System.out.println(e);
            updateTotal();
        }
    }

    /**
     * 
     */
    private void updateTotal() {
        double total1 = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Number n = (Number) model.getValueAt(i, columnIndex1);
            total1 += n.doubleValue();
        }
        double total2 = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Number n = (Number) model.getValueAt(i, columnIndex2);
            total2 += n.doubleValue();
        }
        this.setText(new PrixHT(total1-total2).toString());
    }

}
