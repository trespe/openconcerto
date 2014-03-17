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
 
 package org.openconcerto.utils.change;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

/**
 * Permet de signaler un changement dans une collection, et surtout de voir les éléments
 * ajoutés/rétirés/toujours là.
 * 
 * @author ILM Informatique 1 sept. 2004
 */
public class CollectionChangeEvent extends PropertyChangeEvent {

    /**
     * Crée un evenement avec les propriétés spécifiées. Ne pas oublier de cloner/dupliquer la
     * collection avant de la changer, pour pouvoir renseigner oldVal.
     * 
     * @param src la source.
     * @param propName le nom de la propriété.
     * @param oldVal the old value, must not be <code>null</code>.
     * @param newVal the new value, must not be <code>null</code>.
     * @return un nouveau PropertyChangeEvent.
     * @throws NullPointerException if oldVal of newVal is <code>null</code>.
     */
    static public CollectionChangeEvent create(Object src, String propName, Collection oldVal, Collection newVal) {
        if (oldVal == null || newVal == null)
            throw new NullPointerException();
        return new CollectionChangeEvent(src, propName, oldVal, newVal);
    }

    // *** Instance

    // use static
    protected CollectionChangeEvent(Object source, String propertyName, Collection oldValue, Collection newValue) {
        super(source, propertyName, oldValue, newValue);
    }

    public Collection getItemsAdded() {
        return CollectionUtils.subtract((Collection) this.getNewValue(), (Collection) this.getOldValue());
    }

    public Collection getItemsRemoved() {
        return CollectionUtils.subtract((Collection) this.getOldValue(), (Collection) this.getNewValue());
    }

    public Collection getItemsNotChanged() {
        return CollectionUtils.intersection((Collection) this.getNewValue(), (Collection) this.getOldValue());
    }

    /**
     * Does the new collection is the old one plus some items.
     * 
     * @return <code>true</code> if no items were removed.
     */
    public boolean isOnlyAddition() {
        return this.getItemsRemoved().size() == 0;
    }

    /**
     * Does the new collection is the old one minus some items.
     * 
     * @return <code>true</code> if no items were added.
     */
    public boolean isOnlyRemoval() {
        return this.getItemsAdded().size() == 0;
    }

    /**
     * Returns the indexes that have been added to a list. Items at theses indexes in the new list
     * were not part of the old one.
     * 
     * @return a List of Integer.
     */
    public List<Integer> getIndexesAdded() {
        if (!this.isOnlyAddition())
            throw new IllegalStateException("items were also removed");
        return this.getIndexesChanged();
    }

    /**
     * Returns the indexes that have been removed from a list. Items at theses indexes in the old
     * list aren't part of the new one anymore.
     * 
     * @return a List of Integer.
     */
    public List<Integer> getIndexesRemoved() {
        if (!this.isOnlyRemoval())
            throw new IllegalStateException("items were also added");
        return this.getIndexesChanged();
    }

    /**
     * Returns the list of intervals added.
     * 
     * @return a List of int[2], inclusive.
     */
    public List<int[]> getIntervalsAdded() {
        return org.openconcerto.utils.CollectionUtils.aggregate(this.getIndexesAdded());
    }

    /**
     * Returns the list of intervals removed.
     * 
     * @return a List of int[2], inclusive.
     */
    public List<int[]> getIntervalsRemoved() {
        return org.openconcerto.utils.CollectionUtils.aggregate(this.getIndexesRemoved());
    }

    private List<Integer> getIndexesChanged() {
        if (!(this.getNewValue() instanceof List))
            throw new IllegalStateException("the values must be List");
        return org.openconcerto.utils.CollectionUtils.getIndexesChanged((List) this.getOldValue(), (List) this.getNewValue());
    }

}
