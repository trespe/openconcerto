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

import org.openconcerto.record.spec.RecordSpec;
import net.jcip.annotations.Immutable;

@Immutable
public final class RecordRef {

    private final RecordSpec spec;
    private final RecordKey key;

    public RecordRef(RecordSpec spec, RecordKey key) {
        super();
        if (spec == null)
            throw new NullPointerException("Null spec");
        this.spec = spec;
        if (key == null)
            throw new NullPointerException("Null key");
        this.key = key;
    }

    public final RecordSpec getSpec() {
        return this.spec;
    }

    public final RecordKey getKey() {
        return this.key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.key.hashCode();
        result = prime * result + this.spec.hashCode();
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
        RecordRef other = (RecordRef) obj;
        return this.key.equals(other.key) && this.spec.equals(other.spec);
    }
}
