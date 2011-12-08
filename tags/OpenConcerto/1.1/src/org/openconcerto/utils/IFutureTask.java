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

import java.util.concurrent.FutureTask;

/**
 * A FutureTask with getRunnable().
 * 
 * @author Sylvain
 * 
 * @param <V> The result type returned by this FutureTask's <tt>get</tt> method
 */
public class IFutureTask<V> extends FutureTask<V> {

    private final Runnable runnable;
    private final String detail;

    public IFutureTask(Runnable runnable, V result) {
        this(runnable, result, "");
    }

    public IFutureTask(Runnable runnable, V result, final String detail) {
        super(runnable, result);
        this.runnable = runnable;
        this.detail = detail;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    @Override
    public String toString() {
        return (this.isCancelled() ? "[cancelled] " : "") + this.getClass().getSimpleName() + " running {" + this.getRunnable() + "}" + this.detail;
    }

}
