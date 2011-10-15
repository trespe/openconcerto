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
 
 package org.openconcerto.utils.checks;

import java.util.ArrayList;
import java.util.List;

public final class ValidChangeSupport {

    private final ValidObject target;
    private final List<ValidListener> listeners;
    private ValidState validState;

    public ValidChangeSupport(final ValidObject target) {
        this(target, null);
    }

    public ValidChangeSupport(final ValidObject target, final ValidState initialState) {
        super();
        if (target == null)
            throw new NullPointerException("null target");

        this.listeners = new ArrayList<ValidListener>(3);
        this.target = target;
        this.validState = initialState;
    }

    public final ValidState getValidState() {
        return this.validState;
    }

    public final void fireValidChange(final ValidState newValue) {
        if (!newValue.equals(this.validState)) {
            this.validState = newValue;
            for (final ValidListener l : this.listeners) {
                l.validChange(this.target, this.validState);
            }
        }
    }

    public void addValidListener(ValidListener l) {
        this.listeners.add(l);
    }

    public void removeValidListener(ValidListener l) {
        this.listeners.remove(l);
    }
}
