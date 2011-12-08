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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

// MAYBE mettre un renderer avec une couleur par type de rubrique???
public class ProfilPayeModel extends AbstractTableModel {

    private int idProfil;
    private final static SQLTable TABLE_PROFIL = Configuration.getInstance().getBase().getTable("PROFIL_PAYE_ELEMENT");
    // Vecteur qui contient les rubriques du profil
    private final List<SQLRowValues> vectRowElt = new ArrayList<SQLRowValues>();

    // TODO listenner sur profilPayeElement --> if row id_profil = idProfil reload

    public ProfilPayeModel(final int idProfil) {
        doSelectID(idProfil);
    }

    public int getRowCount() {
        return this.vectRowElt.size();
    }

    public String getColumnName(final int column) {
        return "Nom";
    }

    public int getColumnCount() {
        return 1;
    }

    public Class getColumnClass(final int columnIndex) {
        return String.class;
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return this.vectRowElt.get(rowIndex).getString("NOM");
    }

    public void addRowAt(final SQLRow rowRubrique, final int rowIndex) {
        final SQLRowValues rowVals = new SQLRowValues(TABLE_PROFIL);
        rowVals.put("NOM", rowRubrique.getString("NOM"));
        rowVals.put("SOURCE", rowRubrique.getTable().getName());
        rowVals.put("IDSOURCE", rowRubrique.getID());

        if ((rowIndex > 0) && (rowIndex < this.vectRowElt.size())) {
            this.vectRowElt.add(rowIndex, rowVals);
        } else {
            this.vectRowElt.add(rowVals);
        }
        this.fireTableDataChanged();
    }

    public void updateFields(final int idProfil) {
        for (int i = 0; i < this.vectRowElt.size(); i++) {
            final SQLRowValues rowVals = this.vectRowElt.get(i);
            rowVals.put("ID_PROFIL_PAYE", idProfil);
            rowVals.put("POSITION", i);
            try {
                rowVals.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void selectID(final int idProfil) {
        if (this.idProfil != idProfil) {
            doSelectID(idProfil);
            this.fireTableDataChanged();
        }
    }

    private void doSelectID(final int idProfil) {
        this.idProfil = idProfil;
        this.vectRowElt.clear();

        final SQLSelect selAllIDProfilElt = new SQLSelect(Configuration.getInstance().getBase());
        selAllIDProfilElt.addSelect(TABLE_PROFIL.getField("ID"));
        selAllIDProfilElt.addSelect(TABLE_PROFIL.getField("POSITION"));
        selAllIDProfilElt.setWhere(new Where(TABLE_PROFIL.getField("ID_PROFIL_PAYE"), "=", this.idProfil));
        selAllIDProfilElt.addRawOrder("\"PROFIL_PAYE_ELEMENT\".\"POSITION\"");

        final String reqAllIDProfilElt = selAllIDProfilElt.asString();
        final Object[] objIDProfilElt = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllIDProfilElt, new ArrayListHandler())).toArray();

        for (int i = 0; i < objIDProfilElt.length; i++) {
            final SQLRow rowTmp = TABLE_PROFIL.getRow(Integer.parseInt((((Object[]) objIDProfilElt[i])[0].toString())));
            final SQLRowValues rowValsTmp = new SQLRowValues(TABLE_PROFIL);
            rowValsTmp.loadAbsolutelyAll(rowTmp);
            this.vectRowElt.add(rowValsTmp);
        }

    }

    public int upRow(final int rowIndex) {
        // On vérifie qu'il est possible de remonter la ligne
        if ((this.vectRowElt.size() > 1) && (rowIndex > 0)) {
            System.err.println("UP");
            final SQLRowValues tmp = this.vectRowElt.get(rowIndex);
            this.vectRowElt.set(rowIndex, this.vectRowElt.get(rowIndex - 1));
            this.vectRowElt.set(rowIndex - 1, tmp);
            this.fireTableDataChanged();
            return rowIndex - 1;
        }
        System.err.println("can't up!!");
        return rowIndex;
    }

    public int downRow(final int rowIndex) {
        // On vérifie qu'il est possible de descendre la ligne
        if ((rowIndex >= 0) && (this.vectRowElt.size() > 1) && (rowIndex + 1 < this.vectRowElt.size())) {

            System.err.println("DOWN");
            final SQLRowValues tmp = this.vectRowElt.get(rowIndex);
            this.vectRowElt.set(rowIndex, this.vectRowElt.get(rowIndex + 1));
            this.vectRowElt.set(rowIndex + 1, tmp);
            this.fireTableDataChanged();
            return rowIndex + 1;
        }

        System.err.println("can't down!!!");
        return rowIndex;
    }

    public void removeRow(final int rowIndex) {
        if (rowIndex >= 0) {
            final SQLRowValues rowVals = this.vectRowElt.remove(rowIndex);

            if (rowVals.getID() != SQLRow.NONEXISTANT_ID) {
                rowVals.put("ARCHIVE", 1);

                try {
                    rowVals.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            this.fireTableDataChanged();
        }
    }
}
