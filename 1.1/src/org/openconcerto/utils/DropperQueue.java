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

import org.openconcerto.utils.cc.IClosure;

import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds items and give them one at a time to {@link #process(Object)}. At any time this process can
 * be put on hold by setting it to sleep.
 * 
 * @author Sylvain
 * @param <T> type of item
 */
public abstract class DropperQueue<T> extends Thread {

    private final BlockingDeque<T> items;
    private final Lock addLock;
    private boolean stop;
    private boolean sleeping;
    private boolean looping;
    private boolean executing;

    /**
     * Construct a new instance.
     * 
     * @param name name of this thread.
     */
    public DropperQueue(String name) {
        super(name);
        this.items = new LinkedBlockingDeque<T>();
        this.stop = false;
        this.sleeping = false;
        this.looping = false;
        this.executing = false;
        this.addLock = new ReentrantLock();
    }

    // *** boolean

    /**
     * Whether this queue should stop temporarily.
     * 
     * @param b <code>true</code> to put this to sleep, <code>false</code> to wake it.
     * @return <code>true</code> if sleeping has changed.
     */
    public synchronized final boolean setSleeping(boolean b) {
        if (this.sleeping != b) {
            this.sleeping = b;
            this.signalChange();
            return true;
        } else
            return false;
    }

    public synchronized boolean isSleeping() {
        return this.sleeping;
    }

    private synchronized final void setLooping(boolean b) {
        if (this.looping != b) {
            this.looping = b;
            this.signalChange();
        }
    }

    private synchronized void signalChange() {
        this.signalChange(false);
    }

    private synchronized void signalChange(boolean signalClosure) {
        // interrompt la thread, si elle est dans le await() ou le take()
        // elle recheckera les booleens, ATTN elle peut-être en attente du lock juste après take(),
        // c'est pourquoi on efface le flag avant process()
        // en général pas besoin d'interrompre la closure, puisque tant qu'on l'exécute
        // on ne peut, ni on n'a besoin de prendre depuis la queue
        if (signalClosure || !this.isExecuting())
            this.interrupt();
    }

    private synchronized void setExecuting(boolean b) {
        this.executing = b;
    }

    private synchronized boolean isExecuting() {
        return this.executing;
    }

    private void await() throws InterruptedException {
        synchronized (this) {
            if (this.sleeping || this.looping) {
                this.wait();
            }
        }
    }

    /**
     * Signal that this thread must stop definitely.
     */
    public synchronized final void die() {
        this.stop = true;
        this.signalChange(true);
    }

    public synchronized final boolean isDead() {
        return this.stop;
    }

    // *** Run

    @Override
    public void run() {
        while (!this.isDead()) {
            try {
                this.await();
                final T item = this.items.take();
                this.setExecuting(true);
                // we should not carry the interrupted status in process()
                // we only use it to stop waiting and check variables again, but if we're here we
                // have removed an item and must process it
                Thread.interrupted();
                process(item);
            } catch (InterruptedException e) {
                // rien a faire, on recommence la boucle
            } catch (RuntimeException e) {
                e.printStackTrace();
                // une exn s'est produite, on considère qu'on peut passer à la suite
            } finally {
                this.setExecuting(false);
            }
        }
    }

    abstract protected void process(final T item);

    // *** items

    /**
     * Adds an item to this queue.
     * 
     * @param item the item to add.
     */
    public final void put(T item) {
        this.addLock.lock();
        try {
            this.items.add(item);
        } finally {
            this.addLock.unlock();
        }
    }

    public final void eachItemDo(final IClosure<T> c) {
        this.itemsDo(new IClosure<Collection<T>>() {
            @Override
            public void executeChecked(Collection<T> items) {
                for (final T t : items) {
                    c.executeChecked(t);
                }
            }
        });
    }

    /**
     * Allows <code>c</code> to arbitrarily modify our queue as it is locked during this method.
     * I.e. no items will be removed (passed to the closure) nor added.
     * 
     * @param c what to do with our queue.
     */
    public final void itemsDo(IClosure<? super BlockingDeque<T>> c) {
        this.addLock.lock();
        this.setLooping(true);
        try {
            c.executeChecked(this.items);
        } finally {
            this.addLock.unlock();
            this.setLooping(false);
        }
    }

}
