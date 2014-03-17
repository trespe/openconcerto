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

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

public class QteUnitRowValuesRenderer extends DeviseNiceTableCellRenderer {

    // Black
    private final static Color lightBlack = new Color(215, 215, 215);
    DecimalFormat decimalFormat = new DecimalFormat("##,##0.######");

    public QteUnitRowValuesRenderer() {
        // AlternateTableCellRenderer.setBGColorMap(this, CollectionUtils.createMap(light,
        // lightGrey, lightBlack, lightBlackGrey));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JLabel) comp).setHorizontalAlignment(SwingConstants.RIGHT);
        if (!table.getModel().isCellEditable(row, column) && !isSelected) {

            comp.setBackground(lightBlack);
        }
        if (value == null) {
            ((JLabel) comp).setText("");
        } else {
            ((JLabel) comp).setText(this.decimalFormat.format(((Number) value).doubleValue()));
        }
        return comp;
    }
}
