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
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;


public class FichePayeElementSQLElement extends ComptaSQLConfElement {
    public FichePayeElementSQLElement() {
        super("FICHE_PAYE_ELEMENT", "un élément de fiche de paye", "éléments de fiche de paye");
    }

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("NOM");
        l.add("NB_BASE");
        l.add("TAUX_SAL");
        l.add("MONTANT_SAL_AJ");
        l.add("MONTANT_SAL_DED");
        l.add("TAUX_PAT");
        l.add("MONTANT_PAT");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();

        l.add("NOM");
        l.add("NB_BASE");
        l.add("TAUX_SAL");
        l.add("MONTANT_SAL_AJ");
        l.add("MONTANT_SAL_DED");
        l.add("TAUX_PAT");
        l.add("MONTANT_PAT");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new UISQLComponent(this) {
            public void addViews() {
                this.addRequiredSQLObject(new JTextField(), "NOM", "left");
                this.addRequiredSQLObject(new JTextField(), "NB_BASE", "right");

                this.addSQLObject(new JTextField(), "TAUX_SAL", "left");
                this.addSQLObject(new JTextField(), "MONTANT_SAL_AJ", "right");
                this.addSQLObject(new JTextField(), "MONTANT_SAL_DED", "right");

                this.addSQLObject(new JTextField(), "TAUX_PAT", "left");
                this.addSQLObject(new JTextField(), "MONTANT_PAT", "right");

            }
        };
    }
}
