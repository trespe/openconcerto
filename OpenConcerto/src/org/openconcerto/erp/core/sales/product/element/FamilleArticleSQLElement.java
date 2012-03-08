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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class FamilleArticleSQLElement extends ComptaSQLConfElement {

    public FamilleArticleSQLElement() {
        super("FAMILLE_ARTICLE", "une famille d'article", "familles d'articles");
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
                final JLabel labelFamille = new JLabel(getLabelFor("ID_FAMILLE_ARTICLE_PERE"), SwingConstants.RIGHT);
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelFamille, c);

                final ElementComboBox familleBox = new ElementComboBox(true, 25);
                DefaultGridBagConstraints.lockMinimumSize(familleBox);
                c.gridx++;
                c.weightx = 1;
                this.add(familleBox, c);

                this.addSQLObject(familleBox, "ID_FAMILLE_ARTICLE_PERE");

                // Champ Module
                c.gridx = 0;
                c.gridy++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                final JPanel addP = new JPanel();

                this.setAdditionalFieldsPanel(new FormLayouter(addP, 2));
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                this.add(addP, c);

            }

            @Override
            public void update() {
                // TODO Auto-generated method stub
                super.update();

                SQLRow row = this.getTable().getRow(getSelectedID());
                int idPere = row.getInt("ID_FAMILLE_ARTICLE_PERE");

                // Création du code de famille --> permet de faciliter le filtre de recherche
                StringBuffer code = new StringBuffer();
                if (idPere > 1) {
                    SQLRow rowPere = this.getTable().getRow(idPere);
                    code.append(rowPere.getString("CODE"));
                } else {
                    code.append('1');
                }
                code.append("." + getSelectedID());

                SQLRowValues rowVals = new SQLRowValues(this.getTable());
                rowVals.put("CODE", code.toString());

                try {
                    rowVals.update(getSelectedID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public int insert(SQLRow order) {

                int id = super.insert(order);
                SQLRow row = this.getTable().getRow(id);
                int idPere = row.getInt("ID_FAMILLE_ARTICLE_PERE");

                // Création du code de famille --> permet de faciliter le filtre de recherche
                StringBuffer code = new StringBuffer();
                if (idPere > 1) {
                    SQLRow rowPere = this.getTable().getRow(idPere);
                    code.append(rowPere.getString("CODE"));
                } else {
                    code.append('1');
                }
                code.append("." + id);

                SQLRowValues rowVals = new SQLRowValues(this.getTable());
                rowVals.put("CODE", code.toString());

                try {
                    rowVals.update(id);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return id;
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".family";
    }
}
