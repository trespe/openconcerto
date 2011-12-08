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

import java.io.Serializable;

import javax.swing.SpinnerNumberModel;

public class SpinnerCyclicModel extends SpinnerNumberModel implements Serializable {
    private long stepSize;
    private long value;
    private long minimum;
    private long maximum;

    public SpinnerCyclicModel(long value, long minimum, long maximum, long stepSize) {
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }

    /**
     * @param minimum
     */
    public void setMinimum(long minimum) {
        this.minimum = minimum;
    }

    public Comparable getMinimum() {
        return Long.valueOf(minimum);
    }

    /**
     * @param maximum
     */
    public void setMaximum(long maximum) {
        this.maximum = maximum;
        fireStateChanged();
    }

    public Comparable getMaximum() {
        return Long.valueOf(maximum);
    }

    /**
     * @param stepSize
     */
    public void setStepSize(long stepSize) {

        if (stepSize != this.stepSize) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    public Number getStepSize() {
        return Long.valueOf(stepSize);
    }

    private Number incrValue(int dir) {
        long v = value + stepSize * dir;
        if (v > maximum)
            v = minimum;
        if (v < minimum)
            v = maximum;
        return Long.valueOf(v);
    }

    public Object getNextValue() {
        return incrValue(+1);
    }

    public Object getPreviousValue() {
        return incrValue(-1);
    }

    public Object getValue() {
        return Long.valueOf(value);
    }

    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Number)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (((Number) value).longValue() != this.value) {
            this.value = ((Number) value).longValue();
            fireStateChanged();
        }
    }
}
