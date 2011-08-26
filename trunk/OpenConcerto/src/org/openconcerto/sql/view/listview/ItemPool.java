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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.utils.checks.ValidListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;

/**
 * Handle a list of item in a ListSQLView panel. It handles the creation or deletion, and knows how
 * to persist the list to the DB.
 * 
 * @author Sylvain CUAZ
 */
public abstract class ItemPool {

    private final ItemPoolFactory creator;
    private final ListSQLView panel;
    private final Set<ValidListener> validListeners;

    public ItemPool(ItemPoolFactory parent, ListSQLView panel) {
        this.creator = parent;
        this.panel = panel;
        this.validListeners = new HashSet<ValidListener>();
    }

    public abstract void reset();

    // [SQLRowItemView]
    public abstract void show(final SQLRowAccessor r);

    // ** modif

    /**
     * Can we call {@link #getNewItem()}.
     * 
     * @return <code>true</code> if getNewItem() won't raise an <code>IllegalStateException</code>.
     */
    public abstract boolean availableItem();

    public abstract SQLRowItemView getNewItem() throws IllegalStateException;

    public abstract void removeItem(SQLRowItemView v);

    /**
     * Get the items that will be committed to the DB on the next update/insert.
     * 
     * @return a List of SQLRowItemView.
     */
    public abstract List<SQLRowItemView> getItems();

    /**
     * Get the items that will be added to the DB on the next update/insert.
     * 
     * @return a List of SQLRowItemView.
     */
    public abstract List<SQLRowItemView> getAddedItems();

    /**
     * Get the items that will be deleted from the DB on the next update/insert.
     * 
     * @return a List of SQLRowItemView.
     */
    public abstract List<SQLRowItemView> getRemovedItems();

    protected final ItemPoolFactory getCreator() {
        return this.creator;
    }

    protected final ListSQLView getPanel() {
        return this.panel;
    }

    // ** commit

    public abstract void update(final SQLRowValues vals);

    public abstract void insert(final SQLRowValues vals);

    // ** utils

    protected final void forAllDo(final Collection c, final Closure cl) {
        CollectionUtils.forAllDo(c, cl);
    }

    static protected abstract class Cl implements Closure {
        public abstract void execute(SQLRowItemView input);

        public final void execute(Object input) {
            this.execute((SQLRowItemView) input);
        }
    }

    public final void addValidListener(ValidListener l) {
        this.validListeners.add(l);
    }

    public void removeValidListener(ValidListener l) {
        this.validListeners.remove(l);
    }

    protected synchronized final void fireValidChange() {
        // ATTN called very often during a select() (for each SQLObject empty & value change)
        final boolean validated = this.isValidated();
        for (final ValidListener l : this.validListeners) {
            l.validChange(this.getPanel(), validated);
        }
    }

    final boolean isValidated() {
        // si la liste est vide, elle est valide
        boolean res = true;
        final Iterator<SQLRowItemView> iter = this.getItems().iterator();
        while (iter.hasNext() && res) {
            final SQLRowItemView v = iter.next();
            res = v.isValidated();
        }
        return res;
    }

}
