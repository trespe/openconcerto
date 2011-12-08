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

import org.openconcerto.erp.model.GestionChequesModel;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


public class GestionChequesRenderer extends DefaultTableCellRenderer {

    private final static Color couleurChequeValide = new Color(255, 128, 64);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRENCH);
    private static final NumberFormat numberFormat = new DecimalFormat("0.00");

    private GestionChequesModel model;
    private TableSorter s;

    public GestionChequesRenderer(TableSorter s) {
        this.s = s;
        this.model = (GestionChequesModel) s.getTableModel();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            if (value instanceof Double) {
                // System.out.println("setText Double format" + value.toString());
                float f = ((Double) value).floatValue();
                this.setText(numberFormat.format(f));
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            }

            if (value instanceof Date) {
                // System.out.println("setText Date format" + value.toString());
                if (!this.model.getDateMinimum(this.s.modelIndex(row)).after(new Date())) {
                    setForeground(couleurChequeValide);
                } else {
                    setForeground(Color.BLACK);
                }
                this.setText(dateFormat.format((Date) value));
            }

        } else {
            // System.out.println(value.getClass());

            if (value instanceof Double) {
                // System.out.println("setText Double format" + value.toString());
                float f = ((Double) value).floatValue();
                this.setText(numberFormat.format(f));
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            }

            if (value instanceof Date) {
                // System.out.println("setText Date format" + value.toString());

                this.setText(dateFormat.format((Date) value));
            }

        }

        if (value != null && value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }
        return this;
    }
}
