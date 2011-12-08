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
 
 package org.openconcerto.erp.core.finance.tax.element;

import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class EcoTaxeSQLElement extends ConfSQLElement {

    public EcoTaxeSQLElement() {
        super("ECOTAXE", "une écotaxe", "écotaxes");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("MONTANT_HT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("MONTANT_HT");
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

                // Nom
                this.add(new JLabel(getLabelFor("NOM")), c);
                c.gridx++;
                JTextField nom = new JTextField();
                c.weightx = 1;

                this.add(nom, c);

                c.weightx = 0;
                c.gridx++;
                DeviseField montant = new DeviseField();
                this.add(new JLabel(getLabelFor("MONTANT_HT")), c);
                c.gridx++;
                this.add(montant, c);

                this.addView(montant, "MONTANT_HT");
                this.addView(nom, "NOM");

            }

        };
    }
}
