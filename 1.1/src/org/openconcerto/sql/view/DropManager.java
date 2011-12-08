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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.model.SQLTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropManager {
    private static final DropManager instance = new DropManager();
    private final Map<SQLTable, List<FileDropHandler>> handlers = new HashMap<SQLTable, List<FileDropHandler>>();

    private DropManager() {
    }

    public static DropManager getInstance() {
        return instance;
    }

    public void add(SQLTable table, FileDropHandler handler) {
        List<FileDropHandler> l = handlers.get(table);
        if (l == null) {
            l = new ArrayList<FileDropHandler>();
            handlers.put(table, l);
        }
        if (l.contains(handler)) {
            throw new IllegalArgumentException("Handler already defined for table " + table);
        }
        l.add(handler);
    }

    public List<FileDropHandler> getHandlerForTable(SQLTable tableName) {
        List<FileDropHandler> l = handlers.get(tableName);
        if (l == null) {
            l = new ArrayList<FileDropHandler>(0);
        }
        return l;
    }

}
