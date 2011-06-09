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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.ui.list.selection.BaseListStateModel;
import org.openconcerto.utils.TableSorter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public final class TableListStateModel extends BaseListStateModel {

    private static final String UPDATING = "updating";
    private final TableSorter sorter;

    public TableListStateModel(TableSorter sorter) {
        this.sorter = sorter;
        this.sorter.addPropertyChangeListener("sorting", getUpdateL());
        this.sorter.addPropertyChangeListener("tableModel", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateListener((ITableModel) evt.getOldValue(), (ITableModel) evt.getNewValue());
            }
        });
        // init (the tableModel hasn't changed)
        updateListener(null, this.getTableModel());
    }

    private void updateListener(final ITableModel oldTableModel, final ITableModel newTableModel) {
        if (oldTableModel != null)
            oldTableModel.rmPropertyChangeListener(UPDATING, getUpdateL());

        if (newTableModel != null)
            newTableModel.addPropertyChangeListener(UPDATING, getUpdateL());
    }

    private ITableModel getTableModel() {
        return (ITableModel) this.sorter.getTableModel();
    }

    public int idFromIndex(int rowIndex) {
        try {
            final int modelIndex = this.sorter.modelIndex(rowIndex);
            return this.getTableModel().idFromIndex(modelIndex);
        } catch (IndexOutOfBoundsException e) {
            return INVALID_ID;
        }
    }

    public int indexFromID(int id) {
        final int modelIndex = this.getTableModel().indexFromID(id);
        if (modelIndex == -1)
            return INVALID_INDEX;
        return this.sorter.viewIndex(modelIndex);
    }

}
