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
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ArticleDesignationSQLElement extends ComptaSQLConfElement {

    public ArticleDesignationSQLElement() {
        super("ARTICLE_DESIGNATION", "une désignation d'article", "désignation d'articles");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ARTICLE");
        l.add("NOM");
        l.add("ID_LANGUE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("ID_LANGUE");
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
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                final JLabel labelNom = new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT);
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNom, c);

                final JTextField textNom = new JTextField(15);
                c.gridx++;
                c.weightx = 1;
                DefaultGridBagConstraints.lockMinimumSize(textNom);
                this.add(textNom, c);
                this.addRequiredSQLObject(textNom, "NOM");

                // Famille père
                final JLabel labelLangue = new JLabel(getLabelFor("ID_LANGUE"), SwingConstants.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelLangue, c);

                final ElementComboBox langueBox = new ElementComboBox(true, 25);
                DefaultGridBagConstraints.lockMinimumSize(langueBox);
                c.gridx++;
                c.weightx = 1;
                this.add(langueBox, c);

                this.addSQLObject(langueBox, "ID_LANGUE");
            }

        };
    }

}
