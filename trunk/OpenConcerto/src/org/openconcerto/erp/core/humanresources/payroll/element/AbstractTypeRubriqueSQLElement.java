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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import javax.swing.JTextField;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTypeRubriqueSQLElement extends ConfSQLElement {

    private static final String NOM = "NOM";

    public AbstractTypeRubriqueSQLElement(final String tableName, final String singular, final String plural) {
        super(tableName, singular, plural);
    }

    public List<String> getListFields() {
        final List<String> list = new ArrayList<String>();
        list.add(NOM);
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>();
        list.add(NOM);
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), NOM, "right");
                this.addSQLObject(new JTextField(), NOM, "left");
            }
        };
    }
}
