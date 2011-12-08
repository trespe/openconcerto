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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RowBackedCodeGenerator {

    public static String getCode(SQLTable table, String className, String packageName) {
        StringBuilder result = new StringBuilder();
        if (table == null) {
            return "";
        }
        if (packageName != null && packageName.length() > 0) {
            result.append("package ");
            result.append(packageName);
            result.append(";\n\n");
        }
        result.append("import org.openconcerto.sql.element.RowBacked;\n");
        result.append("import org.openconcerto.sql.model.SQLRowAccessor;\n\n");
        result.append("import java.sql.Timestamp;\n");
        result.append("import java.sql.Blob;\n");
        result.append("import java.math.BigDecimal;\n\n");
        result.append("public class " + className + " extends RowBacked {\n\n");
        // Constructor
        result.append("   public " + className + "(SQLRowAccessor r) {\n");
        result.append("        super(r);\n");
        result.append("   }\n\n");
        // Getters
        List<SQLField> f = new ArrayList<SQLField>(table.getFields());
        Collections.sort(f, new Comparator<SQLField>() {
            public int compare(SQLField o1, SQLField o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (SQLField field : f) {
            final String type = field.getType().getJavaType().getSimpleName();
            String methodName = "get" + getJavaName(field.getName());
            if (methodName.equals("getTable")) {
                methodName = "getFieldTable";
            }
            result.append("   public " + type + " " + methodName + "() {\n");
            result.append("        return (" + type + ") this.get(\"" + field.getName() + "\");\n");
            result.append("   }\n\n");
        }
        result.append("}\n");
        return result.toString();
    }

    public static String getJavaName(String s) {
        s = s.replace('+', '_');
        s = s.replace(' ', '_');
        String result = "";
        String[] ss = s.split("_");
        for (int i = 0; i < ss.length; i++) {
            result += StringUtils.firstUpThenLow(ss[i]);
        }
        return result;
    }

}
