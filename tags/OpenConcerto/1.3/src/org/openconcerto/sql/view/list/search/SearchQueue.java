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
 
 package org.openconcerto.sql.view.list.search;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.LineListener;
import org.openconcerto.sql.view.list.ListAccess;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.IFutureTask;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.SleepingQueue;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public final class SearchQueue extends SleepingQueue {

    /**
     * Whether the passed future performs a search.
     * 
     * @param f a task in this queue, can be <code>null</code>.
     * @return <code>true</code> if <code>f</code> searches.
     */
    public static boolean isSearch(FutureTask<?> f) {
        return (f instanceof IFutureTask) && ((IFutureTask<?>) f).getRunnable() instanceof SearchRunnable;
    }

    /**
     * The last referent step of the passed path.
     * 
     * @param p a path.
     * @return the name of the field of the last referent step, <code>null</code> if it doesn't
     *         exist.
     */
    public static String getLastReferentField(final Path p) {
        final Step lastStep = p.length() == 0 ? null : p.getStep(-1);
        final boolean lastIsForeign = lastStep == null || lastStep.getDirection() == Direction.FOREIGN;
        return lastIsForeign ? null : lastStep.getSingleField().getName();
    }

    private final ITableModel model;
    SearchSpec search;
    private final List<ListSQLLine> fullList;
    private final ListAccess listAccess;
    private final LineListener lineListener;

    public SearchQueue(final ListAccess la) {
        super(SearchQueue.class.getName() + " on " + la.getModel());
        this.listAccess = la;
        this.model = la.getModel();
        this.search = null;
        this.fullList = new ArrayList<ListSQLLine>();

        this.lineListener = new LineListener() {
            @Override
            public void lineChanged(int id, ListSQLLine l, Set<Integer> colIndex) {
                changeFullList(id, l, colIndex);
            }
        };
        this.getModel().getLinesSource().addLineListener(this.lineListener);
    }

    @Override
    protected void dying() {
        super.dying();
        this.getModel().getLinesSource().rmLineListener(this.lineListener);
    }

    /**
     * The lines and their path affected by a change of the passed row.
     * 
     * @param r the row that has changed.
     * @return the refreshed lines and their changed paths.
     */
    public CollectionMap<ListSQLLine, Path> getAffectedLines(final SQLRow r) {
        return this.execGetAffected(r, new CollectionMap<ListSQLLine, Path>(), true);
    }

    public CollectionMap<Path, ListSQLLine> getAffectedPaths(final SQLRow r) {
        return this.execGetAffected(r, new CollectionMap<Path, ListSQLLine>(), false);
    }

    private <K, V> CollectionMap<K, V> execGetAffected(final SQLRow r, final CollectionMap<K, V> res, final boolean byLine) {
        return this.execute(new Callable<CollectionMap<K, V>>() {
            @Override
            public CollectionMap<K, V> call() throws Exception {
                return getAffected(r, res, byLine);
            }
        });
    }

    /**
     * Executes <code>c</code> in this queue, blocking the current thread.
     * 
     * @param <R> type of result
     * @param c what to do.
     * @return the result of <code>c</code>.
     */
    private <R> R execute(final Callable<R> c) {
        try {
            return this.execute(new FutureTask<R>(c)).get();
        } catch (InterruptedException e) {
            throw new RTInterruptedException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    // must be called from within this queue, as this method use fullList
    private <K, V> CollectionMap<K, V> getAffected(final SQLRow r, CollectionMap<K, V> res, boolean byLine) {
        final SQLTable t = r.getTable();
        final int id = r.getID();
        if (id < SQLRow.MIN_VALID_ID)
            throw new IllegalArgumentException("invalid ID: " + id);
        if (!this.fullList.isEmpty()) {
            final SQLRowValues proto = this.getModel().getLinesSource().getParent().getMaxGraph();
            final List<Path> pathsToT = new ArrayList<Path>();
            proto.getGraph().walk(proto, pathsToT, new ITransformer<State<List<Path>>, List<Path>>() {
                @Override
                public List<Path> transformChecked(State<List<Path>> input) {
                    if (input.getCurrent().getTable() == t) {
                        input.getAcc().add(input.getPath());
                    }
                    return input.getAcc();
                }
            }, RecursionType.BREADTH_FIRST, Direction.ANY);
            for (final Path p : pathsToT) {
                final String lastReferentField = getLastReferentField(p);
                for (final ListSQLLine line : this.fullList) {
                    boolean put = false;
                    for (final SQLRowValues current : line.getRow().followPath(p, CreateMode.CREATE_NONE, false)) {
                        // works for rowValues w/o any ID
                        if (current != null && current.getID() == id) {
                            put = true;
                        }
                    }
                    // if the modified row isn't in the existing line, it might still affect it if
                    // it's a referent row insertion
                    if (!put && lastReferentField != null && r.exists()) {
                        final int foreignID = r.getInt(lastReferentField);
                        for (final SQLRowValues current : line.getRow().followPath(p.minusLast(), CreateMode.CREATE_NONE, false)) {
                            if (current.getID() == foreignID) {
                                put = true;
                            }
                        }
                    }
                    if (put) {
                        // add to the list of paths that have been refreshed
                        if (byLine)
                            res.put(line, p);
                        else
                            res.put(p, line);
                    }
                }
            }
        }
        return res;
    }

    private synchronized void changeFullList(final int id, final ListSQLLine modifiedLine, final Collection<Integer> modifiedCols) {
        final SearchOne oneSearchRunnable = new SearchOne(this, id, modifiedLine, modifiedCols);
        this.putTask(new ChangeListOne("changeFullList " + id + " newLine: " + modifiedLine, this, modifiedLine, id, oneSearchRunnable));
        this.putTask(oneSearchRunnable);
    }

    public synchronized void setFullList(final List<ListSQLLine> l) {
        if (l == null)
            throw new NullPointerException();
        this.putTask(new ChangeListAll("setFullList", this, l));
        fullDataChange();
    }

    public synchronized void setSearch(final SearchSpec s) {
        this.putTask(new Runnable() {
            public void run() {
                SearchQueue.this.search = s;
            }
        });
        fullDataChange();
    }

    private synchronized void fullDataChange() {
        this.clearCompute();
        this.putTask(new SearchAll(this));
    }

    private synchronized void putTask(final Runnable r) {
        this.execute(new IFutureTask<Object>(r, null));
    }

    private synchronized void clearCompute() {
        this.cancel(new IPredicate<FutureTask<?>>() {
            @Override
            public boolean evaluateChecked(FutureTask<?> f) {
                return isSearch(f);
            }
        });
    }

    public String toString() {
        return this.getClass().getName() + " for " + this.getModel();
    }

    final SearchSpec getSearch() {
        return this.search;
    }

    final List<ListSQLLine> getFullList() {
        return this.fullList;
    }

    final ListAccess getAccess() {
        return this.listAccess;
    }

    public final int getFullListSize() {
        return this.fullList.size();
    }

    public final ITableModel getModel() {
        return this.model;
    }
}
