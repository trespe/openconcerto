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

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.change.ListChangeIndex;

/**
 * A SQLTableModelSource directly tied to the database. Any changes to its lines are propagated to
 * the database without any delay.
 * 
 * @author Sylvain
 */
public class SQLTableModelSourceOnline extends SQLTableModelSource {

    private final ListSQLRequest req;

    public SQLTableModelSourceOnline(ListSQLRequest req) {
        super(req.getGraph());
        this.req = req;
    }

    public SQLTableModelSourceOnline(SQLTableModelSourceOnline src) {
        super(src);
        this.req = src.req;
    }

    public final ListSQLRequest getReq() {
        return this.req;
    }

    @Override
    protected void colsChanged(ListChangeIndex change) {
        super.colsChanged(change);
        // add needed fields for each new column
        for (final Object o : change.getItemsAdded()) {
            final SQLTableModelColumn col = (SQLTableModelColumn) o;
            for (final FieldPath p : col.getPaths()) {
                final SQLRowValues assurePath = this.getReq().getGraphToFetch().assurePath(p.getPath());
                if (!assurePath.getFields().contains(p.getFieldName()))
                    assurePath.put(p.getFieldName(), null);
            }
        }
    }

    @Override
    protected SQLTableModelLinesSourceOnline _createLinesSource(final ITableModel model) {
        return new SQLTableModelLinesSourceOnline(this, model);
    }
}
