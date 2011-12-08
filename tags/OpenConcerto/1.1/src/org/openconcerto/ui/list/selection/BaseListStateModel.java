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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Let the {@link ListSelectionState} know when we're updating (ie searching, sorting, refreshing,
 * ...) so as to not clear the user selection. Convert between index and ID, the ID must be at least
 * MIN_ID.
 * 
 * @author Sylvain
 */
public abstract class BaseListStateModel {

    public static final int MIN_ID = 0;
    public static final int INVALID_ID = MIN_ID - 1;
    // swing index starts at 0
    public static final int INVALID_INDEX = -1;

    private final PropertyChangeSupport supp;
    private boolean updating;
    private final PropertyChangeListener updateL;

    public BaseListStateModel() {
        this.supp = new PropertyChangeSupport(this);
        this.updating = false;
        this.updateL = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                BaseListStateModel.this.setUpdating((Boolean) evt.getNewValue());
            }
        };
    }

    /**
     * A convenience method to link the real model to this.
     * 
     * @return a listener that does {@link #setUpdating(boolean)} passing evt.getNewValue().
     */
    protected final PropertyChangeListener getUpdateL() {
        return this.updateL;
    }

    protected final void setUpdating(boolean newValue) {
        this.updating = newValue;
        this.supp.firePropertyChange("updating", null, newValue);
    }

    final void addListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    final void rmListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public final boolean isUpdating() {
        return this.updating;
    }

    public abstract int idFromIndex(int rowIndex);

    public abstract int indexFromID(int id);

}
