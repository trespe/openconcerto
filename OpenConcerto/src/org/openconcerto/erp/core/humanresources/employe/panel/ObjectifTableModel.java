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
 
 /*
 * Créé le 26 mars 2012
 */
package org.openconcerto.erp.core.humanresources.employe.panel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class ObjectifTableModel extends AbstractTableModel {

    List<SQLField> cols = new ArrayList<SQLField>();
    SQLTable tableObjectif = Configuration.getInstance().getRoot().findTable("OBJECTIF_COMMERCIAL");
    List<SQLRowValues> values = new ArrayList<SQLRowValues>();

    public ObjectifTableModel() {
        cols.add(tableObjectif.getField("MOIS"));
        cols.add(tableObjectif.getField("CHIFFRE_AFFAIRE"));
        cols.add(tableObjectif.getField("POURCENT_MARGE"));
        cols.add(tableObjectif.getField("MARGE_HT"));

    }

    @Override
    public int getColumnCount() {
        // TODO Raccord de méthode auto-généré
        return cols.size();
    }

    @Override
    public String getColumnName(int column) {
        // TODO Raccord de méthode auto-généré
        return Configuration.getInstance().getTranslator().getTitleFor(cols.get(column));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // TODO Raccord de méthode auto-généré
        return cols.get(columnIndex).getType().getJavaType();
    }

    @Override
    public int getRowCount() {
        // TODO Raccord de méthode auto-généré
        return values.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // TODO Raccord de méthode auto-généré
        return values.get(rowIndex).getObject(cols.get(columnIndex).getName());
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // TODO Raccord de méthode auto-généré
        super.setValueAt(aValue, rowIndex, columnIndex);

        // FIXME check if aValue is null

        SQLRowValues rowValuesAt = this.getRowValuesAt(rowIndex);
        if (columnIndex == 1) {
            BigDecimal decimal = (BigDecimal) rowValuesAt.getObject(cols.get(2).getName());
            if (decimal != null) {
                long result = Math.round(decimal.doubleValue() * (Long) aValue / 100.0D);
                rowValuesAt.put(cols.get(3).getName(), result);
            }
        }

        if (columnIndex == 2) {
            Long decimal = (Long) rowValuesAt.getObject(cols.get(1).getName());
            if (decimal != null) {
                long result = Math.round(decimal * ((BigDecimal) aValue).doubleValue() / 100.0D);
                rowValuesAt.put(cols.get(3).getName(), result);
            }
        }

        if (columnIndex == 3) {
            Long decimal = (Long) rowValuesAt.getObject(cols.get(1).getName());
            if (decimal != null) {
                double result = ((Long) aValue).doubleValue() / decimal.doubleValue() * 100.0D;
                rowValuesAt.put(cols.get(2).getName(), new BigDecimal(result));
            }
        }

        rowValuesAt.put(cols.get(columnIndex).getName(), aValue);
        try {
            rowValuesAt.update();
        } catch (SQLException exn) {
            // TODO Bloc catch auto-généré
            exn.printStackTrace();
        }
        this.fireTableDataChanged();
    }

    public void loadData(int idCommercial, Integer annee) {

        if (idCommercial <= 1 || annee == null) {
            this.values.clear();
        } else {
            SQLSelect sel = new SQLSelect(tableObjectif.getBase());
            sel.addSelectStar(tableObjectif);
            Where w = new Where(tableObjectif.getField("ID_COMMERCIAL"), "=", idCommercial);
            w = w.and(new Where(tableObjectif.getField("ANNEE"), "=", annee));
            sel.setWhere(w);
            sel.addFieldOrder(tableObjectif.getOrderField());
            List<SQLRow> rows = SQLRowListRSH.execute(sel);
            List<SQLRowValues> rowValuesList = new ArrayList<SQLRowValues>();
            for (SQLRow sqlRow : rows) {
                rowValuesList.add(sqlRow.asRowValues());
            }
            this.values.clear();
            this.values.addAll(rowValuesList);
        }
        this.fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // TODO Raccord de méthode auto-généré
        return (columnIndex == 1 || columnIndex == 2 || columnIndex == 3);
    }

    public SQLRowValues getRowValuesAt(int index) {
        if (index >= 0) {
            return this.values.get(index);
        } else {
            return null;
        }
    }
}
