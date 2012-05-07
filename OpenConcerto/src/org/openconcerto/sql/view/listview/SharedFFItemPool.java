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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.itemview.SimpleRowItemView;

import java.util.Collections;

import org.apache.commons.collections.Closure;

// gère les créés, effacés, inchangés
public final class SharedFFItemPool extends FFItemPool {

    public SharedFFItemPool(ItemPoolFactory parent, ListSQLView panel) {
        super(parent, panel);
    }

    public SQLRowItemView getNewItem() throws IllegalStateException {
        if (!this.availableItem())
            throw new IllegalStateException("no more can be added");

        final MutableRowItemView newItem = new SimpleRowItemView<Integer>(new ElementComboBox());
        final SQLField f = (SQLField) this.availables.remove(0);
        this.added.add(newItem);
        newItem.init(f.getName(), Collections.singleton(f));
        return newItem;
    }

    //

    public void update(final SQLRowValues vals) {
        final Cl updateClosure = new Cl() {
            public void execute(SQLRowItemView input) {
                input.update(vals);
            }
        };
        // still does not mean unchanged (change of ARTICLE)
        this.forAllDo(this.stills, updateClosure);
        this.forAllDo(this.added, updateClosure);

        final Cl deleteClosure = new Cl() {
            public void execute(SQLRowItemView input) {
                vals.putEmptyLink(input.getField().getName());
            }
        };
        this.forAllDo(this.removed, deleteClosure);

        this.forAllDo(this.availables, new Closure() {
            public void execute(Object input) {
                vals.putEmptyLink(((SQLField) input).getName());
            }
        });
    }

    public void insert(SQLRowValues vals) {
        // for foreign fields update & insert are one and the same
        this.update(vals);
    }

}
