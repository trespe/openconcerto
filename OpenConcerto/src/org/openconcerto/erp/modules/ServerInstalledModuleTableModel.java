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

import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.IFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class ServerInstalledModuleTableModel extends ModuleTableModel {

    static private final int STATE_INDEX = 3;
    static private final int REQUIRED_INDEX = 4;
    static private final int INFO_INDEX = 5;
    static private final int LAST_INDEX = INFO_INDEX;
    private final Preferences prefs;
    private Set<String> requiredIDs;
    private final PreferenceChangeListener pcl;

    public ServerInstalledModuleTableModel(IFactory<? extends Collection<ModuleReference>> rowSource) {
        super(rowSource);
        this.requiredIDs = null;
        this.prefs = ModuleManager.getInstance().getRequiredIDsPrefs();
        this.pcl = new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                final Set<String> ids = ServerInstalledModuleTableModel.this.requiredIDs;
                if (evt.getNewValue() == null)
                    ids.remove(evt.getKey());
                else
                    ids.add(evt.getKey());
                fireTableChanged(new TableModelEvent(ServerInstalledModuleTableModel.this, 0, getRowCount() - 1, REQUIRED_INDEX));

            }
        };
    }

    protected final Preferences getPrefs() {
        return this.prefs;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        final boolean hadListeners = hasListeners();
        super.addTableModelListener(l);
        if (!hadListeners && hasListeners()) {
            this.getPrefs().addPreferenceChangeListener(this.pcl);
        }
    }

    private final boolean hasListeners() {
        return this.listenerList.getListenerCount() > 0;
    }

    @Override
    public synchronized void removeTableModelListener(TableModelListener l) {
        final boolean hadListeners = hasListeners();
        super.removeTableModelListener(l);
        if (hadListeners && !hasListeners()) {
            this.getPrefs().removePreferenceChangeListener(this.pcl);
        }
    }

    @Override
    public int getColumnCount() {
        return LAST_INDEX + 1;
    }

    @Override
    public String getColumnName(int column) {
        if (column == STATE_INDEX) {
            return "Etat";
        } else if (column == REQUIRED_INDEX) {
            return "Installation locale";
        } else if (column == INFO_INDEX) {
            return "Information";
        } else {
            return super.getColumnName(column);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final ModuleReference f = this.getModuleReference(rowIndex);
        if (columnIndex == STATE_INDEX) {
            return ModuleManager.getInstance().isModuleInstalledLocally(f) ? "Installé sur le poste" : "Non installé sur le poste";
        } else if (columnIndex == REQUIRED_INDEX) {
            return ModuleManager.getInstance().isModuleRequiredLocally(f) ? "Obligatoire" : "Facultative";
        } else if (columnIndex == INFO_INDEX) {
            return ModuleManager.getInstance().getInfo(f);
        } else {
            return super.getValueAt(rowIndex, columnIndex);
        }
    }

    public ModuleReference getModuleReferenceAt(int rowIndex) {
        final ModuleReference f = this.getModuleReference(rowIndex);
        return f;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == STATE_INDEX || columnIndex == REQUIRED_INDEX) {
            return String.class;
        } else {
            return super.getColumnClass(columnIndex);
        }
    }
}
