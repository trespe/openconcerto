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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;

import java.util.Collections;
import java.util.Set;

/**
 * A column which is not stored in the database (ie {@link #getPaths()} is empty).
 * 
 * @author Sylvain
 */
public abstract class SQLTableModelColumnNoDB extends BaseSQLTableModelColumn {

    public SQLTableModelColumnNoDB(String name, Class valClass) {
        super(name, valClass);
    }

    public final Set<FieldPath> getPaths() {
        return Collections.emptySet();
    }

    @Override
    public final Set<SQLField> getFields() {
        return Collections.emptySet();
    }
}
