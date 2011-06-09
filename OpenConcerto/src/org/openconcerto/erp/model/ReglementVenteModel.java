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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLSelect;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;


public class ReglementVenteModel extends AbstractTableModel {

    String[] column = { "Numéro", "Libellé", "Date", "Mode de règlement", "Date Règlement", "Montant" };
    Vector<ReglementVenteObject> vector = new Vector<ReglementVenteObject>();
    ComptaPropsConfiguration conf = ((ComptaPropsConfiguration) Configuration.getInstance());

    public ReglementVenteModel() {

        SQLSelect selectFacture = new SQLSelect(conf.getBase());

    }

    @Override
    public String getColumnName(int column) {
        return this.column[column];
    }

    @Override
    public int getColumnCount() {
        return this.column.length;
    }

    @Override
    public int getRowCount() {
        return vector.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReglementVenteObject regl = this.vector.get(rowIndex);

        if (columnIndex == 0) {
            return regl.getNumero();
        }
        if (columnIndex == 1) {
            return regl.getLib();
        }
        if (columnIndex == 2) {
            return regl.getDFacture();
        }
        if (columnIndex == 3) {
            return regl.getModeRegl();
        }
        if (columnIndex == 4) {
            return regl.getDReglement();
        }
        if (columnIndex == 5) {
            return regl.getMontant();
        }

        return null;
    }
}
