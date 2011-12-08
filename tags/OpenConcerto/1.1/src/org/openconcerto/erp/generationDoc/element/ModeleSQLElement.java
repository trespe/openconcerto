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
 * Créé le 10 oct. 2011
 */
package org.openconcerto.erp.generationDoc.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModeleSQLElement extends ComptaSQLConfElement {

    public ModeleSQLElement() {
        super("MODELE", "un modele ", "modeles");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("ID_TYPE_MODELE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("ID_TYPE_MODELE");
        return l;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this) {

            @Override
            protected Set<String> createRequiredNames() {
                final Set<String> s = new HashSet<String>();
                // s.add("NOM");
                // s.add("ID_TYPE_MODELE");
                return s;
            }

            public void addViews() {
                this.addView("NOM");
                this.addView(new SQLRequestComboBox(false), "ID_TYPE_MODELE");
            }
        };
    }

    public String getDescription(SQLRow fromRow) {
        return fromRow.getString("NOM");
    }

}
