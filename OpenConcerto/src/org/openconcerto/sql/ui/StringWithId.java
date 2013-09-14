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
 
 package org.openconcerto.sql.ui;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class StringWithId implements Externalizable {
    private long id;
    // value is always trimed
    private String value;

    public StringWithId() {
    }

    public StringWithId(long id, String value) {
        this.id = id;
        this.value = value.trim();
    }

    public StringWithId(String condensedValue) {
        int index = condensedValue.indexOf(',');
        if (index <= 0) {
            throw new IllegalArgumentException("invalid condensed value " + condensedValue);
        }
        id = Long.parseLong(condensedValue.substring(0, index));
        value = condensedValue.substring(index + 1).trim();
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(id);
        out.writeUTF(value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readLong();
        value = in.readUTF().trim();
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        StringWithId o = (StringWithId) obj;
        return o.getId() == this.getId() && o.getValue().endsWith(this.getValue());
    }

    @Override
    public int hashCode() {
        return (int) id + value.hashCode();
    }

    public String toCondensedString() {
        return id + "," + value;
    }
}
