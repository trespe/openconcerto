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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ModeleCourrierClientSQLElement extends ComptaSQLConfElement {

    public ModeleCourrierClientSQLElement() {
        super("MODELE_COURRIER_CLIENT", "un modéle de courrier", "modéles de courriers");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("CONTENU");
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

            JTextField textNom = new JTextField();
            JTextArea textContenu = new JTextArea();

            public void addViews() {
                GridBagConstraints c = new DefaultGridBagConstraints();

                this.setLayout(new GridBagLayout());

                // Numéro
                this.add(new JLabel(getLabelFor("NOM")), c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textNom, c);

                // Infos
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(new JLabel(getLabelFor("CONTENU")), c);
                c.gridx++;
                c.weightx = 1;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 1;
                this.add(textContenu, c);

                this.addRequiredSQLObject(this.textNom, "NOM");
                this.addSQLObject(this.textContenu, "CONTENU");
            }
        };
    }

    @Override
    protected String createCode() {
        return super.createCodeFromPackage() + ".mailtemplate";
    }
}
