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

import java.util.Collection;
import java.util.List;

/**
 * A utility class that take a snapshot of a collection when created, and then construct a
 * CollectionChangeEvent.
 * 
 * @author Sylvain CUAZ
 */
public abstract class CollectionChangeEventCreator {
    private final Object src;
    private final String propName;
    private final Collection oldVal;

    /**
     * A creator which delegates cloning to subclass.
     * 
     * @param src the source.
     * @param propName the property name.
     * @param oldVal the old collection to be cloned.
     * @throws NullPointerException if oldVal is null.
     * @throws IllegalStateException if no cloning takes place.
     */
    protected CollectionChangeEventCreator(Object src, String propName, Collection oldVal) {
        if (oldVal == null)
            throw new NullPointerException();

        this.src = src;
        this.propName = propName;
        this.oldVal = clone(oldVal);
        if (this.oldVal == oldVal)
            throw new IllegalStateException("oldVal has not been cloned : " + oldVal + " == " + this.oldVal);
        if (!this.oldVal.equals(oldVal))
            throw new IllegalStateException("clones are not equal : " + oldVal + " != " + this.oldVal);
    }

    protected abstract Collection clone(Collection col);

    /**
     * Creates a CollectionChangeEvent between getOld() and newVal.
     * 
     * @param newVal the new value of the collection passed to the constructor.
     * @return a CollectionChangeEvent between getOld() and newVal.
     */
    public CollectionChangeEvent create(Collection newVal) {
        // clone newVal, to allow this event to be used asynchroniously
        return new CollectionChangeEvent(this.src, this.propName, this.oldVal, clone(newVal));
    }

    public IListDataEvent create(List newVal, int type, int index0, int index1) {
        return new IListDataEvent(this.create(newVal), type, index0, index1);
    }

    public final Collection getOld() {
        return this.oldVal;
    }

    public final String getName() {
        return this.propName;
    }
}
