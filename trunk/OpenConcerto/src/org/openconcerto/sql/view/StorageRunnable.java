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
 
 package org.openconcerto.sql.view;

abstract public class StorageRunnable implements Runnable {
    private Object obj;
    private boolean hasRun = false;

    public synchronized Object getResult() {
        this.waitForRun();
        return this.obj;
    }

    private synchronized void waitForRun() {
        while (true) {
            if (!this.hasRun) {
                try {
                    this.wait();
                } catch (InterruptedException exn) {
                    // si on m'interrompt, réattendre
                }
            } else {
                return;
            }
        }
    }

    public synchronized void run() {
        if (this.hasRun)
            throw new IllegalStateException("already ran");

        this.obj = this.get();
        this.hasRun = true;
        this.notifyAll();
    }

    protected abstract Object get();

}
