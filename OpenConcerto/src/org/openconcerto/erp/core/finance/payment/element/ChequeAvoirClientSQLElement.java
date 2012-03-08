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
 
 package org.openconcerto.erp.core.finance.payment.element;

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
import javax.swing.SwingConstants;

public class ChequeAvoirClientSQLElement extends ComptaSQLConfElement {

    public ChequeAvoirClientSQLElement() {
        super("CHEQUE_AVOIR_CLIENT", "un chéque de remboursement", "chéques remboursements");
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("MONTANT");
        l.add("ID_CLIENT");
        l.add("DATE_AVOIR");
        l.add("DATE_DECAISSE");
        l.add("DECAISSE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_AVOIR");
        l.add("MONTANT");
        l.add("ID_CLIENT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private final DeviseField textMontant = new DeviseField();

            @Override
            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Montant
                final JLabel labelMontant = new JLabel("Montant ");

                this.add(labelMontant, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textMontant, c);

                // Date
                final JLabel labelDate = new JLabel("Date ");
                c.weightx = 0;
                c.gridx++;
                labelDate.setHorizontalAlignment(SwingConstants.RIGHT);
                this.add(labelDate, c);

                final JDate dateAvoir = new JDate(true);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(dateAvoir, c);

                c.gridy++;
                c.gridx = 0;
                final JLabel labelClientNom = new JLabel("Client ");
                this.add(labelClientNom, c);

                final ElementComboBox nomClient = new ElementComboBox();
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(nomClient, c);

                this.addRequiredSQLObject(nomClient, "ID_CLIENT");
                this.addRequiredSQLObject(this.textMontant, "MONTANT");
                this.addRequiredSQLObject(dateAvoir, "DATE_AVOIR");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".cheque.due";
    }
}
