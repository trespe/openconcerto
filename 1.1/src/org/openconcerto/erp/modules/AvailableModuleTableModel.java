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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IFactory;
import org.openconcerto.utils.cc.IPredicate;

import java.util.HashSet;
import java.util.Set;

public class AvailableModuleTableModel extends ModuleTableModel {

    static private IFactory<Set<ModuleFactory>> getNonInstalled() {
        return new IFactory<Set<ModuleFactory>>() {
            @Override
            public Set<ModuleFactory> createChecked() {
                final ModuleManager mngr = ModuleManager.getInstance();
                return CollectionUtils.select(mngr.getFactories().values(), new IPredicate<ModuleFactory>() {
                    @Override
                    public boolean evaluateChecked(ModuleFactory input) {
                        return !mngr.isModuleInstalledLocally(input.getID());
                    }
                }, new HashSet<ModuleFactory>());
            }
        };
    }

    public AvailableModuleTableModel() {
        super(getNonInstalled());
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
        if (column == 3) {
            return "Description";
        }
        return super.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 3) {
            return this.list.get(rowIndex).getDescription();
        }
        return super.getValueAt(rowIndex, columnIndex);
    }

}
