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
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

public class RegimeBaseSQLElement extends ComptaSQLConfElement {

    public RegimeBaseSQLElement() {
        super("REGIME_BASE", "un régime de base", "régimes de base");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_CODE_REGIME_BASE");
        l.add("ID_CODE_REGIME_MALADIE");
        l.add("ID_CODE_REGIME_AT");
        l.add("ID_CODE_REGIME_VIEL_P");
        l.add("ID_CODE_REGIME_VIEL_S");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_CODE_REGIME_BASE");
        l.add("ID_CODE_REGIME_MALADIE");
        l.add("ID_CODE_REGIME_AT");
        l.add("ID_CODE_REGIME_VIEL_P");
        l.add("ID_CODE_REGIME_VIEL_S");
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

                // Code regime base
                JLabel labelBase = new JLabel(getLabelFor("ID_CODE_REGIME_BASE"));
                ElementComboBox comboSelBase = new ElementComboBox();

                this.add(labelBase, c);
                c.gridx++;
                this.add(comboSelBase, c);

                // Code regime maladie
                JLabel labelMaladie = new JLabel(getLabelFor("ID_CODE_REGIME_MALADIE"));
                ElementComboBox comboSelMaladie = new ElementComboBox();

                c.gridy++;
                c.gridx = 0;
                this.add(labelMaladie, c);
                c.gridx++;
                this.add(comboSelMaladie, c);

                // Code regime AT
                JLabel labelAT = new JLabel(getLabelFor("ID_CODE_REGIME_AT"));
                ElementComboBox comboSelAT = new ElementComboBox();

                c.gridy++;
                c.gridx = 0;
                this.add(labelAT, c);
                c.gridx++;
                this.add(comboSelAT, c);

                // Code regime Viellesse pat
                JLabel labelVielP = new JLabel(getLabelFor("ID_CODE_REGIME_VIEL_P"));
                ElementComboBox comboSelVielP = new ElementComboBox();

                c.gridy++;
                c.gridx = 0;
                this.add(labelVielP, c);
                c.gridx++;
                this.add(comboSelVielP, c);

                // Code regime Viellesse pat
                JLabel labelVielS = new JLabel(getLabelFor("ID_CODE_REGIME_VIEL_S"));
                ElementComboBox comboSelVielS = new ElementComboBox();

                c.gridy++;
                c.gridx = 0;
                this.add(labelVielS, c);
                c.gridx++;
                this.add(comboSelVielS, c);

                this.addRequiredSQLObject(comboSelVielS, "ID_CODE_REGIME_VIEL_S");
                this.addRequiredSQLObject(comboSelVielP, "ID_CODE_REGIME_VIEL_P");
                this.addRequiredSQLObject(comboSelAT, "ID_CODE_REGIME_AT");
                this.addRequiredSQLObject(comboSelMaladie, "ID_CODE_REGIME_MALADIE");
                this.addRequiredSQLObject(comboSelBase, "ID_CODE_REGIME_BASE");
            }
        };
    }
}
