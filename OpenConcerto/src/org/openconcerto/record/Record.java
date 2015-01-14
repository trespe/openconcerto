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

import org.openconcerto.utils.CompareUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Record {

    private final String spec;
    private RecordKey key;

    private final Map<String, Object> items;

    public Record(final String spec, final RecordKey key) {
        super();
        if (spec == null)
            throw new NullPointerException("No spec");
        this.spec = spec;
        this.key = key;
        this.items = new HashMap<String, Object>();
    }

    public final String getSpec() {
        return this.spec;
    }

    public final RecordKey getKey() {
        return this.key;
    }

    public final void setKey(RecordKey key) {
        this.key = key;
    }

    public final Map<String, Object> getItems() {
        return this.items;
    }

    public final <T> List<T> getAsList(final String name, Class<T> clazz) {
        final Object val = this.getItems().get(name);
        if (val == null)
            return null;
        if (val instanceof List) {
            for (final Object o : (List<?>) val) {
                clazz.cast(o);
            }
            @SuppressWarnings("unchecked")
            final List<T> res = (List<T>) val;
            return res;
        } else {
            return Collections.singletonList(clazz.cast(val));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.items.hashCode();
        result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
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
        final Record other = (Record) obj;
        return this.spec.equals(other.spec) && CompareUtils.equals(this.key, other.key) && this.items.equals(other.items);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + this.spec + (this.getKey() == null ? "" : "[" + this.getKey().getValue() + "]");
    }
}
