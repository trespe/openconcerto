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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.CollectionUtils;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class QteMultipleRowValuesRenderer extends DefaultTableCellRenderer {

    // Red
    private static final Color red = new Color(255, 31, 52);
    private static final Color redGrey = new Color(224, 115, 137);
    private static final Color redLightGrey = new Color(240, 65, 85);

    /**
     * 
     * @param l liste des modes de vente article qui colore la cellule cf ReferenceArticleSQLElement
     */
    public QteMultipleRowValuesRenderer() {
        AlternateTableCellRenderer.setBGColorMap(this, CollectionUtils.createMap(red, redLightGrey, redGrey, redLightGrey));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setColors(comp, table, isSelected);
        ((JLabel) comp).setHorizontalAlignment(SwingConstants.RIGHT);
        if (table instanceof RowValuesTable) {

            RowValuesTableModel model = ((RowValuesTable) table).getRowValuesTableModel();
            SQLRowValues rowVals = model.getRowValuesAt(row);

            Number qteM = (Number) rowVals.getObject("QTE_ACHAT");
            Number qte = (Number) rowVals.getObject("QTE");
            final int qteAchat = qteM.intValue();
            if (qteAchat != 0 && (qte.intValue() % qteAchat != 0)) {
                if (!isSelected) {
                    comp.setBackground(red);
                } else {
                    comp.setBackground(redGrey);
                }
                return comp;
            }

        }

        return comp;
    }
}
