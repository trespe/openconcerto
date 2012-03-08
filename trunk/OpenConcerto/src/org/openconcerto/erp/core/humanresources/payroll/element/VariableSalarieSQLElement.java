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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public class VariableSalarieSQLElement extends ComptaSQLConfElement {

    public VariableSalarieSQLElement() {
        super("VARIABLE_SALARIE", "une variable salarié", "variables salariés");
    }

    protected List<String> getListFields() {

        final List<String> l = new ArrayList<String>();

        for (org.openconcerto.sql.model.SQLField sqlField : this.getTable().getContentFields()) {

            String field = sqlField.getName();
            if (!field.equalsIgnoreCase("ID_USER_COMMON_CREATE") && !field.equalsIgnoreCase("ID_USER_COMMON_MODIFY") && !field.equalsIgnoreCase("MODIFICATION_DATE")
                    && !field.equalsIgnoreCase("CREATION_DATE")) {
                l.add(field);
            }
        }
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("HEURE_110");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {

                int pos = 0;
                final List<String> listFields = getListFields();

                for (String i : listFields) {

                    if (pos % 2 == 0) {
                        this.addSQLObject(new JTextField(4), i, "left");
                    } else {
                        this.addSQLObject(new JTextField(4), i, "right");
                    }

                }
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".employe.variable";
    }
}
