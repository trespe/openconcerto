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

import java.util.List;
import java.util.Vector;

public class RunnableQueue {
    private Thread th = null;
    private final Vector<Runnable> list = new Vector<Runnable>();
    boolean stop = false;

    public RunnableQueue() {
        checkThread();
    }

    public void invokeLater(Runnable runnable) {
        synchronized (list) {
            this.list.add(runnable);
            this.list.notifyAll();
        }
    }

    private void checkThread() {
        if (th == null) {
            createThread();
        }
    }

    /**
     * 
     */
    private void createThread() {
        th = new Thread(new Runnable() {

            public void run() {
                List<Runnable> toRemove = new Vector<Runnable>();
                while (!stop) {
                    for (int i = 0; i < list.size(); i++) {

                        Runnable element;
                        synchronized (list) {
                            element = list.get(i);
                        }
                        element.run();
                        toRemove.add(element);

                    }
                    synchronized (list) {
                        for (Runnable runnable : toRemove) {
                            list.remove(runnable);

                        }
                        toRemove.clear();
                    }
                    try {
                        synchronized (list) {
                            list.wait();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

}
