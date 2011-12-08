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
 
 package org.openconcerto.ui;

import javax.swing.SwingUtilities;

/**
 * A timer whose interval begins after r ends.
 * 
 * <pre>
 *  &lt;- r -&gt;              &lt;- r -&gt;
 * </pre>
 * <pre>
 *         &lt;- interval -&gt;       &lt;- interval -&gt;
 * </pre>
 * 
 * @author Sylvain CUAZ
 */
public class DelayTimer extends Timer {

    private final QRunnable qr;

    public DelayTimer(Runnable r, int delay) {
        this(r, delay, 0, delay);
    }

    public DelayTimer(Runnable r, int delay, int accel, int min) {
        this(r, delay, accel, min, null);
    }

    public DelayTimer(Runnable r, int delay, int accel, int min, Runnable finalizer) {
        super(r, delay, accel, min, finalizer);
        this.qr = new QRunnable(this.r);
    }

    protected void invokeTask() {
        SwingUtilities.invokeLater(this.qr);
    }

    protected void waitForTask() throws InterruptedException {
        this.qr.join();
    }

    public class QRunnable implements Runnable {

        private final Runnable r;
        private boolean done;

        public QRunnable(Runnable r) {
            this.r = r;
            this.done = false;
        }

        public final void run() {
            if (canceled())
                return;

            synchronized (this) {
                this.done = false;
            }
            try {
                this.r.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                synchronized (this) {
                    this.done = true;
                    this.notifyAll();
                }
            }
        }

        public synchronized void join() throws InterruptedException {
            while (true) {
                if (this.done) {
                    return;
                } else {
                    this.wait();
                }
            }
        }

    }

}
