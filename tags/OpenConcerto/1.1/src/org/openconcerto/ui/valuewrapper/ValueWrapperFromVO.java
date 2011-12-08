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

import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

/**
 * A value wrapper backed by a JComponent implementing {@link MutableValueObject}. If the component
 * doesn't also implement {@link ValidObject}, it is assumed to be always valid.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of value the component has.
 */
public final class ValueWrapperFromVO<T> extends BaseValueWrapper<T> {

    private final MutableValueObject<T> vo;

    public ValueWrapperFromVO(final MutableValueObject<T> vo) {
        if (!(vo instanceof JComponent))
            throw new IllegalArgumentException("vo is not a JComponent: " + vo);

        this.vo = vo;
        this.vo.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange();
            }
        });
        if (this.hasValidObject())
            this.getValidObject().addValidListener(new ValidListener() {
                public void validChange(ValidObject src, ValidState newValue) {
                    firePropertyChange();
                }
            });
    }

    private final ValidObject getValidObject() {
        return (ValidObject) this.vo;
    }

    private boolean hasValidObject() {
        return this.vo instanceof ValidObject;
    }

    public final JComponent getComp() {
        return (JComponent) this.vo;
    }

    public final T getValue() {
        return this.vo.getValue();
    }

    public final void setValue(T val) {
        this.vo.setValue(val);
    }

    @Override
    public ValidState getValidState() {
        if (hasValidObject())
            return this.getValidObject().getValidState();
        else
            return ValidState.getTrueInstance();
    }

}
