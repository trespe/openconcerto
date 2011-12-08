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
 
 package org.openconcerto.erp.core.humanresources.timesheet.element;

import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.JDate;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class PointageSQLElement extends ConfSQLElement {

    public PointageSQLElement() {
        super("POINTAGE", "un pointage", "pointages");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ACTION");
        l.add("CARTE");
        l.add("ID_USER_COMMON");
        l.add("DATE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_USER_COMMON");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    @Override
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addSQLObject(new JTextField(), "ACTION", "right");
                this.addSQLObject(new JTextField(), "CARTE", "left");
                this.addSQLObject(new JDate(), "DATE", "right");
                this.addSQLObject(new ElementComboBox(), "ID_USER_COMMON", "left");
            }
        };
    }
}
