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
import org.openconcerto.utils.cc.IPredicate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * A queue that can be put to sleep. Submitted runnables are converted to FutureTask, that can later
 * be cancelled.
 * 
 * @author Sylvain
 */
public class SleepingQueue {

    private final String name;

    private final PropertyChangeSupport support;
    private FutureTask<?> beingRun;

    private final SingleThreadedExecutor tasksQueue;
    private boolean canceling;
    private IPredicate<FutureTask<?>> cancelPredicate;

    public SleepingQueue() {
        this(SleepingQueue.class.getName() + System.currentTimeMillis());
    }

    public SleepingQueue(String name) {
        super();
        this.name = name;

        this.canceling = false;
        this.cancelPredicate = null;
        this.support = new PropertyChangeSupport(this);
        this.setBeingRun(null);

        this.tasksQueue = new SingleThreadedExecutor();

        this.tasksQueue.start();
    }

    /**
     * Customize the thread used to execute the passed runnables. This implementation sets the
     * priority to the minimum.
     * 
     * @param thr the thread used by this queue.
     */
    protected void customizeThread(Thread thr) {
        thr.setPriority(Thread.MIN_PRIORITY);
    }

    public final FutureTask<?> put(Runnable workRunnable) {
        if (this.shallAdd(workRunnable)) {
            final IFutureTask<Object> t = this.tasksQueue.newTaskFor(workRunnable);
            this.add(t);
            return t;
        } else
            return null;

    }

    public final <F extends FutureTask<?>> F execute(F t) {
        if (this.shallAdd(t)) {
            this.add(t);
            return t;
        } else
            return null;
    }

    private void add(FutureTask<?> t) {
        // no need to synchronize, if die() is called after our test, t won't be executed anyway
        if (this.isDead())
            throw new IllegalStateException("Already dead, cannot exec " + t);

        this.tasksQueue.put(t);
    }

    private final boolean shallAdd(Runnable runnable) {
        if (runnable == null)
            throw new NullPointerException("null runnable");
        try {
            this.willPut(runnable);
            return true;
        } catch (InterruptedException e) {
            // si on interrompt, ne pas ajouter
            return false;
        }
    }

    /**
     * Give subclass the ability to reject runnables.
     * 
     * @param r the runnable that is being added.
     * @throws InterruptedException if r should not be added to this queue.
     */
    protected void willPut(Runnable r) throws InterruptedException {
    }

    /**
     * Cancel all queued tasks and the current task.
     */
    protected final void cancel() {
        this.cancel(null);
    }

    /**
     * Cancel only tasks for which pred is <code>true</code>.
     * 
     * @param pred a predicate to know which tasks to cancel.
     */
    protected final void cancel(final IPredicate<FutureTask<?>> pred) {
        this.tasksDo(new IClosure<Collection<FutureTask<?>>>() {
            @Override
            public void executeChecked(Collection<FutureTask<?>> tasks) {
                cancel(pred, tasks);
            }
        });
    }

    private final void cancel(IPredicate<FutureTask<?>> pred, Collection<FutureTask<?>> tasks) {
        try {
            synchronized (this) {
                this.canceling = true;
                this.cancelPredicate = pred;
                this.cancelCheck(this.getBeingRun());
            }

            for (final FutureTask<?> t : tasks) {
                this.cancelCheck(t);
            }
        } finally {
            synchronized (this) {
                this.canceling = false;
                // allow the predicate to be gc'd
                this.cancelPredicate = null;
            }
        }
    }

    public final void tasksDo(IClosure<? super BlockingDeque<FutureTask<?>>> c) {
        this.tasksQueue.itemsDo(c);
    }

    private void cancelCheck(FutureTask<?> t) {
        if (t != null)
            synchronized (this) {
                if (this.canceling && (this.cancelPredicate == null || this.cancelPredicate.evaluateChecked(t)))
                    t.cancel(true);
            }
    }

    private void setBeingRun(final FutureTask<?> beingRun) {
        final Future old;
        synchronized (this) {
            old = this.beingRun;
            this.beingRun = beingRun;
        }
        this.support.firePropertyChange("beingRun", old, beingRun);
    }

    protected final synchronized FutureTask<?> getBeingRun() {
        return this.beingRun;
    }

    public boolean isSleeping() {
        return this.tasksQueue.isSleeping();
    }

    public void setSleeping(boolean sleeping) {
        if (this.tasksQueue.setSleeping(sleeping)) {
            this.support.firePropertyChange("sleeping", null, this.isSleeping());
        }
    }

    /**
     * Stops this queue. Once this method returns, it is guaranteed that no other task will be taken
     * from the queue to be started, and that this thread will die.
     */
    public final void die() {
        this.tasksQueue.die();
        this.dying();
    }

    protected void dying() {
        // nothing by default
    }

    /**
     * Whether this queue is dying, ie if die() has been called.
     * 
     * @return <code>true</code> if this queue will not execute any more tasks.
     * @see #die()
     */
    public final boolean isDead() {
        return this.tasksQueue.isDead();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.support.addPropertyChangeListener(l);
    }

    public void rmPropertyChangeListener(PropertyChangeListener l) {
        this.support.removePropertyChangeListener(l);
    }

    private final class SingleThreadedExecutor extends DropperQueue<FutureTask<?>> {
        private SingleThreadedExecutor() {
            super(SleepingQueue.this.name + System.currentTimeMillis());
            customizeThread(this);
        }

        protected <T> IFutureTask<T> newTaskFor(final Runnable task) {
            return this.newTaskFor(task, null);
        }

        protected <T> IFutureTask<T> newTaskFor(final Runnable task, T value) {
            return new IFutureTask<T>(task, value, " for {" + SleepingQueue.this.name + "}");
        }

        @Override
        protected void process(FutureTask<?> task) {
            if (!task.isDone()) {
                /*
                 * From ThreadPoolExecutor : Track execution state to ensure that afterExecute is
                 * called only if task completed or threw exception. Otherwise, the caught runtime
                 * exception will have been thrown by afterExecute itself, in which case we don't
                 * want to call it again.
                 */
                boolean ran = false;
                beforeExecute(task);
                try {
                    task.run();
                    ran = true;
                    afterExecute(task, null);
                } catch (RuntimeException ex) {
                    if (!ran)
                        afterExecute(task, ex);
                    // don't throw ex, afterExecute() can do whatever needs to be done (like killing
                    // this queue)
                }
            }
        }

        protected void beforeExecute(final FutureTask<?> f) {
            cancelCheck(f);
            setBeingRun(f);
        }

        protected void afterExecute(final FutureTask<?> f, final Throwable t) {
            setBeingRun(null);

            try {
                f.get();
            } catch (CancellationException e) {
                // don't care
            } catch (InterruptedException e) {
                // f was interrupted : eg we're dying or f was canceled
            } catch (ExecutionException e) {
                // f.run() raised an exn
                e.printStackTrace();
            }
        }
    }

    public String toString() {
        return super.toString() + " Queue: " + this.tasksQueue + " run:" + this.getBeingRun();
    }

}
