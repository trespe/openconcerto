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
import org.openconcerto.utils.convertor.ValueConvertor;

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

public class ConvertorValueWrapper<T, U> implements ValueWrapper<T> {

    private final ValueWrapper<U> delegate;
    private final ValueConvertor<T, U> conv;

    public ConvertorValueWrapper(ValueWrapper<U> delegate, ValueConvertor<T, U> conv) {
        super();
        if(delegate == null || conv == null)
            throw new NullPointerException();
        this.delegate = delegate;
        this.conv = conv;
    }

    public JComponent getComp() {
        return this.delegate.getComp();
    }

    // * valid

    public void addValidListener(ValidListener l) {
        this.delegate.addValidListener(l);
    }
    
    @Override
    public void removeValidListener(ValidListener l) {
        this.delegate.removeValidListener(l);  
    }

    public String getValidationText() {
        return this.delegate.getValidationText();
    }

    public boolean isValidated() {
        return this.delegate.isValidated();
    }

    // * value

    public void addValueListener(PropertyChangeListener l) {
        this.delegate.addValueListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.delegate.rmValueListener(l);
    }

    public void resetValue() {
        this.delegate.resetValue();
    }

    public T getValue() {
        return this.conv.unconvert(this.delegate.getValue());
    }

    public void setValue(T val) {
        this.delegate.setValue(this.conv.convert(val));
    }

}
