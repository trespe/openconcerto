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
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.math.BigInteger;
import java.text.SimpleDateFormat;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

public class DeviseNiceTableCellRenderer extends DefaultTableCellRenderer {

    public final static Color couleurFacture = new Color(225, 254, 207);
    public final static Color couleurFactureMore = new Color(215, 244, 197);
    public final static Color couleurFactureDark = couleurFacture.darker();

    public final static Color couleurBon = new Color(253, 243, 204);
    public final static Color couleurBonMore = new Color(243, 233, 194);
    public final static Color couleurBonDark = couleurBon.darker();
    private final static SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TableCellRendererUtils.setColors(this, table, isSelected);

        this.setHorizontalAlignment(SwingConstants.LEFT);
        if (table.getColumnClass(column) == Long.class || table.getColumnClass(column) == BigInteger.class) {
            if (value != null && (value.getClass() == Long.class || value.getClass() == BigInteger.class)) {
                this.setText(GestionDevise.currencyToString(Long.valueOf(value.toString())));
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            }
        } else {
            if (table.getColumnClass(column) == java.util.Date.class || table.getColumnClass(column) == java.sql.Date.class) {
                if (value != null && (value.getClass() == java.util.Date.class || value.getClass() == java.sql.Date.class)) {
                    java.util.Date date = (java.util.Date) value;
                    this.setText(format.format(date));
                }
            }
        }

        return this;
    }
}
