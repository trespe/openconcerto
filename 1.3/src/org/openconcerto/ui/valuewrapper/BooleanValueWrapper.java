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

import org.openconcerto.utils.checks.ValidState;

import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComponent;
import javax.swing.JToggleButton;

public class BooleanValueWrapper extends BaseValueWrapper<Boolean> {

    private final JComponent comp;
    private final JToggleButton b;

    public BooleanValueWrapper(final JToggleButton b) {
        this(b, b);
    }

    public BooleanValueWrapper(final JComponent comp, final JToggleButton b) {
        Container ancestor = b;
        while (ancestor != null && ancestor != comp) {
            ancestor = b.getParent();
        }
        if (ancestor == null)
            throw new IllegalArgumentException("Comp is not an ancestor of " + b);
        this.comp = comp;
        this.b = b;
        this.b.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                firePropertyChange();
            }
        });
    }

    @Override
    public JComponent getComp() {
        return this.comp;
    }

    @Override
    public Boolean getValue() {
        return this.b.isSelected();
    }

    @Override
    public void setValue(Boolean val) {
        this.b.setSelected(val == null ? false : val);
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }
}
