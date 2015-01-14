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
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

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
        if (this.dieCalled())
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
     * An exception was thrown by a task. This implementation merely
     * {@link Exception#printStackTrace()}.
     * 
     * @param exn the exception thrown.
     */
    protected void exceptionThrown(final ExecutionException exn) {
        exn.printStackTrace();
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

    public final void tasksDo(IClosure<? super Deque<FutureTask<?>>> c) {
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
        final Future<?> old;
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

    public boolean setSleeping(boolean sleeping) {
        final boolean res = this.tasksQueue.setSleeping(sleeping);
        if (res) {
            this.support.firePropertyChange("sleeping", null, this.isSleeping());
        }
        return res;
    }

    /**
     * Stops this queue. Once this method returns, it is guaranteed that no other task will be taken
     * from the queue to be started, and that this queue will die. But the already executing task
     * will complete unless it checks for interrupt.
     * 
     * @return the future killing.
     */
    public final Future<?> die() {
        return this.die(true, null, null);
    }

    /**
     * Stops this queue. Once the returned future completes successfully then no task is executing (
     * {@link #isDead()} will happen sometimes later, the time for the thread to terminate). If the
     * returned future throws an exception because of the passed runnables or of {@link #willDie()}
     * or {@link #dying()}, one can check with {@link #dieCalled()} to see if the queue is dying.
     * 
     * @param force <code>true</code> if this is guaranteed to die (even if <code>willDie</code> or
     *        {@link #willDie()} throw an exception).
     * @param willDie the last actions to take before killing this queue.
     * @param dying the last actions to take before this queue is dead.
     * @return the future killing, which will return <code>dying</code> result.
     * @see #dieCalled()
     */
    public final <V> Future<V> die(final boolean force, final Runnable willDie, final Callable<V> dying) {
        // reset sleeping to original value if die not effective
        final AtomicBoolean resetSleeping = new AtomicBoolean(false);
        final FutureTask<V> res = new FutureTask<V>(new Callable<V>() {
            @Override
            public V call() throws Exception {
                Exception willDieExn = null;
                try {
                    willDie();
                    if (willDie != null) {
                        willDie.run();
                        // handle Future like runnable, i.e. check right away for exception
                        if (willDie instanceof Future) {
                            final Future<?> f = (Future<?>) willDie;
                            assert f.isDone() : "Ran but not done: " + f;
                            try {
                                f.get();
                            } catch (ExecutionException e) {
                                throw (Exception) e.getCause();
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!force)
                        throw e;
                    else
                        willDieExn = e;
                }
                try {
                    // don't interrupt ourselves
                    SleepingQueue.this.tasksQueue.die(false);
                    assert SleepingQueue.this.tasksQueue.isDying();
                    // since there's already been an exception, throw it as soon as possible
                    // also dying() might itself throw an exception for the same reason or we now
                    // have 2 exceptions to throw
                    if (willDieExn != null)
                        throw willDieExn;
                    dying();
                    final V res;
                    if (dying != null)
                        res = dying.call();
                    else
                        res = null;

                    return res;
                } finally {
                    // if die is effective, this won't have any consequences
                    if (resetSleeping.get())
                        SleepingQueue.this.tasksQueue.setSleeping(true);
                }
            }
        });
        // die as soon as possible not after all currently queued tasks
        this.tasksQueue.itemsDo(new IClosure<Deque<FutureTask<?>>>() {
            @Override
            public void executeChecked(Deque<FutureTask<?>> input) {
                // since we cancel the current task, we might as well remove all of them since they
                // might depend on the cancelled one
                input.clear();
                input.addFirst(res);
                // die as soon as possible, even if there's a long task already running
                final FutureTask<?> beingRun = getBeingRun();
                // since we hold the lock on items
                assert beingRun != res : "beingRun: " + beingRun + " ; res: " + res;
                if (beingRun != null)
                    beingRun.cancel(true);
            }
        });
        // force execution of our task
        resetSleeping.set(this.setSleeping(false));
        return res;
    }

    protected void willDie() {
        // nothing by default
    }

    protected void dying() {
        // nothing by default
    }

    /**
     * Whether this will die. If this method returns <code>true</code>, it is guaranteed that no
     * other task will be taken from the queue to be started, and that this queue will die. But the
     * already executing task will complete unless it checks for interrupt. Note: this method
     * doesn't return <code>true</code> right after {@link #die()} as the method is asynchronous and
     * if {@link #willDie()} fails it may not die at all ; as explained in its comment you may use
     * its returned future to wait for the killing.
     * 
     * @return <code>true</code> if this queue will not execute any more tasks (but it may finish
     *         one last task).
     * @see #isDead()
     */
    public final boolean dieCalled() {
        return this.tasksQueue.dieCalled();
    }

    /**
     * Whether this queue is dead, i.e. if die() has been called and all tasks have completed.
     * 
     * @return <code>true</code> if this queue will not execute any more tasks and isn't executing
     *         any.
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
                // f was interrupted : e.g. we're dying or f was cancelled
            } catch (ExecutionException e) {
                // f.run() raised an exception
                exceptionThrown(e);
            }
        }
    }

    public String toString() {
        return super.toString() + " Queue: " + this.tasksQueue + " run:" + this.getBeingRun();
    }

}
