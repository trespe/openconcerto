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
 
 package org.openconcerto.erp.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ModuleTableModel extends AbstractTableModel {

    protected List<ModuleFactory> list;
    private Boolean[] selection;

    public ModuleTableModel(Collection<ModuleFactory> l) {
        this.list = new ArrayList<ModuleFactory>(l);
        this.selection = new Boolean[l.size()];
        for (int i = 0; i < this.selection.length; i++) {
            this.selection[i] = Boolean.FALSE;
        }
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public int getRowCount() {
        return this.list.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == 0);
    }

    @Override
    public String getColumnName(int column) {
        if (column == 1) {
            return "Nom";
        } else if (column == 2) {
            return "Version";
        } else if (column == 3) {
            return "Etat";
        }
        return "";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 1) {
            return this.list.get(rowIndex).getName();
        } else if (columnIndex == 2) {
            return this.list.get(rowIndex).getVersion();
        } else if (columnIndex == 3) {
            return ModuleManager.getInstance().isModuleRunning(this.list.get(rowIndex).getID()) ? "Actif" : "Inactif";
        } else if (columnIndex == 0) {
            return this.selection[rowIndex];
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            this.selection[rowIndex] = (Boolean) value;
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
}
