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
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class MetriqueSQLElement extends ComptaSQLConfElement {

    public MetriqueSQLElement() {
        super("METRIQUE", "une métrique", "métriques");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("UNITE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("UNITE");
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
                this.add(new JLabel(getLabelFor("NOM")), c);
                c.gridx++;
                final JTextField textNom = new JTextField();
                this.add(textNom, c);

                // Nom
                c.gridx++;
                this.add(new JLabel("exprimé en"), c);
                c.gridx++;
                final SQLTextCombo textUnite = new SQLTextCombo();
                this.add(textUnite, c);
                this.addRequiredSQLObject(textNom, "NOM");
                this.addRequiredSQLObject(textUnite, "UNITE");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".quantity";
    }
}
