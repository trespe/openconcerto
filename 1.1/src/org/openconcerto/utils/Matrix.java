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

import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * A 2D matrix. Each row has its own length.
 * 
 * @author Sylvain
 * 
 * @param <T> type of items
 */
public final class Matrix<T> {
    private final List<List<T>> rows;
    private final T defaultItem;

    public Matrix() {
        this(8);
    }

    public Matrix(int initialHeight) {
        this(initialHeight, null);
    }

    /**
     * Create a new matrix. The default item will be used in 0,0 for example if you call
     * {@link #put(int, int, Object)} with 1,0 on an empty matrix.
     * 
     * @param initialCapacity the initial capacity of the rows.
     * @param defaultItem item to be used at defined points without value, can be <code>null</code>.
     */
    public Matrix(int initialCapacity, final T defaultItem) {
        this.rows = new ArrayList<List<T>>(initialCapacity);
        this.defaultItem = defaultItem;
    }

    public Matrix(final Matrix<? extends T> o) {
        this(o.rows.size(), o.defaultItem);
        for (final List<? extends T> l : o.rows)
            this.rows.add(new ArrayList<T>(l));
    }

    private final List<List<T>> createEmptyRows(int count) {
        final List<List<T>> res = new ArrayList<List<T>>(count);
        for (int i = 0; i < count; i++) {
            res.add(new ArrayList<T>());
        }
        return res;
    }

    public final int getHeight() {
        return this.rows.size();
    }

    public final int getWidth(int y) {
        if (y >= this.getHeight())
            throw new IndexOutOfBoundsException(y + " >=" + this.getHeight());
        return this.rows.get(y).size();
    }

    public final void put(int x, int y, T item) {
        if (this.rows.size() <= y)
            this.rows.addAll(createEmptyRows(y - this.rows.size() + 1));
        final List<T> row = this.rows.get(y);
        if (row.size() <= x)
            row.addAll(Collections.<T> nCopies(x - row.size() + 1, this.defaultItem));
        row.set(x, item);
    }

    public final T get(int x, int y) {
        if (this.rows.size() <= y)
            return null;
        final List<T> row = this.rows.get(y);
        if (row.size() <= x)
            return null;
        return row.get(x);
    }

    /**
     * Iterate on each point defined in this matrix. NOTE: if you call put(1,0, item) 0,0 will be
     * defined (with the default item).
     * 
     * @param c the closure which will passed the point and the value at that point.
     */
    public final void iterate(final IClosure<Tuple2<Point, T>> c) {
        final ListIterator<List<T>> iter1 = this.rows.listIterator();
        while (iter1.hasNext()) {
            final List<T> l = iter1.next();
            final ListIterator<T> iter2 = l.listIterator();
            while (iter2.hasNext()) {
                final T item = iter2.next();
                c.executeChecked(Tuple2.create(new Point(iter2.previousIndex(), iter1.previousIndex()), item));
            }
        }
    }

    public final void iteratePoints(final IClosure<Point> c) {
        this.iterate(new IClosure<Tuple2<Point, T>>() {
            @Override
            public void executeChecked(Tuple2<Point, T> input) {
                c.executeChecked(input.get0());
            }
        });
    }

    /**
     * Return a graphical representation (tabular) of this matrix.
     * 
     * @param cellLength the length of each cell if positive, if negative the values will be
     *        separated by tabs.
     * @param transf how to print each item, can be <code>null</code>.
     * @return the string representation.
     */
    public final String print(int cellLength, final ITransformer<T, ?> transf) {
        final String spaces;
        if (cellLength < 0)
            spaces = null;
        else {
            final char[] array = new char[cellLength];
            Arrays.fill(array, ' ');
            spaces = new String(array);
        }
        final StringBuilder sb = new StringBuilder();
        for (final List<T> row : this.rows) {
            for (final T item : row) {
                // item == null means no item, not the client put null
                final String s = item == null ? "" : (transf == null ? item : transf.transformChecked(item)).toString();
                if (spaces == null) {
                    sb.append(s);
                    sb.append('\t');
                } else {
                    if (s.length() < cellLength) {
                        sb.append(s);
                        sb.append(spaces.substring(s.length()));
                    } else if (s.length() == cellLength) {
                        sb.append(s.substring(0, cellLength));
                    } else {
                        // for values larger than the cell, signal it by using '...'
                        // plus we keep the beginning and the end by removing the middle part
                        final int middle = cellLength / 2;
                        sb.append(s.substring(0, middle));
                        sb.append('…');
                        sb.append(s.substring(s.length() - (cellLength - middle - 1), s.length()));
                    }
                    sb.append(' ');
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Matrix)
            return this.rows.equals(((Matrix) obj).rows);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.rows.hashCode();
    }

    @Override
    public String toString() {
        return this.print(20, null);
    }
}
