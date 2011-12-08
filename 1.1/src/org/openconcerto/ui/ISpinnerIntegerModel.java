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

import javax.swing.AbstractSpinnerModel;

public class ISpinnerIntegerModel extends AbstractSpinnerModel {

    Integer val = null;
    Integer max;
    Integer min;

    public ISpinnerIntegerModel(Integer min, Integer max, Integer value) {
        this.max = max;
        this.min = min;
        this.val = value;
    }

    @Override
    public Object getNextValue() {
        if (this.val == null) {
            return this.min;
        } else {
            return (this.val + 1 <= this.max) ? this.val + 1 : this.min;
        }
    }

    @Override
    public Object getPreviousValue() {
        if (this.val == null) {
            return this.min;
        } else {
            return (this.val - 1 >= this.min) ? this.val - 1 : this.max;
        }
    }

    @Override
    public Object getValue() {
        return this.val;
    }

    @Override
    public void setValue(Object value) {
        if (value != this.val && value != null && !value.equals(this.val)) {
            this.val = (Integer) value;
            fireStateChanged();
        }
    }

}
