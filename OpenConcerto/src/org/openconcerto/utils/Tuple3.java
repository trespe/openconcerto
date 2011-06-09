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

import java.util.Arrays;
import java.util.List;

/**
 * A simple class to hold 3 values in a type-safe manner.
 * 
 * @author Sylvain
 * 
 * @param <A> type of first value.
 * @param <B> type of second value.
 * @param <C> type of third value.
 */
public class Tuple3<A, B, C> extends Tuple2<A, B> {

    // just to make the code shorter
    public static final <A, B, C> Tuple3<A, B, C> create(A a, B b, C c) {
        return new Tuple3<A, B, C>(a, b, c);
    }

    private final C c;

    public Tuple3(A a, B b, C c) {
        super(a, b);
        this.c = c;
    }

    public final C get2() {
        return this.c;
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(get0(), get1(), get2());
    }
}
