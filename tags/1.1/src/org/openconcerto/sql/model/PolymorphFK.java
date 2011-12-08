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

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing a polymorphic foreign key, ie not a foreign key in the SQL sense, rather a
 * OO link. That is a link that can refer to different tables. To achieve that 2 fields are used one
 * for the table one for the id.
 * 
 * @author Sylvain CUAZ
 */
public final class PolymorphFK {
    public static final String POLY_ID = "_ID";
    public static final String POLY_TABLE = "_TABLE";

    public static final String tableField2propName(SQLField f) {
        final String fName = f.getName();
        if (fName.endsWith(POLY_TABLE)) {
            return fName.substring(0, fName.length() - POLY_TABLE.length());
        } else
            return null;
    }

    /**
     * Finds all polymophic fk that follow the convention "NAME"+POLY_TABLE and "NAME"+POLY_ID.
     * 
     * @param t the table to search.
     * @return a set of PolymorphFK.
     */
    public static final Set<PolymorphFK> findPolymorphFK(SQLTable t) {
        final Set<PolymorphFK> res = new HashSet<PolymorphFK>();
        for (final SQLField f : t.getFields()) {
            final String propName = tableField2propName(f);
            if (propName != null) {
                if (t.contains(propName + POLY_ID))
                    res.add(new PolymorphFK(t, propName));
            }
        }
        return res;
    }

    private final SQLTable t;
    private final String tableFName;
    private final String idFName;
    private final String name;

    public PolymorphFK(SQLTable t, String propName) {
        this(t, propName + POLY_TABLE, propName + POLY_ID, propName);
    }

    public PolymorphFK(SQLTable t, String tableFName, String idFName, String name) {
        super();
        this.t = t;
        this.tableFName = tableFName;
        this.idFName = idFName;
        this.name = name;
    }

    public final SQLTable getTable() {
        return this.t;
    }

    public final SQLField getTableField() {
        return this.getTable().getField(this.tableFName);
    }

    public final SQLField getIdField() {
        return this.getTable().getField(this.idFName);
    }

    public final String getName() {
        return this.name;
    }
}
