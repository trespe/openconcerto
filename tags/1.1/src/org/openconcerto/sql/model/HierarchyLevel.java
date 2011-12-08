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

import org.openconcerto.utils.EnumOrderedSet;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * A level in the tree structure of a database, each corresponding to a DBStructureItemJDBC.
 * 
 * @author Sylvain
 * @see #get(Class)
 * @see #getJDBCClass()
 */
public enum HierarchyLevel {
    SQLSERVER {
        @Override
        public Class<? extends DBStructureItemJDBC> getJDBCClass() {
            return SQLServer.class;
        }
    },
    SQLBASE {
        @Override
        public Class<? extends DBStructureItemJDBC> getJDBCClass() {
            return SQLBase.class;
        }
    },
    SQLSCHEMA {
        @Override
        public Class<? extends DBStructureItemJDBC> getJDBCClass() {
            return SQLSchema.class;
        }
    },
    SQLTABLE {
        @Override
        public Class<? extends DBStructureItemJDBC> getJDBCClass() {
            return SQLTable.class;
        }
    },
    SQLFIELD {
        @Override
        public Class<? extends DBStructureItemJDBC> getJDBCClass() {
            return SQLField.class;
        }
    };

    abstract Class<? extends DBStructureItemJDBC> getJDBCClass();

    // ** static

    public static final EnumOrderedSet<HierarchyLevel> getAll() {
        return new EnumOrderedSet<HierarchyLevel>(EnumSet.allOf(HierarchyLevel.class));
    }

    static final HierarchyLevel get(Class<? extends DBStructureItemJDBC> clazz) {
        final HierarchyLevel res = getByClass().get(clazz);
        if (res != null)
            return res;
        // else check if it's a subclass
        for (final Map.Entry<Class<? extends DBStructureItemJDBC>, HierarchyLevel> e : getByClass().entrySet()) {
            if (e.getKey().isAssignableFrom(clazz))
                return e.getValue();
        }
        throw new IllegalArgumentException(clazz + " hierarchy unknown");
    }

    static private Map<Class<? extends DBStructureItemJDBC>, HierarchyLevel> byClass;

    static private final Map<Class<? extends DBStructureItemJDBC>, HierarchyLevel> getByClass() {
        // java : Cannot refer to the static enum field HierarchyLevel.byClass within an initializer
        // thus not in ctor but on demand.
        if (byClass == null) {
            byClass = new HashMap<Class<? extends DBStructureItemJDBC>, HierarchyLevel>();
            for (HierarchyLevel l : HierarchyLevel.values())
                byClass.put(l.getJDBCClass(), l);
        }
        return byClass;
    }

}
