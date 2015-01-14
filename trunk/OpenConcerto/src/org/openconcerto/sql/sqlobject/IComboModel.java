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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.ComboSQLRequest.KeepMode;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.sql.view.search.SearchSpecUtils;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.ui.component.combo.Log;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.SwingWorker2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.model.DefaultIMutableListModel;
import org.openconcerto.utils.model.ISearchable;
import org.openconcerto.utils.model.NewSelection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.jcip.annotations.GuardedBy;

/**
 * A model that takes its values from a {@link ComboSQLRequest}. It listens to table changes, but
 * can also be reloaded by calling {@link #fillCombo()}. It can be searched using
 * {@link #search(SearchSpec)}. Like all Swing model, it ought too be manipulated in the EDT.
 * 
 * @author Sylvain CUAZ
 */
public class IComboModel extends DefaultIMutableListModel<IComboSelectionItem> implements SQLTableModifiedListener, MutableValueObject<IComboSelectionItem>, EmptyObj, ISearchable {

    private final ComboSQLRequest req;

    private boolean filledOnce = false;
    private ITransformer<List<IComboSelectionItem>, IComboSelectionItem> firstFillTransf = null;
    private boolean isADirtyDrityGirl = true;
    private boolean isOnScreen = false;
    private boolean sleepAllowed = true;
    @GuardedBy("this")
    private int requestDelay = 50;

    // supports
    private final EmptyChangeSupport emptySupp;
    private final PropertyChangeSupport propSupp;

    // est ce que la combo va se remplir, access must be synchronized
    private SwingWorker2<?, ?> willUpdate;
    protected final List<Runnable> runnables;
    // true from when the combo is filled with the sole "dots" item until it is loaded with actual
    // items, no need to synchronize (EDT)
    private boolean updating;
    // l'id à sélectionner à la fin du updateAll
    private int idToSelect;

    // index des éléments par leurs IDs
    private Map<Integer, IComboSelectionItem> itemsByID;

    @GuardedBy("this")
    private SearchSpec search;

    private PropertyChangeListener filterListener;
    // whether this is listening in order to self-update
    private boolean running;

    private boolean debug = false;
    private boolean addMissingItem;

    public IComboModel(final ComboSQLRequest req) {
        if (req == null)
            throw new NullPointerException("null request");
        this.req = req;

        this.emptySupp = new EmptyChangeSupport(this);
        this.propSupp = new PropertyChangeSupport(this);
        this.idToSelect = SQLRow.NONEXISTANT_ID;

        this.search = null;
        this.runnables = new ArrayList<Runnable>();
        this.willUpdate = null;
        this.updating = false;
        this.itemsByID = new HashMap<Integer, IComboSelectionItem>();
        this.addMissingItem = true;

        this.running = false;

        this.setSelectOnAdd(false);
        // we always change the selection after changing the items
        this.setOnRemovingOrReplacingSelection(NewSelection.NO);

        this.uiInit();
    }

    public final boolean neverBeenFilled() {
        return !this.filledOnce;
    }

    private final ITransformer<List<IComboSelectionItem>, IComboSelectionItem> getFirstFillSelection() {
        return this.firstFillTransf;
    }

    /**
     * Specify which item will be selected the first time the combo is filled (unless setValue() is
     * called before the fill).
     * 
     * @param firstFillTransf will be passed the items and should return the wanted selection.
     */
    public final void setFirstFillSelection(ITransformer<List<IComboSelectionItem>, IComboSelectionItem> firstFillTransf) {
        this.firstFillTransf = firstFillTransf;
    }

    // consider that undef means empty if the undefined row is not in the combo
    // otherwise treat it like any other row.
    private boolean isUndefIDEmpty() {
        return this.getRequest().getUndefLabel() == null;
    }

    private boolean isUndefIDEmpty(int id) {
        return isUndefIDEmpty() && (id == this.getRequest().getPrimaryTable().getUndefinedID());
    }

    private final void uiInit() {
        // listeners
        this.filterListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fillCombo();
            }
        };

        // ATTN this listener is notified last (see fireContentsChanged()) but this OK as it merely
        // fire and doesn't update our state
        this.addListDataListener(new ListDataListener() {
            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                if (e.getIndex0() != -1 || e.getIndex1() != -1) {
                    itemsChanged();
                } // else selection change
            }
        });
    }

    @Override
    protected void fireContentsChanged(Object source, int index0, int index1) {
        // Our superclass notifies the listeners in reverse order, i.e. our listener is notified
        // last. Thus if a listener access getWantedID() it will get the previous value. To avoid
        // that we overload the fire() to change our state before notifying other listeners.
        if (index0 == -1 && index1 == -1) {
            // selection change
            comboValueChanged();
        }

        super.fireContentsChanged(source, index0, index1);
    }

    void setRunning(final boolean b) {
        assert SwingUtilities.isEventDispatchThread();
        if (this.running != b) {
            this.running = b;
            if (this.running) {
                this.req.addTableListener(this);
                this.req.addWhereListener(this.filterListener);
                // since we weren't listening, we must have missed lots of things
                this.fillCombo();
            } else {
                this.req.removeTableListener(this);
                this.req.rmWhereListener(this.filterListener);
            }
        }
    }

    public final ComboSQLRequest getRequest() {
        return this.req;
    }

    public void setDebug(boolean trace) {
        this.debug = trace;
    }

    private void log(String s) {
        if (this.debug)
            Log.get().info(s);
    }

    synchronized void setOnScreen(boolean isOnScreen) {
        if (this.isOnScreen != isOnScreen) {
            this.isOnScreen = isOnScreen;
            if (this.isOnScreen && this.isADirtyDrityGirl) {
                this.fillCombo();
            }
        }
    }

    private synchronized boolean isOnScreen() {
        return this.isOnScreen;
    }

    /**
     * Whether this combo is allowed to delay {@link #fillCombo()} when it isn't visible.
     * 
     * @param sleepAllowed <code>true</code> if reloads can be delayed.
     */
    public final void setSleepAllowed(boolean sleepAllowed) {
        this.sleepAllowed = sleepAllowed;
    }

    public final boolean isSleepAllowed() {
        return this.sleepAllowed;
    }

    public synchronized final int getRequestDelay() {
        return this.requestDelay;
    }

    /**
     * Set the delay before the request is executed. I.e. if two {@link #fillCombo()} are less than
     * <code>delay</code> apart the first one won't be executed.
     * 
     * @param delay the delay in milliseconds.
     */
    public synchronized final void setRequestDelay(final int delay) {
        this.requestDelay = delay;
    }

    /**
     * Reload this combo. This method is thread-safe.
     */
    public synchronized final void fillCombo() {
        this.fillCombo(null, true);
    }

    public synchronized final void fillCombo(final Runnable r, final boolean readCache) {
        // wholly synch otherwise we might get onScreen after the if
        // and thus completely ignore that fillCombo()
        if (!this.isSleepAllowed() || this.isOnScreen() || r != null) {
            this.doUpdateAll(r, readCache);
        } else {
            this.isADirtyDrityGirl = true;
        }
    }

    private void updateAllBegun() {
        // need to be in EDT since we access selection and modify items
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateAllBegun();
                }
            });
        } else {
            log("entering updateAllBegun");
            assert !isUpdating() : "Otherwise our modeToSelect = DISABLED and setEnabled() would overwrite modeToSelect";
            this.setUpdating(true);

            // Like ITableModel, don't remove all items, so that if the request fails we still
            // keep old items (we used to have uiItems=true while setting the list to "Loading...")
        }
    }

    private void doUpdateAll(final Runnable r, final boolean readCache) {
        log("entering doUpdateAll");
        synchronized (this) {
            this.isADirtyDrityGirl = false;
            // déjà en train de se rafraîchir
            if (this.willUpdate != null) {
                this.willUpdate.cancel(true);
            } else {
                updateAllBegun();
            }
            // add the runnable to an attribute since the worker we are creating might be canceled
            // and thus done() and r might never be called
            if (r != null)
                this.runnables.add(r);
            final int delay = this.getRequestDelay();
            // copy the current search, if it changes fillCombo() will be called
            final SearchSpec search = this.getSearch();
            // commencer l'update après, sinon modeToSelect == 0
            final SwingWorker2<List<IComboSelectionItem>, Object> worker = new SwingWorker2<List<IComboSelectionItem>, Object>() {

                @Override
                protected List<IComboSelectionItem> doInBackground() throws InterruptedException {
                    // attends 1 peu pour voir si on va pas être annulé
                    Thread.sleep(delay);
                    return SearchSpecUtils.filter(IComboModel.this.req.getComboItems(readCache), search);
                }

                // Runs on the event-dispatching thread.
                @Override
                public void done() {
                    synchronized (IComboModel.this) {
                        // if cancel() is called after doInBackground() nothing happens
                        // but updating is set to a new instance
                        if (this.isCancelled() || IComboModel.this.willUpdate != this)
                            // une autre maj arrive
                            return;

                        final boolean firstFill = !IComboModel.this.filledOnce;
                        // store before removing since it can trigger a selection change
                        final int idToSelect = IComboModel.this.idToSelect;
                        List<IComboSelectionItem> items = null;
                        try {
                            items = this.get();
                            setAllItems(items);
                            IComboModel.this.filledOnce = true;
                        } catch (InterruptedException e) {
                            // ne devrait pas arriver puisque done() appelée après doInBackground()
                            e.printStackTrace();
                        } catch (CancellationException e) {
                            // ne devrait pas arriver puisqu'on teste isCancelled()
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            if (!(e.getCause() instanceof RTInterruptedException))
                                // pas normal
                                e.printStackTrace();
                        } finally {
                            // always clear willUpdate otherwise the combo can't recover
                            assert IComboModel.this.willUpdate == this;
                            IComboModel.this.setWillUpdate(null);
                        }
                        // check if items could be retrieved
                        // TODO otherwise show the error to the user so he knows that items are
                        // stale and he could reload them
                        if (items != null) {
                            // restaurer l'état
                            // if there's only one item in the list and no previous ID to select
                            // and org.openconcerto.sql.sqlCombo.selectSoleItem=true,select the item
                            final boolean noSelection = idToSelect == SQLRow.NONEXISTANT_ID;
                            if (items.size() == 1 && noSelection && Boolean.getBoolean("org.openconcerto.sql.sqlCombo.selectSoleItem"))
                                IComboModel.this.setSelectedItem(items.get(0));
                            else if (noSelection && firstFill && getFirstFillSelection() != null)
                                IComboModel.this.setSelectedItem(getFirstFillSelection().transformChecked(items));
                            else
                                selectID(idToSelect);

                            for (final Runnable r : IComboModel.this.runnables)
                                r.run();
                            IComboModel.this.runnables.clear();
                        }
                    }
                }
            };
            this.setWillUpdate(worker);
            worker.execute();
        }
    }

    // combo

    private DefaultIMutableListModel<IComboSelectionItem> getComboModel() {
        return this;
    }

    private void setAllItems(List<IComboSelectionItem> items) {
        // don't change the selection, the caller will
        this.getComboModel().setAllElements(items, false);
        this.itemsByID.clear();
        for (final IComboSelectionItem item : items)
            this.itemsByID.put(item.getId(), item);
    }

    private void addItem(IComboSelectionItem item) {
        this.getComboModel().addElement(item);
        this.itemsByID.put(item.getId(), item);
    }

    private void removeItem(final int id) {
        final IComboSelectionItem removed = this.itemsByID.remove(id);
        if (removed != null)
            this.getComboModel().removeElement(removed);
    }

    private IComboSelectionItem getComboItem(int id) {
        assert SwingUtilities.isEventDispatchThread();
        return this.itemsByID.get(id);
    }

    public final IComboSelectionItem getItem(int id) {
        final IComboSelectionItem privateItem = this.getComboItem(id);
        return privateItem == null ? null : new IComboSelectionItem(privateItem);
    }

    // refresh, delete or add the passed row
    private void reloadComboItem(final int id, final IComboSelectionItem nItem) {
        assert SwingUtilities.isEventDispatchThread();
        if (nItem == null) {
            removeItem(id);
        } else {
            final IComboSelectionItem item = this.getComboItem(id);
            // does this combo currently displays id
            if (item != null) {
                // before replace() which empties the selection
                final boolean selectedID = this.getSelectedId() == id;
                this.getComboModel().replace(item, nItem);
                this.itemsByID.put(id, nItem);
                if (selectedID) {
                    // selectedItem is NOT part of the items, even for non-editable combos
                    this.setValue(id);
                }
            } else {
                // don't know if and where to put the new item, so call fillCombo()
                this.fillCombo();
            }
        }
    }

    private final void itemsChanged() {
        final List<IComboSelectionItem> newVal = this.getList();
        this.propSupp.firePropertyChange("items", null, newVal);
    }

    // *** value

    @Override
    public final void resetValue() {
        this.setValue(null);
    }

    public final void setValue(int id) {
        // check if undefinedID means empty
        this.selectID(isUndefIDEmpty(id) ? SQLRow.NONEXISTANT_ID : id);
    }

    @Override
    public final void setValue(IComboSelectionItem o) {
        if (o == null)
            this.setValue(SQLRow.NONEXISTANT_ID);
        else
            this.setValue(o.getId());
    }

    /**
     * Return the item that is or *will* be selected.
     * 
     * @return the want item (with a possibly <code>null</code> label).
     * @see #getWantedID()
     */
    @Override
    public final IComboSelectionItem getValue() {
        if (!this.isUpdating())
            return this.getSelectedValue();
        else if (this.getWantedID() == SQLRow.NONEXISTANT_ID)
            return null;
        else if (this.getRequest().getKeepMode() == KeepMode.NONE)
            return new IComboSelectionItem(getWantedID(), null);
        else
            // no point in passing an SQLRowValues as the graph would be limited to just this row
            return new IComboSelectionItem(new SQLRow(this.getForeignTable(), getWantedID()), null);
    }

    /**
     * The currently selected item. I.e. the value before the last {@link #setValue(int)}) (possibly
     * <code>null</code>) while {@link #isUpdating()}.
     * 
     * @return the selected item (with a non-<code>null</code> label).
     */
    public final IComboSelectionItem getSelectedValue() {
        return this.getSelectedItem();
    }

    public final SQLTable getForeignTable() {
        return this.req.getPrimaryTable();
    }

    /**
     * Return the ID that is or *will* be selected (after {@link #fillCombo()}).
     * 
     * @return the wanted ID.
     */
    public final int getWantedID() {
        return this.idToSelect;
    }

    private final void setWantedID(int id) {
        if (this.idToSelect != id) {
            final int old = this.idToSelect;
            this.idToSelect = id;
            this.propSupp.firePropertyChange("wantedID", old, id);
            this.propSupp.firePropertyChange("value", null, getValue());
            this.emptySupp.fireEmptyChange(this.isEmpty());
        }
    }

    /**
     * Renvoie l'ID de l'item sélectionné.
     * 
     * @return l'ID de l'item sélectionné, <code>SQLRow.NONEXISTANT_ID</code> si combo vide.
     */
    public final int getSelectedId() {
        final IComboSelectionItem o = this.getSelectedValue();
        if (o != null && o.getId() >= SQLRow.MIN_VALID_ID)
            return o.getId();
        else {
            return SQLRow.NONEXISTANT_ID;
        }
    }

    /**
     * The wanted row or <code>null</code> if this is empty.
     * 
     * @return a SQLRow or <code>null</code>.
     * @see ComboSQLRequest#keepRows(boolean)
     */
    public final SQLRow getWantedRow() {
        if (this.isEmpty()) {
            return null;
        } else {
            final IComboSelectionItem o = this.getValue();
            final SQLRowAccessor r = o.getRow();
            if (r != null) {
                return r.asRow();
            } else {
                return new SQLRow(this.getForeignTable(), o.getId());
            }
        }
    }

    private final void comboValueChanged() {
        // update our state before firing
        if (!this.isUpdating())
            this.setWantedID(this.getSelectedId());
        this.propSupp.firePropertyChange("selectedValue", null, getSelectedValue());
    }

    private void selectID(int id) {
        log("entering selectID " + id);
        assert SwingUtilities.isEventDispatchThread();

        // no need to launch another updateAll() if one is already underway
        if (this.neverBeenFilled() && !isUpdating())
            // don't use fillCombo() which won't really update unless we're on screen
            this.doUpdateAll(null, true);

        if (this.isUpdating()) {
            this.setWantedID(id);
            log("isUpdating: this.idToSelect = " + id);
        } else if (id == SQLRow.NONEXISTANT_ID) {
            this.setSelectedItem(null);
            log("NONEXISTANT_ID: setSelectedItem(null)");
        } else {
            final IComboSelectionItem item = this.getComboItem(id);
            log("valid id : " + id + " item: " + item);
            // * setSelectedItem() use IComboSelectionItem.equals() so it must compare the ID and
            // the flag since even if ID doesn't change the combo might get refreshed and the
            // selected row :
            // 1. get removed : in that case we want to add the "warning" item
            // 2. get added : in that case remove the "warning"
            // * ATTN item being null means id isn't in the result set, getSelectedValue() being
            // null means nothing is selected. For example if the current selection is empty and
            // now we want ID 34 but it isn't returned by the request, both will be null.
            if (item == null && this.addMissingItem()) {
                // si l'ID voulu n'est pas la, essayer d'aller le chercher directement dans la base
                // sans respecter le filtre
                final ComboSQLRequest comboSQLRequest = new ComboSQLRequest(this.req);
                comboSQLRequest.setFilterEnabled(false);
                comboSQLRequest.setWhere(null);
                final ITransformer<SQLSelect, SQLSelect> transf = comboSQLRequest.getSelectTransf();
                if (transf != null)
                    comboSQLRequest.setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                        @Override
                        public SQLSelect transformChecked(SQLSelect input) {
                            final SQLSelect res = transf.transformChecked(input);
                            res.setWhere(null);
                            return res;
                        }
                    });
                IComboSelectionItem newItem = comboSQLRequest.getComboItem(id);
                if (newItem != null) {
                    newItem.setFlag(IComboSelectionItem.WARNING_FLAG);
                } else {
                    // TODO y faire un cran plus haut pour savoir quelle table référence
                    // cette erreur
                    new IllegalStateException("ID " + id + " cannot be found in " + this.req).printStackTrace();
                    final SQLRow row = new SQLRow(this.req.getPrimaryTable(), id);
                    final String error;
                    if (!row.exists())
                        error = " inexistante";
                    else if (row.isArchived())
                        error = " archivée";
                    else
                        error = " existe mais est non atteignable: " + row.findDistantArchived(2);
                    newItem = new IComboSelectionItem(row, "ERREUR !!! " + row + error);
                    newItem.setFlag(IComboSelectionItem.ERROR_FLAG);
                }
                this.addItem(newItem);
                this.setSelectedItem(newItem);
            } else {
                this.setSelectedItem(item);
            }
        }
    }

    /**
     * Whether missing item are fetched from the database. If {@link #setValue(int)} is called with
     * an ID not present in the list and addMissingItem is <code>true</code> then that ID will be
     * fetched and added to the list, if it is <code>false</code> the selection will be cleared.
     * 
     * @return <code>true</code> if missing item are fetched.
     */
    public final boolean addMissingItem() {
        return this.addMissingItem;
    }

    public final void setAddMissingItem(boolean addMissingItem) {
        this.addMissingItem = addMissingItem;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + this.req;
    }

    public final String dump() {
        String res = this.toString();
        for (final IComboSelectionItem it : this.getComboModel().getList()) {
            res += "\n" + it.dump();
        }
        return res;
    }

    @Override
    public final boolean isEmpty() {
        return this.getWantedID() == SQLRow.NONEXISTANT_ID || this.isUndefIDEmpty(this.getWantedID());
    }

    @Override
    public final void addEmptyListener(EmptyListener l) {
        this.emptySupp.addEmptyListener(l);
    }

    @Override
    public void removeEmptyListener(EmptyListener l) {
        this.emptySupp.removeEmptyListener(l);
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.addListener("value", l);
    }

    @Override
    public final void rmValueListener(PropertyChangeListener l) {
        this.rmListener("value", l);
    }

    public final void addListener(final String propName, PropertyChangeListener l) {
        this.propSupp.addPropertyChangeListener(propName, l);
    }

    public final void rmListener(final String propName, PropertyChangeListener l) {
        this.propSupp.removePropertyChangeListener(propName, l);
    }

    public final void addItemsListener(PropertyChangeListener l) {
        this.addItemsListener(l, false);
    }

    /**
     * Adds a listener on the items of this combo.
     * 
     * @param l the listener.
     * @param all <code>true</code> if <code>l</code> should be called for all changes, including UI
     *        ones (e.g. adding a '-- loading --' item).
     */
    public final void addItemsListener(PropertyChangeListener l, final boolean all) {
        // there's no uiItems anymore, so ignore the boolean
        this.addListener("items", l);
    }

    public final void rmItemsListener(PropertyChangeListener l) {
        this.rmListener("items", l);
    }

    // *** une table que nous affichons a changé

    private boolean tableOnlyOnce() {
        final SQLRowValues graphToFetch = this.req.getGraphToFetch();
        for (final SQLRowValues item : graphToFetch.getGraph().getItems()) {
            if (item != graphToFetch && item.getTable() == graphToFetch.getTable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void tableModified(SQLTableEvent evt) {
        final int id = evt.getId();
        if (id >= SQLRow.MIN_VALID_ID && this.getForeignTable().equals(evt.getTable()) && tableOnlyOnce()) {
            // MAYBE SwingWorker à la fillCombo()
            final IComboSelectionItem nItem = SearchSpecUtils.filterOne(this.req.getComboItem(id), getSearch());
            SwingThreadUtils.invoke(new Runnable() {
                @Override
                public void run() {
                    reloadComboItem(id, nItem);
                }
            });
        } else {
            // if multiple rows were changed or if one row can affect multiple combo items (e.g.
            // displaying mission and its previous date)
            this.fillCombo();
        }
    }

    // *** search

    public synchronized final void search(SearchSpec spec) {
        this.search = spec;
        this.fillCombo();
    }

    private synchronized SearchSpec getSearch() {
        return this.search;
    }

    protected final boolean isFiltered() {
        return this.getSearch() != null && !this.getSearch().isEmpty();
    }

    private synchronized void setWillUpdate(SwingWorker2<?, ?> w) {
        this.willUpdate = w;
        this.propSupp.firePropertyChange("willUpdate", null, this.willUpdate);
        if (this.willUpdate == null) {
            assert SwingUtilities.isEventDispatchThread() : "The end of an update should be in the EDT to be able change swing related attributes";
            this.setUpdating(false);
        }
    }

    private final void setUpdating(boolean b) {
        assert SwingUtilities.isEventDispatchThread();
        this.updating = b;
        this.propSupp.firePropertyChange("updating", null, this.updating);
    }

    public final boolean isUpdating() {
        assert SwingUtilities.isEventDispatchThread();
        return this.updating;
    }

    @Override
    public boolean isSearchable() {
        return !this.getRequest().getSearchFields().isEmpty();
    }

    @Override
    public boolean setSearch(String s, Runnable r) {
        if (this.getRequest().setSearch(s)) {
            if (r != null) {
                synchronized (this) {
                    this.runnables.add(r);
                }
            }
            return true;
        } else {
            SwingUtilities.invokeLater(r);
            return false;
        }
    }
}
