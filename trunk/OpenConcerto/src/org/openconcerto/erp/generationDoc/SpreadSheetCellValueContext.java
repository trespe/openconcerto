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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.StringUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpreadSheetCellValueContext {
    private SQLRowAccessor row;
    private Map<String, String> map = new HashMap<String, String>();

    public SpreadSheetCellValueContext(SQLRowAccessor row) {
        this.row = row;
    }

    public void dump() {
        dump(System.out);
    }

    public void dump(PrintStream prt) {
        prt.println("Row id: " + row.getID() + " table: " + row.getTable().getName());
        final List<String> fields = new ArrayList<String>();
        fields.addAll(row.getFields());
        Collections.sort(fields);
        for (String field : fields) {
            prt.print(StringUtils.rightAlign(field, 30));
            prt.print(" : ");
            prt.println(row.getObject(field));
        }
        prt.println("Parameters:");
        final List<String> params = new ArrayList<String>();
        params.addAll(map.keySet());
        Collections.sort(params);
        for (String param : params) {
            prt.print(StringUtils.rightAlign(param, 30));
            prt.print(" : ");
            prt.println(map.get(param));
        }

    }

    public SQLRowAccessor getRow() {
        return row;
    }

    public void put(String name, String value) {
        map.put(name, value);
    }
}
