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
 
 package org.openconcerto.ui.valuewrapper.format;

import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.ui.filters.FormatFilter;
import org.openconcerto.ui.valuewrapper.BaseValueWrapper;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidObjectCombiner;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.convertor.NumberConvertor.OverflowException;
import org.openconcerto.utils.convertor.NumberConvertor.RoundingException;
import org.openconcerto.utils.convertor.ValueConvertor;
import org.openconcerto.utils.convertor.ValueConvertorFactory;

import javax.swing.JComponent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

/**
 * A value wrapper that use a Format to convert between a String and the desired class.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of the ValueWrapper.
 */
public abstract class FormatValueWrapper<T> extends BaseValueWrapper<T> {

    public static <Z> FormatValueWrapper<Z> create(JComponent o, final Class<Z> c) {
        final Document doc = TextComponentUtils.getDocument(o);
        if (doc != null) {
            return new FilterFormatValueWrapper<Z>(o, (AbstractDocument) doc, c);
        } else if (o instanceof ValueWrapper) {
            return new VWFormatValueWrapper<Z>((ValueWrapper<String>) o, c);
        } else
            throw new IllegalArgumentException(o + "");
    }

    // whether text parse to a null value
    private static boolean isNull(String text) {
        return text == null || text.length() == 0;
    }

    // class only needed to please Java (captures F)
    private static class FilterAndConvertor<F, T extends F> {
        public static <U, T extends U> FilterAndConvertor<U, T> create(final FormatFilter<U> formatFilter, final Class<T> clazz) {
            return new FilterAndConvertor<U, T>(formatFilter, clazz);
        }

        private final Class<T> valueClass;
        private final FormatFilter<F> formatFilter;
        private final ValueConvertor<F, T> convertor;

        public FilterAndConvertor(final FormatFilter<F> formatFilter, final Class<T> clazz) {
            this.valueClass = clazz;
            this.formatFilter = formatFilter;
            this.convertor = ValueConvertorFactory.find(formatFilter.getValueClass(), clazz);
            if (this.convertor == null)
                throw new IllegalArgumentException("No convertor found between " + clazz + " and " + formatFilter.getValueClass());
        }

        public final Class<T> getValueClass() {
            return this.valueClass;
        }

        public final FormatFilter<F> getFormatFilter() {
            return this.formatFilter;
        }

        public final Tuple2<ValidState, T> parseText(final String text) {
            ValidState newState;
            T newValue;
            if (isNull(text)) {
                newState = ValidState.getTrueInstance();
                newValue = null;
            } else {
                final Tuple2<Boolean, F> res = this.getFormatFilter().parse(text);
                if (!res.get0()) {
                    newState = ValidState.create(false, this.getFormatFilter().getValidationText(text));
                    newValue = null;
                } else {
                    try {
                        newValue = this.convertor.convert(res.get1());
                        newState = ValidState.getTrueInstance();
                    } catch (Exception e) {
                        final String msg;
                        if (e instanceof OverflowException)
                            msg = "Nombre trop grand";
                        else if (e instanceof RoundingException)
                            msg = "Entier attendu";
                        else if (e instanceof ClassCastException)
                            msg = "Mauvais type, attendu " + this.getValueClass();
                        else {
                            e.printStackTrace();
                            msg = e.getLocalizedMessage();
                        }
                        newValue = null;
                        newState = ValidState.create(false, msg);
                    }
                }
            }
            return Tuple2.create(newState, newValue);
        }
    }

    private final JComponent comp;
    private final FilterAndConvertor<? super T, T> formatFilter;

    private final ValidChangeSupport selfValidSupp;
    private final ValidObjectCombiner comb;
    private T value;

    protected FormatValueWrapper(final JComponent b, final FormatFilter<? super T> f, final Class<T> clazz) {
        this.comp = b;
        this.formatFilter = FilterAndConvertor.create(f, clazz);

        // this validObject is the combination of comp & this format
        final ValidChangeSupport validSupp = new ValidChangeSupport(this);
        this.selfValidSupp = validSupp;
        this.comb = ValidObjectCombiner.create(this, this.comp, new ValidObject() {
            @Override
            public void addValidListener(final ValidListener l) {
                validSupp.addValidListener(l);
            }

            @Override
            public void removeValidListener(ValidListener l) {
                validSupp.removeValidListener(l);
            }

            @Override
            public ValidState getValidState() {
                return validSupp.getValidState();
            }
        });
        // if any of comb objects change, we change
        this.comb.addValidListener(new ValidListener() {
            public void validChange(ValidObject src, ValidState newValue) {
                FormatValueWrapper.this.supp.fireValidChange();
            }
        });
        // ATTN must call textChanged() but subclass might not yet be initialized
    }

    protected final void textChanged() {
        final Tuple2<ValidState, T> newState = this.formatFilter.parseText(getText());
        this.selfValidSupp.fireValidChange(newState.get0());
        this.setSelfValue(newState.get1());
    }

    private final void setSelfValue(T val) {
        if (!CompareUtils.equals(this.value, val)) {
            this.value = val;
            this.supp.fireValueChange();
        }
    }

    public final JComponent getComp() {
        return this.comp;
    }

    public final T getValue() {
        return this.value;
    }

    public final void setValue(T val) {
        this.setText(val == null ? "" : this.formatFilter.getFormatFilter().format(val));
    }

    abstract protected String getText();

    abstract protected void setText(String s);

    @Override
    public ValidState getValidState() {
        return this.comb.getValidState();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.getComp();
    }
}
