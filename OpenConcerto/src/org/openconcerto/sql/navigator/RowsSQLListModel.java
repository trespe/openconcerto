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
 * Créé le 21 mai 2005
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;

import java.util.List;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;

public class RowsSQLListModel extends SQLListModel<SQLRow> implements SQLTableModifiedListener {

    private final SQLElement element;
    private final ResultSetHandler handler;

    public RowsSQLListModel(SQLElement element) {
        super();
        this.element = element;
        this.handler = new SQLRowListRSH(this.getElement().getTable(), true);
        this.getElement().getTable().addTableModifiedListener(this);
    }

    @Override
    protected void reload(final boolean noCache) {
        final Set<Number> ids = getIds();
        final String key = this.getElement().getParentForeignField();

        final SQLSelect sel = new SQLSelect(this.getElement().getTable().getBase());
        sel.addSelectStar(this.getElement().getTable());
        sel.addOrderSilent(this.getElement().getTable().getName());

        // si null pas de where, on montre tout
        if (ids != null && key != null) {
            sel.setWhere(new Where(this.getElement().getTable().getField(key), ids));
        }
        final SQLDataSource source = this.getElement().getTable().getBase().getDataSource();

        // cannot just use a SwingWorker, cause some methods (like SQLBrowser#selectPath())
        // expect reload() to by synchronous.
        @SuppressWarnings("unchecked")
        final List<SQLRow> rows = (List<SQLRow>) source.execute(sel.asString(), new IResultSetHandler(this.handler) {
            @Override
            public boolean readCache() {
                return !noCache;
            }

            @Override
            public boolean writeCache() {
                return true;
            }
        });
        this.setAll(rows);
    }

    /**
     * Search the row with the passed <code>id</code> and return its index.
     * 
     * @param id an ID of a SQLRow.
     * @return the index of the SQLRow or -1 if none is found.
     */
    public int indexFromID(int id) {
        for (int i = 0; i < this.getDisplayedItems().size(); i++) {
            final SQLRow row = this.getDisplayedItems().get(i);
            if (row != this.getALLValue() && row.getID() == id)
                return i;
        }
        return -1;
    }

    protected String toString(SQLRow item) {
        return this.getElement().getDescription(item);
    }

    public final SQLElement getElement() {
        return this.element;
    }

    @Override
    public void tableModified(SQLTableEvent evt) {
        // TODO test if that concern us
        this.reload();
    }

    public String toString() {
        return this.getClass().getName() + " on " + this.getElement();
    }

    protected void die() {
        this.getElement().getTable().removeTableModifiedListener(this);
    }

    @Override
    protected void idsChanged() {
        this.reload();
    }

    @Override
    protected boolean hasALLValue() {
        return true;
    }
}
