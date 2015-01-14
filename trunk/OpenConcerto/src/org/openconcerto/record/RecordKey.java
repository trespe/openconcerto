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
 
 package org.openconcerto.record;

import net.jcip.annotations.Immutable;

@Immutable
public class RecordKey {

    private final Object val;

    public RecordKey(Object val) {
        super();
        if (val == null)
            throw new NullPointerException("Null value");
        this.val = val;
    }

    public final Object getValue() {
        return this.val;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.val.hashCode();
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
        final RecordKey other = (RecordKey) obj;
        return this.val.equals(other.val);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getValue();
    }
}
