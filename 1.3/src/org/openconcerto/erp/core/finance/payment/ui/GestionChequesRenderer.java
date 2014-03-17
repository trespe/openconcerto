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
 
 package org.openconcerto.erp.core.finance.payment.ui;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class GestionChequesRenderer extends DefaultTableCellRenderer {

    private final static Color couleurChequeValide = new Color(255, 128, 64);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final Component res = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final Color fgColor;
        if (!isSelected && System.currentTimeMillis() > ((Date) value).getTime()) {
            fgColor = couleurChequeValide;
        } else {
            fgColor = table.getForeground();
        }
        res.setForeground(fgColor);
        return res;
    }

    @Override
    protected void setValue(Object value) {
        super.setValue(dateFormat.format((Date) value));
    }
}
