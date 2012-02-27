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

import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JComponent;

/**
 * Allow to add validation to the value of a {@link ValueWrapper}.
 * 
 * @author Sylvain CUAZ
 * @param <T> the type of value.
 * @see #add(ValueWrapper, ITransformer)
 */
public final class ValidatedValueWrapper<T> implements ValueWrapper<T> {

    /**
     * Adds a validation to the passed value wrapper.
     * 
     * @param <T> the type of value.
     * @param vw the value wrapper to be validated.
     * @param validator will be passed the value of <code>vw</code> (only when it is itself valid).
     * @return a valid wrapper performing additional validation.
     */
    static public final <T> ValidatedValueWrapper<T> add(final ValueWrapper<T> vw, final ITransformer<? super T, ValidState> validator) {
        if (vw instanceof ValidatedValueWrapper) {
            return ((ValidatedValueWrapper<T>) vw).add(validator);
        } else {
            return new ValidatedValueWrapper<T>(vw, validator);
        }
    }

    static public final <T extends Number> ITransformer<T, ValidState> createTransformer(final T min, final String belowMin, final T max, final String aboveMax) {
        return createTransformer(min, false, belowMin, max, aboveMax);
    }

    /**
     * A transformer that checks if a value is in a specified range.
     * 
     * @param min the lower bound.
     * @param minValid whether the lower bound is valid or not.
     * @param belowMin the explanation if the value is below <code>min</code>.
     * @param max the upper bound.
     * @param aboveMax the explanation if the value is above <code>max</code>.
     * @return a new transformer.
     */
    static public final <T extends Number> ITransformer<T, ValidState> createTransformer(final T min, final boolean minValid, final String belowMin, final T max, final String aboveMax) {
        return new ITransformer<T, ValidState>() {
            @Override
            public ValidState transformChecked(T input) {
                if (input == null)
                    return ValidState.getTrueInstance();

                final int minComp = NumberUtils.compare(input, min);
                if (minComp < 0 || (!minValid && minComp == 0))
                    return ValidState.createCached(false, belowMin);
                else if (NumberUtils.compare(input, max) > 0)
                    return ValidState.createCached(false, aboveMax);
                else
                    return ValidState.getTrueInstance();
            }
        };
    }

    private final ValueWrapper<T> delegate;
    private final Set<ITransformer<? super T, ValidState>> validators;
    private ValidState delegateValid;
    private ValidState selfValid;
    private final ValidChangeSupport validSupp;

    private ValidatedValueWrapper(final ValueWrapper<T> vw, final ITransformer<? super T, ValidState> validator) {
        this.delegate = vw;
        this.validators = new LinkedHashSet<ITransformer<? super T, ValidState>>();

        this.validSupp = new ValidChangeSupport(this);
        final ValidListener validL = new ValidListener() {
            @Override
            public void validChange(ValidObject src, ValidState newValue) {
                ValidatedValueWrapper.this.delegateValid = newValue;
                updateValidated();
            }
        };
        // initialize delegateValid
        validL.validChange(this.delegate, this.delegate.getValidState());
        this.delegate.addValidListener(validL);
        this.delegate.addValueListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateValidated();
            }
        });

        this.add(validator);
    }

    public final ValidatedValueWrapper<T> add(final ITransformer<? super T, ValidState> validator) {
        this.validators.add(validator);
        updateValidated();
        return this;
    }

    private void updateValidated() {
        if (!this.delegateValid.isValid()) {
            this.selfValid = this.delegateValid;
        } else {
            // only ask getValue() if delegate is valid, otherwise might be meaningless
            final T value = this.delegate.getValue();
            ValidState valid = ValidState.getTrueInstance();
            for (final ITransformer<? super T, ValidState> validator : this.validators) {
                valid = valid.and(validator.transformChecked(value));
            }
            this.selfValid = valid;
        }
        this.validSupp.fireValidChange(this.selfValid);
    }

    @Override
    public ValidState getValidState() {
        return this.validSupp.getValidState();
    }

    @Override
    public void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
        this.delegate.addValueListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.delegate.rmValueListener(l);
    }

    @Override
    public void setValue(T val) {
        this.delegate.setValue(val);
    }

    @Override
    public void resetValue() {
        this.delegate.resetValue();
    }

    @Override
    public T getValue() {
        return this.delegate.getValue();
    }

    @Override
    public JComponent getComp() {
        return this.delegate.getComp();
    }
}
