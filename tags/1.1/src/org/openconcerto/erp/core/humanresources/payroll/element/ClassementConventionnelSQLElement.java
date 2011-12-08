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
import javax.swing.SwingConstants;


public class ClassementConventionnelSQLElement extends ComptaSQLConfElement {

    public ClassementConventionnelSQLElement() {
        super("CLASSEMENT_CONVENTIONNEL", "un classement conventionnel", "classements conventionnels");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NIVEAU");
        l.add("COEFF");
        l.add("INDICE");
        l.add("POSITION");
        l.add("ECHELON");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NIVEAU");
        l.add("COEFF");
        l.add("INDICE");
        l.add("POSITION");
        l.add("ECHELON");
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

                // Niveau
                JLabel labelNiveau = new JLabel(getLabelFor("NIVEAU"));
                labelNiveau.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNiveau = new JTextField();

                this.add(labelNiveau, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNiveau, c);

                // Coeff
                JLabel labelCoeff = new JLabel(getLabelFor("COEFF"));
                labelCoeff.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCoeff = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelCoeff, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textCoeff, c);

                // Indice
                JLabel labelIndice = new JLabel(getLabelFor("INDICE"));
                labelIndice.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textIndice = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelIndice, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textIndice, c);

                // Position
                JLabel labelPosition = new JLabel(getLabelFor("POSITION"));
                labelPosition.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textPosition = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;
                this.add(labelPosition, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textPosition, c);

                // Echelon
                JLabel labelEchelon = new JLabel(getLabelFor("ECHELON"));
                labelEchelon.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textEchelon = new JTextField();

                c.gridx = 0;
                c.gridy++;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 0;
                this.add(labelEchelon, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textEchelon, c);

                this.addSQLObject(textEchelon, "ECHELON");
                this.addSQLObject(textPosition, "POSITION");
                this.addSQLObject(textNiveau, "NIVEAU");
                this.addSQLObject(textCoeff, "COEFF");
                this.addSQLObject(textIndice, "INDICE");
            }
        };
    }
}
