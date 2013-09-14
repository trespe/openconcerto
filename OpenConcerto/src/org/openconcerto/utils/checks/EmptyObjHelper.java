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

/**
 * The empty state of an EmptyObj linked to another one.
 * 
 * @author Sylvain CUAZ
 */
public final class EmptyObjHelper {

    private final EmptyObj delegate;
    private final EmptyChangeSupport supp;

    public EmptyObjHelper(EmptyObj target, EmptyObj delegate) {
        super();
        this.supp = new EmptyChangeSupport(target);

        this.delegate = delegate;
        this.delegate.addEmptyListener(new EmptyListener() {
            public void emptyChange(EmptyObj src, boolean newValue) {
                EmptyObjHelper.this.supp.fireEmptyChange(newValue);
            }
        });
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public void addEmptyListener(EmptyListener l) {
        this.supp.addEmptyListener(l);
    }

    public void removeEmptyListener(EmptyListener l) {
        this.supp.removeEmptyListener(l);
    }
}
