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

    private final IFactory<? extends Collection<ModuleReference>> rowSource;
    private List<ModuleReference> list;
    private final Set<ModuleReference> selection;

    public ModuleTableModel(IFactory<? extends Collection<ModuleReference>> rowSource) {
        this.rowSource = rowSource;
        this.selection = new HashSet<ModuleReference>();
        this.reload();
    }

    public final void reload() {
        this.list = new ArrayList<ModuleReference>(this.rowSource.createChecked());
        // sort alphabetically
        Collections.sort(this.list, new Comparator<ModuleReference>() {
            @Override
            public int compare(ModuleReference o1, ModuleReference o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        this.selection.retainAll(this.list);
        this.fireTableDataChanged();
    }

    public final Set<ModuleReference> getCheckedRows() {
        return Collections.unmodifiableSet(this.selection);
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public final int getRowCount() {
        return this.list.size();
    }

    protected final ModuleReference getModuleReference(int i) {
        return this.list.get(i);
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
        }
        return "";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final ModuleReference f = this.getModuleReference(rowIndex);
        if (columnIndex == 1) {
            try {
                final ModuleFactory moduleFactory = ModuleManager.getInstance().getFactories().get(f.getId());
                if (moduleFactory != null) {
                    return moduleFactory.getName();
                }
                return f.getId();
            } catch (Exception e) {
                return e.getMessage();
            }
        } else if (columnIndex == 2) {
            try {
                return f.getVersion();
            } catch (Exception e) {
                return e.getMessage();
            }
        } else if (columnIndex == CB_INDEX) {
            return this.selection.contains(f);
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == CB_INDEX) {
            if ((Boolean) value)
                this.selection.add(this.getModuleReference(rowIndex));
            else
                this.selection.remove(this.getModuleReference(rowIndex));
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
