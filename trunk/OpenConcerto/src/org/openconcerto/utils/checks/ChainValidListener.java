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
 * Allow a ValidObject to delegate its implementation to another ValidObject, but still appear as
 * the source of changes.
 * 
 * @author Sylvain
 */
public final class ChainValidListener implements ValidListener {

    private final ValidListener delegate;
    private final ValidObject target;

    public ChainValidListener(ValidObject target, ValidListener delegate) {
        if (target == null || delegate == null)
            throw new NullPointerException();
        this.target = target;
        this.delegate = delegate;
    }

    public void validChange(ValidObject src, boolean newValue) {
        this.delegate.validChange(this.target, newValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.delegate.hashCode();
        result = prime * result + this.target.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ChainValidListener other = (ChainValidListener) obj;
        return this.delegate.equals(other.delegate) && this.target.equals(other.target);
    }

}
