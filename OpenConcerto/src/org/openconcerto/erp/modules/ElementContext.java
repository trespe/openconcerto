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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;

abstract class ElementContext {

    private final SQLElementDirectory dir;
    private final DBRoot root;

    ElementContext(final SQLElementDirectory dir, final DBRoot root) {
        super();
        this.dir = dir;
        this.root = root;
    }

    protected final DBRoot getRoot() {
        return this.root;
    }

    public final SQLElement getElement(final String tableName) {
        final SQLElement element = this.dir.getElement(this.getRoot().getTable(tableName));
        if (element == null) {
            throw new IllegalArgumentException("Not element found for table " + tableName);
        }
        return element;
    }
}
