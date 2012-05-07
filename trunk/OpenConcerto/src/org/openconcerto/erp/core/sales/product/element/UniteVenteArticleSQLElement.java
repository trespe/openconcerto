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
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class UniteVenteArticleSQLElement extends ComptaSQLConfElement {

    public static final int A_LA_PIECE = 2;

    public UniteVenteArticleSQLElement() {
        super("UNITE_VENTE", "une unité de vente", "unité de vente");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("CODE");
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
                GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                JLabel labelCode = new JLabel(getLabelFor("CODE"));
                JTextField textCode = new JTextField(25);

                this.add(labelCode, c);
                c.gridx++;
                DefaultGridBagConstraints.lockMinimumSize(textCode);
                this.add(textCode, c);

                // Nom
                c.gridy++;
                c.gridx = 0;
                JLabel labelNom = new JLabel(getLabelFor("NOM"));
                JTextField textNom = new JTextField(50);

                this.add(labelNom, c);
                c.gridx++;
                DefaultGridBagConstraints.lockMinimumSize(textNom);
                this.add(textNom, c);

                this.addRequiredSQLObject(textCode, "CODE");
                this.addRequiredSQLObject(textNom, "NOM");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".unit";
    }
}
