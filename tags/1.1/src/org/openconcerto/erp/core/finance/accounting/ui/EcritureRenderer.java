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

import org.openconcerto.erp.core.finance.accounting.model.EcrituresModel;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.TableSorter;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class EcritureRenderer extends DefaultTableCellRenderer {

    private EcrituresModel model;

    public EcritureRenderer(EcrituresModel model) {
        this.model = model;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            int viewRow = ((TableSorter) table.getModel()).viewIndex(row);

            // System.out.println(((Ecriture)(this.model.getEcritures().get(viewRow))).toString());

            if (((this.model.getEcritures().get(viewRow))).getValide()) {
                cell.setForeground(Color.RED);
            } else {
                cell.setForeground(Color.BLACK);
            }
        }

        if (value.getClass() == Long.class) {
            this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
        }

        return cell;
    }
}
