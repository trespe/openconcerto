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

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// use SQLRowValues to allow graph
public abstract class SQLTableModelLinesSource {

    private final ITableModel model;
    // to notify of columns change, better than having one listener per line
    private final List<WeakReference<ListSQLLine>> lines;
    private final List<LineListener> lineListeners;
    private final List<PropertyChangeListener> listeners;
    private IPredicate<SQLRowValues> filter;

    {
        this.lineListeners = new ArrayList<LineListener>();
        this.listeners = new ArrayList<PropertyChangeListener>();
        this.lines = new ArrayList<WeakReference<ListSQLLine>>();
        this.filter = null;
    }

    protected SQLTableModelLinesSource(final ITableModel model) {
        this.model = model;
    }

    void die() {
    }

    public final ITableModel getModel() {
        return this.model;
    }

    public abstract SQLTableModelSource getParent();

    public abstract List<ListSQLLine> getAll();

    /**
     * A row in the db has been changed, fetch its current value.
     * 
     * @param id a valid ID of a database row.
     * @return the new value, <code>null</code> if the passed id is not part of this.
     */
    public abstract ListSQLLine get(final int id);

    public abstract int compare(ListSQLLine l1, ListSQLLine l2);

    public final void setFilter(final IPredicate<SQLRowValues> filter) {
        // always fire since for now there's no other way for the caller
        // (ie if the meaning of filter change, it has to do setFilter(getFilter()) )
        this.filter = filter;
        this.fireChanged(new PropertyChangeEvent(this, "filter", null, this.filter));
    }

    public final IPredicate<SQLRowValues> getFilter() {
        return this.filter;
    }

    /**
     * Adds a listener to be notified when {@link #getAll()} change value.
     * 
     * @param l the listener.
     */
    public final void addListener(PropertyChangeListener l) {
        this.listeners.add(l);
    }

    public final void rmListener(PropertyChangeListener l) {
        this.listeners.remove(l);
    }

    protected final void fireChanged(PropertyChangeEvent evt) {
        for (final PropertyChangeListener l : this.listeners)
            l.propertyChange(evt);
    }

    public final void addLineListener(LineListener l) {
        this.lineListeners.add(l);
    }

    public final void rmLineListener(LineListener l) {
        this.lineListeners.remove(l);
    }

    final void fireLineChanged(int id, ListSQLLine line, Set<Integer> colIndexes) {
        for (final LineListener l : this.lineListeners)
            l.lineChanged(id, line, colIndexes);
        // update dependent columns unless they've just been all refreshed
        if (line != null && colIndexes != null) {
            // use set do refresh only once a column even if all its used columns are refreshed
            final Set<Integer> dependantIndex = new HashSet<Integer>();
            for (final int colIndex : colIndexes) {
                final SQLTableModelColumn colChanged = this.getParent().getColumn(colIndex);
                final String colID = colChanged.getIdentifier();
                int i = 0;
                for (final SQLTableModelColumn col : this.getParent().getColumns()) {
                    if (col != colChanged && col.getUsedCols().contains(colID))
                        dependantIndex.add(i);
                    i++;
                }
            }
            if (!dependantIndex.isEmpty())
                line.updateValueAt(dependantIndex);
        }
    }

    protected final ListSQLLine createLine(final SQLRowValues v) {
        if (v == null || (this.filter != null && !this.filter.evaluateChecked(v)))
            return null;
        final ListSQLLine res = new ListSQLLine(this, v, this.getID(v));
        this.lines.add(new WeakReference<ListSQLLine>(res));
        this.lineCreated(res);
        return res;
    }

    /**
     * A new ListSQLLine is being created.
     * 
     * @param r the row.
     * @return the ID to give to the new line.
     */
    protected abstract int getID(SQLRowValues r);

    /**
     * A new line has been created. This implementation does nothing.
     * 
     * @param res the newly created line.
     */
    protected void lineCreated(ListSQLLine res) {
    }

    final void colsChanged() {
        int i = 0;
        while (i < this.lines.size()) {
            final WeakReference<ListSQLLine> l = this.lines.get(i);
            final ListSQLLine line = l.get();
            if (line == null)
                this.lines.remove(i);
            else {
                line.clearCache();
                i++;
            }
        }
    }

    /**
     * Change the line <code>l</code> at the passed path with the passed values.
     * 
     * @param l the line to change, eg RECEPTEUR[12].
     * @param path the changing path, eg RECEPTEUR.ID_LIMITEUR.
     * @param vals the new values, eg LIMITEUR{ID=4, DESIGNATION="dess"}.
     * @throws SQLException if the values cannot be commited.
     */
    public abstract void commit(ListSQLLine l, Path path, SQLRowValues vals) throws SQLException;
}
