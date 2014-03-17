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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.utils.CollectionUtils;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JTable;

public class ArticleRowValuesRenderer extends DeviseNiceTableCellRenderer {

    private List<Integer> listColorModeVente;

    // Blue
    private final static Color light = new Color(232, 238, 250);
    private final static Color lightGrey = new Color(211, 220, 222);
    private final static Color darker = new Color(170, 180, 183);

    // Black
    private final static Color lightBlack = new Color(192, 192, 192);
    private final static Color lightBlackGrey = new Color(155, 155, 155);
    private final static Color lightBlackDarker = new Color(128, 128, 128);

    /**
     * 
     * @param l liste des modes de vente article qui colore la cellule cf ReferenceArticleSQLElement
     */
    public ArticleRowValuesRenderer(List<Integer> l) {
        this.listColorModeVente = l;
        AlternateTableCellRenderer.setBGColorMap(this, CollectionUtils.createMap(light, lightGrey, lightBlack, lightBlackGrey));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (table instanceof RowValuesTable) {

            RowValuesTableModel model = ((RowValuesTable) table).getRowValuesTableModel();
            SQLRowValues rowVals = model.getRowValuesAt(row);

            Number mode = (Number) rowVals.getObject("ID_MODE_VENTE_ARTICLE");
            if (mode != null && this.listColorModeVente != null && this.listColorModeVente.contains(Integer.valueOf(mode.intValue()))) {
                if (!isSelected) {
                    comp.setBackground(light);
                } 
//                else {
//                    comp.setBackground(darker);
//                }
                return comp;
            }

            if (!model.isCellEditable(row, column)) {
                if (!isSelected) {
                    comp.setBackground(lightBlack);
                } 
                // else {
                // comp.setBackground(lightBlackDarker);
                // }
                return comp;
            }
        }

        return comp;
    }
}
