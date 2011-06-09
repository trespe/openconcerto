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
 
 package org.openconcerto.erp.core.sales.product.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import javax.swing.JTextField;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

public class ModeVenteArticleSQLElement extends ComptaSQLConfElement {

    public ModeVenteArticleSQLElement() {
        super("MODE_VENTE_ARTICLE", "un mode de vente", "mode de vente");
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

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {

                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.gridheight = 1;
                c.gridwidth = 1;
                c.weightx = 0;
                c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.WEST;

                // Nom
                JLabel labelNom = new JLabel("Nom");
                JTextField textNom = new JTextField();

                this.add(labelNom, c);
                c.gridx++;
                this.add(textNom, c);

                this.addRequiredSQLObject(textNom, "NOM");
            }
        };
    }
}
