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
import org.openconcerto.sql.model.Where;

import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class PointageModel extends AbstractTableModel {

    private String[] titresCol;
    private String[] titresRow;

    private long debitPointe, creditPointe, debitNonPointe, creditNonPointe, creditSelection, debitSelection;

    private static final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private static final SQLTable tableEcr = base.getTable("ECRITURE");
    int idCpt;

    public PointageModel(int idCpt) {

        this.creditNonPointe = 0;
        this.creditPointe = 0;
        this.creditSelection = 0;

        this.debitNonPointe = 0;
        this.debitPointe = 0;
        this.debitSelection = 0;

        this.idCpt = idCpt;
        this.titresCol = new String[6];
        this.titresCol[0] = "Totaux";
        this.titresCol[1] = "Pointé";
        this.titresCol[2] = "Non Pointé";
        this.titresCol[3] = "Total";

        this.titresCol[4] = "Sélection";
        this.titresCol[5] = "Pointéé + sélection";

        this.titresRow = new String[3];
        this.titresRow[0] = "Débit";
        this.titresRow[1] = "Crédit";
        this.titresRow[2] = "Solde";

        updateTotauxCompte();
    }

    public void setIdCompte(int id) {
        this.idCpt = id;
        updateTotauxCompte();
        updateSelection(null);
    }

    public void updateSelection(int[] rowIndex) {
        System.err.println("Update Selection");
        this.creditSelection = 0;
        this.debitSelection = 0;

        if (rowIndex != null) {
            for (int i = 0; i < rowIndex.length; i++) {

                SQLRow row = tableEcr.getRow(rowIndex[i]);

                if (row != null) {

                    if (row.getString("POINTEE").trim().length() == 0) {
                        this.debitSelection += ((Long) row.getObject("DEBIT")).longValue();
                        this.creditSelection += ((Long) row.getObject("CREDIT")).longValue();
                    } /*
                       * else {
                       * 
                       * this.debitSelection -= row.getFloat("DEBIT"); this.creditSelection -=
                       * row.getFloat("CREDIT"); }
                       */
                }
            }
        }
        this.fireTableDataChanged();
    }

    public void updateTotauxCompte() {

        new SwingWorker<String, Object>() {

            @Override
            protected String doInBackground() throws Exception {

                SQLSelect sel = new SQLSelect(base);
                sel.addSelect(tableEcr.getField("CREDIT"), "SUM");
                sel.addSelect(tableEcr.getField("DEBIT"), "SUM");

                Where w = new Where(tableEcr.getField("ID_COMPTE_PCE"), "=", PointageModel.this.idCpt);
                sel.setWhere(w.and(new Where(tableEcr.getField("POINTEE"), "!=", "")));

                String reqPointee = sel.toString();

                Object obPointee = base.getDataSource().execute(reqPointee, new ArrayListHandler());

                List myListPointee = (List) obPointee;

                PointageModel.this.creditPointe = 0;
                PointageModel.this.debitPointe = 0;
                if (myListPointee.size() != 0) {

                    for (int i = 0; i < myListPointee.size(); i++) {
                        Object[] objTmp = (Object[]) myListPointee.get(i);

                        if (objTmp[0] != null) {
                            PointageModel.this.creditPointe += ((Number) objTmp[0]).longValue();
                        }
                        if (objTmp[1] != null) {
                            PointageModel.this.debitPointe += ((Number) objTmp[1]).longValue();
                        }
                    }
                }

                sel.setWhere(w.and(new Where(tableEcr.getField("POINTEE"), "=", "")));
                String reqNotPointee = sel.toString();

                Object obNotPointee = base.getDataSource().execute(reqNotPointee, new ArrayListHandler());

                List myListNotPointee = (List) obNotPointee;

                PointageModel.this.creditNonPointe = 0;
                PointageModel.this.debitNonPointe = 0;
                if (myListNotPointee.size() != 0) {

                    for (int i = 0; i < myListNotPointee.size(); i++) {
                        Object[] objTmp = (Object[]) myListNotPointee.get(i);

                        if (objTmp[0] != null) {
                            PointageModel.this.creditNonPointe += ((Number) objTmp[0]).longValue();
                        }

                        if (objTmp[1] != null) {
                            PointageModel.this.debitNonPointe += ((Number) objTmp[1]).longValue();
                        }
                    }
                }

                return null;
            }

            @Override
            protected void done() {

                PointageModel.this.fireTableDataChanged();
            }
        }.execute();

    }

    public String getColumnName(int column) {

        return this.titresCol[column];
    }

    public int getColumnCount() {

        return this.titresCol.length;
    }

    public int getRowCount() {

        return this.titresRow.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else {
            return Long.class;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            return this.titresRow[rowIndex];
        }

        if (columnIndex == 1) {
            if (rowIndex == 0) {
                return new Long(this.debitPointe);
            }
            if (rowIndex == 1) {
                return new Long(this.creditPointe);
            }
            if (rowIndex == 2) {
                return new Long(this.debitPointe - this.creditPointe);
            }
        }

        if (columnIndex == 2) {
            if (rowIndex == 0) {
                return new Long(this.debitNonPointe);
            }
            if (rowIndex == 1) {
                return new Long(this.creditNonPointe);
            }
            if (rowIndex == 2) {
                return new Long(this.debitNonPointe - this.creditNonPointe);
            }
        }

        if (columnIndex == 3) {
            if (rowIndex == 0) {
                return new Long(this.debitNonPointe + this.debitPointe);
            }
            if (rowIndex == 1) {
                return new Long(this.creditNonPointe + this.creditPointe);
            }
            if (rowIndex == 2) {
                return new Long((this.debitNonPointe - this.creditNonPointe) + (this.debitPointe - this.creditPointe));
            }
        }

        if (columnIndex == 4) {
            if (rowIndex == 0) {
                return new Long(this.debitSelection);
            }
            if (rowIndex == 1) {
                return new Long(this.creditSelection);
            }
            if (rowIndex == 2) {
                return new Long(this.debitSelection - this.creditSelection);
            }
        }

        if (columnIndex == 5) {
            if (rowIndex == 0) {
                return new Long(this.debitSelection + this.debitPointe);
            }
            if (rowIndex == 1) {
                return new Long(this.creditSelection + this.creditPointe);
            }
            if (rowIndex == 2) {
                return new Long(this.debitSelection - this.creditSelection + this.debitPointe - this.creditPointe);
            }
        }

        return null;
    }
}
