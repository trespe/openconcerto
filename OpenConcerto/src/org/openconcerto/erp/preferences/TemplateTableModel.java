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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.erp.generationDoc.TemplateManager;
import org.openconcerto.erp.generationDoc.TemplateProvider;

import javax.swing.table.AbstractTableModel;

public class TemplateTableModel extends AbstractTableModel {
    public TemplateTableModel() {
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Modèle";
        }
        return "Synchronisé";
    }

    @Override
    public int getRowCount() {
        return TemplateManager.getInstance().getKnownTemplateIds().size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final String templateId = getTemplateId(rowIndex);
        if (columnIndex == 0) {
            return templateId;
        } else {
            final TemplateProvider p = TemplateManager.getInstance().getProvider(templateId);
            if (p.isSynced(templateId, null, null)) {
                return "oui";
            } else {
                return "non";
            }
        }
    }

    public TemplateProvider getTemplateProvider(int rowIndex) {
        final String templateId = getTemplateId(rowIndex);
        return TemplateManager.getInstance().getProvider(templateId);
    }

    public boolean isSynced(int rowIndex) {
        return getTemplateProvider(rowIndex).isSynced(getTemplateId(rowIndex), null, null);
    }

    public void sync(int rowIndex) {
        getTemplateProvider(rowIndex).sync(getTemplateId(rowIndex), null, null);
        fireTableDataChanged();
    }

    public void unsync(int rowIndex) {
        getTemplateProvider(rowIndex).unSync(getTemplateId(rowIndex), null, null);
        fireTableDataChanged();
    }

    public void restore(int rowIndex) {
        getTemplateProvider(rowIndex).restore(getTemplateId(rowIndex), null, null);
        fireTableDataChanged();
    }

    public String getTemplateId(int rowIndex) {
        return TemplateManager.getInstance().getKnownTemplateIds().get(rowIndex);
    }

}
