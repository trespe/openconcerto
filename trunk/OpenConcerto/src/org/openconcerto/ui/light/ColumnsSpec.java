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

public class ColumnsSpec implements Externalizable {
    private String id;
    // All the columns that could be displayed
    private List<ColumnSpec> columns;
    // Ids visible in the table, in the same order of the display
    private List<String> visibleIds;
    // Ids of the sorted columns
    private List<String> sortedIds;
    // number of fixed columns, used for vertical "split"
    private int fixedColumns;

    public ColumnsSpec() {
    }

    public ColumnsSpec(String id, List<ColumnSpec> columns, List<String> visibleIds, List<String> sortedIds) {
        // Id checks
        if (id == null) {
            throw new IllegalArgumentException("null id");
        }
        this.id = id;
        this.columns = columns;
        // Visible checks
        if (visibleIds == null) {
            throw new IllegalArgumentException("null visible columns");
        }
        if (visibleIds.isEmpty()) {
            throw new IllegalArgumentException("empty visible columns");
        }

        this.visibleIds = visibleIds;
        // Sort checks
        if (sortedIds == null) {
            sortedIds = Collections.EMPTY_LIST;
        }

        this.sortedIds = sortedIds;

    }

    public String getId() {
        return id;
    }

    public List<String> getVisibleIds() {
        return visibleIds;
    }

    public List<String> getSortedIds() {
        return sortedIds;
    }

    public int getFixedColumns() {
        return fixedColumns;

    }

    public int getColumnCount() {
        return this.columns.size();

    }

    public ColumnSpec getColumn(int i) {
        return this.columns.get(i);
    }

    public List<String> getColumnsIds() {
        ArrayList<String> result = new ArrayList<String>(this.columns.size());
        for (ColumnSpec c : this.columns) {
            result.add(c.getId());
        }
        return result;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(id);
        out.writeInt(fixedColumns);
        out.writeObject(this.columns);
        out.writeObject(this.visibleIds);
        out.writeObject(this.sortedIds);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.fixedColumns = in.readInt();
        this.columns = (List<ColumnSpec>) in.readObject();
        this.visibleIds = (List<String>) in.readObject();
        this.sortedIds = (List<String>) in.readObject();
    }

    public List<Object> getDefaultValues() {
        List<Object> l = new ArrayList<Object>();
        for (String id : this.visibleIds) {
            Object v = getColumn(id).getDefaultValue();
            l.add(v);
        }
        return l;
    }

    public ColumnSpec getColumn(String id) {
        for (ColumnSpec c : this.columns) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

}
