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
import java.util.Collections;
import java.util.List;

public class RowsBulk implements Externalizable {

    private List<Row> rows;
    private int offset;
    private int total;

    public RowsBulk() {// Serialization
    }

    public RowsBulk(List<Row> rows, int offset, int total) {
        this.rows = rows;
        this.offset = offset;
        this.total = total;
    }

    // Sending by column : size gain is 5%
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int rowCount = in.readInt();
        if (rowCount == 0) {
            this.rows = Collections.EMPTY_LIST;
        } else {
            this.rows = new ArrayList<Row>(rowCount);// colcount
            int columnCount = in.readByte();
            // id
            for (int j = 0; j < rowCount; j++) {
                Row row = new Row(in.readLong(), columnCount);
                this.rows.add(row);
            }

            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    Object v = in.readObject();
                    this.rows.get(j).addValue(v);
                }

            }

        }
        this.offset = in.readInt();
        this.total = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // nb rows
        int rowCount = rows.size();
        out.writeInt(rowCount);
        // content
        if (rows.size() > 0) {
            // nbcols
            int columnCount = rows.get(0).getValues().size();
            out.writeByte(columnCount);
            // ids
            for (int j = 0; j < rowCount; j++) {
                Row row = rows.get(j);
                out.writeLong(row.getId());

            }

            // send cols by cols
            for (int i = 0; i < columnCount; i++) {

                for (int j = 0; j < rowCount; j++) {
                    Row row = rows.get(j);
                    Object v = row.getValues().get(i);
                    out.writeObject(v);
                }

            }

        }
        out.writeInt(offset);
        out.writeInt(total);
    }

    public List<Row> getRows() {
        return this.rows;
    }

    public int getOffset() {
        return offset;
    }

    public int getTotal() {
        return total;
    }
}
