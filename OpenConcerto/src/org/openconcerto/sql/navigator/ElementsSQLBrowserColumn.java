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
 * Créé le 28 mai 2005
 * 
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.util.List;

import javax.swing.ListSelectionModel;

/**
 * @author Sylvain CUAZ
 */
public class ElementsSQLBrowserColumn extends SQLBrowserColumn<SQLElement, ElementsSQLListModel> {

    private final SQLElement parent;

    public ElementsSQLBrowserColumn(SQLElement parent, List<SQLElement> elements) {
        super(new ElementsSQLListModel(elements), false);
        this.parent = parent;
    }

    protected int getSelectionMode() {
        return ListSelectionModel.SINGLE_SELECTION;
    }

    public SQLTable getTable() {
        return this.previous().getTable();
    }

    public List<Integer> getSelectedIDs() {
        return this.previous().getSelectedIDs();
    }

    public List<SQLRow> getSelectedRows() {
        return this.previous().getSelectedRows();
    }

    protected SQLBrowserColumn selectionChanged(ListSelectionModel m) {
        // we're SINGLE
        final SQLElement sel = (SQLElement) this.list.getSelectedValue();
        final RowsSQLBrowserColumn res = new RowsSQLBrowserColumn(sel, this.previous().isSearchable());
        // doit afficher les lignes pointant vers les parents selectionnés
        res.setParentIDs(this.previousRowsColumn().getSelectedIDs());
        return res;
    }

    protected String getHeaderName() {
        final String res;
        if (this.parent == null) {
            res = "Selection";
        } else {
            final String name = "Contenu des " + this.parent.getPluralName();
            // Mets la 1ere lettre en majuscule
            res = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return res;
    }

    public void setSelectedRow(SQLRow r) {
        final SQLElement elem = Configuration.getInstance().getDirectory().getElement(r.getTable());
        this.setSelectedValue(elem);
        this.next().setSelectedRow(r);
    }

    protected void live() {
        // nothing to do
    }

    protected void die() {
        // nothing to do
    }

}
