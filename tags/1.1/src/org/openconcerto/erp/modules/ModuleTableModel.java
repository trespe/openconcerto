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

import org.openconcerto.utils.cc.IFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

public class ModuleTableModel extends AbstractTableModel {

    static private final int CB_INDEX = 0;

    private final IFactory<? extends Collection<ModuleFactory>> rowSource;
    protected List<ModuleFactory> list;
    private final Set<ModuleFactory> selection;

    public ModuleTableModel(IFactory<? extends Collection<ModuleFactory>> rowSource) {
        this.rowSource = rowSource;
        this.selection = new HashSet<ModuleFactory>();
        this.reload();
    }

    public final void reload() {
        this.list = new ArrayList<ModuleFactory>(this.rowSource.createChecked());
        // sort alphabetically
        Collections.sort(this.list, new Comparator<ModuleFactory>() {
            @Override
            public int compare(ModuleFactory o1, ModuleFactory o2) {
                return o1.getID().compareTo(o2.getID());
            }
        });
        this.selection.retainAll(this.list);
        this.fireTableDataChanged();
    }

    public final Collection<ModuleFactory> getCheckedRows() {
        return Collections.unmodifiableSet(this.selection);
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
        return columnIndex == CB_INDEX;
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
            try {
                return this.list.get(rowIndex).getName();
            } catch (Exception e) {
                return e.getMessage();
            }
        } else if (columnIndex == 2) {
            try {
                return this.list.get(rowIndex).getVersion();
            } catch (Exception e) {
                return e.getMessage();
            }
        } else if (columnIndex == 3) {
            return ModuleManager.getInstance().isModuleRunning(this.list.get(rowIndex).getID()) ? "Actif" : "Inactif";
        } else if (columnIndex == CB_INDEX) {
            return this.selection.contains(this.list.get(rowIndex));
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == CB_INDEX) {
            if ((Boolean) value)
                this.selection.add(this.list.get(rowIndex));
            else
                this.selection.remove(this.list.get(rowIndex));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == CB_INDEX) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }
}
