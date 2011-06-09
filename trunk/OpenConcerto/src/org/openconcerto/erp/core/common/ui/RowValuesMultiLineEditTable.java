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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;

import java.awt.Window;
import java.io.File;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

public abstract class RowValuesMultiLineEditTable extends RowValuesTable {

    // AFFAIRE[POURCENT_SERVICE] --> champ text qui affiche getStringValue
    // POURCENT_SERVICE[ID_AFFAIRE]

    // jtable parente
    private RowValuesTable tableRoot;

    // rowValues de la jtable parente
    private SQLRowValues rowValsRoot;

    // index de la rowValues de la jtable parente
    private int indexRowRoot;

    // foreignKey de l'element de la jtable parente ex:ID_AFFAIRE
    private String foreignField;

    // field de la table parente ex: POURCENT_SERVICE
    private String field;

    public RowValuesMultiLineEditTable(RowValuesTableModel model, File f, String field) {
        super(model, f);
        this.field = field;
    }

    public void closeTable() {
        Window window = ((Window) SwingUtilities.getRoot(RowValuesMultiLineEditTable.this));

        TableCellEditor editor = this.tableRoot.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }

        final String string = getStringValue(this.tableRoot.getRowValuesTableModel().getRowValuesAt(this.indexRowRoot));
        this.tableRoot.getRowValuesTableModel().putValue(string, this.indexRowRoot, this.field);
        this.tableRoot.revalidate();
        this.tableRoot.repaint();
        if (window != null) {
            window.setVisible(false);
            window.dispose();
        }
    }

    public String getStringValue() {
        return getStringValue(this.rowValsRoot);
    }

    public abstract String getStringValue(SQLRowValues rowVals);

    public void setRoots(RowValuesTable tableRoot, int row, SQLRowValues rowVals) {
        this.rowValsRoot = rowVals;
        this.indexRowRoot = row;
        this.tableRoot = tableRoot;
        Set<SQLField> s = this.getRowValuesTableModel().getSQLElement().getTable().getForeignKeys(rowVals.getTable());
        if (s != null && s.size() > 0) {
            this.foreignField = ((SQLField) s.toArray()[0]).getName();
        } else {
            throw new IllegalArgumentException("La table " + rowVals.getTable().getName() + " n'est pas référencée par la table " + this.getRowValuesTableModel().getSQLElement().getTable());
        }
        insertFrom(this.foreignField, rowVals);
    }

    public SQLRowValues getRowValuesRoot() {
        return this.rowValsRoot;
    }

    public String getForeignField() {
        return this.foreignField;
    }

}
