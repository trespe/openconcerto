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

import org.openconcerto.erp.core.finance.accounting.model.ConsultCompteModel;
import org.openconcerto.erp.element.objet.Ecriture;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class EcritureGrandLivreRenderer extends DefaultTableCellRenderer {

    private TableSorter model;
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRENCH);
    private final static Color couleurEcritureValide = new Color(253, 243, 204);
    private final static Color couleurEcritureToDay = new Color(225, 254, 207);

    public EcritureGrandLivreRenderer(TableSorter model) {
        this.model = model;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TableCellRendererUtils.setBackgroundColor(this, table, isSelected);

        if (!isSelected) {

            Ecriture ecrTmp = ((ConsultCompteModel) this.model.getTableModel()).getEcritures().get(this.model.viewIndex(row));

            if (!ecrTmp.getValide()) {
                // this.setForeground(couleurEcritureValide);
                Date dateEcr = ecrTmp.getDate();
                Date dateToDay = new Date();

                if ((dateEcr.getDate() == dateToDay.getDate()) && (dateEcr.getMonth() == dateToDay.getMonth()) && (dateEcr.getYear() == dateToDay.getYear())) {
                    // System.out.println("ToDay :: " + dateToDay + " Ecr ::: " + dateEcr);

                    this.setBackground(couleurEcritureToDay);
                } else {
                    this.setBackground(couleurEcritureValide);
                }
            }
        }

        if (value instanceof Date) {
            this.setText(dateFormat.format((Date) value));
        }
        if (value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }

        return this;
    }
}
