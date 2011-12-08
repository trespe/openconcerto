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

public final class EmptyChangeSupport {

    private final EmptyObj target;
    private final List<EmptyListener> listeners;
    private Boolean emptyState;

    public EmptyChangeSupport(final EmptyObj target) {
        this(target, null);
    }

    public EmptyChangeSupport(final EmptyObj target, final Boolean initialState) {
        super();
        if (target == null)
            throw new NullPointerException("null target");

        this.listeners = new ArrayList<EmptyListener>(3);
        this.target = target;
        this.emptyState = initialState;
    }

    public final void fireEmptyChange(final Boolean newValue) {
        if (!newValue.equals(this.emptyState)) {
            this.emptyState = newValue;
            for (final EmptyListener l : this.listeners) {
                l.emptyChange(this.target, this.emptyState);
            }
        }
    }

    public void addEmptyListener(EmptyListener l) {
        this.listeners.add(l);
    }

}
