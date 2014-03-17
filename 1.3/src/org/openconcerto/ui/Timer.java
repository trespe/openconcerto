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
 * A thread that takes a runnable that it will run (in the EDT) at varying intervalls.
 * 
 * @author Sylvain CUAZ
 */
public abstract class Timer extends Thread {
    protected final Runnable r;
    private final Runnable finalizer;
    private boolean stop;
    private int delay;
    private final int min;
    private final int accel;

    public Timer(final Runnable r, int delay) {
        this(r, delay, 0, delay);
    }

    public Timer(final Runnable r, int delay, int accel, int min) {
        this(r, delay, accel, min, null);
    }

    public Timer(final Runnable r, int delay, int accel, int min, final Runnable finalizer) {
        super("timer on " + r + " initial delay: " + delay + " created " + System.currentTimeMillis());
        this.stop = false;

        if (delay <= 0)
            throw new IllegalArgumentException("delay must be strictly positive");
        this.delay = delay;
        if (accel < 0)
            throw new IllegalArgumentException("accel must be positive");
        this.accel = accel;
        if (min <= 0)
            throw new IllegalArgumentException("min must be strictly positive");
        this.min = min;

        this.r = r;
        this.finalizer = finalizer;
    }

    public final void run() {
        while (!this.canceled()) {
            this.invokeTask();
            try {
                this.waitForTask();
                Thread.sleep(this.delay);
                if (this.delay > this.min) {
                    this.delay -= this.accel;
                    if (this.delay < this.min)
                        this.delay = this.min;
                }
            } catch (InterruptedException e) {
                // si on est interrompu, on s'arrête
                this.cancel();
            }
        }
        if (this.finalizer != null)
            SwingUtilities.invokeLater(this.finalizer);
    }

    /**
     * Should launch r.
     */
    protected abstract void invokeTask();

    /**
     * If desired, this method can wait or to finish running.
     * 
     * @throws InterruptedException if the wait is inerrupted.
     */
    protected abstract void waitForTask() throws InterruptedException;

    /**
     * After this method returns r will not be called again. And this thread will die (after
     * invoking a finalizer if specified).
     */
    public final synchronized void cancel() {
        if (!this.canceled()) {
            this.stop = true;
            this.interrupt();
        }
    }

    final synchronized boolean canceled() {
        // this.isInterrupted() is necessary if we're interrupted outside the sleep()
        // cause a subsequent call to sleep() will NOT throw InterruptedException
        return this.stop || this.isInterrupted();
    }

}
