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
 
 package org.openconcerto.erp.core.supplychain.supplier.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class EcheanceFournisseurSQLElement extends ComptaSQLConfElement {

    public EcheanceFournisseurSQLElement() {
        super("ECHEANCE_FOURNISSEUR", "une échéance fournisseur", "échéances fournisseurs");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("MONTANT");
        l.add("DATE");
        l.add("ID_MOUVEMENT");
        l.add("ID_FOURNISSEUR");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_FOURNISSEUR");
        l.add("MONTANT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private DeviseField montant;
            private JDate date;
            private JTextField idMouvement;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.montant = new DeviseField();
                this.date = new JDate();
                this.idMouvement = new JTextField();
                final ElementComboBox fournisseur = new ElementComboBox();

                // Mouvement
                JLabel labelMouvement = new JLabel("Mouvement");
                this.add(labelMouvement, c);

                c.weightx = 1;
                c.gridx++;
                this.add(this.idMouvement, c);

                // Date
                JLabel labelDate = new JLabel("Date");
                c.gridx++;
                this.add(labelDate, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.date, c);

                // Fournisseur
                JLabel labelFournisseur = new JLabel("Fournisseur");
                c.gridy++;
                c.gridx = 0;

                this.add(labelFournisseur, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(fournisseur, c);

                // montant
                c.gridwidth = 1;
                JLabel labelMontant = new JLabel("Montant");
                c.gridy++;
                c.gridx = 0;
                this.add(labelMontant, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.montant, c);

                this.addSQLObject(this.montant, "MONTANT");
                this.addSQLObject(this.date, "DATE");
                this.addRequiredSQLObject(fournisseur, "ID_FOURNISSEUR");
                this.addSQLObject(this.idMouvement, "ID_MOUVEMENT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".commitment";
    }
}
