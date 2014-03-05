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

public abstract class Closure<E> implements IClosure<E>, ITransformer<E, Object>, org.apache.commons.collections.Closure {

    private static final IClosure<Object> nop = new IClosure<Object>() {
        @Override
        public void executeChecked(Object input) {
        }
    };

    @SuppressWarnings("unchecked")
    public static final <N> IClosure<N> nopClosure() {
        return (IClosure<N>) nop;
    }

    @SuppressWarnings("unchecked")
    public final void execute(Object input) {
        this.executeChecked((E) input);
    }

    public abstract void executeChecked(E input);

    public final Object transformChecked(E input) {
        this.executeChecked(input);
        return null;
    };

}
