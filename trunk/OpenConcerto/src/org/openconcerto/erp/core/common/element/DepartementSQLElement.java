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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class DepartementSQLElement extends ComptaSQLConfElement {

    public DepartementSQLElement() {
        super("DEPARTEMENT", "Département", "Départements");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("NOM");
        l.add("CHEF_LIEU");
        l.add("REGION_ADMIN");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = GridBagConstraints.RELATIVE;

                JTextField textNom = new JTextField();
                JTextField textCode = new JTextField();
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JLabel labelCode = new JLabel(getLabelFor("NUMERO"));

                this.add(labelCode, c);
                this.add(textCode, c);
                this.add(labelNom, c);
                this.add(textNom, c);

                c.gridy++;
                JTextField textChef = new JTextField();
                JTextField textRegionAdmin = new JTextField();
                JLabel labelChef = new JLabel(getLabelFor("CHEF_LIEU"));
                JLabel labelRegionAdmin = new JLabel(getLabelFor("REGION_ADMIN"));
                this.add(labelChef, c);
                this.add(textChef, c);
                this.add(labelRegionAdmin, c);
                this.add(textRegionAdmin, c);

                this.addSQLObject(textCode, "NUMERO");
                this.addSQLObject(textNom, "NOM");
            }
        };
    }
}
