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
 
 package org.openconcerto.ui.list.selection;

import static org.openconcerto.ui.list.selection.BaseListStateModel.INVALID_ID;
import static org.openconcerto.ui.list.selection.BaseListStateModel.INVALID_INDEX;
import org.openconcerto.ui.Log;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A class that maintain the selection of a list (via a ListSelectionModel) not with an index but
 * with a unique ID. That means that after the items of the list change (eg a search is performed),
 * the selection is set to the previous ID not the previous index (note that the same ID might not
 * mean the same object).
 * 
 * @author Sylvain
 */
public final class ListSelectionState implements ListSelection {

    public static ListSelectionState manage(final ListSelectionModel sel, final BaseListStateModel model) {
        return new ListSelectionState(model, sel).start();
    }

    // * models
    private final BaseListStateModel model;
    private final ListSelectionModel selModel;

    // * selection
    // {index => ID}, les index et ID des lignes sélectionnées
    private final SortedMap<Integer, Integer> selection;
    // les ID volontairement sélectionnés (eg != de selection si recherche)
    private final Set<Integer> userSelectedIDs;

    // * listeners
    private final PropertyChangeListener updateListener;
    private final ListSelectionListener selectionListener;

    private final PropertyChangeSupport supp;

    private boolean updating;
    private boolean strict;

    /**
     * Create a new instance.
     * 
     * @param model to listen to item changes.
     * @param sel to listen to selection changes.
     */
    private ListSelectionState(final BaseListStateModel model, final ListSelectionModel sel) {
        this.model = model;
        this.selModel = sel;

        this.supp = new PropertyChangeSupport(this);
        this.updating = false;
        this.strict = false;
        this.selection = new TreeMap<Integer, Integer>();
        this.userSelectedIDs = new HashSet<Integer>();

        this.updateListener = new PropertyChangeListener() {
            // pour resélectionner après une maj
            public void propertyChange(PropertyChangeEvent evt) {
                final String propName = evt.getPropertyName();
                if (propName.equals("updating")) {
                    ListSelectionState.this.setUpdating((Boolean) evt.getNewValue());
                }
            }
        };
        this.selectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // ne pas filtrer les ValueIsAdjusting pour etre dynamique
                rowSelected(e);
            }
        };

        start();
    }

    private ListSelectionState start() {
        this.model.addListener(this.updateListener);
        this.getSelModel().addListSelectionListener(this.selectionListener);
        return this;
    }

    void stop() {
        this.model.rmListener(this.updateListener);
        this.getSelModel().removeListSelectionListener(this.selectionListener);
    }

    public final BaseListStateModel getModel() {
        return this.model;
    }

    protected final ListSelectionModel getSelModel() {
        return this.selModel;
    }

    /*
     * Nous prévient qu'une ligne a été sélectionnée.
     */
    private void rowSelected(ListSelectionEvent e) {
        // compute the new selection
        final Map<Integer, Integer> newIDs = new HashMap<Integer, Integer>(this.getSelection());
        for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
            if (!this.getSelModel().isSelectedIndex(i))
                newIDs.remove(i);
            else {
                final int id = this.idFromIndex(i);
                if (id == INVALID_ID)
                    throw new IllegalStateException("selected index " + i + " has no id");
                newIDs.put(i, id);
            }
        }

        // filtrer sur les réels changements car le ListSelectionListener
        // nous envoie absolument tous les changements de sélection
        if (!newIDs.equals(this.getSelection())) {
            this.setSelectedIDs(newIDs);
        }
        // ne pas mettre dans le if précédent : on sélectionne 3 lignes puis 2 sont filtrées,
        // si l'user clique sur la sélection, newIDs == getSelection(), mais on veut enregistrer
        // que l'userID est maintenant juste cette ligne et pas les 3 initiales
        // ne changer que si l'utilisateur change directement
        if (!this.getModel().isUpdating())
            this.setUserSelectedIDs(this.getSelectedIDs());
    }

    public void selectID(final int id) {
        this.selectIDs(Collections.singletonList(id));
    }

    public void selectIDs(final Collection<Integer> idsOrig) {
        final List<Integer> ids = new ArrayList<Integer>(idsOrig);

        if (!this.getModel().isUpdating()) {
            // sorted asc for use by CollectionUtils.aggregate()
            final SortedSet<Integer> newIndexes = new TreeSet<Integer>();
            for (final Integer id : ids) {
                final int index = indexFromID(id);
                // if the id cannot be selected don't add it
                if (index != BaseListStateModel.INVALID_INDEX)
                    newIndexes.add(index);
            }
            if (!this.getSelectedIndexes().equals(newIndexes)) {
                List<int[]> intervals = CollectionUtils.aggregate(new ArrayList<Number>(newIndexes));
                if (this.getSelModel().getSelectionMode() != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION && intervals.size() > 1) {
                    final String msg = "need MULTIPLE_INTERVAL_SELECTION to select " + CollectionUtils.join(intervals, ", ", new ITransformer<int[], String>() {
                        @Override
                        public String transformChecked(int[] input) {
                            return Arrays.toString(input);
                        }
                    });
                    if (this.isStrict())
                        throw new IllegalStateException(msg);
                    else {
                        final int[] firstInterval = intervals.get(0);
                        intervals = Collections.singletonList(firstInterval);
                        ids.clear();
                        for (int index = firstInterval[0]; index <= firstInterval[1]; index++) {
                            ids.add(idFromIndex(index));
                        }
                        Log.get().info(msg);
                    }
                }

                if (intervals.size() == 1) {
                    // avoid clearSelection() and its fire
                    final int[] interval = intervals.get(0);
                    this.getSelModel().setSelectionInterval(interval[0], interval[1]);
                } else {
                    this.getSelModel().setValueIsAdjusting(true);
                    this.getSelModel().clearSelection();
                    for (final int[] interval : intervals) {
                        this.getSelModel().addSelectionInterval(interval[0], interval[1]);
                    }
                    this.getSelModel().setValueIsAdjusting(false);
                }
            }
        }

        // if the ID is not visible that will clear the selection,
        // hence make sure the wanted id is indeed id.
        // also if this is not strict the ids might be altered
        this.setUserSelectedIDs(ids);
    }

    // retourne l'ID de la ligne rowIndex à l'écran.
    public int idFromIndex(int rowIndex) {
        try {
            return this.getModel().idFromIndex(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return INVALID_ID;
        }
    }

    // retourne l'index de la ligne d'ID id.
    private int indexFromID(int id) {
        return this.getModel().indexFromID(id);
    }

    // * selections

    private SortedMap<Integer, Integer> getSelection() {
        return this.selection;
    }

    @Override
    public final List<Integer> getSelectedIDs() {
        return new ArrayList<Integer>(this.getSelection().values());
    }

    /**
     * All selected indexes.
     * 
     * @return all selected indexes, ascendant sorted.
     */
    private Set<Integer> getSelectedIndexes() {
        return this.getSelection().keySet();
    }

    /**
     * The currently selected index, that is the lead if it is selected or the first index selected.
     * 
     * @return the currently selected index.
     */
    private Integer getSelectedIndex() {
        final int res;
        if (this.getSelection().isEmpty())
            res = INVALID_INDEX;
        else {
            // getLeadSelectionIndex() renvoie le dernier setSel y compris remove
            final int lead = this.getSelModel().getLeadSelectionIndex();
            if (this.getSelectedIndexes().contains(lead))
                res = lead;
            else
                res = this.getSelectedIndexes().iterator().next();
        }
        return res;
    }

    /**
     * The currently selected id (at the lead index).
     * 
     * @return the currently selected id or INVALID_ID if no selection.
     */
    @Override
    public final int getSelectedID() {
        return this.getSelection().isEmpty() ? INVALID_ID : this.getSelection().get(this.getSelectedIndex());
    }

    @Override
    public final Set<Integer> getUserSelectedIDs() {
        return this.userSelectedIDs;
    }

    /**
     * The desired id. It may not be currently selected but it will be as soon as possible.
     * 
     * @return the desired id or INVALID_ID if no selection.
     */
    @Override
    public final int getUserSelectedID() {
        return this.getUserSelectedIDs().size() > 0 ? this.getUserSelectedIDs().iterator().next() : INVALID_ID;
    }

    // setters

    private void setSelectedIDs(Map<Integer, Integer> selectedIDs) {
        this.selection.clear();
        this.selection.putAll(selectedIDs);
        this.supp.firePropertyChange("selectedIDs", null, this.getSelectedIDs());
        this.supp.firePropertyChange("selectedID", null, this.getSelectedID());
        this.supp.firePropertyChange("selectedIndexes", null, this.getSelectedIndexes());
        this.supp.firePropertyChange("selectedIndex", null, this.getSelectedIndex());
    }

    private void setUserSelectedIDs(Collection<Integer> userSelectedIDs) {
        if (!this.userSelectedIDs.equals(new HashSet<Integer>(userSelectedIDs))) {
            this.userSelectedIDs.clear();
            this.userSelectedIDs.addAll(userSelectedIDs);
            this.supp.firePropertyChange("userSelectedIDs", null, this.getUserSelectedIDs());
            this.supp.firePropertyChange("userSelectedID", null, this.getUserSelectedID());
        }
    }

    // *** other props

    public final boolean isUpdating() {
        return this.updating;
    }

    private final void setUpdating(boolean upd) {
        if (upd != this.isUpdating()) {
            this.updating = upd;
            if (!this.isUpdating()) {
                // on finit 1 maj
                selectIDs(ListSelectionState.this.getUserSelectedIDs());
            }
            this.supp.firePropertyChange("updating", null, this.updating);
        }
    }

    /**
     * Whether this is strict when selecting, ie if the asked selection is non-contiguous but the
     * selection model is.
     * 
     * @return <code>true</code> if an exception should be thrown, <code>false</code> if the
     *         selection should be changed to be compatible with the mode.
     */
    public final boolean isStrict() {
        return this.strict;
    }

    public final void setStrict(boolean strict) {
        this.strict = strict;
    }

    // *** Listeners ***//

    public final void addPropertyChangeListener(String name, final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(name, l);
    }

    public final void addPropertyChangeListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }
}
