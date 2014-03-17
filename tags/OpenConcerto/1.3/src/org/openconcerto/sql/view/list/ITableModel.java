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
 
 package org.openconcerto.sql.view.list;

import static org.openconcerto.sql.view.list.ITableModel.SleepState.AWAKE;
import static org.openconcerto.sql.view.list.ITableModel.SleepState.HIBERNATING;
import static org.openconcerto.sql.view.list.ITableModel.SleepState.SLEEPING;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.users.rights.TableAllRights;
import org.openconcerto.sql.users.rights.UserRights;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.list.search.SearchQueue;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.TableSorter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * A model that takes its values from a ListRequest. The request lines can be searched using
 * {@link #search(SearchSpec)}. Like all Swing model, it ought too be manipulated in the EDT. ATTN
 * as soon as nobody listens to an instance (using addTableModelListener()) it dies and cannot be
 * used again.
 * 
 * @author Sylvain CUAZ
 */
public class ITableModel extends AbstractTableModel {
    public static enum SleepState {
        /**
         * The model processes events as they arrive.
         */
        AWAKE,
        /**
         * The events are queued to be executed when {@link #AWAKE}.
         */
        SLEEPING,
        /**
         * The model is sleeping plus its list is emptied to release memory.
         */
        HIBERNATING
    }

    private static Timer autoHibernateTimer = null;
    // not editable since default editors are potentially not safe (no validation)
    private static boolean defaultEditable = false;

    public static Timer getAutoHibernateTimer() {
        if (autoHibernateTimer == null)
            autoHibernateTimer = new Timer(ITableModel.class.getSimpleName() + " auto-hibernate timer", true);
        return autoHibernateTimer;
    }

    public static void setDefaultEditable(boolean defaultEditable) {
        ITableModel.defaultEditable = defaultEditable;
    }

    /**
     * Return the line of a JTable at the passed index, handling {@link TableSorter}.
     * 
     * @param m the model of a JTable.
     * @param row an index in the JTable.
     * @return the line at <code>row</code>.
     */
    public static ListSQLLine getLine(final TableModel m, int row) {
        if (m instanceof ITableModel)
            return ((ITableModel) m).getRow(row);
        else if (m instanceof TableSorter) {
            final TableSorter sorter = (TableSorter) m;
            return getLine(sorter.getTableModel(), sorter.modelIndex(row));
        } else
            throw new IllegalArgumentException("neither ITableModel nor TableSorter : " + m);
    }

    // comment remplir la table
    private final SQLTableModelLinesSource linesSource;
    private final List<String> colNames;
    private final PropertyChangeListener colListener;
    // la liste des lignes
    private final List<ListSQLLine> liste;
    // si on est en train de maj liste
    private boolean updating;
    private boolean filledOnce;

    private final PropertyChangeSupport supp;
    private List<TableModelListener> fullListeners;

    private final UpdateQueue updateQ;
    private boolean loading;
    // sleep state
    private SleepState wantedState;
    private SleepState actualState;
    private int hibernateDelay;
    private TimerTask autoHibernate;
    // number of runnables needing our queue to be awake
    private final AtomicInteger runSleep;
    private final SearchQueue searchQ;
    private boolean searching;
    private final MoveQueue moveQ;

    // whether we should allow edition
    private boolean editable;
    private boolean debug;

    public ITableModel(SQLTableModelSource src) {
        this.supp = new PropertyChangeSupport(this);
        this.fullListeners = new LinkedList<TableModelListener>();

        this.liste = new ArrayList<ListSQLLine>(100);
        this.updating = false;
        this.filledOnce = false;

        this.editable = defaultEditable;
        this.debug = false;

        // don't use CopyUtils.copy() since this prevent the use of anonymous inner class
        this.linesSource = src.createLinesSource(this);
        this.colNames = new ArrayList<String>();
        this.colListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateColNames();
            }
        };
        this.getReq().addColumnListener(this.colListener);
        updateColNames();

        this.updateQ = new UpdateQueue(this);
        this.loading = false;
        this.updateQ.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("beingRun")) {
                    final boolean isLoading = UpdateQueue.isUpdate((FutureTask<?>) evt.getNewValue());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(isLoading);
                        }
                    });
                }
            }
        });
        this.actualState = SleepState.AWAKE;
        this.wantedState = this.actualState;
        this.setHibernateDelay(30);
        this.autoHibernate = null;
        this.runSleep = new AtomicInteger(0);
        this.searchQ = new SearchQueue(new ListAccess(this));
        this.searching = false;
        this.searchQ.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("beingRun")) {
                    final boolean isSearching = SearchQueue.isSearch((FutureTask<?>) evt.getNewValue());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setSearching(isSearching);
                        }
                    });
                }
            }
        });
        this.moveQ = new MoveQueue(this);

        this.updateAll();
    }

    void print(String s) {
        print(s, Level.FINE);
    }

    void print(String s, Level l) {
        Log.get().log(l, this.getTable() + " " + this.hashCode() + " : " + s);
    }

    /**
     * The passed runnable will be run in the EDT after all current actions in the queue have
     * finished.
     * 
     * @param r the runnable to run in Swing.
     */
    public void invokeLater(final Runnable r) {
        if (r == null)
            return;
        this.runnableAdded();
        this.updateQ.put(new Runnable() {
            public void run() {
                try {
                    getSearchQueue().put(new Runnable() {
                        public void run() {
                            SwingUtilities.invokeLater(r);
                        }
                    });
                } finally {
                    runnableCompleted();
                }
            }
        });
    }

    // *** refresh

    final UpdateQueue getUpdateQ() {
        return this.updateQ;
    }

    /**
     * Recharge toutes les lignes depuis la base.
     */
    public void updateAll() {
        this.updateQ.putUpdateAll();
    }

    /**
     * If there's a where not on the primary table, the list doesn't know which lines to refresh and
     * it must reload all lines.
     * 
     * @param b <code>true</code> if the list shouldn't search for lines to refresh, but just reload
     *        all of them.
     */
    public final void setAlwaysUpdateAll(final boolean b) {
        this.getUpdateQ().setAlwaysUpdateAll(b);
    }

    // *** change list
    // none are synchronized since, they all are called from the EDT

    // liste is sorted
    void setList(List<ListSQLLine> liste) {
        this.setUpdating(true);
        this.liste.clear();
        this.liste.addAll(liste);
        this.filledOnce = true;
        print("liste filled : " + this.liste.size());
        this.fireTableDataChanged();
        this.setUpdating(false);
    }

    void addToList(ListSQLLine modifiedLine) {
        this.setUpdating(true);

        this.liste.add(modifiedLine);
        Collections.sort(this.liste);
        final int index = this.indexFromID(modifiedLine.getID());
        this.fireTableRowsInserted(index, index);

        this.setUpdating(false);
    }

    // modifiedLine match : it must be displayed
    void fullListChanged(ListSQLLine modifiedLine, final Collection<Integer> modifiedFields) {
        this.setUpdating(true);

        final int index = this.indexFromID(modifiedLine.getID());
        final boolean orderChanged;
        if (index >= 0) {
            this.liste.set(index, modifiedLine);
            final boolean afterPred;
            if (index > 0)
                afterPred = modifiedLine.compareTo(this.liste.get(index - 1)) > 0;
            else
                afterPred = true;
            final boolean beforeSucc;
            if (index < this.liste.size() - 1)
                beforeSucc = modifiedLine.compareTo(this.liste.get(index + 1)) < 0;
            else
                beforeSucc = true;
            orderChanged = !(afterPred && beforeSucc);
        } else {
            this.liste.add(modifiedLine);
            orderChanged = true;
        }
        if (orderChanged) {
            Collections.sort(this.liste);
            this.fireTableDataChanged();
        } else {
            if (modifiedFields == null)
                this.fireTableRowsUpdated(index, index);
            else
                for (final Integer i : modifiedFields) {
                    this.fireTableCellUpdated(index, i);
                }
        }

        this.setUpdating(false);
    }

    void removeFromList(int id) {
        this.setUpdating(true);

        final int index = this.indexFromID(id);
        // si la ligne n'existe pas, rien à faire
        if (index >= 0) {
            this.liste.remove(index);
            this.fireTableRowsDeleted(index, index);
        }

        this.setUpdating(false);
    }

    @Override
    public void fireTableChanged(TableModelEvent e) {
        // only fire for currently displaying cells
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS || e.getColumn() < this.getColumnCount()) {
            super.fireTableChanged(e);
        } else {
            for (final TableModelListener l : this.fullListeners) {
                l.tableChanged(e);
            }
        }
    }

    // *** tableModel

    protected void updateColNames() {
        this.setUpdating(true);

        // getColumnNames() used to take more than 20% of SearchRunnable.matchFilter(), so cache it.
        this.colNames.clear();
        for (final SQLTableModelColumn col : getCols())
            this.colNames.add(this.isDebug() ? col.getName() + " " + col.getPaths().toString() : col.getName());
        this.fireTableStructureChanged();

        this.setUpdating(false);
    }

    public List<String> getColumnNames() {
        return this.colNames;
    }

    private List<? extends SQLTableModelColumn> getCols() {
        return this.isDebug() ? this.getReq().getAllColumns() : this.getReq().getColumns();
    }

    public int getRowCount() {
        return this.liste.size();
    }

    /**
     * The total number of lines fetched. Equals to {@link #getRowCount()} if there's no search.
     * 
     * @return the total number of lines, or 0 if the first fill hasn't completed.
     */
    public int getTotalRowCount() {
        return this.getSearchQueue().getFullListSize();
    }

    // pas besoin de synch les méthode ne se servant que des colonnes, elles ne changent pas

    public int getColumnCount() {
        return this.getColumnNames().size();
    }

    public String getColumnName(int columnIndex) {
        // handle null names (as opposed to .toString())
        return String.valueOf(this.getColumnNames().get(columnIndex));
    }

    public Class<?> getColumnClass(int columnIndex) {
        return this.getReq().getColumn(columnIndex).getValueClass();
    }

    public final void setEditable(boolean b) {
        this.editable = b;
    }

    public final boolean isEditable() {
        return this.editable;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (!this.editable)
            return false;
        final SQLTableModelColumn col = this.getReq().getColumn(columnIndex);
        // hasRight is expensive so put it last
        return col.isEditable() && !isReadOnly(rowIndex) && hasRight(col);
    }

    private boolean isReadOnly(int rowIndex) {
        final SQLRowValues r = getRow(rowIndex).getRow();
        return r.getTable().contains(SQLComponent.READ_ONLY_FIELD) && SQLComponent.isReadOnly(r);
    }

    private boolean hasRight(final SQLTableModelColumn col) {
        final UserRights u = UserRightsManager.getCurrentUserRights();
        for (final SQLTable t : new SQLFieldsSet(col.getFields()).getTables()) {
            if (!TableAllRights.hasRight(u, TableAllRights.MODIFY_ROW_TABLE, t))
                return false;
        }
        return true;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= this.getRowCount())
            throw new IllegalArgumentException("!!!+ acces a la ligne :" + rowIndex + " et la taille est de:" + this.getRowCount());
        return getRow(rowIndex).getList(columnIndex + 1).get(columnIndex);
    }

    public final ListSQLLine getRow(int rowIndex) {
        return this.liste.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getRow(rowIndex).setValueAt(aValue, columnIndex);
    }

    // *** ids

    /**
     * Retourne l'ID de la ligne index.
     * 
     * @param index la ligne dont on veut l'ID.
     * @return l'ID de la ligne voulue, ou -1 si index n'est pas valide.
     */
    public int idFromIndex(int index) {
        if (index >= 0 && this.liste.size() > index)
            return getRow(index).getID();
        else
            return -1;
    }

    /**
     * Retourne l'index de la ligne d'ID voulue.
     * 
     * @param id l'id recherché.
     * @return l'index de la ligne correspondante, ou -1 si non trouvé.
     */
    public int indexFromID(int id) {
        return ListSQLLine.indexFromID(this.liste, id);
    }

    /**
     * The lines affected by the passed row.
     * 
     * @param t the table.
     * @param id an ID in <code>t</code>.
     * @return all affected lines in the fullList (un-searched).
     */
    public CollectionMap<ListSQLLine, Path> getAffectedLines(final SQLTable t, final int id) {
        return this.getSearchQueue().getAffectedLines(new SQLRow(t, id));
    }

    // *** search

    final SearchQueue getSearchQueue() {
        return this.searchQ;
    }

    /**
     * Effectue une recherche.
     * 
     * @param list description de la recherche à effectuer.
     * @param r sera exécuté dans la queue de recherche une fois <code>list</code> recherchée.
     */
    public synchronized void search(SearchSpec list, Runnable r) {
        this.getSearchQueue().setSearch(list);
        if (r != null)
            this.getSearchQueue().put(r);
    }

    public void search(SearchSpec list) {
        this.search(list, null);
    }

    // *** move

    public void moveBy(final List<? extends SQLRowAccessor> rows, final int inc) {
        this.moveQ.move(rows, inc);
    }

    public void moveTo(final List<? extends SQLRowAccessor> rows, final int rowIndex) {
        this.moveQ.moveTo(rows, rowIndex);
    }

    /**
     * Search the row which is <code>inc</code> lines from rowID.
     * 
     * @param rowID an ID of a row of this table.
     * @param inc the offset of visible lines.
     * @return the destination line or <code>null</code> if it's the same as <code>rowID</code> or
     *         <code>rowID</code> is inexistant.
     */
    ListSQLLine getDestLine(int rowID, int inc) {
        final int rowIndex = this.indexFromID(rowID);
        if (rowIndex < 0)
            return null;
        int destIndex = rowIndex + inc;
        final int min = 0;
        final int max = this.getRowCount() - 1;
        if (destIndex < min)
            destIndex = min;
        else if (destIndex > max)
            destIndex = max;
        if (destIndex != rowIndex) {
            return this.getRow(destIndex);
        } else
            return null;
    }

    // *** boolean

    /**
     * Whether this model has been filled at least once. Allow to differentiate between request has
     * not yet executed and request returned no rows.
     * 
     * @return <code>true</code> if the rows reflect the database.
     */
    public final boolean filledOnce() {
        return this.filledOnce;
    }

    public synchronized final boolean isUpdating() {
        return this.updating;
    }

    // signify that the program is making a change not the user
    // i.e. should be called before and after every fireTable*()
    private synchronized void setUpdating(boolean searching) {
        final boolean old = this.updating;
        if (old != searching) {
            this.updating = searching;
            this.supp.firePropertyChange("updating", old, this.updating);
        }
    }

    private void setLoading(boolean isLoading) {
        // keep the value in an attribute since we are invoked later in EDT and by that time
        // the updateQueue might be doing something else and isLoading() could never return true
        final boolean old = this.loading;
        if (old != isLoading) {
            this.loading = isLoading;
            this.supp.firePropertyChange("loading", old, this.loading);
        }
    }

    public final boolean isLoading() {
        return this.loading;
    }

    private void setSearching(boolean searching) {
        final boolean old = this.searching;
        if (old != searching) {
            this.searching = searching;
            this.supp.firePropertyChange("searching", old, this.searching);
        }
    }

    public final boolean isSearching() {
        return this.searching;
    }

    // when the model is sleeping, no more updates are performed
    void setSleeping(boolean sleeping) {
        this.setSleeping(sleeping ? SleepState.SLEEPING : SleepState.AWAKE);
    }

    void setSleeping(SleepState state) {
        synchronized (this.runSleep) {
            this.wantedState = state;
            this.sleepUpdated();
        }
    }

    /**
     * Set the number of seconds between reaching the {@link SleepState#SLEEPING} state and setting
     * the {@link SleepState#HIBERNATING} state.
     * 
     * @param seconds the number of seconds, less than 0 to disable automatic hibernating.
     */
    public final void setHibernateDelay(int seconds) {
        this.hibernateDelay = seconds;
    }

    private void runnableAdded() {
        synchronized (this.runSleep) {
            this.runSleep.incrementAndGet();
            this.sleepUpdated();
        }
    }

    protected void runnableCompleted() {
        synchronized (this.runSleep) {
            this.runSleep.decrementAndGet();
            this.sleepUpdated();
        }
    }

    private void sleepUpdated() {
        // set to null to do nothing
        final SleepState res;
        // if there's a user runnable we must wake up
        if (this.runSleep.get() > 0)
            res = AWAKE;
        // else we can go where we want
        else if (this.wantedState == this.actualState)
            res = null;
        else if (this.actualState == AWAKE) {
            // no need to test for runSleep
            // we cannot go from AWAKE directly to HIBERNATING
            res = SleepState.SLEEPING;
        } else if (this.actualState == SLEEPING) {
            res = this.wantedState;
        } else if (this.actualState == HIBERNATING) {
            // we cannot go from HIBERNATING to SLEEPING, since we are empty
            // besides we are already sleeping
            res = this.wantedState == AWAKE ? this.wantedState : null;
        } else
            throw new IllegalStateException("unknown state: " + this.actualState);

        if (res != null)
            this.setActual(res);
    }

    private void setActual(SleepState state) {
        if (this.actualState != state) {
            print("changing state " + this.actualState + " => " + state);
            this.actualState = state;

            if (this.autoHibernate != null)
                this.autoHibernate.cancel();

            switch (this.actualState) {
            case AWAKE:
                this.updateQ.setSleeping(false);
                break;
            case SLEEPING:
                this.updateQ.setSleeping(true);
                if (this.hibernateDelay >= 0) {
                    this.autoHibernate = new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                setSleeping(HIBERNATING);
                            } catch (Exception e) {
                                // never let an exception pass, otherwise the timer thread will die,
                                // and the *static* timer will become unusable for everyone
                                // OK to ignore setSleeping() since it's merely an optimization
                                print("HIBERNATING failed : " + e.getMessage(), Level.WARNING);
                                e.printStackTrace();
                            }
                        }
                    };
                    getAutoHibernateTimer().schedule(this.autoHibernate, this.hibernateDelay * 1000);
                }
                break;
            case HIBERNATING:
                this.updateQ.putRemoveAll();
                break;
            }

            this.sleepUpdated();
        }
    }

    public final SQLTableModelLinesSource getLinesSource() {
        return this.linesSource;
    }

    public final SQLTableModelSource getReq() {
        return this.linesSource.getParent();
    }

    public final SQLTable getTable() {
        return this.getReq().getPrimaryTable();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(final String propName, PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(propName, l);
    }

    public void rmPropertyChangeListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public void rmPropertyChangeListener(final String propName, PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(propName, l);
    }

    public final boolean isDebug() {
        return this.debug;
    }

    /**
     * Set the debug mode : add keys to the normal columns and use the field names for the column
     * names.
     * 
     * @param debug <code>true</code> to enable the debug mode.
     */
    public final void setDebug(boolean debug) {
        this.debug = debug;
        this.updateColNames();
    }

    public String toString() {
        return this.getClass().getSimpleName() + "@" + this.hashCode() + " for " + this.getTable();
    }

    @Override
    public synchronized void addTableModelListener(TableModelListener l) {
        this.addTableModelListener(l, false);
    }

    /**
     * Adds a listener that's notified each time a change to the data model occurs.
     * 
     * @param l the listener.
     * @param full if <code>true</code> <code>l</code> will be notified even if the data changed
     *        isn't displayed ({@link #setDebug(boolean) debug columns}).
     */
    public synchronized void addTableModelListener(TableModelListener l, final boolean full) {
        if (this.isDead())
            throw new IllegalStateException("dead tableModel: " + this);
        if (full)
            this.fullListeners.add(l);
        super.addTableModelListener(l);
    }

    @Override
    public synchronized void removeTableModelListener(TableModelListener l) {
        this.fullListeners.remove(l);
        super.removeTableModelListener(l);
        // nobody listens to us so we die
        if (this.listenerList.getListenerCount() == 0) {
            print("dying");
            if (this.autoHibernate != null)
                this.autoHibernate.cancel();
            this.updateQ.die();
            this.getSearchQueue().die();
            this.moveQ.die();
            this.getLinesSource().die();
            this.getReq().rmColumnListener(this.colListener);
        }
    }

    private synchronized final boolean isDead() {
        // in ctor queue can be null, but we're obviously not dead
        return this.updateQ != null && this.updateQ.isDead();
    }

}
