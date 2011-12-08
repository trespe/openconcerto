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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;

/**
 * A SQLTableModelSource *not* directly tied to the database.
 * 
 * @author Sylvain
 */
public class SQLTableModelSourceOffline extends SQLTableModelSource {

    private final SQLRowValuesListFetcher fetcher;
    private final SQLElement elem;

    public SQLTableModelSourceOffline(final SQLRowValuesListFetcher fetcher, final SQLElement elem) {
        super(fetcher.getGraph());
        this.fetcher = fetcher;
        this.elem = elem;
        if (!this.getPrimaryTable().equals(this.elem.getTable()))
            throw new IllegalArgumentException("not the same table: " + this.getPrimaryTable() + " != " + this.elem);
    }

    public final SQLRowValuesListFetcher getFetcher() {
        return this.fetcher;
    }

    public SQLElement getElem() {
        return this.elem;
    }

    @Override
    protected SQLTableModelLinesSourceOffline _createLinesSource(final ITableModel model) {
        return new SQLTableModelLinesSourceOffline(this, model);
    }

    @Override
    public SQLRowValues getMaxGraph() {
        return this.getFetcher().getGraph();
    }
}
