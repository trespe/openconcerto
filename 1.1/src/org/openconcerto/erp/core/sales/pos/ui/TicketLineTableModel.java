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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.model.TicketLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TicketLineTableModel extends AbstractTableModel {
    private final List<TicketLine> list = new ArrayList<TicketLine>();

    TicketLineTableModel() {

    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Texte";
        }
        return "Style";
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final TicketLine l = this.list.get(rowIndex);
        if (columnIndex == 0) {
            return l.getText();
        }
        return l.getStyle();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (rowIndex >= this.list.size()) {
            return;
        }
        final TicketLine l = this.list.get(rowIndex);

        if (columnIndex == 0) {
            l.setText(value.toString());
        } else {
            if (value == null) {
                value = "normal";
            }
            l.setStyle(value.toString());
        }
    }

    public void setContent(List<TicketLine> data) {
        this.list.clear();
        this.list.addAll(data);
    }

    public void removeLine(int i) {
        if (i >= 0 && i < this.list.size()) {
            this.list.remove(i);
        }
        fireTableDataChanged();
    }

    public void addLine(int i) {
        if (i >= 0 && i < this.list.size()) {
            this.list.add(i, new TicketLine("--", "normal"));
        } else {
            this.list.add(new TicketLine("--", "normal"));
        }
        fireTableDataChanged();
    }

    public List<TicketLine> getLines() {
        return Collections.unmodifiableList(this.list);
    }
}
