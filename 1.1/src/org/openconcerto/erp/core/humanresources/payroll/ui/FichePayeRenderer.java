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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.erp.model.FichePayeModel;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;


public class FichePayeRenderer extends DefaultTableCellRenderer {

    private final static Color couleurBrut = new Color(225, 254, 207);

    private final static Color couleurCot = new Color(253, 243, 204);

    private final static Color couleurNet = new Color(206, 247, 255);

    // private final static Color couleurComm = new Color(255, 232, 245);

    private final static Color couleurComm = new Color(245, 245, 245);

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {

            if (!((FichePayeModel) table.getModel()).containValueAt(row, column)) {

                this.setBackground(SwingUtilities.getRoot(table).getBackground());
            } else {

                if ((value != null) && (value.getClass() == String.class)) {
                    String source = ((FichePayeModel) table.getModel()).getSourceAt(row);

                    if (source.equalsIgnoreCase("RUBRIQUE_COMM")) {
                        this.setBackground(couleurComm);
                    }
                    if (source.equalsIgnoreCase("RUBRIQUE_BRUT")) {
                        this.setBackground(couleurBrut);
                    }
                    if (source.equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                        this.setBackground(couleurCot);
                    }
                    if (source.equalsIgnoreCase("RUBRIQUE_NET")) {
                        this.setBackground(couleurNet);
                    }
                } else {
                    this.setBackground(Color.WHITE);
                }
            }
        }

        return this;
    }
}
