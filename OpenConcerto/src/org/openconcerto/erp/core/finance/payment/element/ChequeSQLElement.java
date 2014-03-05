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
 
 package org.openconcerto.erp.core.finance.payment.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.payment.ui.GestionChequesRenderer;
import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.ShowAs;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.util.List;

public abstract class ChequeSQLElement extends ComptaSQLConfElement implements ChequeType {

    public ChequeSQLElement(String tableName, String singular, String plural) {
        super(tableName, singular, plural);
    }

    protected final SQLTableModelSourceOnline createDepositTableSource(final List<String> fields, final ShowAs showAs, final Where w) {
        final ListSQLRequest req = new ListSQLRequest(getTable(), fields) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.putNulls(getDoneFieldName());
            }

            @Override
            protected FieldExpander getShowAs() {
                return showAs;
            }
        };
        req.setWhere(new Where(this.getTable().getField(this.getDoneFieldName()), "=", Boolean.FALSE).and(w));

        final SQLTableModelSourceOnline res = new SQLTableModelSourceOnline(req);
        res.getColumn(getTable().getField(getMinDateFieldName())).setRenderer(new GestionChequesRenderer());

        // TODO a check box column to ease multi-selection

        return initTableSource(res);
    }
}
