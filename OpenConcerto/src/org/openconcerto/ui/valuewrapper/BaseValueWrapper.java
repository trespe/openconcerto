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

/**
 * Simple class providing a PropertyChangeSupport.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of value the component has.
 */
abstract public class BaseValueWrapper<T> implements ValueWrapper<T> {

    protected final ValueChangeSupport<T> supp;

    public BaseValueWrapper() {
        this.supp = new ValueChangeSupport<T>(this);
    }

    protected final void firePropertyChange() {
        this.supp.fireValueChange();
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addValueListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.rmValueListener(l);
    }

    public void addValidListener(ValidListener l) {
        this.supp.addValidListener(l);
    }

    public void removeValidListener(ValidListener l) {
        this.supp.removeValidListener(l);
    }

    public String getValidationText() {
        return null;
    }

    public void resetValue() {
        this.setValue(null);
    }

}
