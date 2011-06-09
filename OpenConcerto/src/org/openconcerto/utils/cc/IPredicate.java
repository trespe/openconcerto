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
 
 package org.openconcerto.utils.cc;

import org.apache.commons.collections.Predicate;

public abstract class IPredicate<E> implements Predicate {

    private static final IPredicate<Object> truePred = new IPredicate<Object>() {
        @Override
        public boolean evaluateChecked(Object input) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    public static final <N> IPredicate<N> truePredicate() {
        return (IPredicate<N>) truePred;
    }

    @SuppressWarnings("unchecked")
    public boolean evaluate(Object object) {
        return this.evaluateChecked((E) object);
    }

    public abstract boolean evaluateChecked(E input);

}
