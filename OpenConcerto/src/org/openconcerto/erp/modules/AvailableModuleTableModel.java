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

    static private IFactory<Set<ModuleReference>> getNonInstalled() {
        return new IFactory<Set<ModuleReference>>() {
            @Override
            public Set<ModuleReference> createChecked() {
                final ModuleManager mngr = ModuleManager.getInstance();

                return CollectionUtils.select(mngr.getAllKnownModuleReference(), new IPredicate<ModuleReference>() {
                    @Override
                    public boolean evaluateChecked(ModuleReference input) {
                        return !mngr.isModuleInstalledLocally(input) && !mngr.isModuleInstalledOnServer(input);
                    }
                }, new HashSet<ModuleReference>());
            }
        };
    }

    public AvailableModuleTableModel() {
        super(getNonInstalled());
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 3) {
            return "Description";
        } else if (column == 4) {
            return "Information";
        }
        return super.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final ModuleReference moduleReference = this.getModuleReference(rowIndex);
        if (columnIndex == 3) {
            final ModuleFactory f = ModuleManager.getInstance().getFactories().get(moduleReference);
            if (f != null) {
                return f.getDescription();
            } else {
                return "";
            }
        } else if (columnIndex == 4) {
            return ModuleManager.getInstance().getInfo(moduleReference);
        }
        return super.getValueAt(rowIndex, columnIndex);
    }
}
