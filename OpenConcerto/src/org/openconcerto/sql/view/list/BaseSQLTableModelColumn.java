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

import java.util.Collections;
import java.util.Set;

/**
 * A column with sensible default methods.
 * 
 * @author Sylvain
 */
public abstract class BaseSQLTableModelColumn extends SQLTableModelColumn {

    private final Class valClass;

    public BaseSQLTableModelColumn(String name, Class valClass) {
        super(name);
        this.valClass = valClass;
    }

    protected final Class getValueClass_() {
        return this.valClass;
    }

    @Override
    public String getIdentifier() {
        return this.getName();
    }

    @Override
    public Set<String> getUsedCols() {
        return Collections.emptySet();
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    protected void put_(ListSQLLine r, Object obj) {
        throw new IllegalStateException();
    }

}
