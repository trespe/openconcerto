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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TransfFieldExpander;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.BaseFillSQLRequest;
import org.openconcerto.sql.ui.StringWithId;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementComboBoxUtils {
    public static final SQLRowValues getGraphToFetch(Configuration conf, SQLTable table, List<SQLField> fieldsToFetch) {
        if (fieldsToFetch == null)
            return null;

        final SQLRowValues vals = new SQLRowValues(table);
        for (final SQLField f : fieldsToFetch) {
            vals.put(f.getName(), null);
        }
        // keep order field in graph (not only in graphToFetch) so that a debug column is created
        for (final Path orderP : Collections.singletonList(new Path(table))) {
            final SQLRowValues orderVals = vals.followPath(orderP);
            if (orderVals != null && orderVals.getTable().isOrdered()) {
                orderVals.put(orderVals.getTable().getOrderField().getName(), null);
            }
        }
        getShowAs(conf).expand(vals);
        return vals;
    }

    public static final StringWithId createItem(Configuration conf, SQLTable primaryTable, final SQLRowValues rs, List<SQLField> fields) {
        final String desc;
        if (rs.getID() == primaryTable.getUndefinedID())
            desc = "?";
        else
            desc = CollectionUtils.join(getShowAs(conf).expandGroupBy(fields), " ◄ ", new ITransformer<Tuple2<Path, List<FieldPath>>, Object>() {
                public Object transformChecked(Tuple2<Path, List<FieldPath>> ancestorFields) {
                    final List<String> filtered = CollectionUtils.transformAndFilter(ancestorFields.get1(), new ITransformer<FieldPath, String>() {
                        // no need to keep this Transformer in an attribute
                        // even when creating one per line it's the same speed
                        public String transformChecked(FieldPath input) {
                            return input.getString(rs);
                        }
                    }, IPredicate.notNullPredicate(), new ArrayList<String>());
                    return CollectionUtils.join(filtered, " ");
                }
            });
        // don't store the whole SQLRowValues to save some memory
        final StringWithId res = new StringWithId(rs.getID(), desc);

        return res;
    }

    public static final List<SQLRowValues> fetchRows(final Configuration conf, final SQLTable foreignTable, List<SQLField> fieldsToFetch, final Where where) {
        // TODO: a cleaner par Sylvain
        SQLRowValues graphToFetch = ElementComboBoxUtils.getGraphToFetch(conf, foreignTable, fieldsToFetch);
        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(graphToFetch, false);
        final String tableName = foreignTable.getName();
        BaseFillSQLRequest.setupForeign(fetcher);
        final ITransformer<SQLSelect, SQLSelect> origSelTransf = fetcher.getSelTransf();
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {
                if (origSelTransf != null)
                    sel = origSelTransf.transformChecked(sel);
                boolean lockSelect = true;
                if (lockSelect) {
                    sel.addWaitPreviousWriteTXTable(tableName);
                }
                for (final Path orderP : Collections.singletonList(new Path(foreignTable))) {
                    sel.addOrder(sel.assurePath(tableName, orderP), false);
                }
                if (where != null) {
                    sel.andWhere(where);
                }
                return sel;
            }
        });

        final SQLRowValuesListFetcher comboSelect = fetcher.freeze();
        final List<SQLRowValues> fetchedRows = comboSelect.fetch();
        return fetchedRows;
    }

    public static final TransfFieldExpander getShowAs(final Configuration conf) {
        final TransfFieldExpander exp = new TransfFieldExpander(new ITransformer<SQLField, List<SQLField>>() {
            @Override
            public List<SQLField> transformChecked(SQLField fk) {
                final SQLTable foreignTable = fk.getDBSystemRoot().getGraph().getForeignTable(fk);
                return conf.getDirectory().getElement(foreignTable).getComboRequest().getFields();
            }
        });
        return exp;
    }
}
