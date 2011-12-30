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

import org.openconcerto.sql.element.DefaultElementSQLObject;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLRowItemView;

import java.util.Collections;

import org.apache.commons.collections.Closure;

// gère les créés, effacés, inchangés
public final class PrivateFFItemPool extends FFItemPool {

    public PrivateFFItemPool(ItemPoolFactory parent, ListSQLView panel) {
        super(parent, panel);
    }

    public SQLRowItemView getNewItem() throws IllegalStateException {
        if (!this.availableItem())
            throw new IllegalStateException("no more can be added");

        final SQLField f = (SQLField) this.availables.remove(0);
        final SQLComponent parent = this.getPanel().getSQLParent();
        final DefaultElementSQLObject newItem = new DefaultElementSQLObject(parent, parent.getElement().getPrivateElement(f.getName()).createDefaultComponent());
        newItem.setCreated(true);
        // bare panel
        newItem.showSeparator(false);
        newItem.setDecorated(false);
        this.added.add(newItem);
        ((ElementSQLObject) newItem).init(f.getName(), Collections.singleton(f));
        ((ElementSQLObject) newItem).setDescription(this.getLabel(f));
        return newItem;
    }

    protected void itemRemoved(SQLRowItemView v) {
        ((ElementSQLObject) v).setCreated(false);
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
        this.forAllDo(this.removed, updateClosure);

        // do not forget to blank the availables, since an OBSERVATION that was referenced by
        // ID_OBSERVATION_2 can now be referenced by ID_OBSERVATION_1
        this.forAllDo(this.availables, new Closure() {
            public void execute(Object input) {
                vals.putEmptyLink(((SQLField) input).getName());
            }
        });
    }

    public void insert(final SQLRowValues vals) {
        final Cl insertClosure = new Cl() {
            public void execute(SQLRowItemView input) {
                input.insert(vals);
            }
        };
        // still does not mean unchanged (change of ARTICLE)
        this.forAllDo(this.stills, insertClosure);
        this.forAllDo(this.added, insertClosure);
        this.forAllDo(this.removed, insertClosure);
    }

}
