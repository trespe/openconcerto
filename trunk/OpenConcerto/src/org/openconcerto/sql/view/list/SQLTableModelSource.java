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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.change.ListChangeIndex;
import org.openconcerto.utils.change.ListChangeRecorder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Define the columns and lines for ITableModel.
 * 
 * @author Sylvain
 */
public abstract class SQLTableModelSource {

    private final SQLTable table;
    private SQLRowValues inited;
    private final List<SQLTableModelColumn> allCols;
    private final ListChangeRecorder<SQLTableModelColumn> cols;
    private final Map<String, SQLTableModelColumn> colsByName;
    // to notify of columns change, better than having one listener per line
    private final List<WeakReference<SQLTableModelLinesSource>> lines;

    private final PropertyChangeSupport supp;

    {
        this.supp = new PropertyChangeSupport(this);
        this.lines = new ArrayList<WeakReference<SQLTableModelLinesSource>>();
    }

    public SQLTableModelSource(SQLRowValues graph) {
        this.table = graph.getTable();
        this.allCols = new ArrayList<SQLTableModelColumn>();
        this.cols = new ListChangeRecorder<SQLTableModelColumn>(new ArrayList<SQLTableModelColumn>());
        this.colsByName = new HashMap<String, SQLTableModelColumn>();
        this.inited = graph;
    }

    // lazy initialization since this method calls colsChanged() which subclasses overload and
    // they need their own attribute that aren't set yet since super() must be the first statement.
    private void init() {
        if (this.inited == null)
            return;

        final SQLRowValues graph = this.inited;

        listenToCols();

        graph.walkFields(new IClosure<FieldPath>() {
            @Override
            public void executeChecked(final FieldPath input) {
                final SQLField f = input.getField();
                if (f.getTable().getLocalContentFields().contains(f)) {
                    final SQLTableModelColumnPath col = new SQLTableModelColumnPath(input);
                    SQLTableModelSource.this.cols.add(col);
                } else
                    SQLTableModelSource.this.allCols.add(new SQLTableModelColumnPath(input.getPath(), f.getName(), f.toString()) {
                        // don't show the rowValues since it's very verbose (and all content fields
                        // are already displayed as normal columns) and unsortable
                        @Override
                        protected Object show_(SQLRowAccessor r) {
                            final Object res = super.show_(r);
                            return res instanceof SQLRowValues ? ((SQLRowValues) res).getID() : res;
                        }
                    });
            }
        }, true);
        // allCols = this.cols + debugCols
        // don't put SQLRowAccessor: it's an interface and thus JTable.getDefaultRenderer()
        // returns null
        this.allCols.add(new BaseSQLTableModelColumn("Fields", Object.class) {
            @Override
            protected Object show_(SQLRowAccessor r) {
                if (r instanceof SQLRow)
                    return r;
                else
                    return new SQLRowValues((SQLRowValues) r, ForeignCopyMode.COPY_ID_OR_RM);
            }

            @Override
            public Set<SQLField> getFields() {
                return getPrimaryTable().getFields();
            }

            @Override
            public Set<FieldPath> getPaths() {
                return FieldPath.create(new Path(getPrimaryTable()), getPrimaryTable().getFieldsName());
            }
        });
        this.allCols.add(new SQLTableModelColumnPath(new Path(getPrimaryTable()), getPrimaryTable().getKey().getName(), "PrimaryKey"));

        // at the end so that fireColsChanged() can use it
        this.inited = null;
    }

    public SQLTableModelSource(SQLTableModelSource src) {
        this.table = src.table;
        this.allCols = new ArrayList<SQLTableModelColumn>(src.allCols);
        this.cols = new ListChangeRecorder<SQLTableModelColumn>(new ArrayList<SQLTableModelColumn>(src.cols));
        this.colsByName = new HashMap<String, SQLTableModelColumn>(src.colsByName);
        this.inited = null;
        listenToCols();
    }

    private void listenToCols() {
        // keep allCols in sync with cols, and listen to any change
        this.cols.getRecipe().bind(this.allCols);
        this.cols.getRecipe().addListener(new IClosure<ListChangeIndex<SQLTableModelColumn>>() {
            @Override
            public void executeChecked(ListChangeIndex<SQLTableModelColumn> change) {
                for (final SQLTableModelColumn col : change.getItemsRemoved()) {
                    SQLTableModelSource.this.colsByName.remove(col.getName());
                }
                for (final SQLTableModelColumn col : change.getItemsAdded()) {
                    SQLTableModelSource.this.colsByName.put(col.getName(), col);
                }
                colsChanged(change);
                fireColsChanged();
            }
        });
    }

    protected void colsChanged(final ListChangeIndex<SQLTableModelColumn> change) {
    }

    private void fireColsChanged() {
        // do not fire while initializing, otherwise the first getColumns() will fire, eg :
        // a class does that in constructor :
        // -addColumnListener({updateColNames();});
        // -updateColNames();
        // and in updateColNames(), uses getColumns() :
        // -this.colNames.clear();
        // -for (final SQLTableModelColumn col : getCols())
        // -this.colNames.add(col.getName());
        // with this code this.colNames will contain twice our scolumns
        if (this.inited != null)
            return;

        // let know each of our LinesSource that the columns have changed
        int i = 0;
        while (i < this.lines.size()) {
            final WeakReference<SQLTableModelLinesSource> l = this.lines.get(i);
            final SQLTableModelLinesSource line = l.get();
            if (line == null)
                this.lines.remove(i);
            else {
                line.colsChanged();
                i++;
            }
        }
        // before notifying our regular listeners
        this.supp.firePropertyChange("cols", null, this.cols);
    }

    public final SQLTableModelLinesSource createLinesSource(ITableModel model) {
        final SQLTableModelLinesSource res = this._createLinesSource(model);
        this.lines.add(new WeakReference<SQLTableModelLinesSource>(res));
        return res;
    }

    protected abstract SQLTableModelLinesSource _createLinesSource(ITableModel model);

    /**
     * The maximum graph of the lines returned by {@link #createLinesSource(ITableModel)}.
     * 
     * @return the maximum graph of our lines.
     */
    public abstract SQLRowValues getMaxGraph();

    // * columns

    /**
     * The normal columns.
     * 
     * @return the normal columns.
     */
    public final List<SQLTableModelColumn> getColumns() {
        this.init();
        return this.cols;
    }

    /**
     * The normal columns plus some debug columns. Usually primary and foreign keys.
     * 
     * @return the debub columns.
     */
    public final List<SQLTableModelColumn> getAllColumns() {
        this.init();
        return Collections.unmodifiableList(this.allCols);
    }

    public final SQLTableModelColumn getColumn(int index) {
        return this.getAllColumns().get(index);
    }

    /**
     * All the columns that depends on the passed field.
     * 
     * @param f the field.
     * @return all columns needing <code>f</code>.
     */
    public final List<SQLTableModelColumn> getColumns(SQLField f) {
        final List<SQLTableModelColumn> res = new ArrayList<SQLTableModelColumn>();
        for (final SQLTableModelColumn col : this.getColumns())
            if (col.getFields().contains(f))
                res.add(col);
        return res;
    }

    /**
     * The column depending solely on the passed field.
     * 
     * @param f the field.
     * @return the column needing only <code>f</code>.
     * @throws IllegalArgumentException if more than one column matches.
     */
    public final SQLTableModelColumn getColumn(SQLField f) {
        final Set<SQLField> singleton = Collections.singleton(f);
        SQLTableModelColumn res = null;
        for (final SQLTableModelColumn col : this.getColumns())
            if (col.getFields().equals(singleton)) {
                if (res == null)
                    res = col;
                else
                    throw new IllegalArgumentException("Not exactly one column for " + f);
            }
        return res;
    }

    public final void addColumnListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("cols", l);
    }

    public final void rmColumnListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("cols", l);
    }

    // * SQLIdentifier

    public final SQLTable getPrimaryTable() {
        return this.table;
    }

    /**
     * All the displayed tables, i.e. tables of {@link #getLineFields()}.
     * 
     * @return the displayed tables.
     */
    public final Set<SQLTable> getTables() {
        return new SQLFieldsSet(this.getLineFields()).getTables();
    }

    /**
     * All fields that affects a line of this source. I.e. not just the displayed fields, but also
     * the foreign keys, including intermediate ones (e.g. if this displays [BATIMENT.DES, CPI.DES]
     * LOCAL.ID_BATIMENT matters).
     * 
     * @return the fields affecting this.
     */
    public final Set<SQLField> getLineFields() {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final SQLRowValues v : getMaxGraph().getGraph().getItems()) {
            for (final String f : v.getFields())
                res.add(v.getTable().getField(f));
            if (v.getTable().isArchivable())
                res.add(v.getTable().getArchiveField());
        }
        return res;
    }

}
