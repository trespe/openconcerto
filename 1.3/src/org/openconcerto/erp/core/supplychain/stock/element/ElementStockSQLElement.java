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
 
 package org.openconcerto.erp.core.supplychain.stock.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.product.model.Article;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class ElementStockSQLElement extends ComptaSQLConfElement {

    public ElementStockSQLElement() {
        super("ELEMEENt_STOCK", "un article en stock", "articles en stock");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ARTICLE");
        l.add("QTE_METRIQUE");
        l.add("QTE");

        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ARTICLE");
        l.add("QTE_METRIQUE");
        l.add("QTE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private JTextField textQteMetriques;
            private JTextField textQte;

            public void addViews() {
                final ElementComboBox articleSelector = new ElementComboBox();

                this.textQteMetriques = new JTextField();
                this.textQte = new JTextField();

                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Article
                this.add(articleSelector, c);

                // Quantité de la métrique, ex: 3 (pour 3 mètres)
                c.gridx = 0;
                c.gridy++;
                final JLabel labelMetrique = new JLabel();
                this.add(labelMetrique, c);
                c.gridx++;
                this.add(this.textQteMetriques, c);
                c.gridx++;
                final JLabel labelUnite = new JLabel();
                this.add(labelUnite, c);
                // Quantité, ex 6 pour 6 toles de 3 m
                c.gridx = 0;
                c.gridy++;
                this.add(new JLabel("Quantité"), c);
                c.gridx++;
                this.add(this.textQte, c);

                this.addRequiredSQLObject(articleSelector, "NOM");
                this.addRequiredSQLObject(this.textQteMetriques, "QTE_METRIQUE");
                this.addRequiredSQLObject(this.textQte, "QTE");

                articleSelector.addValueListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt != null) {
                            Integer id = Integer.valueOf(evt.getNewValue().toString());
                            Article article = new Article(id.intValue());
                            labelMetrique.setText(article.getNomPourQuantiteMetrique());
                            labelUnite.setText(article.getUnitePourQuantiteMetrique());
                        }
                    }
                });

            }

        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".item";
    }
}
