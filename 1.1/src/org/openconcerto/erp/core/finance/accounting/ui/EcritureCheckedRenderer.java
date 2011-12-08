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

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.table.TableCellRendererDecorator;
import org.openconcerto.ui.table.TableCellRendererUtils;

import java.awt.Color;
import java.awt.Component;
import java.util.Calendar;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class EcritureCheckedRenderer extends TableCellRendererDecorator {
    private final static Color couleurEcritureNonValide = new Color(253, 243, 204);
    // private final static Color couleurEcritureToDay = new Color(255, 252, 236);
    private final static Color couleurEcritureToDay = new Color(225, 254, 207);

    private final static Color couleurEcritureLettree = new Color(255, 232, 245);

    // so that all subclasses replace one another, e.g. LettrageRenderer replaces ListEcritureRenderer
    public final static class EcritureUtils<R extends EcritureCheckedRenderer> extends TableCellRendererDecoratorUtils<R> {

        protected EcritureUtils(Class<R> clazz) {
            super(clazz);
        }

        @Override
        protected boolean replaces(TableCellRenderer r) {
            return EcritureCheckedRenderer.class.isAssignableFrom(r.getClass());
        }
    }

    private final String fieldName;

    public EcritureCheckedRenderer(TableCellRenderer r, final String fieldName) {
        super(r);
        this.fieldName = fieldName;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        final Component res = getRenderer(table, column).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setBackgroundColor(res, table, isSelected);

        if (!isSelected) {
            final SQLRowValues ecritureRow = ITableModel.getLine(table.getModel(), row).getRow();
            if (!ecritureRow.getBoolean("VALIDE")) {
                // this.setForeground(couleurEcritureNonValide);
                final Calendar dateEcr = ecritureRow.getDate("DATE");
                final Calendar dateToDay = Calendar.getInstance();

                if (dateEcr.get(Calendar.DAY_OF_YEAR) == dateToDay.get(Calendar.DAY_OF_YEAR) && dateEcr.get(Calendar.YEAR) == dateToDay.get(Calendar.YEAR)) {
                    // System.out.println("ToDay :: " + dateToDay + " Ecr ::: " + dateEcr);

                    res.setBackground(couleurEcritureToDay);
                } else {
                    res.setBackground(couleurEcritureNonValide);
                }
            }

            if (this.fieldName != null) {
                final String string = ecritureRow.getString(this.fieldName);
                if (string != null && string.trim().length() > 0) {
                    res.setBackground(couleurEcritureLettree);
                }
            }
        }

        return res;
    }

    public static Color getCouleurEcritureNonValide() {
        return couleurEcritureNonValide;
    }

    public static Color getCouleurEcritureToDay() {
        return couleurEcritureToDay;
    }

    public static Color getCouleurEcriturePointee() {
        return couleurEcritureLettree;
    }
}
