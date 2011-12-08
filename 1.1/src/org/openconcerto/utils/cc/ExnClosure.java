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

/**
 * Closure able to throw an exception.
 * 
 * @author Sylvain
 * 
 * @param <E> input type
 * @param <X> exception type
 */
public abstract class ExnClosure<E, X extends Exception> extends ExnTransformer<E, Object, X> {

    public final void execute(Object input) {
        this.transform(input);
    }

    /**
     * Execute this closure, making sure that an exception of type <code>exnClass</code> is
     * thrown.
     * 
     * @param <Y> type of exception to throw.
     * @param input the input of the closure.
     * @param exnClass class exception to throw.
     * @throws Y if {@link #executeChecked(Object)} throws an exception, it will be wrapped (if
     *         necessary) in an exception of class <code>exnClass</code>.
     */
    public final <Y extends Exception> void executeCheckedWithExn(E input, Class<Y> exnClass) throws Y {
        this.transformCheckedWithExn(input, exnClass);
    }

    @Override
    public final Object transformChecked(E input) throws X {
        this.executeChecked(input);
        return null;
    }

    public abstract void executeChecked(E input) throws X;
}
