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
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class ContratSalarieSQLElement extends ComptaSQLConfElement {

    public ContratSalarieSQLElement() {
        super("CONTRAT_SALARIE", "un contrat salarié", "contrats salariés");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NATURE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NATURE");
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

                // Nature
                JLabel labelNature = new JLabel(getLabelFor("NATURE"));
                labelNature.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNature = new JTextField();

                this.add(labelNature, c);
                c.gridx++;
                c.weightx = 1;
                this.add(textNature, c);

                // Catégorie socioprofessionnelle
                JLabel labelCatSocio = new JLabel(getLabelFor("ID_CODE_EMPLOI"));
                labelCatSocio.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selCodeCatSocio = new ElementComboBox();
                selCodeCatSocio.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCatSocio, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selCodeCatSocio, c);

                // Contrat de travail
                JLabel labelContratTravail = new JLabel(getLabelFor("ID_CODE_CONTRAT_TRAVAIL"));
                labelContratTravail.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selContratTravail = new ElementComboBox();
                selContratTravail.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelContratTravail, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selContratTravail, c);

                // Droit Contrat de travail
                JLabel labelDroitContrat = new JLabel(getLabelFor("ID_CODE_DROIT_CONTRAT"));
                labelDroitContrat.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selDroitContrat = new ElementComboBox();
                selDroitContrat.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelDroitContrat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selDroitContrat, c);

                // caracteristiques activité
                JLabel labelCaractActivite = new JLabel(getLabelFor("ID_CODE_CARACT_ACTIVITE"));
                labelCaractActivite.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selCaractActivite = new ElementComboBox();
                selCaractActivite.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCaractActivite, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selCaractActivite, c);

                // Statut profesionnel
                JLabel labelStatutProf = new JLabel(getLabelFor("ID_CODE_STATUT_PROF"));
                labelStatutProf.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutProf = new ElementComboBox();
                selStatutProf.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutProf, c);
                c.gridx++;
                c.weightx = 1;
                this.add(selStatutProf, c);

                // Statut categoriel
                JLabel labelStatutCat = new JLabel(getLabelFor("ID_CODE_STATUT_CATEGORIEL"));
                labelStatutCat.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutCat = new ElementComboBox();
                selStatutCat.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutCat, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(selStatutCat, c);

                this.addSQLObject(selCodeCatSocio, "ID_CODE_EMPLOI");
                this.addSQLObject(selContratTravail, "ID_CODE_CONTRAT_TRAVAIL");
                this.addSQLObject(selCaractActivite, "ID_CODE_CARACT_ACTIVITE");
                this.addSQLObject(selDroitContrat, "ID_CODE_DROIT_CONTRAT");
                this.addSQLObject(selStatutProf, "ID_CODE_STATUT_PROF");
                this.addSQLObject(selStatutCat, "ID_CODE_STATUT_CATEGORIEL");
                this.addRequiredSQLObject(textNature, "NATURE");
            }
        };
    }
}
