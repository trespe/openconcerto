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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread factory that appends a counter to threads' names.
 * 
 * @author Sylvain CUAZ
 */
public class ThreadFactory implements java.util.concurrent.ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String name;
    private final Boolean daemon;
    private Integer priority = null;

    public ThreadFactory(String name) {
        this(name, null);
    }

    public ThreadFactory(String name, Boolean daemon) {
        super();
        this.name = name;
        this.daemon = daemon;
    }

    public final ThreadFactory setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread res = new Thread(r);
        res.setName(this.name + this.threadNumber.getAndIncrement());
        if (this.daemon != null)
            res.setDaemon(this.daemon.booleanValue());
        if (this.priority != null)
            res.setPriority(this.priority.intValue());
        return res;
    }
}
