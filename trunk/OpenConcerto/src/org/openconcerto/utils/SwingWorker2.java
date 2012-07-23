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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import sun.awt.AppContext;
import sun.swing.AccumulativeRunnable;

/**
 * Like standard SwingWorker with the addition of {@link #setMaxWorkerThreads(int)}.
 * 
 * @see SwingWorker
 */
public abstract class SwingWorker2<T, V> implements RunnableFuture<T> {
    /**
     * number of worker threads.
     */
    private static int maxWorkerThreads = 10;

    /**
     * current progress.
     */
    private volatile int progress;

    /**
     * current state.
     */
    private volatile StateValue state;

    /**
     * everything is run inside this FutureTask. Also it is used as a delegatee for the Future API.
     */
    private final FutureTask<T> future;

    /**
     * all propertyChangeSupport goes through this.
     */
    private final PropertyChangeSupport propertyChangeSupport;

    /**
     * handler for {@code process} mehtod.
     */
    private AccumulativeRunnable<V> doProcess;

    /**
     * handler for progress property change notifications.
     */
    private AccumulativeRunnable<Integer> doNotifyProgressChange;

    private final AccumulativeRunnable<Runnable> doSubmit = getDoSubmit();

    /**
     * Values for the {@code state} bound property.
     * 
     * @since 1.6
     */
    public enum StateValue {
        /**
         * Initial {@code SwingWorker2} state.
         */
        PENDING,
        /**
         * {@code SwingWorker2} is {@code STARTED} before invoking {@code doInBackground}.
         */
        STARTED,

        /**
         * {@code SwingWorker2} is {@code DONE} after {@code doInBackground} method is finished.
         */
        DONE
    };

    /**
     * Constructs this {@code SwingWorker2}.
     */
    public SwingWorker2() {
        Callable<T> callable = new Callable<T>() {
            public T call() throws Exception {
                setState(StateValue.STARTED);
                return doInBackground();
            }
        };

        future = new FutureTask<T>(callable) {
            @Override
            protected void done() {
                doneEDT();
                setState(StateValue.DONE);
            }
        };

        state = StateValue.PENDING;
        propertyChangeSupport = new SwingWorker2PropertyChangeSupport(this);
        doProcess = null;
        doNotifyProgressChange = null;
    }

    /**
     * Defines the maximum number of threads the worker will use It MUST be called before the first
     * SwingWorker2 use
     * 
     * @param max the max number of threads
     * */
    public static void setMaxWorkerThreads(int max) {
        maxWorkerThreads = max;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     * 
     * <p>
     * Note that this method is executed only once.
     * 
     * <p>
     * Note: this method is executed in a background thread.
     * 
     * 
     * @return the computed result
     * @throws Exception if unable to compute a result
     * 
     */
    protected abstract T doInBackground() throws Exception;

    /**
     * Sets this {@code Future} to the result of computation unless it has been cancelled.
     */
    public final void run() {
        future.run();
    }

    /**
     * Sends data chunks to the {@link #process} method. This method is to be used from inside the
     * {@code doInBackground} method to deliver intermediate results for processing on the <i>Event
     * Dispatch Thread</i> inside the {@code process} method.
     * 
     * <p>
     * Because the {@code process} method is invoked asynchronously on the <i>Event Dispatch
     * Thread</i> multiple invocations to the {@code publish} method might occur before the
     * {@code process} method is executed. For performance purposes all these invocations are
     * coalesced into one invocation with concatenated arguments.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * publish(&quot;1&quot;);
     * publish(&quot;2&quot;, &quot;3&quot;);
     * publish(&quot;4&quot;, &quot;5&quot;, &quot;6&quot;);
     * </pre>
     * 
     * might result in:
     * 
     * <pre>
     * process(&quot;1&quot;, &quot;2&quot;, &quot;3&quot;, &quot;4&quot;, &quot;5&quot;, &quot;6&quot;)
     * </pre>
     * 
     * <p>
     * <b>Sample Usage</b>. This code snippet loads some tabular data and updates
     * {@code DefaultTableModel} with it. Note that it safe to mutate the tableModel from inside the
     * {@code process} method because it is invoked on the <i>Event Dispatch Thread</i>.
     * 
     * <pre>
     * class TableSwingWorker2 extends 
     *         SwingWorker2&lt;DefaultTableModel, Object[]&gt; {
     *     private final DefaultTableModel tableModel;
     * 
     *     public TableSwingWorker2(DefaultTableModel tableModel) {
     *         this.tableModel = tableModel;
     *     }
     * 
     *     {@code @Override}
     *     protected DefaultTableModel doInBackground() throws Exception {
     *         for (Object[] row = loadData(); 
     *                  ! isCancelled() &amp;&amp; row != null; 
     *                  row = loadData()) {
     *             publish((Object[]) row);
     *         }
     *         return tableModel;
     *     }
     * 
     *     {@code @Override}
     *     protected void process(List&lt;Object[]&gt; chunks) {
     *         for (Object[] row : chunks) {
     *             tableModel.addRow(row);
     *         }
     *     }
     * }
     * </pre>
     * 
     * @param chunks intermediate results to process
     * 
     * @see #process
     * 
     */
    protected final void publish(V... chunks) {
        synchronized (this) {
            if (doProcess == null) {
                doProcess = new AccumulativeRunnable<V>() {
                    @Override
                    public void run(List<V> args) {
                        process(args);
                    }

                    @Override
                    protected void submit() {
                        doSubmit.add(this);
                    }
                };
            }
        }
        doProcess.add(chunks);
    }

    /**
     * Receives data chunks from the {@code publish} method asynchronously on the <i>Event Dispatch
     * Thread</i>.
     * 
     * <p>
     * Please refer to the {@link #publish} method for more details.
     * 
     * @param chunks intermediate results to process
     * 
     * @see #publish
     * 
     */
    protected void process(List<V> chunks) {
    }

    /**
     * Executed on the <i>Event Dispatch Thread</i> after the {@code doInBackground} method is
     * finished. The default implementation does nothing. Subclasses may override this method to
     * perform completion actions on the <i>Event Dispatch Thread</i>. Note that you can query
     * status inside the implementation of this method to determine the result of this task or
     * whether this task has been cancelled.
     * 
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     */
    protected void done() {
    }

    /**
     * Sets the {@code progress} bound property. The value should be from 0 to 100.
     * 
     * <p>
     * Because {@code PropertyChangeListener}s are notified asynchronously on the <i>Event Dispatch
     * Thread</i> multiple invocations to the {@code setProgress} method might occur before any
     * {@code PropertyChangeListeners} are invoked. For performance purposes all these invocations
     * are coalesced into one invocation with the last invocation argument only.
     * 
     * <p>
     * For example, the following invokations:
     * 
     * <pre>
     * setProgress(1);
     * setProgress(2);
     * setProgress(3);
     * </pre>
     * 
     * might result in a single {@code PropertyChangeListener} notification with the value {@code 3}.
     * 
     * @param progress the progress value to set
     * @throws IllegalArgumentException is value not from 0 to 100
     */
    protected final void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        if (this.progress == progress) {
            return;
        }
        int oldProgress = this.progress;
        this.progress = progress;
        if (!getPropertyChangeSupport().hasListeners("progress")) {
            return;
        }
        synchronized (this) {
            if (doNotifyProgressChange == null) {
                doNotifyProgressChange = new AccumulativeRunnable<Integer>() {
                    @Override
                    public void run(List<Integer> args) {
                        firePropertyChange("progress", args.get(0), args.get(args.size() - 1));
                    }

                    @Override
                    protected void submit() {
                        doSubmit.add(this);
                    }
                };
            }
        }
        doNotifyProgressChange.add(oldProgress, progress);
    }

    /**
     * Returns the {@code progress} bound property.
     * 
     * @return the progress bound property.
     */
    public final int getProgress() {
        return progress;
    }

    /**
     * Schedules this {@code SwingWorker2} for execution on a <i>worker</i> thread. There are a
     * number of <i>worker</i> threads available. In the event all <i>worker</i> threads are busy
     * handling other {@code SwingWorker2s} this {@code SwingWorker2} is placed in a waiting queue.
     * 
     * <p>
     * Note: {@code SwingWorker2} is only designed to be executed once. Executing a
     * {@code SwingWorker2} more than once will not result in invoking the {@code doInBackground}
     * method twice.
     */
    public final void execute() {
        getWorkersExecutorService().execute(this);
    }

    // Future methods START
    /**
     * {@inheritDoc}
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDone() {
        return future.isDone();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: calling {@code get} on the <i>Event Dispatch Thread</i> blocks <i>all</i> events,
     * including repaints, from being processed until this {@code SwingWorker2} is complete.
     * 
     * <p>
     * When you want the {@code SwingWorker2} to block on the <i>Event Dispatch Thread</i> we
     * recommend that you use a <i>modal dialog</i>.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * class SwingWorker2CompletionWaiter extends PropertyChangeListener {
     *     private JDialog dialog;
     * 
     *     public SwingWorker2CompletionWaiter(JDialog dialog) {
     *         this.dialog = dialog;
     *     }
     * 
     *     public void propertyChange(PropertyChangeEvent event) {
     *         if (&quot;state&quot;.equals(event.getPropertyName()) &amp;&amp; SwingWorker2.StateValue.DONE == event.getNewValue()) {
     *             dialog.setVisible(false);
     *             dialog.dispose();
     *         }
     *     }
     * }
     * JDialog dialog = new JDialog(owner, true);
     * swingWorker.addPropertyChangeListener(new SwingWorker2CompletionWaiter(dialog));
     * swingWorker.execute();
     * // the dialog will be visible until the SwingWorker2 is done
     * dialog.setVisible(true);
     * </pre>
     */
    public final T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Please refer to {@link #get} for more details.
     */
    public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    // Future methods END

    // PropertyChangeSupports methods START
    /**
     * Adds a {@code PropertyChangeListener} to the listener list. The listener is registered for
     * all properties. The same listener object may be added more than once, and will be called as
     * many times as it is added. If {@code listener} is {@code null}, no exception is thrown and no
     * action is taken.
     * 
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     * 
     * @param listener the {@code PropertyChangeListener} to be added
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the listener list. This removes a
     * {@code PropertyChangeListener} that was registered for all properties. If {@code listener}
     * was added more than once to the same event source, it will be notified one less time after
     * being removed. If {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     * 
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     * 
     * @param listener the {@code PropertyChangeListener} to be removed
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is fired if {@code old}
     * and {@code new} are equal and non-null.
     * 
     * <p>
     * This {@code SwingWorker2} will be the source for any generated events.
     * 
     * <p>
     * When called off the <i>Event Dispatch Thread</i> {@code PropertyChangeListeners} are notified
     * asynchronously on the <i>Event Dispatch Thread</i>.
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     * 
     * 
     * @param propertyName the programmatic name of the property that was changed
     * @param oldValue the old value of the property
     * @param newValue the new value of the property
     */
    public final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Returns the {@code PropertyChangeSupport} for this {@code SwingWorker2}. This method is used
     * when flexible access to bound properties support is needed.
     * <p>
     * This {@code SwingWorker2} will be the source for any generated events.
     * 
     * <p>
     * Note: The returned {@code PropertyChangeSupport} notifies any {@code PropertyChangeListener}s
     * asynchronously on the <i>Event Dispatch Thread</i> in the event that
     * {@code firePropertyChange} or {@code fireIndexedPropertyChange} are called off the <i>Event
     * Dispatch Thread</i>.
     * 
     * @return {@code PropertyChangeSupport} for this {@code SwingWorker2}
     */
    public final PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    // PropertyChangeSupports methods END

    /**
     * Returns the {@code SwingWorker2} state bound property.
     * 
     * @return the current state
     */
    public final StateValue getState() {
        /*
         * DONE is a speacial case to keep getState and isDone is sync
         */
        if (isDone()) {
            return StateValue.DONE;
        } else {
            return state;
        }
    }

    /**
     * Sets this {@code SwingWorker2} state bound property.
     * 
     * @param state the state to set
     */
    private void setState(StateValue state) {
        StateValue old = this.state;
        this.state = state;
        firePropertyChange("state", old, state);
    }

    /**
     * Invokes {@code done} on the EDT.
     */
    private void doneEDT() {
        Runnable doDone = new Runnable() {
            public void run() {
                done();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            doDone.run();
        } else {
            doSubmit.add(doDone);
        }
    }

    /**
     * returns workersExecutorService.
     * 
     * returns the service stored in the appContext or creates it if necessary.
     * 
     * @return ExecutorService for the {@code SwingWorker2s}
     */
    private static synchronized ExecutorService getWorkersExecutorService() {
        final AppContext appContext = AppContext.getAppContext();
        ExecutorService executorService = (ExecutorService) appContext.get(SwingWorker2.class);
        if (executorService == null) {
            // this creates daemon threads.
            ThreadFactory threadFactory = new ThreadFactory() {
                final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                public Thread newThread(final Runnable r) {
                    Thread thread = defaultFactory.newThread(r);
                    thread.setName("SwingWorker2-" + thread.getName());
                    thread.setDaemon(true);
                    return thread;
                }
            };

            executorService = new ThreadPoolExecutor(maxWorkerThreads, maxWorkerThreads, 10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), threadFactory);

            appContext.put(SwingWorker2.class, executorService);

            // Don't use ShutdownHook here as it's not enough. We should track
            // AppContext disposal instead of JVM shutdown, see 6799345 for details
            final ExecutorService es = executorService;

            appContext.addPropertyChangeListener(AppContext.DISPOSED_PROPERTY_NAME, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent pce) {
                    boolean disposed = (Boolean) pce.getNewValue();
                    if (disposed) {
                        final WeakReference<ExecutorService> executorServiceRef = new WeakReference<ExecutorService>(es);
                        final ExecutorService executorService = executorServiceRef.get();
                        if (executorService != null) {
                            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                                public Void run() {
                                    executorService.shutdown();
                                    return null;
                                }
                            });
                        }
                    }
                }
            });
        }
        return executorService;
    }

    private static final Object DO_SUBMIT_KEY = new Object(); // doSubmit

    private static AccumulativeRunnable<Runnable> getDoSubmit() {
        synchronized (DO_SUBMIT_KEY) {
            final AppContext appContext = AppContext.getAppContext();
            Object doSubmit = appContext.get(DO_SUBMIT_KEY);
            if (doSubmit == null) {
                doSubmit = new DoSubmitAccumulativeRunnable();
                appContext.put(DO_SUBMIT_KEY, doSubmit);
            }
            return (AccumulativeRunnable<Runnable>) doSubmit;
        }
    }

    private static class DoSubmitAccumulativeRunnable extends AccumulativeRunnable<Runnable> implements ActionListener {
        private final static int DELAY = (int) (1000 / 30);

        @Override
        protected void run(List<Runnable> args) {
            for (Runnable runnable : args) {
                runnable.run();
            }
        }

        @Override
        protected void submit() {
            Timer timer = new Timer(DELAY, this);
            timer.setRepeats(false);
            timer.start();
        }

        public void actionPerformed(ActionEvent event) {
            run();
        }
    }

    private class SwingWorker2PropertyChangeSupport extends PropertyChangeSupport {
        SwingWorker2PropertyChangeSupport(Object source) {
            super(source);
        }

        @Override
        public void firePropertyChange(final PropertyChangeEvent evt) {
            if (SwingUtilities.isEventDispatchThread()) {
                super.firePropertyChange(evt);
            } else {
                doSubmit.add(new Runnable() {
                    public void run() {
                        SwingWorker2PropertyChangeSupport.this.firePropertyChange(evt);
                    }
                });
            }
        }
    }
}
