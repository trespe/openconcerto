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
 
 package org.openconcerto.erp.core.sales.invoice.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class EcheanceClientSQLElement extends ComptaSQLConfElement {

    public EcheanceClientSQLElement() {
        super("ECHEANCE_CLIENT", "une échéance client", "échéances clients");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("ID_MOUVEMENT");
        l.add("DATE");
        l.add("MONTANT");
        l.add("ID_CLIENT");
            l.add("NOMBRE_RELANCE");
        l.add("INFOS");
        l.add("DATE_LAST_RELANCE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("MONTANT");
        return l;
    }

    @Override
    public synchronized ListSQLRequest createListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("REG_COMPTA", null);
                graphToFetch.put("REGLE", null);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private DeviseField montant;
            private JTextField nbRelance;
            private JDate date;
            private JTextField idMouvement;
            private ElementComboBox comboClient;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.montant = new DeviseField();
                this.nbRelance = new JTextField();
                this.date = new JDate();
                this.idMouvement = new JTextField();
                this.comboClient = new ElementComboBox();

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

                // Client
                JLabel labelClient = new JLabel("Client");
                c.gridy++;
                c.gridx = 0;

                this.add(labelClient, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.comboClient, c);

                // libellé
                JLabel labelRelance = new JLabel("Nombre de relance");
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = 1;
                this.add(labelRelance, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.nbRelance, c);

                // montant
                c.gridwidth = 1;
                JLabel labelMontant = new JLabel("Montant");
                c.gridx++;
                this.add(labelMontant, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.montant, c);

                this.addSQLObject(this.montant, "MONTANT");
                this.addRequiredSQLObject(this.date, "DATE");
                this.addSQLObject(this.nbRelance, "NOMBRE_RELANCE");
                this.addRequiredSQLObject(this.comboClient, "ID_CLIENT");
                this.addSQLObject(this.idMouvement, "ID_MOUVEMENT");
            }
        };
    }

}
