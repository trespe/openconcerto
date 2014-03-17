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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.cc.ITransformer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class BaseFillSQLRequest extends BaseSQLRequest {

    private static boolean DEFAULT_SELECT_LOCK = true;

    /**
     * Whether to use "FOR SHARE" in list requests (preventing roles with just SELECT right from
     * seeing the list).
     */
    public static final boolean getDefaultLockSelect() {
        return DEFAULT_SELECT_LOCK;
    }

    public static final void setDefaultLockSelect(final boolean b) {
        DEFAULT_SELECT_LOCK = b;
    }

    static public void setupForeign(final SQLRowValuesListFetcher fetcher) {
        // include rows having NULL (not undefined ID) foreign keys
        fetcher.setFullOnly(false);
        // treat the same way tables with or without undefined ID
        fetcher.setIncludeForeignUndef(false);
        // be predictable
        fetcher.setReferentsOrdered(true, true);
    }

    private final SQLTable primaryTable;
    private Where where;
    private ITransformer<SQLSelect, SQLSelect> selTransf;
    private boolean lockSelect;

    private SQLRowValues graph;
    private SQLRowValues graphToFetch;

    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    public BaseFillSQLRequest(final SQLTable primaryTable, final Where w) {
        super();
        if (primaryTable == null)
            throw new NullPointerException();
        this.primaryTable = primaryTable;
        this.where = w;
        this.selTransf = null;
        this.lockSelect = getDefaultLockSelect();
        this.graph = null;
        this.graphToFetch = null;
    }

    public BaseFillSQLRequest(final BaseFillSQLRequest req) {
        super();
        this.primaryTable = req.getPrimaryTable();
        this.where = req.where;
        this.selTransf = req.selTransf;
        this.lockSelect = req.lockSelect;
        // TODO copy
        // use methods since they're both lazy
        this.graph = req.getGraph();
        this.graphToFetch = req.getGraphToFetch();
    }

    private final SQLRowValues computeGraph() {
        if (this.getFields() == null)
            return null;

        final SQLRowValues vals = new SQLRowValues(this.getPrimaryTable());
        for (final SQLField f : this.getFields()) {
            vals.put(f.getName(), null);
        }
        // keep order field in graph (not only in graphToFetch) so that a debug column is created
        for (final Path orderP : this.getOrder()) {
            final SQLRowValues orderVals = vals.followPath(orderP);
            if (orderVals != null && orderVals.getTable().isOrdered()) {
                orderVals.put(orderVals.getTable().getOrderField().getName(), null);
            }
        }
        this.getShowAs().expand(vals);
        return vals;
    }

    /**
     * The graph computed by expanding {@link #getFields()} by {@link #getShowAs()}.
     * 
     * @return the expanded graph.
     */
    public final SQLRowValues getGraph() {
        if (this.graph == null)
            this.graph = this.computeGraph();
        return this.graph;
    }

    // should be called if getFields(), getOrder() or getShowAs() change
    protected final void clearGraph() {
        this.graph = null;
        this.graphToFetch = null;
    }

    /**
     * The graph to fetch, should be a superset of {@link #getGraph()}.
     * 
     * @return the graph to fetch, can be modified.
     */
    public final SQLRowValues getGraphToFetch() {
        if (this.graphToFetch == null && this.getGraph() != null) {
            this.graphToFetch = this.getGraph().deepCopy();
            this.customizeToFetch(this.graphToFetch);
        }
        return this.graphToFetch;
    }

    protected void customizeToFetch(final SQLRowValues graphToFetch) {
    }

    protected final SQLRowValuesListFetcher getFetcher(final Where w) {
        // graphToFetch can be modified freely so don't the use the simple constructor
        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(getGraphToFetch(), false);
        return setupFetcher(fetcher, w);
    }

    // allow to pass fetcher since they are mostly immutable (and for huge graphs they are slow to
    // create)
    protected final SQLRowValuesListFetcher setupFetcher(final SQLRowValuesListFetcher fetcher, final Where w) {
        final String tableName = getPrimaryTable().getName();
        setupForeign(fetcher);
        fetcher.setOrder(getOrder());
        final ITransformer<SQLSelect, SQLSelect> origSelTransf = fetcher.getSelTransf();
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {
                if (origSelTransf != null)
                    sel = origSelTransf.transformChecked(sel);
                sel = transformSelect(sel);
                if (isLockSelect())
                    sel.addWaitPreviousWriteTXTable(tableName);
                return sel.andWhere(getWhere()).andWhere(w);
            }
        });
        return fetcher;
    }

    protected List<Path> getOrder() {
        return Collections.singletonList(Path.get(getPrimaryTable()));
    }

    public final void setWhere(final Where w) {
        this.where = w;
        fireWhereChange();
    }

    public final Where getWhere() {
        return this.where;
    }

    public final void setLockSelect(boolean lockSelect) {
        this.lockSelect = lockSelect;
    }

    public final boolean isLockSelect() {
        return this.lockSelect;
    }

    @Override
    public final Collection<SQLField> getAllFields() {
        // don't rely on the expansion of our fields, since our fetcher can be arbitrary modified
        // (eg by adding a where on a field of a non-displayed table)
        return this.getFetcher(null).getReq().getFields();
    }

    protected abstract Collection<SQLField> getFields();

    protected SQLSelect transformSelect(final SQLSelect sel) {
        return this.selTransf == null ? sel : this.selTransf.transformChecked(sel);
    }

    public final ITransformer<SQLSelect, SQLSelect> getSelectTransf() {
        return this.selTransf;
    }

    /**
     * Allows to transform the SQLSelect returned by getFillRequest().
     * 
     * @param transf the transformer to apply.
     */
    public final void setSelectTransf(final ITransformer<SQLSelect, SQLSelect> transf) {
        this.selTransf = transf;
        this.fireWhereChange();
    }

    protected abstract FieldExpander getShowAs();

    public final SQLTable getPrimaryTable() {
        return this.primaryTable;
    }

    protected final void fireWhereChange() {
        this.supp.firePropertyChange("where", null, null);
    }

    public final void addWhereListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("where", l);
    }

    public final void rmWhereListener(final PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("where", l);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " on " + this.getPrimaryTable();
    }
}
