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

public class RowSelectionSpec implements Externalizable {
    private String tableId;
    private long[] ids;

    /**
     * Define selected ids of a table. ids are ids from selected lines
     * */
    public RowSelectionSpec() {
        // Serialization
    }

    public RowSelectionSpec(String tableId, long[] ids) {
        this.tableId = tableId;
        this.ids = ids;
    }

    public long[] getIds() {
        return ids;
    }

    public String getTableId() {
        return tableId;
    }

    @Override
    public String toString() {
        String r = "RowSelectionSpec:" + tableId + " : ";
        for (int i = 0; i < ids.length; i++) {
            if (i < ids.length - 1) {
                r += ids[i] + ", ";
            } else {
                r += ids[i];
            }
        }
        return r;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeUTF(tableId);
            out.writeObject(ids);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        tableId = in.readUTF();
        ids = (long[]) in.readObject();

    }

}
