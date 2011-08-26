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
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class LangueSQLElement extends ComptaSQLConfElement {

    public LangueSQLElement() {
        super("LANGUE", "Langue", "Langue");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("NOM");

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
                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.gridx = GridBagConstraints.RELATIVE;
                final JTextField textNom = new JTextField();
                final JTextField textChemin = new JTextField();
                final JLabel labelNom = new JLabel(getLabelFor("NOM"));
                final JLabel labelChemin = new JLabel(getLabelFor("CHEMIN"));
                this.add(labelNom, c);
                c.weightx = 0.5;
                this.add(textNom, c);
                c.weightx = 0;
                this.add(labelChemin, c);
                c.weightx = 0.5;
                this.add(textChemin, c);

                this.addSQLObject(textChemin, "CHEMIN");
                this.addSQLObject(textNom, "NOM");
            }
        };
    }
}
