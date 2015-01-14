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

public class ArticleTarifSQLElement extends ComptaSQLConfElement {

    public ArticleTarifSQLElement() {
        super("ARTICLE_TARIF", "un tarif d'article", "tarifs d'article");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("PV_TTC");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("PV_TTC");
        return l;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Nom
                JLabel labelNom = new JLabel("PV_TTC");
                JTextField textNom = new JTextField();

                this.add(labelNom, c);
                c.gridx++;
                this.add(textNom, c);

                this.addRequiredSQLObject(textNom, "PV_TTC");

            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".price";
    }
}
