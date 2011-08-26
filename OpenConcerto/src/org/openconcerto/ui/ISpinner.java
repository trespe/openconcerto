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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;

public class ISpinner implements ValueWrapper<Integer> {

    JSpinner spinner;

    public ISpinner(ISpinnerIntegerModel model) {

        this.spinner = new JSpinner(model);
    }

    @Override
    public JComponent getComp() {

        return this.spinner;
    }

    @Override
    public void addValidListener(ValidListener l) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public void removeValidListener(ValidListener l) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getValidationText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidated() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void resetValue() {
        this.setValue(null);
    }

    @Override
    public void setValue(Integer val) {

        this.spinner.setValue(val);

    }

    @Override
    public void addValueListener(PropertyChangeListener l) {
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        // TODO Auto-generated method stub

    }

    @Override
    public Integer getValue() {
        // TODO Auto-generated method stub
        return (Integer) this.spinner.getValue();
    }

}
