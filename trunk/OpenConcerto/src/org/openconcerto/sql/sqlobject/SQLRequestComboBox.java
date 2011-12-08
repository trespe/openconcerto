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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.sql.view.search.SearchSpec;
import org.openconcerto.ui.FontUtils;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.coreanimation.Pulseable;
import org.openconcerto.ui.valuewrapper.ValueChangeSupport;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.EmptyChangeSupport;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.model.DefaultIMutableListModel;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

/**
 * A comboBox who lists items provided by a ComboSQLRequest. It listens to table changes, but can
 * also be reloaded by calling {@link #fillCombo()}. Search is also available , see
 * {@link #search(SearchSpec)}.
 * 
 * @author Sylvain CUAZ
 * @see #uiInit(ComboSQLRequest)
 */
public class SQLRequestComboBox extends JPanel implements SQLForeignRowItemView, ValueWrapper<Integer>, EmptyObj, TextComponent, Pulseable, RowItemViewComponent {

    public static final String UNDEFINED_STRING = "----- ??? -----";

    public static enum ComboMode {
        /** The combo is completely disabled */
        DISABLED,
        /** The combo is disabled but the magnifying glass is enabled */
        ENABLED,
        /** The combo is fully functionnal (its default) */
        EDITABLE
    }

    // on l'a pas toujours à l'instanciation
    private IComboModel req;

    // Interface graphique
    protected final ISearchableCombo<IComboSelectionItem> combo;

    // supports
    private final ValueChangeSupport<Integer> supp;
    private final EmptyChangeSupport emptySupp;

    // le mode actuel
    private ComboMode mode;
    // le mode à sélectionner à la fin du updateAll
    private ComboMode modeToSelect;

    // to speed up the combo
    private final String stringStuff;

    public SQLRequestComboBox() {
        this(true);
    }

    public SQLRequestComboBox(boolean addUndefined) {
        this(addUndefined, -1);
    }

    public SQLRequestComboBox(boolean addUndefined, int preferredWidthInChar) {
        this.setOpaque(false);
        this.mode = ComboMode.EDITABLE;
        // necessary when uiInit() is called with a model already updating
        // (otherwise when it finishes modeToSelect will still be null)
        this.modeToSelect = this.mode;
        if (preferredWidthInChar > 0) {
            final char[] a = new char[preferredWidthInChar];
            Arrays.fill(a, ' ');
            this.stringStuff = String.valueOf(a);
        } else
            this.stringStuff = "123456789012345678901234567890";

        this.combo = new ISearchableCombo<IComboSelectionItem>(ComboLockedMode.LOCKED, 1, this.stringStuff.length());
        this.combo.setIncludeEmpty(addUndefined);
        this.combo.getActions().add(new AbstractAction("Recharger") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ignore cache since a user explicitly asked for an update
                fillCombo(null, false);
            }
        });

        this.emptySupp = new EmptyChangeSupport(this);
        this.supp = new ValueChangeSupport<Integer>(this);
    }

    /**
     * Specify which item will be selected the first time the combo is filled (unless setValue() is
     * called before the fill).
     * 
     * @param firstFillTransf will be passed the items and should return the wanted selection.
     */
    public final void setFirstFillSelection(ITransformer<List<IComboSelectionItem>, IComboSelectionItem> firstFillTransf) {
        this.req.setFirstFillSelection(firstFillTransf);
    }

    @Override
    public void init(SQLRowItemView v) {
        final SQLTable foreignTable = v.getField().getDBSystemRoot().getGraph().getForeignTable(v.getField());
        if (!this.hasModel())
            this.uiInit(Configuration.getInstance().getDirectory().getElement(foreignTable).getComboRequest());
        else if (this.getRequest().getPrimaryTable() != foreignTable)
            throw new IllegalArgumentException("Tables are different " + getRequest().getPrimaryTable().getSQLName() + " != " + foreignTable.getSQLName());
    }

    /**
     * Init de l'interface graphique.
     * 
     * @param req which table to display and how.
     */
    public final void uiInit(final ComboSQLRequest req) {
        this.uiInit(new IComboModel(req));
    }

    private boolean hasModel() {
        return this.req != null;
    }

    public final void uiInit(final IComboModel req) {
        if (hasModel())
            throw new IllegalStateException(this + " already inited.");

        this.req = req;
        // listeners
        this.req.addListener("updating", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updatingChanged((Boolean) evt.getNewValue());
            }
        });
        this.req.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                modelValueChanged();
            }
        });

        // remove listeners to allow this to be gc'd
        this.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
                    updateListeners();
            }
        });
        // initial state ; since we're in the EDT, the DISPLAYABILITY cannot change between
        // addHierarchyListener() and here
        updateListeners();
        this.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                SQLRequestComboBox.this.req.setOnScreen(true);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                SQLRequestComboBox.this.req.setOnScreen(false);
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // don't care
            }
        });

        FontUtils.setFontFor(this.combo, "ComboBox", this.getRequest().getSeparatorsChars());
        // try to speed up that damned JList, as those fine Swing engineers put it "This is
        // currently hacky..."
        for (int i = 0; i < this.combo.getUI().getAccessibleChildrenCount(this.combo); i++) {
            final Accessible acc = this.combo.getUI().getAccessibleChild(this.combo, i);
            if (acc instanceof ComboPopup) {
                final ComboPopup cp = (ComboPopup) acc;
                cp.getList().setPrototypeCellValue(new IComboSelectionItem(-1, this.stringStuff));
            }
        }

        this.combo.setIconFactory(new ITransformer<IComboSelectionItem, Icon>() {
            @Override
            public Icon transformChecked(final IComboSelectionItem input) {
                return getIconFor(input);
            }
        });
        this.combo.initCache(this.req);
        this.combo.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                comboValueChanged();
            }
        });
        // synchronize with the state of req (after adding our value listener)
        this.modelValueChanged();

        // getValidSate() depends on this.req
        this.supp.fireValidChange();

        this.uiLayout();

        // *without* : resetValue() => doUpdateAll() since it was never filled
        // then this is made displayable => setRunning(true) => dirty = true since not on screen
        // finally made visible => setOnScreen(true) => second doUpdateAll()
        // *with* : setRunning(true) => update ignored since not on screen, dirty = true
        // resetValue() => doUpdateAll() since it was never filled, dirty = false
        // then this is made displayable => setRunning(true) => no change
        // finally made visible => setOnScreen(true) => not dirty => no update
        this.req.setRunning(true);
    }

    private final void updatingChanged(final Boolean newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            this.modeToSelect = this.getEnabled();
            // ne pas interagir pendant le chargement
            this.setEnabled(ComboMode.DISABLED, true);
        } else {
            this.setEnabled(this.modeToSelect);
        }
    }

    public final List<Action> getActions() {
        return this.combo.getActions();
    }

    protected void updateListeners() {
        if (hasModel()) {
            this.req.setRunning(this.isDisplayable());
        }
    }

    public final ComboSQLRequest getRequest() {
        return this.req.getRequest();
    }

    protected void uiLayout() {
        this.setLayout(new GridLayout(1, 1));
        this.add(this.combo);
    }

    public void setDebug(boolean trace) {
        this.req.setDebug(trace);
        this.combo.setDebug(trace);
    }

    /**
     * Whether this combo is allowed to delay {@link #fillCombo()} when it isn't visible.
     * 
     * @param sleepAllowed <code>true</code> if reloads can be delayed.
     */
    public final void setSleepAllowed(boolean sleepAllowed) {
        this.req.setSleepAllowed(sleepAllowed);
    }

    public final boolean isSleepAllowed() {
        return this.req.isSleepAllowed();
    }

    /**
     * Reload this combo. This method is thread-safe.
     */
    public synchronized final void fillCombo() {
        this.fillCombo(null);
    }

    public synchronized final void fillCombo(final Runnable r) {
        this.fillCombo(r, true);
    }

    public synchronized final void fillCombo(final Runnable r, final boolean readCache) {
        this.req.fillCombo(r, readCache);
    }

    // combo

    public final List<IComboSelectionItem> getItems() {
        return this.getComboModel().getList();
    }

    private DefaultIMutableListModel<IComboSelectionItem> getComboModel() {
        return (DefaultIMutableListModel<IComboSelectionItem>) this.combo.getCache();
    }

    public final IComboSelectionItem getItem(int id) {
        return this.req.getItem(id);
    }

    // *** value

    public final void resetValue() {
        this.setValue((Integer) null);
    }

    public final void setValue(int id) {
        this.req.setValue(id);
    }

    public final void setValue(Integer id) {
        if (id == null)
            this.setValue(SQLRow.NONEXISTANT_ID);
        else
            this.setValue((int) id);
    }

    public final void setValue(SQLRowAccessor r) {
        this.setValue(r == null ? null : r.getID());
    }

    public final Integer getValue() {
        final IComboSelectionItem o = this.req.getSelectedItem();
        if (o != null && o.getId() >= SQLRow.MIN_VALID_ID)
            return o.getId();
        else {
            return null;
        }
    }

    /**
     * Renvoie l'ID de l'item sélectionné.
     * 
     * @return l'ID de l'item sélectionné, <code>SQLRow.NONEXISTANT_ID</code> si combo vide.
     */
    public final int getSelectedId() {
        return this.req.getSelectedId();
    }

    /**
     * The selected row or <code>null</code> if this is empty.
     * 
     * @return a SQLRow (non fetched) or <code>null</code>.
     */
    public final SQLRow getSelectedRow() {
        if (this.isEmpty())
            return null;
        else {
            return this.req.getSelectedRow();
        }
    }

    private void modelValueChanged() {
        final IComboSelectionItem newValue = this.req.getValue();
        // user makes invalid edit => combo invalid=true and value=null => model value=null
        // and if we call combo.setValue() it will change invalid to false
        if (this.combo.getValue() != newValue)
            this.combo.setValue(newValue);
    }

    private final void comboValueChanged() {
        this.req.setValue(this.combo.getValue());
        this.supp.fireValueChange();
        this.emptySupp.fireEmptyChange(this.isEmpty());
    }

    /**
     * Whether missing item are fetched from the database. If {@link #setValue(Integer)} is called
     * with an ID not present in the list and addMissingItem is <code>true</code> then that ID will
     * be fetched and added to the list, if it is <code>false</code> the selection will be cleared.
     * 
     * @return <code>true</code> if missing item are fetched.
     */
    public final boolean addMissingItem() {
        return this.req.addMissingItem();
    }

    public final void setAddMissingItem(boolean addMissingItem) {
        this.req.setAddMissingItem(addMissingItem);
    }

    public final void setEditable(boolean b) {
        this.setEnabled(b ? ComboMode.EDITABLE : ComboMode.ENABLED);
    }

    public final void setEnabled(boolean b) {
        // FIXME add mode to RIV
        this.setEnabled(b ? ComboMode.EDITABLE : ComboMode.ENABLED);
    }

    public final void setEnabled(ComboMode mode) {
        this.setEnabled(mode, false);
    }

    private final void setEnabled(ComboMode mode, boolean priv) {
        assert SwingUtilities.isEventDispatchThread();
        if (!priv && this.isUpdating()) {
            this.modeToSelect = mode;
        } else {
            this.mode = mode;
            modeChanged(mode);
        }
    }

    protected void modeChanged(ComboMode mode) {
        this.combo.setEnabled(mode == ComboMode.EDITABLE);
    }

    public final ComboMode getEnabled() {
        return this.mode;
    }

    public String toString() {
        return this.getClass().getName() + " " + this.req;
    }

    public final boolean isEmpty() {
        return this.req == null || this.req.isEmpty();
    }

    public final void addEmptyListener(EmptyListener l) {
        this.emptySupp.addEmptyListener(l);
    }

    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public final void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
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
        this.req.addItemsListener(l, all);
    }

    public final void rmItemsListener(PropertyChangeListener l) {
        this.req.rmItemsListener(l);
    }

    @Override
    public ValidState getValidState() {
        // OK, since we fire every time the combo does (see our ctor)
        // we are valid if we can return a value and getValue() needs this.req
        return ValidState.getNoReasonInstance(hasModel()).and(this.combo.getValidState());
    }

    @Override
    public final void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    private Icon getIconFor(IComboSelectionItem value) {
        final Icon i;
        if (value == null) {
            // happens when the combo is empty
            i = null;
        } else {
            final int flag = value.getFlag();
            if (flag == IComboSelectionItem.WARNING_FLAG)
                i = new ImageIcon(this.getClass().getResource("warning.png"));
            else if (flag == IComboSelectionItem.ERROR_FLAG)
                i = new ImageIcon(this.getClass().getResource("error.png"));
            else
                i = null;
        }
        return i;
    }

    public final JComponent getComp() {
        return this;
    }

    public JTextComponent getTextComp() {
        return this.combo.getTextComp();
    }

    public Component getPulseComponent() {
        return this.combo;
    }

    // *** search

    public final void search(SearchSpec spec) {
        this.req.search(spec);
    }

    public final boolean isUpdating() {
        return this.req.isUpdating();
    }
}
