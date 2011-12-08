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
 
 package org.openconcerto.sql.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author ILM Informatique 23 juil. 2004 TODO suppr
 */
public class SQLQuery {

    static public String and(Collection<String> clause) {
        return join(clause, " AND ");
    }

    static public String or(Collection<String> clause) {
        return join(clause, " OR ");
    }

    static public String from(Collection<String> clause) {
        return join(clause, ", ");
    }

    static public String archive(Collection clause) {
        List<String> fields = new ArrayList<String>(clause.size());
        Iterator iter = clause.iterator();
        while (iter.hasNext()) {
            // TODO tester que la table possède archive
            String s = toString(iter.next());
            fields.add(s + ".ARCHIVE=0");
        }
        return and(fields);
    }

    static private String join(Collection<String> c, String sep) {
        String res = "";
        Iterator<String> iter = c.iterator();
        while (iter.hasNext()) {
            String s = toString(iter.next());
            res += s;
            if (iter.hasNext())
                res += sep;
        }
        return res;
    }

    static private String toString(Object obj) {
        String res = null;
        if (obj instanceof String)
            res = (String) obj;
        else if (obj instanceof SQLTable)
            res = ((SQLTable) obj).getName();
        else if (obj instanceof SQLField)
            res = ((SQLField) obj).getFullName();
        else
            throw new IllegalArgumentException("only String, SQLTable or SQLField are allowed");
        return res;
    }

}
