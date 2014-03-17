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

import java.io.Serializable;

public class TableSpec implements Serializable {
    private String id;
    private ColumnsSpec columns;
    private TableContent content;

    public TableSpec() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ColumnsSpec getColumns() {
        return columns;
    }

    public void setColumns(ColumnsSpec columns) {
        this.columns = columns;
    }

    public TableContent getContent() {
        return content;
    }

    public void setContent(TableContent content) {
        this.content = content;
    }
    
}
