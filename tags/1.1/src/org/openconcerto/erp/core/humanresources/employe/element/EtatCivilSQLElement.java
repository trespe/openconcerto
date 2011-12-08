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
 
 package org.openconcerto.erp.core.humanresources.employe.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class EtatCivilSQLElement extends ComptaSQLConfElement {

    public EtatCivilSQLElement() {
        super("ETAT_CIVIL", "un état civil", "états civils");
    }

    // MAYBE un champ nationnalité???

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_NAISSANCE");
        l.add("NB_ENFANTS");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE_NAISSANCE");
        l.add("NB_ENFANTS");
        return l;
    }

    protected List<String> getPrivateFields() {

        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            JDate dateNaissance;
            JTextField numeroSS, numeroTel, mail, portable;
            JTextField nbEnfants, lieuNaissance, nombrePersonne;
            ElementComboBox comboSituationFam;
            ElementComboBox comboPays, comboDepartement;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                /***********************************************************************************
                 * PANEL ADRESSE
                 **********************************************************************************/
                JPanel panelInfos = new JPanel();
                panelInfos.setOpaque(false);
                panelInfos.setBorder(BorderFactory.createTitledBorder(getLabelFor("ID_ADRESSE")));
                panelInfos.setLayout(new GridBagLayout());

                this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltModeRegl = (ElementSQLObject) this.getView("ID_ADRESSE");
                panelInfos.add(eltModeRegl, c);

                // c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(panelInfos, c);
                c.anchor = GridBagConstraints.WEST;
                // c.fill = GridBagConstraints.HORIZONTAL;

                /***********************************************************************************
                 * PANEL CONTACT
                 **********************************************************************************/
                JPanel panelContact = new JPanel();
                panelContact.setOpaque(false);
                panelContact.setBorder(BorderFactory.createTitledBorder("Contact"));
                panelContact.setLayout(new GridBagLayout());

                // Téléphone
                c.fill = GridBagConstraints.NONE;
                this.numeroTel = new JTextField(12);
                JLabel labelNumeroTel = new JLabel(getLabelFor("TELEPHONE"));
                labelNumeroTel.setHorizontalAlignment(SwingConstants.RIGHT);
                panelContact.add(labelNumeroTel, c);
                c.gridx++;
                c.weightx = 1;
                panelContact.add(this.numeroTel, c);

                // portable
                c.gridwidth = 1;
                c.gridx = 0;
                c.weightx = 0;
                c.gridy++;

                this.portable = new JTextField(12);
                JLabel labelNumeroPort = new JLabel(getLabelFor("PORTABLE"));
                labelNumeroPort.setHorizontalAlignment(SwingConstants.RIGHT);
                panelContact.add(labelNumeroPort, c);
                c.gridx++;
                c.weightx = 1;
                panelContact.add(this.portable, c);
                c.fill = GridBagConstraints.HORIZONTAL;

                // mail
                c.gridwidth = 1;
                c.gridx = 0;
                c.gridy++;
                c.weightx = 0;

                this.mail = new JTextField();
                JLabel labelMail = new JLabel(getLabelFor("MAIL"));
                labelMail.setHorizontalAlignment(SwingConstants.RIGHT);
                panelContact.add(labelMail, c);
                c.gridx++;
                c.weightx = 1;
                c.weighty = 1;
                panelContact.add(this.mail, c);

                c.gridx = 1;
                c.gridy = 0;

                c.anchor = GridBagConstraints.NORTHWEST;
                c.weighty = 0;
                this.add(panelContact, c);
                c.anchor = GridBagConstraints.WEST;

                /***********************************************************************************
                 * PANEL NAISSANCE
                 **********************************************************************************/
                JPanel panelNaissance = new JPanel();
                panelNaissance.setOpaque(false);
                panelNaissance.setBorder(BorderFactory.createTitledBorder("Date et lieu de naissance"));
                panelNaissance.setLayout(new GridBagLayout());

                // Date de naissance
                c.gridwidth = 1;
                c.gridx = 0;
                c.gridy = 0;
                JLabel labelDateNaissance = new JLabel(getLabelFor("DATE_NAISSANCE"));
                labelDateNaissance.setHorizontalAlignment(SwingConstants.RIGHT);
                this.dateNaissance = new JDate();
                panelNaissance.add(labelDateNaissance, c);
                c.gridx++;
                panelNaissance.add(this.dateNaissance, c);

                // Commune de naissance
                JLabel labelLieuNaissance = new JLabel(getLabelFor("COMMUNE_NAISSANCE"));
                labelLieuNaissance.setHorizontalAlignment(SwingConstants.RIGHT);
                this.lieuNaissance = new JTextField();
                c.gridx++;
                panelNaissance.add(labelLieuNaissance, c);
                c.gridx++;
                c.weightx = 1;
                panelNaissance.add(this.lieuNaissance, c);

                // Departement de naissance
                c.weightx = 0;
                JLabel labelDptNaissance = new JLabel(getLabelFor("ID_DEPARTEMENT_NAISSANCE"));
                labelDptNaissance.setHorizontalAlignment(SwingConstants.RIGHT);
                this.comboDepartement = new ElementComboBox(false, 20);

                c.gridx = 0;
                c.gridy++;
                panelNaissance.add(labelDptNaissance, c);
                c.gridx++;
                c.gridwidth = 1;
                c.weightx = 1;
                panelNaissance.add(this.comboDepartement, c);
                c.gridwidth = 1;

                // Pays de naissance
                JLabel labelPaysNaissance = new JLabel(getLabelFor("ID_PAYS_NAISSANCE"));
                labelPaysNaissance.setHorizontalAlignment(SwingConstants.RIGHT);
                this.comboPays = new ElementComboBox(false, 20);
                c.gridx++;
                c.weightx = 0;
                panelNaissance.add(labelPaysNaissance, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                c.weighty = 1;
                panelNaissance.add(this.comboPays, c);
                c.gridwidth = 1;

                c.gridx = 0;
                c.weighty = 0;
                c.gridy = 1;
                c.weightx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(panelNaissance, c);
                c.anchor = GridBagConstraints.WEST;
                c.gridwidth = 1;

                /***********************************************************************************
                 * SITUATION
                 **********************************************************************************/
                JPanel panelSituation = new JPanel();
                panelSituation.setOpaque(false);
                panelSituation.setBorder(BorderFactory.createTitledBorder("Situation personnelle"));
                panelSituation.setLayout(new GridBagLayout());
                // Numero SS
                c.gridy = 0;
                c.gridx = 0;
                this.numeroSS = new JTextField();
                JLabel labelNumeroSS = new JLabel(getLabelFor("NUMERO_SS"));
                labelNumeroSS.setHorizontalAlignment(SwingConstants.RIGHT);
                panelSituation.add(labelNumeroSS, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                panelSituation.add(this.numeroSS, c);
                c.weightx = 0;
                c.gridwidth = 1;

                // Situation familiale
                JLabel labelSitutationFamiliale = new JLabel(getLabelFor("ID_SITUATION_FAMILIALE"));
                labelSitutationFamiliale.setHorizontalAlignment(SwingConstants.RIGHT);

                this.comboSituationFam = new ElementComboBox(false);

                c.gridx = 0;
                c.gridy++;
                panelSituation.add(labelSitutationFamiliale, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                panelSituation.add(this.comboSituationFam, c);
                c.gridwidth = 1;
                c.weightx = 0;

                // Nombre d'enfants
                JLabel labelNombreEnfants = new JLabel(getLabelFor("NB_ENFANTS"));
                labelNombreEnfants.setHorizontalAlignment(SwingConstants.RIGHT);
                this.nbEnfants = new JTextField();
                c.gridx = 0;
                c.gridy++;
                panelSituation.add(labelNombreEnfants, c);
                c.gridx++;
                c.weightx = 1;
                panelSituation.add(this.nbEnfants, c);
                c.weightx = 0;

                // Nombre de personnes à charge
                JLabel labelNombrePersonne = new JLabel(getLabelFor("NB_PERS_A_CHARGE"));
                labelNombrePersonne.setHorizontalAlignment(SwingConstants.RIGHT);
                this.nombrePersonne = new JTextField(3);
                c.gridx++;
                panelSituation.add(labelNombrePersonne, c);
                c.gridx++;
                c.weightx = 1;
                c.weighty = 1;
                panelSituation.add(this.nombrePersonne, c);
                c.weightx = 1;
                c.weighty = 0;

                c.gridx = 0;
                c.gridy = 2;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weighty = 1;
                this.add(panelSituation, c);
                c.gridwidth = 1;

                this.addRequiredSQLObject(this.dateNaissance, "DATE_NAISSANCE");
                this.addSQLObject(this.lieuNaissance, "COMMUNE_NAISSANCE");
                this.addSQLObject(this.nbEnfants, "NB_ENFANTS");
                this.addSQLObject(this.nombrePersonne, "NB_PERS_A_CHARGE");
                this.addRequiredSQLObject(this.numeroSS, "NUMERO_SS");
                this.addSQLObject(this.comboPays, "ID_PAYS_NAISSANCE");
                this.addSQLObject(this.comboDepartement, "ID_DEPARTEMENT_NAISSANCE");
                this.addRequiredSQLObject(this.comboSituationFam, "ID_SITUATION_FAMILIALE");
                this.addSQLObject(this.numeroTel, "TELEPHONE");
                this.addSQLObject(this.portable, "PORTABLE");
                this.addSQLObject(this.mail, "MAIL");
                this.comboPays.setButtonsVisible(false);
                this.comboDepartement.setButtonsVisible(false);
                this.comboSituationFam.setButtonsVisible(false);
            }

            /*
             * 
             * protected SQLRowValues createDefaults() {
             * 
             * final SQLRowValues vals = new SQLRowValues(EtatCivilSQLElement.this.getTable());
             * 
             * vals.put("NB_ENFANTS", 0); vals.put("NB_PERS_A_CHARGE", 0);
             * vals.put("ID_PAYS_NAISSANCE", 61);
             * 
             * System.err.println("GET DEFAULTS");
             * 
             * return vals; }
             */
            protected SQLRowValues createDefaults() {
                final SQLRowValues vals = new SQLRowValues(EtatCivilSQLElement.this.getTable());

                vals.loadAllSafe(EtatCivilSQLElement.this.getTable().getRow(1));

                System.err.println("GET DEFAULTS");

                return vals;
            }
        };

    }
}
