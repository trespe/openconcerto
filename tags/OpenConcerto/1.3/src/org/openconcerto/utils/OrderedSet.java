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
 
 package org.openconcerto.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Un Set qui garde l'ordre... cad une List sans elements en double
 * 
 * @param <E> type of items
 */
public class OrderedSet<E> extends Vector<E> implements Set<E> {

    private final Set<E> set = new HashSet<E>();

    public OrderedSet(List<E> nodes) {
        this.addAll(nodes);
    }

    public OrderedSet() {
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#insertElementAt(java.lang.Object, int)
     */
    public synchronized void insertElementAt(E obj, int index) {
        if (!this.contains(obj)) {
            this.set.add(obj);
            super.insertElementAt(obj, index);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#add(java.lang.Object)
     */
    public synchronized boolean add(E o) {
        if (!this.contains(o)) {
            super.add(o);
            this.set.add(o);
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#addAll(java.util.Collection)
     */
    public synchronized boolean addAll(Collection<? extends E> c) {
        final int s = size();
        for (int i = 0; i < s; i++) {
            if (c.contains(this.get(i))) {
                return false;
            }
        }
        this.set.addAll(c);
        return super.addAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#addAll(int, java.util.Collection)
     */
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        final int s = size();
        for (int i = 0; i < s; i++) {
            if (c.contains(this.get(i))) {
                return false;
            }
        }
        this.set.addAll(c);
        return super.addAll(index, c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#addElement(java.lang.Object)
     */
    public synchronized void addElement(E obj) {
        this.add(obj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#contains(java.lang.Object)
     */
    public boolean contains(Object elem) {
        return this.set.contains(elem);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#remove(int)
     */
    public synchronized E remove(int index) {
        // TODO Auto-generated method stub
        return super.remove(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#removeAll(java.util.Collection)
     */
    public synchronized boolean removeAll(Collection c) {
        // TODO Auto-generated method stub
        return super.removeAll(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#removeAllElements()
     */
    public synchronized void removeAllElements() {
        // TODO Auto-generated method stub
        super.removeAllElements();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#removeElement(java.lang.Object)
     */
    public synchronized boolean removeElement(Object obj) {
        this.set.remove(obj);
        return super.removeElement(obj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#removeElementAt(int)
     */
    public synchronized void removeElementAt(int index) {
        // TODO Auto-generated method stub
        super.removeElementAt(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Vector#removeRange(int, int)
     */
    protected void removeRange(int fromIndex, int toIndex) {
        // TODO Auto-generated method stub
        super.removeRange(fromIndex, toIndex);
    }

}
