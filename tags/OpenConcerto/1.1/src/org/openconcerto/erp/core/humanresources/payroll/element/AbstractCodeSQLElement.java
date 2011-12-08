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
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public abstract class AbstractCodeSQLElement extends ComptaSQLConfElement {

    public AbstractCodeSQLElement(final String tableName, final String singular, final String plural) {
        super(tableName, singular, plural);
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("CODE");
        list.add("NOM");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("CODE");
        list.add("NOM");
        return list;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Code
                final JLabel labelCode = new JLabel("Code");
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textCode = new JTextField();
                this.add(textCode, c);

                // Nom
                c.gridx++;
                c.weightx = 0;
                final JLabel labelNom = new JLabel("Libellé");
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                final JTextField textNom = new JTextField();
                this.add(textNom, c);

                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textCode, "CODE");
            }
        };
    }

}
