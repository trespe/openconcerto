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
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidObjectCombiner;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.text.ParseException;

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

    private final JComponent comp;
    private final FormatFilter formatFilter;

    private final ValidObjectCombiner comb;

    protected FormatValueWrapper(final JComponent b, final FormatFilter f) {
        this.comp = b;
        this.formatFilter = f;

        // this validObject is the combination of comp & this format
        this.comb = ValidObjectCombiner.create(this, this.comp, new ValidObject() {
            public void addValidListener(final ValidListener l) {
                final ValidObject me = this;
                // isValidated() depends on getText(), ie the value
                FormatValueWrapper.this.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        l.validChange(me, isValidated());
                    }
                });
            }

            public String getValidationText() {
                return f.getValidationText(getText());
            }

            public boolean isValidated() {
                return isFormatValidated();
            }
        });
        // if any of comb objects change, we change
        this.comb.addValidListener(new ValidListener() {
            public void validChange(ValidObject src, boolean newValue) {
                firePropertyChange();
            }
        });
    }

    public final JComponent getComp() {
        return this.comp;
    }

    public final Format getFormat() {
        return this.formatFilter.getFormat();
    }

    @SuppressWarnings("unchecked")
    public final T getValue() {
        final String text = this.getText();
        try {
            if (isNull(text) || this.formatFilter.isPartialValid(text))
                return null;
            else
                return (T) this.getFormat().parseObject(text);
        } catch (ParseException e) {
            throw ExceptionUtils.createExn(IllegalStateException.class, "unable to parse " + text, e);
        }
    }

    public final void setValue(T val) {
        this.setText(val == null ? "" : this.formatFilter.format(val));
    }

    abstract protected String getText();

    abstract protected void setText(String s);

    public final boolean isValidated() {
        return this.comb.isValidated();
    }

    private final boolean isFormatValidated() {
        final String text = this.getText();
        return isNull(text) || this.formatFilter.isCompleteValid(text);
    }

    @Override
    public String getValidationText() {
        return this.comb.getValidationText();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.getComp();
    }
}
