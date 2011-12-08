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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.ListSQLRequest;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lines are taken directly from the database.
 * 
 * @author Sylvain
 */
public class SQLTableModelLinesSourceOnline extends SQLTableModelLinesSource {

    private final SQLTableModelSourceOnline parent;
    private final PropertyChangeListener listener;

    public SQLTableModelLinesSourceOnline(SQLTableModelSourceOnline parent, final ITableModel model) {
        super(model);
        this.parent = parent;
        this.listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireChanged(evt);
            }
        };
        this.getReq().addWhereListener(this.listener);
    }

    @Override
    protected void die() {
        this.getReq().rmWhereListener(this.listener);
        super.die();
    }

    @Override
    public final SQLTableModelSourceOnline getParent() {
        return this.parent;
    }

    public final ListSQLRequest getReq() {
        return this.getParent().getReq();
    }

    public List<ListSQLLine> getAll() {
        final List<SQLRowValues> values = this.getReq().getValues();
        final List<ListSQLLine> res = new ArrayList<ListSQLLine>(values.size());
        for (final SQLRowValues v : values) {
            final ListSQLLine newLine = createLine(v);
            if (newLine != null)
                res.add(newLine);
        }
        return res;
    }

    public ListSQLLine get(final int id) {
        return createLine(this.getReq().getValues(id));
    }

    @Override
    protected int getID(SQLRowValues r) {
        return r.getID();
    }

    private BigDecimal getOrder(SQLRowAccessor r) {
        return (BigDecimal) r.getObject(r.getTable().getOrderField().getName());
    }

    @Override
    public int compare(ListSQLLine l1, ListSQLLine l2) {
        return getOrder(l1.getRow()).compareTo(getOrder(l2.getRow()));
    }

    @Override
    public void commit(ListSQLLine l, Path path, SQLRowValues vals) throws SQLException {
        vals.update();
    }

}
