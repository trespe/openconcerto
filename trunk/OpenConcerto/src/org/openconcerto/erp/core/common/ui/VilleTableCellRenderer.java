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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class VilleTableCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TableCellRendererUtils.setColors(this, table, isSelected);

        this.setHorizontalAlignment(SwingConstants.LEFT);

        RowValuesTable rowValuesTable = ((RowValuesTable) table);
        SQLRowValues rowVals = rowValuesTable.getRowValuesTableModel().getRowValuesAt(row);
        String v = rowVals.getString("VILLE");
        String code = rowVals.getString("CODE_POSTAL");
        Ville ville = Ville.getVilleFromVilleEtCode(v + " (" + code + ")");

        this.setText(ville == null ? "" : ville.toString());

        return this;
    }
}
