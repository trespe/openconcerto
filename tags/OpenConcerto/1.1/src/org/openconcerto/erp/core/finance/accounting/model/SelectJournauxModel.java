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
 
 package org.openconcerto.erp.core.finance.accounting.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;


public class SelectJournauxModel extends AbstractTableModel {

    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tableJournal = base.getTable("JOURNAL");
    private Vector journaux;
    private String[] titres;

    public SelectJournauxModel() {

        this.journaux = new Vector();
        SQLSelect selJrnl = new SQLSelect(base);
        selJrnl.addSelect("JOURNAL.ID");
        String req = selJrnl.asString();
        List l = (List) base.getDataSource().execute(req, new ArrayListHandler());

        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            this.journaux.add(tableJournal.getRow(Integer.parseInt(tmp[0].toString())));
        }

        this.titres = new String[2];
        this.titres[0] = "Code";
        this.titres[1] = "Libellé";
    }

    public int getRowCount() {

        return this.journaux.size();
    }

    public int getColumnCount() {

        return this.titres.length;
    }

    public String getColumnName(int column) {

        return this.titres[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return ((SQLRow) this.journaux.get(rowIndex)).getObject("CODE");
        } else {
            if (columnIndex == 1) {
                return ((SQLRow) this.journaux.get(rowIndex)).getObject("NOM");
            } else {
                return null;
            }
        }
    }

    public int[] getSelectedIds(int[] rows) {

        int[] idS = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            idS[i] = getIdForRow(rows[i]);
            System.err.println("Row " + rows[i] + " has ID " + idS[i]);
        }
        return idS;
    }

    public int getIdForRow(int row) {
        return ((SQLRow) this.journaux.get(row)).getID();
    }

}
