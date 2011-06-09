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
 
 package org.openconcerto.ui.valuewrapper;

import org.openconcerto.utils.checks.ValidListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple class providing a PropertyChangeSupport.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of value the component has.
 */
public final class ValueChangeSupport<T> {

    public static final String INVALID_VALUE = "invalidValue";

    private final ValueWrapper<T> vw;

    private final PropertyChangeSupport supp;

    private final List<ValidListener> validListeners;
    private Boolean valid;
    private boolean signalInvalid;

    public ValueChangeSupport(ValueWrapper<T> vw) {
        this.vw = vw;
        this.validListeners = new ArrayList<ValidListener>();
        this.supp = new PropertyChangeSupport(vw);
        this.valid = null;
        this.signalInvalid = true;
    }

    public final void fireValueChange() {
        final boolean newValid = this.vw.isValidated();

        // check the validity (eg '-' for a number wrapper is an unvalid change)
        if (newValid)
            this.supp.firePropertyChange("value", null, this.vw.getValue());
        else if (this.signalInvalid)
            this.supp.firePropertyChange(INVALID_VALUE, null, null);

        this.setValid(newValid);
    }

    /**
     * Some components do not fire a value change when becoming invalid.
     */
    public final void fireValidChange() {
        this.setValid(this.vw.isValidated());
    }

    private final void setValid(final Boolean newValid) {
        if (!newValid.equals(this.valid)) {
            this.valid = newValid;
            for (final ValidListener l : this.validListeners) {
                l.validChange(this.vw, this.valid);
            }
        }
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public void addValidListener(ValidListener l) {
        this.validListeners.add(l);
    }

    public void rmValidListener(ValidListener l) {
        this.validListeners.remove(l);
    }

    public boolean isSignalInvalid() {
        return this.signalInvalid;
    }

    /**
     * Whether to signal all changes, including invalid ones. An invalid change is signaled with the
     * property name {@link #INVALID_VALUE} and null values.
     * 
     * @param signalInvalid <code>true</code> if this will fire invalid change.
     */
    public void setSignalInvalid(boolean signalInvalid) {
        this.signalInvalid = signalInvalid;
    }

}
