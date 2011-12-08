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
 
 package org.openconcerto.utils.model;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.change.CollectionChangeEventCreator;
import org.openconcerto.utils.change.ConstructorCreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Default implementation for IMutableListModel. Note: for removal operations IListDataEvent are
 * fired.
 * 
 * @author Sylvain CUAZ
 * 
 * @param <T> type of items
 */
public class DefaultIMutableListModel<T> extends DefaultIListModel<T> implements IMutableListModel<T> {

    private T selectedObject;
    private boolean selectOnAdd;
    private boolean selectOnRm;

    /**
     * Constructs an empty model.
     */
    public DefaultIMutableListModel() {
        this(Collections.<T> emptyList());
    }

    public DefaultIMutableListModel(T[] v) {
        this(Arrays.asList(v));
    }

    public DefaultIMutableListModel(Collection<? extends T> v) {
        super(v);
        this.selectOnAdd = true;
        this.selectOnRm = false;

        if (getSize() > 0) {
            this.selectedObject = getElementAt(0);
        }
    }

    // implements javax.swing.ComboBoxModel
    /**
     * Set the value of the selected item. The selected item may be null.
     * <p>
     * 
     * @param anObject The combo box value or null for no selection.
     */
    public void setSelectedItem(T anObject) {
        if ((this.selectedObject != null && !this.selectedObject.equals(anObject)) || this.selectedObject == null && anObject != null) {
            this.selectedObject = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    // implements javax.swing.ComboBoxModel
    public T getSelectedItem() {
        return this.selectedObject;
    }

    // implements javax.swing.MutableComboBoxModel
    public void addElement(T anObject) {
        this.objects.add(anObject);
        fireIntervalAdded(this, this.objects.size() - 1, this.objects.size() - 1);
        if (this.selectOnAdd && this.objects.size() == 1 && this.selectedObject == null && anObject != null) {
            setSelectedItem(anObject);
        }
    }

    public void addAll(Collection<? extends T> items) {
        this.addAll(null, items);
    }

    public void addAll(int index, Collection<? extends T> items) {
        this.addAll(Integer.valueOf(index), items);
    }

    private void addAll(Integer index, Collection<? extends T> items) {
        if (items.size() > 0) {
            if (index == null) {
                index = this.objects.size();
                this.objects.addAll(items);
            } else
                this.objects.addAll(index.intValue(), items);
            fireIntervalAdded(this, index, index + items.size() - 1);
            if (this.selectOnAdd && this.selectedObject == null) {
                setSelectedItem(items.iterator().next());
            }
        }
    }

    // implements javax.swing.MutableComboBoxModel
    public void insertElementAt(T anObject, int index) {
        this.objects.add(index, anObject);
        fireIntervalAdded(this, index, index);
    }

    // implements javax.swing.MutableComboBoxModel
    public void removeElementAt(int index) {
        this.removeElementsAt(index, index);
    }

    // from, to inclusive : to remove the fifth item call with (5,5)
    public void removeElementsAt(final int from, final int to) {
        final CollectionChangeEventCreator c = createCreator();
        final int selectedIndex = this.objects.indexOf(this.selectedObject);
        if (selectedIndex >= from && selectedIndex <= to) {
            if (!this.selectOnRm)
                setSelectedItem(null);
            else if (from == 0) {
                // is this a removeAll
                setSelectedItem(getSize() == to + 1 ? null : getElementAt(to + 1));
            } else {
                setSelectedItem(getElementAt(from - 1));
            }
        }

        // sublist exclusive
        this.objects.subList(from, to + 1).clear();
        this.fireIntervalRemoved(from, to, c);
    }

    public void set(int index, T o) {
        final CollectionChangeEventCreator c = createCreator();
        this.objects.set(index, o);
        this.fireContentsChanged(index, index, c);
    }

    /**
     * Replace a slice of this list with <code>l</code>.
     * 
     * @param index0 the start of the slice to remove, inclusive.
     * @param index1 the end of the slice to remove, inclusive.
     * @param l the list to insert.
     */
    public void replace(int index0, int index1, Collection<? extends T> l) {
        final CollectionChangeEventCreator c = createCreator();
        this.objects.subList(index0, index1 + 1).clear();
        this.objects.addAll(index0, l);
        this.fireContentsChanged(index0, index1, c);
    }

    /**
     * Replace <code>old</code> by <code>o</code>.
     * 
     * @param old the object to be replaced.
     * @param o the object that will be set at the index of <code>old</code>.
     * @throws IndexOutOfBoundsException if this does not contain <code>old</code>.
     */
    public void replace(T old, T o) {
        this.set(this.getList().indexOf(old), o);
    }

    // implements javax.swing.MutableComboBoxModel
    public void removeElement(T anObject) {
        int index = this.objects.indexOf(anObject);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    public void removeAll(Collection<? extends T> items) {
        final SortedSet<Integer> indexes = new TreeSet<Integer>();
        for (final T item : items) {
            indexes.add(this.objects.indexOf(item));
        }
        for (final int[] interval : CollectionUtils.aggregate(indexes)) {
            removeElementsAt(interval[0], interval[1]);
        }
    }

    /**
     * Empties the list.
     */
    public void removeAllElements() {
        if (this.objects.size() > 0) {
            final CollectionChangeEventCreator c = createCreator();
            int firstIndex = 0;
            int lastIndex = this.objects.size() - 1;
            this.objects.clear();
            this.selectedObject = null;
            fireIntervalRemoved(firstIndex, lastIndex, c);
        } else {
            this.selectedObject = null;
        }
    }

    private ConstructorCreator createCreator() {
        return new ConstructorCreator(this, "items", this.objects);
    }

    protected void fireIntervalRemoved(int index0, int index1, CollectionChangeEventCreator c) {
        this.fire(ListDataEvent.INTERVAL_REMOVED, index0, index1, c);
    }

    protected void fireContentsChanged(int index0, int index1, CollectionChangeEventCreator c) {
        this.fire(ListDataEvent.CONTENTS_CHANGED, index0, index1, c);
    }

    protected void fire(int type, int index0, int index1, CollectionChangeEventCreator c) {
        Object[] listeners = this.listenerList.getListenerList();
        ListDataEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = c.create(this.objects, type, index0, index1);
                }
                switch (type) {
                case ListDataEvent.INTERVAL_REMOVED:
                    ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
                    break;
                case ListDataEvent.CONTENTS_CHANGED:
                    ((ListDataListener) listeners[i + 1]).contentsChanged(e);
                    break;
                case ListDataEvent.INTERVAL_ADDED:
                    ((ListDataListener) listeners[i + 1]).intervalAdded(e);
                    break;

                default:
                    throw new IllegalArgumentException("wrong type: " + type);
                }

            }
        }
    }

    public final boolean isSelectOnAdd() {
        return this.selectOnAdd;
    }

    /**
     * Sets whether an add() or addAll() will select the first added item if the current selection
     * is empty.
     * 
     * @param selectOnAdd <code>false</code> if the selection should not be changed.
     */
    public final void setSelectOnAdd(boolean selectOnAdd) {
        this.selectOnAdd = selectOnAdd;
    }

    /**
     * Sets whether the removal of the selected item will select the closest item left, or if the
     * selection will be empty.
     * 
     * @param selectOnRm <code>true</code> to select the closest, <code>false</code> to empty
     *        the selection.
     */
    public final void setSelectOnRm(boolean selectOnRm) {
        this.selectOnRm = selectOnRm;
    }
}
