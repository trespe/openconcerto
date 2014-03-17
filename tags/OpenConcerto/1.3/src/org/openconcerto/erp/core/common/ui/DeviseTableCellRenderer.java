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

import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class DeviseTableCellRenderer extends DefaultTableCellRenderer {
    private final DecimalFormat decimalFormat = new DecimalFormat("##,##0.00#");
    private final DecimalFormat decimalFormat2 = new DecimalFormat("##,##0.00#######");
    private BigDecimal oneCents = new BigDecimal(0.01f);

    public DeviseTableCellRenderer() {
        final DecimalFormatSymbols symbol = DecimalFormatSymbols.getInstance();
        symbol.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(symbol);
        decimalFormat2.setDecimalFormatSymbols(symbol);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setColors(this, table, isSelected);
        this.setHorizontalAlignment(SwingConstants.RIGHT);
        if (table.getColumnClass(column) != BigDecimal.class) {
            throw new IllegalStateException("Value is not a BigDecimal :" + table.getColumnClass(column));
        }
        if (value != null) {
            if (((BigDecimal) value).compareTo(oneCents) < 0)
                this.setText(decimalFormat2.format(value));
            else
                this.setText(decimalFormat.format(value));
        } else {
            this.setText("");
        }

        return this;
    }

}
