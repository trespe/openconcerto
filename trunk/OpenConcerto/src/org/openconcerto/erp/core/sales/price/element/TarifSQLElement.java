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
 
 package org.openconcerto.erp.core.sales.price.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class TarifSQLElement extends ComptaSQLConfElement {

    public TarifSQLElement() {
        super("TARIF", "un tarif", "tarifs");
    }

    @Override
    public boolean isShared() {
        return true;
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

    @Override
    public CollectionMap<String, String> getShowAs() {
        CollectionMap<String, String> map = new CollectionMap<String, String>();
        map.put(null, "NOM");
        return map;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField();

                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // Devise
                JLabel labelDevise = new JLabel(getLabelFor("ID_DEVISE"));
                ElementComboBox boxDevise = new ElementComboBox();
                c.gridx++;
                c.weightx = 0;
                this.add(labelDevise, c);
                c.gridx++;
                c.weightx = 1;
                this.add(boxDevise, c);

                // Devise
                JLabel labelTaxe = new JLabel(getLabelFor("ID_TAXE"));
                ElementComboBox boxTaxe = new ElementComboBox();
                c.gridx++;
                c.weightx = 0;
                this.add(labelTaxe, c);
                c.gridx++;
                c.weightx = 1;
                this.add(boxTaxe, c);

                this.addRequiredSQLObject(textNom, "NOM");
                this.addSQLObject(boxDevise, "ID_DEVISE");
                this.addSQLObject(boxTaxe, "ID_TAXE");

            }
        };
    }
}
