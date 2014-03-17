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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.Component;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.openconcerto.ui.table.TableCellRendererUtils;

public class EtatDevisRenderer extends DefaultTableCellRenderer {

    public final static Color couleurBeige = new Color(253, 243, 204);

    public final static Color couleurVert = new Color(225, 254, 207);

    public final static Color couleurRed = new Color(255, 232, 245);

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableCellRendererUtils.setColors(this, table, isSelected);
        if (table.getColumnClass(column) == Long.class || table.getColumnClass(column) == BigInteger.class) {
            if (value.getClass() == Long.class || value.getClass() == BigInteger.class) {
                this.setText(GestionDevise.currencyToString(((Long) value).longValue()));
            }
        } else {
            if (value != null && (table.getColumnClass(column) == Date.class || table.getColumnClass(column) == java.sql.Date.class)) {
                this.setText(dateFormat.format((Date) value));
            }
        }
        if (!isSelected) {
            final SQLRowValues rowElt = ITableModel.getLine(table.getModel(), row).getRow();
            switch (getEtat(rowElt)) {
            case EtatDevisSQLElement.EN_ATTENTE:
                this.setBackground(couleurBeige);
                break;
            case EtatDevisSQLElement.ACCEPTE:
                this.setBackground(Color.WHITE);
                break;
            case EtatDevisSQLElement.REFUSE:
                this.setBackground(couleurRed);
                break;
            default:
                break;
            }
        }

        return this;
    }

    protected int getEtat(SQLRowValues rowElt) {
        return rowElt.getInt("ID_ETAT_DEVIS");
    }

}
