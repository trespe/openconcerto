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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * Un comboBoxModel qui utilise une liste, et possède un addAll().
 * 
 * @author Sylvain CUAZ
 * @param <T> type of items
 */
public class DefaultIListModel<T> extends AbstractListModel implements IListModel<T> {

   protected final List<T> objects;

    /**
     * Constructs an empty DefaultComboBoxModel object.
     */
    public DefaultIListModel() {
        this(Collections.<T> emptyList());
    }

    public DefaultIListModel(T[] v) {
        this(Arrays.asList(v));
    }

    /**
     * Constructs a DefaultComboBoxModel object initialized with a vector.
     * 
     * @param v a Vector object ...
     */
    public DefaultIListModel(Collection<? extends T> v) {
        this.objects = new ArrayList<T>(v);
    }

    // implements javax.swing.ListModel
    public int getSize() {
        return this.objects.size();
    }

    // implements javax.swing.ListModel
    public T getElementAt(int index) {
        if (index >= 0 && index < this.objects.size())
            return this.objects.get(index);
        else
            return null;
    }

    public List<T> getList() {
        return Collections.unmodifiableList(this.objects);
    }

}
