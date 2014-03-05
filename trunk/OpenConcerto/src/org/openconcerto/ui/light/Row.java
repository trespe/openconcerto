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
 
 package org.openconcerto.ui.light;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class Row implements Externalizable {

    private long id;
    private List<Object> values;

    public Row() {
        // Serialization
    }

    public Row(long id, int valueCount) {
        this.id = id;
        if (valueCount > 0)
            this.values = new ArrayList<Object>(valueCount);
    }

    public Row(long id) {
        this.id = id;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public List<Object> getValues() {
        return values;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Row id: " + id + " values: " + values;
    }

    public void addValue(Object v) {
        values.add(v);

    }

    public void setValue(int index, Object v) {
        values.set(index, v);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(id);
        out.writeObject(values);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readLong();
        values = (List<Object>) in.readObject();

    }

}
