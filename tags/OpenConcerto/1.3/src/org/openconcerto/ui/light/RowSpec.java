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

public class RowSpec implements Externalizable {
    private String tableId;
    private String[] columnIds;

    public RowSpec() {
        // Serialization
    }

    public RowSpec(String tableId, String[] columnIds) {
        this.tableId = tableId;
        this.columnIds = columnIds;
    }

    public String[] getIds() {
        return columnIds;
    }

    public void setIds(String[] columnIds) {
        this.columnIds = columnIds;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableId() {
        return tableId;
    }

    @Override
    public String toString() {
        String r = "RowSpec:" + tableId + " : ";
        for (int i = 0; i < columnIds.length; i++) {
            if (i < columnIds.length - 1) {
                r += columnIds[i] + ", ";
            } else {
                r += columnIds[i];
            }
        }
        return r;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeUTF(tableId);
            out.writeObject(columnIds);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        tableId = in.readUTF();
        columnIds = (String[]) in.readObject();

    }

}
