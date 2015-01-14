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
 
 package org.openconcerto.sql.ui.textmenu;

import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TextFieldMenuTableModel extends AbstractTableModel {

    List<Tuple2<TextFieldMenuItem, Boolean>> data = new ArrayList<Tuple2<TextFieldMenuItem, Boolean>>();

    private final String colName;

    public TextFieldMenuTableModel(List<TextFieldMenuItem> items, String colName) {
        this.colName = colName;
        fill(items);
    }

    private void fill(List<TextFieldMenuItem> items) {
        for (TextFieldMenuItem textFieldMenuItem : items) {
            data.add(Tuple2.create(textFieldMenuItem, textFieldMenuItem.isSelected()));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public String getColumnName(int column) {
        if (column == 1) {
            return this.colName;
        } else {
            return "";
        }
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public List<Tuple2<TextFieldMenuItem, Boolean>> getItems() {
        return data;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Tuple2<TextFieldMenuItem, Boolean> item = data.get(rowIndex);
        if (columnIndex == 0) {
            return item.get1();
        } else {
            return item.get0().getName();
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        data.set(rowIndex, new Tuple2<TextFieldMenuItem, Boolean>(data.get(rowIndex).get0(), (Boolean) aValue));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        Tuple2<TextFieldMenuItem, Boolean> item = data.get(rowIndex);
        if (columnIndex == 0) {
            return item.get0().isEnabled();
        } else {
            return false;
        }
    }

}
