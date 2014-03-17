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

public class PaysSQLElement extends ComptaSQLConfElement {

    public PaysSQLElement() {
        super("PAYS", "Pays", "Pays");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
        l.add("NOM");
        l.add("ID_TARIF");
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
                final JTextField textCode = new JTextField();
                final JLabel labelNom = new JLabel(getLabelFor("NOM"));
                final JLabel labelCode = new JLabel(getLabelFor("CODE"));
                final JLabel labelTarif = new JLabel(getLabelFor("ID_TARIF"));
                final ElementComboBox comboTarif = new ElementComboBox();
                this.add(labelCode, c);
                c.weightx = 0.5;
                this.add(textCode, c);
                c.weightx = 0;
                this.add(labelNom, c);
                c.weightx = 0.5;
                this.add(textNom, c);
                c.gridy++;
                c.weightx = 0;
                this.add(labelTarif, c);
                c.weightx = 0.5;
                this.add(comboTarif, c);

                final JLabel labelLangue = new JLabel(getLabelFor("ID_LANGUE"));
                final ElementComboBox comboLangue = new ElementComboBox();
                c.weightx = 0;
                this.add(labelLangue, c);
                c.weightx = 0.5;
                this.add(comboLangue, c);

                this.addSQLObject(textCode, "CODE");
                this.addSQLObject(textNom, "NOM");
                this.addSQLObject(comboTarif, "ID_TARIF");
                this.addSQLObject(comboLangue, "ID_LANGUE");
            }
        };
    }

    @Override
    protected String createCode() {
        return "country";
    }
}
