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
 
 package org.openconcerto.erp.model;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class ImportModel extends AbstractTableModel {

    private Vector champs;
    private Map longueur;
    private String[] titres;

    /**
     * TableModel pour l'importation de données dans la base
     * 
     * @param l liste des SQLField des tables à charger
     */
    public ImportModel(List l) {

        this.champs = new Vector();
        this.longueur = new HashMap();

        this.titres = new String[2];
        this.titres[0] = "Champs";
        this.titres[1] = "Longueur";
        this.champs.addAll(l);
    }

    public int getRowCount() {

        return this.champs.size();
    }

    public int getColumnCount() {

        return this.titres.length;
    }

    public Class getColumnClass(int c) {
        if (c == 0) {
            return String.class;
        }
        return Integer.class;
    }

    public String getColumnName(int col) {
        return this.titres[col];
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex != 0);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        this.longueur.put(this.champs.get(rowIndex), aValue);

    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {

            return Configuration.getInstance().getTranslator().getTitleFor((SQLField) this.champs.get(rowIndex));
        }

        if (columnIndex == 1) {

            return this.longueur.get(this.champs.get(rowIndex));
        }
        return null;
    }

    public int getLongueur(SQLField f) {

        Integer val = ((Integer) this.longueur.get(f));

        if (val == null) {
            return 0;
        } else {
            return val.intValue();
        }
    }

    public int upField(int row) {

        if (row > 0) {
            SQLField tmp = (SQLField) this.champs.get(row);
            this.champs.set(row, this.champs.get(row - 1));
            this.champs.set(row - 1, tmp);
            this.fireTableDataChanged();
            return row - 1;
        } else {
            return row;
        }
    }

    public int downField(int row) {

        if (row < this.getRowCount() - 1) {
            SQLField tmp = (SQLField) this.champs.get(row);
            this.champs.set(row, this.champs.get(row + 1));
            this.champs.set(row + 1, tmp);
            this.fireTableDataChanged();
            return row + 1;
        } else {
            return row;
        }
    }

    public int getLongueur(int row) {

        Integer val = ((Integer) getValueAt(row, 1));

        if (val == null) {
            return 0;
        } else {
            return val.intValue();
        }
    }

    public Class getClassForIndex(int row) {
        SQLField f = (SQLField) this.champs.get(row);
        return f.getType().getJavaType();
    }

    public SQLField getFieldForIndex(int row) {
        return (SQLField) this.champs.get(row);
    }

    public String getFieldNameForIndex(int row) {
        SQLField f = (SQLField) this.champs.get(row);
        return f.getName();
    }

    public String getTableNameForIndex(int row) {
        SQLField f = (SQLField) this.champs.get(row);
        return f.getTable().getName();
    }

    public int[] getDelimiteur() {
        int[] delim = new int[getRowCount()];
        for (int i = 0; i < getRowCount(); i++) {
            delim[i] = getLongueur(i);
        }
        return delim;
    }
}
