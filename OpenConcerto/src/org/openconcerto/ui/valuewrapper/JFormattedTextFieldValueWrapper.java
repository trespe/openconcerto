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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;

public class JFormattedTextFieldValueWrapper<Z> extends BaseValueWrapper<Z> {

    public static <Z> boolean isCompatible(JFormattedTextField comp, final Class<Z> c) {
        if (!(comp.getFormatter() instanceof DefaultFormatter))
            return false;

        final DefaultFormatter formatter = (DefaultFormatter) comp.getFormatter();
        return formatter.getValueClass() == null || c.isAssignableFrom(formatter.getValueClass());
    }

    private final JFormattedTextField tf;
    private final Class<Z> c;

    JFormattedTextFieldValueWrapper(JFormattedTextField comp, final Class<Z> c) {
        this.tf = comp;
        final Class<?> vc = this.getFormatter().getValueClass();
        if (vc == null)
            this.getFormatter().setValueClass(c);
        else if (!c.isAssignableFrom(vc))
            throw new IllegalArgumentException("value class :" + vc + " not a subclass of " + c);

        this.c = c;
        this.tf.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("value"))
                    firePropertyChange();
                else if (evt.getPropertyName().equals("editValid"))
                    JFormattedTextFieldValueWrapper.this.supp.fireValidChange();
            }
        });
    }

    private final DefaultFormatter getFormatter() {
        return (DefaultFormatter) this.tf.getFormatter();
    }

    @Override
    public JComponent getComp() {
        return this.tf;
    }

    @Override
    public boolean isValidated() {
        return this.tf.isEditValid();
    }

    @Override
    public void setValue(Z val) {
        this.tf.setValue(val);
    }

    @Override
    public Z getValue() {
        return this.c.cast(this.tf.getValue());
    }
}
