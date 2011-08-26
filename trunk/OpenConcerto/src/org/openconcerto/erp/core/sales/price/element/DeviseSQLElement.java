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

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.CollectionMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class DeviseSQLElement extends ConfSQLElement {

    public DeviseSQLElement() {
        super("DEVISE", "une devise", "devises");
    }

    @Override
    public boolean isShared() {
        return true;
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("TAUX");
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
                JLabel labelCode = new JLabel("CODE");
                JTextField textCode = new JTextField();
                this.add(labelCode, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textCode, c);

                // Nom
                JLabel labelNom = new JLabel("NOM");
                JTextField textNom = new JTextField();
                c.gridx++;
                c.weightx = 0;
                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNom, c);

                // Nom
                JLabel labelTaux = new JLabel("TAUX");
                JTextField textTaux = new JTextField();
                c.gridx++;
                c.weightx = 0;
                this.add(labelTaux, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textTaux, c);

                // Nom
                JLabel labelNomDevise = new JLabel("LIBELLE");
                JTextField textNomDevise = new JTextField();
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelNomDevise, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNomDevise, c);

                // Nom
                JLabel labelNomCent = new JLabel("LIBELLE_CENT");
                JTextField textNomCent = new JTextField();
                c.gridx++;
                c.weightx = 0;
                this.add(labelNomCent, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNomCent, c);

                this.addView(textCode, "CODE");
                this.addView(textNomDevise, "LIBELLE");
                this.addView(textNomCent, "LIBELLE_CENT");
                this.addView(textNom, "NOM");
                this.addRequiredSQLObject(textTaux, "TAUX");

            }
        };
    }
}
