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

public class TaxeSQLElement extends ComptaSQLConfElement {

    public TaxeSQLElement() {
        super("TAXE", "une taxe", "taxes");
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

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            @Override
            protected void addViews() {

                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();
                JLabel labelNom = new JLabel(getLabelFor("NOM"));

                this.add(labelNom, c);
                c.gridx++;
                c.weightx = 1;
                JTextField fieldNom = new JTextField(40);
                DefaultGridBagConstraints.lockMinimumSize(fieldNom);
                this.add(fieldNom, c);
                c.gridx++;
                c.weightx = 0;
                JLabel labelTaux = new JLabel(getLabelFor("TAUX"));
                this.add(labelTaux, c);
                c.gridx++;
                JTextField fieldTaux = new JTextField(6);
                DefaultGridBagConstraints.lockMinimumSize(fieldTaux);
                this.add(fieldTaux, c);

                // JLabel labelCompteCol = new JLabel(getLabelFor("ID_COMPTE_PCE_COLLECTE"));
                // c.gridx = 0;
                // c.gridy++;
                // this.add(labelCompteCol, c);
                // c.gridx++;
                // c.weightx = 1;
                // c.gridwidth = GridBagConstraints.REMAINDER;
                // ISQLCompteSelector compteCol = new ISQLCompteSelector();
                // this.add(compteCol, c);
                //
                // JLabel labelCompteDed = new JLabel(getLabelFor("ID_COMPTE_PCE_DED"));
                // c.gridx = 0;
                // c.gridy++;
                // c.weightx = 0;
                // c.gridwidth = 1;
                // this.add(labelCompteDed, c);
                // c.gridx++;
                // c.weightx = 1;
                // c.gridwidth = GridBagConstraints.REMAINDER;
                // ISQLCompteSelector compteDed = new ISQLCompteSelector();
                // this.add(compteDed, c);
                //
                // this.addSQLObject(compteCol, "ID_COMPTE_PCE_COLLECTE");
                // this.addSQLObject(compteDed, "ID_COMPTE_PCE_DED");

                this.addRequiredSQLObject(fieldNom, "NOM");
                this.addRequiredSQLObject(fieldTaux, "TAUX");
            }
        };
    }

}
