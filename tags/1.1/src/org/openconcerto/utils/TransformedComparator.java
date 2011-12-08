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

import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;

import java.util.Comparator;

import org.apache.commons.collections.ComparatorUtils;

/**
 * A comparator that transforms before comparing.
 * 
 * @author Sylvain
 * 
 * @param <E> the type of the objects before being transformed.
 * @param <T> the type of the objects after being transformed.
 */
public class TransformedComparator<E, T> implements Comparator<E> {

    public static final <T> TransformedComparator<T, T> from(final Comparator<T> comp) {
        return new TransformedComparator<T, T>(Transformer.<T> nopTransformer(), comp);
    }

    private final ITransformer<E, T> transf;
    private final Comparator<T> comp;

    @SuppressWarnings("unchecked")
    public TransformedComparator(final ITransformer<E, T> transf) {
        this(transf, ComparatorUtils.NATURAL_COMPARATOR);
    }

    public TransformedComparator(final ITransformer<E, T> transf, final Comparator<T> comp) {
        super();
        this.transf = transf;
        this.comp = comp;
    }

    public int compare(E o1, E o2) {
        return this.comp.compare(this.transf.transformChecked(o1), this.transf.transformChecked(o2));
    }
}
