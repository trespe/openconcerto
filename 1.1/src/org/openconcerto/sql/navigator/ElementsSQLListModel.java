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
 
 /*
 * Créé le 28 mai 2005
 * 
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO hide empty item
 * 
 * @author Sylvain CUAZ
 */
public class ElementsSQLListModel extends SQLListModel<SQLElement> {

    private final List<SQLElement> elements;
    private final Map<SQLElement, Number> counts;

    public ElementsSQLListModel(List<SQLElement> elements) {
        super();
        this.counts = new HashMap<SQLElement, Number>();
        if (elements.size() == 0)
            throw new IllegalArgumentException("elements empty");
        this.elements = new ArrayList<SQLElement>(elements);
    }

    protected void reload() {
        this.counts.clear();
        final List<SQLElement> res = new ArrayList<SQLElement>(this.elements.size());
        for (final SQLElement elem : this.elements) {
            // if (this.getCount(elem) > 0)
            res.add(elem);
        }
        this.setAll(res);
    }

    private final int getCount(SQLElement elem) {
        if (!this.counts.containsKey(elem)) {
            final SQLSelect sel = new SQLSelect(elem.getTable().getBase());
            sel.addSelectFunctionStar("count");
            sel.setWhere(new Where(elem.getTable().getField(elem.getParentForeignField()), this.getIds()));
            final Number count = (Number) elem.getTable().getBase().getDataSource().executeScalar(sel.asString());
            this.counts.put(elem, count);
        }
        return this.counts.get(elem).intValue();
    }

    protected String toString(SQLElement e) {
        return e.getPluralName() + " (" + this.getCount(e) + ")";
    }

    @Override
    protected void idsChanged() {
        this.reload();
    }

}
