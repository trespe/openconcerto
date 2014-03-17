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

public class CumulsCongesSQLElement extends ComptaSQLConfElement {
    public CumulsCongesSQLElement() {
        super("CUMULS_CONGES", "un cumul de congés", "cumuls de congés");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("RESTANT");
        l.add("ACQUIS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("RESTANT");
        l.add("ACQUIS");
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

                GridBagConstraints cPanel = new DefaultGridBagConstraints();

                // Conges restant du 1 juin N-1 au 31 mai N
                JLabel labelRestant = new JLabel(getLabelFor("RESTANT"));
                this.add(labelRestant, cPanel);
                JTextField textRestant = new JTextField(4);
                cPanel.gridx++;
                cPanel.weightx = 1;
                this.add(textRestant, cPanel);
                cPanel.weightx = 0;

                // Conges acquis du 1 juin N au 31 mai N+1
                cPanel.gridx = 0;
                cPanel.gridy++;
                JLabel labelAcquis = new JLabel(getLabelFor("ACQUIS"));
                this.add(labelAcquis, cPanel);

                JTextField textAcquis = new JTextField(4);
                cPanel.gridx++;
                cPanel.weighty = 1;
                cPanel.weightx = 1;
                this.add(textAcquis, cPanel);

                this.addSQLObject(textAcquis, "ACQUIS");
                this.addSQLObject(textRestant, "RESTANT");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".vacation.total";
    }
}
