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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class UserRightsManagerModel extends AbstractTableModel {

    private SQLTable tableRight = Configuration.getInstance().getRoot().findTable("RIGHTS");
    private SQLTable tableUserRight = Configuration.getInstance().getRoot().findTable("USER_RIGHT");

    private int idUser = -1;

    private Vector<SQLRowValues> listRowValues = new Vector<SQLRowValues>(this.tableRight.getRowCount());

    private List<String> columns = Arrays.asList("Actif", "Libellé");

    List<SQLRow> cache = new ArrayList<SQLRow>();

    public UserRightsManagerModel() {
        super();

        // On recupere l'ensemble des droits
        SQLSelect sel2 = new SQLSelect(Configuration.getInstance().getBase());
        sel2.addSelect(this.tableRight.getKey());
        sel2.addFieldOrder(this.tableRight.getField("CODE"));
        List<SQLRow> rowsRights = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel2.asString(), new SQLRowListRSH(this.tableRight, true));
        this.cache.addAll(rowsRights);

        this.tableRight.addTableListener(new SQLTableListener() {
            @Override
            public void rowAdded(SQLTable table, int id) {

                UserRightsManagerModel.this.cache.add(table.getRow(id));
            }

            @Override
            public void rowDeleted(SQLTable table, int id) {

            }

            @Override
            public void rowModified(SQLTable table, int id) {
                SQLRow row = table.getRow(id);

                for (int i = 0; i < UserRightsManagerModel.this.cache.size(); i++) {
                    SQLRow row2 = UserRightsManagerModel.this.cache.get(i);
                    if (row2.getID() == id) {
                        if (row.isArchived()) {
                            UserRightsManagerModel.this.cache.remove(i);
                        } else {
                            UserRightsManagerModel.this.cache.set(i, row2);
                        }
                        break;
                    }
                }
            }
        });

        this.tableUserRight.addTableListener(new SQLTableListener() {
            @Override
            public void rowAdded(SQLTable table, int id) {

                SQLRow row = table.getRow(id);
                if (row.getInt("ID_USER_COMMON") == UserRightsManagerModel.this.idUser) {
                    SQLRowValues rowVals = getSQLRowValuesForID(id);
                    if (rowVals == null) {
                        UserRightsManagerModel.this.listRowValues.add(row.createUpdateRow());
                        fireTableRowsInserted(UserRightsManagerModel.this.listRowValues.size() - 2, UserRightsManagerModel.this.listRowValues.size() - 1);
                    }
                }
            }

            @Override
            public void rowDeleted(SQLTable table, int id) {
            }

            @Override
            public void rowModified(SQLTable table, int id) {
                SQLRow row = table.getRow(id);
                if (row.getInt("ID_USER_COMMON") == UserRightsManagerModel.this.idUser) {
                    SQLRowValues rowVals = getSQLRowValuesForID(id);
                    int index = UserRightsManagerModel.this.listRowValues.indexOf(rowVals);
                    if (row.isArchived()) {
                        UserRightsManagerModel.this.listRowValues.removeElement(rowVals);
                        fireTableRowsDeleted(index - 1, index + 1);
                    } else {
                        rowVals.loadAbsolutelyAll(row);
                        fireTableRowsUpdated(index - 1, index + 1);
                    }
                }
            }
        });
    }

    private SQLRowValues getSQLRowValuesForID(int id) {
        SQLRow row = this.tableUserRight.getRow(id);
        final String string2 = row.getString("CODE");
        for (SQLRowValues rowVals : this.listRowValues) {
            final String string = rowVals.getString("CODE");
            if (rowVals.getID() == id || (string != null && string2 != null && string.equalsIgnoreCase(string2))) {
                return rowVals;
            }
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return this.columns.get(column);
    }

    @Override
    public int getColumnCount() {
        return this.columns.size();
    }

    @Override
    public int getRowCount() {
        return this.listRowValues.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {

            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {

        return (columnIndex == 0);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {

        this.listRowValues.get(rowIndex).put("HAVE_RIGHT", value);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            Boolean b = this.listRowValues.get(rowIndex).getBoolean("HAVE_RIGHT");
            return (b == null) ? true : b;
        } else {
            return this.listRowValues.get(rowIndex).getString("NOM");
        }

    }

    public SQLRowValues getRowValuesAt(int index) {
        return this.listRowValues.get(index);
    }

    /**
     * Valide les modifications dans la base
     */
    public void commitData() {

        List<SQLRowValues> listRowVals = new ArrayList<SQLRowValues>(this.listRowValues);

        for (SQLRowValues rowVals : listRowVals) {

            try {
                SQLRow row = rowVals.commit();
                rowVals.loadAbsolutelyAll(row);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Charge les droits de l'utilisateur, vide la table si il n'existe pas
     * 
     * @param idUser
     */
    public void loadRightsForUser(int idUser) {
        this.idUser = idUser;
        if (idUser > 1) {

            // On recupere les droits deja définit pour cet utilisateur
            SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
            sel.addSelect(this.tableUserRight.getKey());
            sel.setWhere(new Where(this.tableUserRight.getField("ID_USER_COMMON"), "=", idUser));
            List<SQLRow> rows = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(this.tableUserRight, true));
            Map<String, SQLRowValues> map = new HashMap<String, SQLRowValues>(rows.size());
            for (SQLRow row : rows) {
                map.put(row.getString("CODE"), row.createUpdateRow());
            }

            this.listRowValues.clear();

            for (SQLRow row : this.cache) {
                final SQLRowValues e = map.get(row.getString("CODE"));
                if (e != null) {
                    e.put("NOM", row.getString("NOM"));
                    e.put("DESCRIPTION", row.getString("DESCRIPTION"));
                    this.listRowValues.add(e);
                } else {
                    SQLRowValues rowVals = new SQLRowValues(this.tableUserRight);
                    rowVals.put("ID_USER_COMMON", idUser);
                    rowVals.put("CODE", row.getString("CODE"));
                    rowVals.put("NOM", row.getString("NOM"));
                    rowVals.put("DESCRIPTION", row.getString("DESCRIPTION"));
                    this.listRowValues.add(rowVals);
                }
            }
        } else {
            this.listRowValues.clear();
        }
        fireTableDataChanged();
    }
}
