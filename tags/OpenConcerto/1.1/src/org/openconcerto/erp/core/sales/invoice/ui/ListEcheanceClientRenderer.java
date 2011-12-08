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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ListEcheanceClientRenderer extends DefaultTableCellRenderer {

    private final static Color couleurEcheance = new Color(255, 128, 64);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRENCH);

    // Beige
    private final static Color couleur1 = new Color(253, 243, 204);

    // Vert
    private final static Color couleur2 = new Color(225, 254, 207);

    // Rouge
    private final static Color couleur3 = new Color(255, 232, 245);

    // Rouge
    private final static Color couleurRegCompta = new Color(255, 202, 255);

    public ListEcheanceClientRenderer() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            setForeground(Color.BLACK);
            if (value instanceof Date) {
                if (!((Date) value).after(new Date())) {
                    setForeground(couleurEcheance);
                }
            }
        }

        if (value instanceof Date) {
            this.setText(dateFormat.format((Date) value));
        }

        // System.err.println(value + " Value.class :: --> " + value.getClass());
        if (value != null && (table.getColumnClass(column) == Long.class || table.getColumnClass(column) == BigInteger.class)
                && (value.getClass() == Long.class || value.getClass() == BigInteger.class)) {

            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }

            final ListSQLLine line = ITableModel.getLine(table.getModel(), row);

            final SQLRowValues row2 = line.getRow();

            if (row2.getBoolean("REG_COMPTA")) {
                if (!isSelected) {
                    setBackground(couleurRegCompta);
                }
            } else {
                final int nbRelance = row2.getInt("NOMBRE_RELANCE");
                if (!isSelected) {
                    switch (nbRelance) {
                    case 0:
                        setBackground(Color.WHITE);
                        break;
                    case 1:
                        setBackground(couleur1);
                        break;
                    case 2:
                        setBackground(couleur2);
                        break;
                    default:
                        setBackground(couleur3);
                        break;
                    }
                }

            }
        return this;
    }
}
