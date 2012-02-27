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

                // Statut categoriel
                JLabel labelStatutCatConv = new JLabel(getLabelFor("ID_CODE_STATUT_CAT_CONV"));
                labelStatutCatConv.setHorizontalAlignment(SwingConstants.RIGHT);
                ElementComboBox selStatutCatConv = new ElementComboBox();
                selStatutCatConv.setInfoIconVisible(false);
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelStatutCatConv, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(selStatutCatConv, c);

                // Code UGRR
                JLabel labelCodeUGRR = new JLabel(getLabelFor("CODE_IRC_UGRR"));
                labelCodeUGRR.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeUGRR = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeUGRR, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeUGRR, c);
                addView(textCodeUGRR, "CODE_IRC_UGRR");

                JLabel labelNumUGRR = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_UGRR"));
                labelNumUGRR.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumUGRR = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumUGRR, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumUGRR, c);
                addView(textNumUGRR, "NUMERO_RATTACHEMENT_UGRR");

                // Code UGRC
                JLabel labelCodeUGRC = new JLabel(getLabelFor("CODE_IRC_UGRC"));
                labelCodeUGRC.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeUGRC = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeUGRC, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeUGRC, c);
                addView(textCodeUGRC, "CODE_IRC_UGRC");

                JLabel labelNumUGRC = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_UGRC"));
                labelNumUGRC.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumUGRC = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumUGRC, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumUGRC, c);
                addView(textNumUGRC, "NUMERO_RATTACHEMENT_UGRC");

                // Retraite
                JLabel labelCodeRetraite = new JLabel(getLabelFor("CODE_IRC_RETRAITE"));
                labelCodeRetraite.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textCodeRetraite = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelCodeRetraite, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textCodeRetraite, c);
                addView(textCodeRetraite, "CODE_IRC_RETRAITE");

                JLabel labelNumRetraite = new JLabel(getLabelFor("NUMERO_RATTACHEMENT_RETRAITE"));
                labelNumRetraite.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textNumRetraite = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                this.add(labelNumRetraite, c);
                c.gridx++;
                c.weighty = 1;
                c.weightx = 1;
                this.add(textNumRetraite, c);
                addView(textNumRetraite, "NUMERO_RATTACHEMENT_RETRAITE");

                this.addSQLObject(selCodeCatSocio, "ID_CODE_EMPLOI");
                this.addSQLObject(selContratTravail, "ID_CODE_CONTRAT_TRAVAIL");
                this.addSQLObject(selCaractActivite, "ID_CODE_CARACT_ACTIVITE");
                this.addSQLObject(selDroitContrat, "ID_CODE_DROIT_CONTRAT");
                this.addSQLObject(selStatutProf, "ID_CODE_STATUT_PROF");
                this.addSQLObject(selStatutCat, "ID_CODE_STATUT_CATEGORIEL");
                this.addSQLObject(selStatutCatConv, "ID_CODE_STATUT_CAT_CONV");
                this.addRequiredSQLObject(textNature, "NATURE");
            }
        };
    }
}
