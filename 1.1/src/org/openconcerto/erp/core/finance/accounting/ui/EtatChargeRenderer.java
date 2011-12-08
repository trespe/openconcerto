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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


public class EtatChargeRenderer extends DefaultTableCellRenderer {

    private final static Color couleurCot = new Color(253, 243, 204);

    private static final NumberFormat numberFormat = new DecimalFormat("0.00");

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            if ((value != null) && (value.getClass() == String.class)) {

                this.setBackground(couleurCot);

            } else {
                this.setBackground(Color.WHITE);
            }
        }

        if (value instanceof Float) {
            // System.out.println("setText Double format" + value.toString());
            float f = ((Float) value).floatValue();
            this.setText(numberFormat.format(f));
            this.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        if (value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }
        return this;
    }
}
