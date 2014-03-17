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

import org.openconcerto.ui.filters.FormatFilter;
import org.openconcerto.ui.valuewrapper.ValueWrapper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class VWFormatValueWrapper<T> extends FormatValueWrapper<T> {

    private final ValueWrapper<String> vw;

    public VWFormatValueWrapper(final ValueWrapper<String> b, final Class<T> c) {
        super(b.getComp(), FormatFilter.create(c), c);
        this.vw = b;
        this.vw.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                textChanged();
            }
        });
        // initial values
        this.textChanged();
    }

    protected String getText() {
        return this.vw.getValue();
    }

    @Override
    protected void setText(String s) {
        this.vw.setValue(s);
    }

}
