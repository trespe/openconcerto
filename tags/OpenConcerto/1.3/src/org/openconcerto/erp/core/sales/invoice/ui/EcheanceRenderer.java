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

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;

public class EcheanceRenderer extends DeviseNiceTableCellRenderer {

    private static EcheanceRenderer instance = null;

    public synchronized static EcheanceRenderer getInstance() {
        if (instance == null) {
            instance = new EcheanceRenderer();
        }
        return instance;
    }

    private EcheanceRenderer() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final SQLRowValues rowAt = ITableModel.getLine(table.getModel(), row).getRow();

        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            final SQLRowAccessor foreignRow = rowAt.getForeign("ID_MODE_REGLEMENT");
            if (foreignRow != null) {
                int ajours = foreignRow.getInt("AJOURS");
                int njour = foreignRow.getInt("LENJOUR");

                if (ajours == 0 && njour == 0) {
                    if (foreignRow.getBoolean("COMPTANT") != null && !foreignRow.getBoolean("COMPTANT")) {
                        label.setText("Date de facture");
                    } else {
                        label.setText("Comptant");
                    }
                } else {
                    String s = "";
                    if (ajours != 0) {
                        s = ajours + ((ajours > 1) ? " jours" : " jour");
                    }
                    if (njour > 0 && njour < 31) {
                        s += " le " + njour;
                    } else {
                        if (njour == 0) {
                            s += " date de facture";
                        } else {
                            s += " fin de mois";
                        }
                    }
                    label.setText(s);
                }
            }
        }

        return comp;
    }
}
