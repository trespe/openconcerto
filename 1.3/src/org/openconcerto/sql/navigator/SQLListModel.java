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

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.utils.EnumOrderedSet;
import org.openconcerto.utils.SortDirection;
import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;

public abstract class SQLListModel<T> extends AbstractListModel {

    private static final EnumOrderedSet<SortDirection> ALL = EnumOrderedSet.allOf(SortDirection.class);

    // displayed items
    private final List<T> items;
    // complete list (unsearched)
    private final List<T> initialItems;
    // search
    private SearchSpec search;
    // sort
    private SortDirection sortAscending;
    private final Comparator<T> comp;

    private Set<SQLRow> highlighted;

    private final Set<Number> ids;

    private final PropertyChangeSupport supp;
    private boolean updating;

    public SQLListModel() {
        this.supp = new PropertyChangeSupport(this);
        this.updating = false;
        this.items = new ArrayList<T>();
        this.initialItems = new ArrayList<T>();
        this.search = null;
        this.sortAscending = SortDirection.NONE;
        this.comp = new Comparator<T>() {
            public int compare(T o1, T o2) {
                return SQLListModel.this.toString(o1).compareTo(SQLListModel.this.toString(o2));
            }
        };
        this.highlighted = Collections.emptySet();
        this.ids = new HashSet<Number>();

        // our caller knows better how to load us (in general by calling setParentIDs() which is
        // more efficient than reload())
    }

    protected final void reload() {
        this.reload(false);
    }

    protected abstract void reload(final boolean noCache);

    // *** items

    public final int getSize() {
        return this.items.size();
    }

    public final T getElementAt(int index) {
        return this.items.get(index);
    }

    protected final List<T> getDisplayedItems() {
        return this.items;
    }

    /**
     * Returns all non-virtual items, eg without the All at the begining.
     * 
     * @return a sublist of items.
     */
    protected final List<T> getRealItems() {
        if (this.hasALLValue())
            return this.getDisplayedItems().subList(1, this.getDisplayedItems().size());
        else
            return this.getDisplayedItems();
    }

    protected final void setAll(List<T> items) {
        this.getInitialItems().clear();
        this.getInitialItems().addAll(items);
        this.computeItems();
    }

    protected final List<T> getInitialItems() {
        return this.initialItems;
    }

    /**
     * Select some items for this model.
     * 
     * @param displayed whether to search in the currently displayed items or in the complete list.
     * @param pred the items to select, <code>null</code> meaning all.
     * @return the selected items.
     */
    final List<T> selectItems(final boolean displayed, final IPredicate<T> pred) {
        final List<T> l = displayed ? this.getDisplayedItems() : this.getInitialItems();
        if (pred == null)
            return l;
        final List<T> res = new ArrayList<T>();
        for (int i = 0; i < l.size(); i++) {
            final T row = l.get(i);
            if (!this.isALLValue(row) && pred.evaluateChecked(row))
                res.add(row);
        }
        return res;
    }

    /**
     * Trie les éléments selon {@link #toString(Object)}. Chaque appel change l'ordre du tri.
     */
    public final void sort() {
        this.sortAscending = ALL.getFrom(this.sortAscending, 1, true);
        this.computeItems();
    }

    public final SortDirection getSortDirection() {
        return this.sortAscending;
    }

    // *** search

    public void setSearchString(final String text) {
        this.setSearch(new SearchSpec() {

            public boolean isEmpty() {
                return text == null || text.length() == 0;
            }

            public boolean match(Object line) {
                return line.toString().toLowerCase().indexOf(text.toLowerCase()) != -1;
            }
        });

    }

    public final void setSearch(final SearchSpec search) {
        this.search = search;
        this.computeItems();
    }

    public final SearchSpec getSearch() {
        return this.search;
    }

    // compute displayed items from initialItems (influenced by initialItems, sortAscending and
    // search)
    private void computeItems() {
        this.setUpdating(true);

        final int oldSize = this.getSize();
        this.items.clear();
        this.items.addAll(this.filter(this.getInitialItems()));
        if (this.sortAscending == SortDirection.ASCENDING)
            Collections.sort(this.items, this.comp);
        else if (this.sortAscending == SortDirection.DESCENDING) {
            Collections.sort(this.items, Collections.reverseOrder(this.comp));
        }
        if (this.hasALLValue())
            this.items.add(0, this.getALLValue());

        // fireContentsChanged() doesn't work : in BasicListUI.Handler.contentsChanged()
        // getSelectionModel() is never notified, so the selection doesn't change,
        // and getSelectedItem() can thus throw IndexOutOfBoundsException
        this.fireIntervalRemoved(this, 0, oldSize);
        this.fireIntervalAdded(this, 0, this.getSize());

        this.supp.firePropertyChange("items", null, this.items);
        this.setUpdating(false);
    }

    private final List<T> filter(List<T> l) {
        if (this.search == null || this.search.isEmpty()) {
            return l;
        } else {
            final List<T> res = new ArrayList<T>();
            for (final T value : l) {
                if (this.search.match(this.toString(value)))
                    res.add(value);
            }
            return res;
        }
    }

    public final void setParentIDs(Collection<? extends Number> ids) {
        // assure that ids is of same type that this.ids (otherwise equals() is false)
        if (!this.ids.equals(new HashSet<Number>(ids))) {
            this.ids.clear();
            this.ids.addAll(ids);
            this.idsChanged();
        }
    }

    protected final Set<Number> getIds() {
        return this.ids;
    }

    protected abstract void idsChanged();

    protected abstract String toString(T item);

    // *** highlight

    void setHighlighted(Set<SQLRow> rows) {
        if (!this.highlighted.equals(rows)) {
            this.highlighted = rows;
            this.fireContentsChanged(this, 0, this.getSize());
        }
    }

    Set<SQLRow> getHighlighted() {
        return this.highlighted;
    }

    int getFirstHighlighted() {
        if (this.getHighlighted().size() > 0) {
            for (int i = 0; i < this.items.size(); i++) {
                final Object item = this.items.get(i);
                if (this.getHighlighted().contains(item))
                    return i;
            }
        }
        return -1;
    }

    // *** ALL

    public final boolean isALLValue(T val) {
        return this.hasALLValue() && val == this.getALLValue();
    }

    protected boolean hasALLValue() {
        return false;
    }

    protected T getALLValue() {
        return null;
    }

    // *** boolean

    public synchronized final boolean isUpdating() {
        return this.updating;
    }

    private synchronized void setUpdating(final boolean updating) {
        final boolean old = this.updating;
        if (old != updating) {
            this.updating = updating;
            this.supp.firePropertyChange("updating", old, this.updating);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(PropertyChangeListener l, String propName) {
        this.supp.addPropertyChangeListener(propName, l);
    }

    public void rmPropertyChangeListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public void rmPropertyChangeListener(PropertyChangeListener l, String propName) {
        this.supp.removePropertyChangeListener(propName, l);
    }

}
