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
import org.openconcerto.erp.model.PrixHT;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class InfosSalariePayeSQLElement extends ComptaSQLConfElement {

    public InfosSalariePayeSQLElement() {
        super("INFOS_SALARIE_PAYE", "des informations salarié-paye", "informations salariés-payes");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_IDCC");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_IDCC");
        return l;
    }

    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_CONTRAT_SALARIE");
        l.add("ID_CLASSEMENT_CONVENTIONNEL");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JTextField dureeMois;
            private JTextField dureeHebdo;

            private DocumentListener listenerMois = new DocumentListener() {

                public void insertUpdate(DocumentEvent e) {
                    updateHebdo();
                }

                public void removeUpdate(DocumentEvent e) {
                    updateHebdo();
                }

                public void changedUpdate(DocumentEvent e) {
                    updateHebdo();
                }
            };

            private DocumentListener listenerHebdo = new DocumentListener() {

                public void insertUpdate(DocumentEvent e) {
                    updateMois();
                }

                public void removeUpdate(DocumentEvent e) {
                    updateMois();
                }

                public void changedUpdate(DocumentEvent e) {
                    updateMois();
                }
            };

            public void addViews() {
                this.setLayout(new GridBagLayout());
                GridBagConstraints c = new DefaultGridBagConstraints();

                // Convention collective
                JLabel labelConvention = new JLabel(getLabelFor("ID_IDCC"));
                labelConvention.setHorizontalAlignment(SwingConstants.RIGHT);
                final ElementComboBox comboConvention = new ElementComboBox();

                this.add(labelConvention, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.add(comboConvention, c);
                c.gridwidth = 1;

                // Classement conventionnel
                c.fill = GridBagConstraints.BOTH;
                JPanel panelClassement = new JPanel();
                panelClassement.setOpaque(false);
                panelClassement.setBorder(BorderFactory.createTitledBorder("Classement conventionnel"));
                panelClassement.setLayout(new GridBagLayout());
                this.addView("ID_CLASSEMENT_CONVENTIONNEL", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltClassement = (ElementSQLObject) this.getView("ID_CLASSEMENT_CONVENTIONNEL");
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1;
                panelClassement.add(eltClassement, c);

                c.gridy = 1;
                c.gridx = 0;
                c.gridwidth = 2;
                c.weightx = 0;
                this.add(panelClassement, c);
                c.gridwidth = 1;

                // Contrat
                c.fill = GridBagConstraints.BOTH;
                JPanel panelContrat = new JPanel();
                panelContrat.setOpaque(false);
                panelContrat.setBorder(BorderFactory.createTitledBorder("Contrat de travail"));
                panelContrat.setLayout(new GridBagLayout());
                GridBagConstraints c2 = new DefaultGridBagConstraints();
                c2.fill = GridBagConstraints.HORIZONTAL;
                c2.weighty = 1;
                c2.weightx = 1;
                c2.anchor = GridBagConstraints.NORTH;
                this.addView("ID_CONTRAT_SALARIE", REQ + ";" + DEC + ";" + SEP);
                ElementSQLObject eltContrat = (ElementSQLObject) this.getView("ID_CONTRAT_SALARIE");
                panelContrat.add(eltContrat, c2);

                c.gridx = 2;
                c.gridy = 1;
                c.gridheight = 2;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                this.add(panelContrat, c);
                c.gridwidth = 1;
                c.gridheight = 1;

                /***********************************************************************************
                 * DATE ENTREE / SORTIE
                 **********************************************************************************/
                JPanel panelEntreeSortie = new JPanel();
                panelEntreeSortie.setOpaque(false);
                panelEntreeSortie.setBorder(BorderFactory.createTitledBorder("Entrée/Sortie"));
                panelEntreeSortie.setLayout(new GridBagLayout());

                // Date d'arrivée dans la société
                JLabel labelDateArrive = new JLabel(getLabelFor("DATE_ARRIVE"));
                labelDateArrive.setHorizontalAlignment(SwingConstants.RIGHT);
                JDate dateArrive = new JDate(true);
                c.gridy = 0;
                c.gridx = 0;
                c.weightx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                panelEntreeSortie.add(labelDateArrive, c);
                c.gridx++;
                panelEntreeSortie.add(dateArrive, c);

                // Date de sortie
                JLabel labelDateSortie = new JLabel(getLabelFor("DATE_SORTIE"));
                labelDateSortie.setHorizontalAlignment(SwingConstants.RIGHT);
                JDate dateSortie = new JDate();

                c.gridy++;
                c.gridx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                panelEntreeSortie.add(labelDateSortie, c);
                c.gridx++;
                panelEntreeSortie.add(dateSortie, c);

                /*
                 * // Ancienneté JLabel labelAnc = new
                 * JLabel(Configuration.getInstance().getTranslator
                 * ().getLabelFor(InfosSalariePayeSQLElement
                 * .this.getTable().getField("DATE_SORTIE")));
                 * labelDateSortie.setHorizontalAlignment(SwingConstants.RIGHT); JTextField textAnc
                 * = new JTextField(5, false);
                 * 
                 * c.gridy++; c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL;
                 * panelEntreeSortie.add(labelAnc, c); c.gridx++; panelEntreeSortie.add(textAnc, c);
                 */
                c.gridx = 0;
                c.gridy = 2;
                c.gridwidth = 2;
                this.add(panelEntreeSortie, c);
                c.gridwidth = 1;

                /***********************************************************************************
                 * Valeurs de bases
                 **********************************************************************************/
                JPanel panelBase = new JPanel();
                panelBase.setOpaque(false);
                panelBase.setBorder(BorderFactory.createTitledBorder("Valeurs de base"));
                panelBase.setLayout(new GridBagLayout());

                // Durée mensuelle
                JLabel labelDureeMois = new JLabel(getLabelFor("DUREE_MOIS"));
                labelDureeMois.setHorizontalAlignment(SwingConstants.RIGHT);
                this.dureeMois = new JTextField();
                c.gridy = 0;
                c.gridx = 0;
                c.gridheight = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                panelBase.add(labelDureeMois, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(this.dureeMois, c);

                // Durée hebdomadaire
                JLabel labelDureeHebdo = new JLabel(getLabelFor("DUREE_HEBDO"));
                labelDureeHebdo.setHorizontalAlignment(SwingConstants.RIGHT);
                this.dureeHebdo = new JTextField();
                c.gridx++;
                c.weightx = 0;
                panelBase.add(labelDureeHebdo, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(this.dureeHebdo, c);

                // Salaire de base
                JLabel labelSalaireBase = new JLabel(getLabelFor("SALAIRE_MOIS"));
                labelSalaireBase.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField salaireBase = new JTextField();
                c.gridx++;
                c.weightx = 0;
                panelBase.add(labelSalaireBase, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(salaireBase, c);

                // Taux AT
                JLabel labelTauxAT = new JLabel(getLabelFor("TAUX_AT"));
                labelTauxAT.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField tauxAT = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelBase.add(labelTauxAT, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(tauxAT, c);

                // Congés payés
                JLabel labelConges = new JLabel(getLabelFor("CONGES_PAYES"));
                labelConges.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField conges = new JTextField();
                c.gridx++;
                c.weightx = 0;
                panelBase.add(labelConges, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(conges, c);

                // Code AT
                JLabel labelCodeAT = new JLabel(getLabelFor("CODE_AT"));
                labelCodeAT.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField CodeAT = new JTextField();
                c.gridy++;
                c.gridx = 0;
                c.weightx = 0;
                panelBase.add(labelCodeAT, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(CodeAT, c);
                addView(CodeAT, "CODE_AT");

                // Code section AT
                JLabel labelSectionAT = new JLabel(getLabelFor("CODE_SECTION_AT"));
                labelSectionAT.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField sectionAT = new JTextField();
                c.gridx++;
                c.weightx = 0;
                panelBase.add(labelSectionAT, c);
                c.gridx++;
                c.weightx = 1;
                panelBase.add(sectionAT, c);
                addView(sectionAT, "CODE_SECTION_AT");

                c.gridy = 3;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weighty = 1;
                c.weightx = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(panelBase, c);

                this.addRequiredSQLObject(comboConvention, "ID_IDCC");
                this.addRequiredSQLObject(dateArrive, "DATE_ARRIVE");
                this.addSQLObject(dateSortie, "DATE_SORTIE");
                this.addRequiredSQLObject(this.dureeHebdo, "DUREE_HEBDO");
                this.addRequiredSQLObject(this.dureeMois, "DUREE_MOIS");
                this.addRequiredSQLObject(tauxAT, "TAUX_AT");
                this.addRequiredSQLObject(conges, "CONGES_PAYES");
                this.addRequiredSQLObject(salaireBase, "SALAIRE_MOIS");
                // this.addRequiredSQLObject(textAnc, "ANCIENNETE");

                // Listener
                this.dureeMois.getDocument().addDocumentListener(this.listenerMois);
                this.dureeHebdo.getDocument().addDocumentListener(this.listenerHebdo);
            }

            private void updateHebdo() {

                if (this.dureeMois.getText().trim().length() == 0) {
                    return;
                } else {

                    this.dureeHebdo.getDocument().removeDocumentListener(this.listenerHebdo);

                    this.dureeHebdo.setText(String.valueOf(new PrixHT(Float.parseFloat(this.dureeMois.getText()) / (52.0 / 12.0)).getValue()));

                    this.dureeHebdo.getDocument().addDocumentListener(this.listenerHebdo);
                }
            }

            private void updateMois() {

                if (this.dureeHebdo.getText().trim().length() == 0) {
                    return;
                } else {

                    this.dureeMois.getDocument().removeDocumentListener(this.listenerMois);

                    this.dureeMois.setText(String.valueOf(new PrixHT(Float.parseFloat(this.dureeHebdo.getText()) * (52.0 / 12.0)).getValue()));

                    this.dureeMois.getDocument().addDocumentListener(this.listenerMois);
                }
            }

            @Override
            public void select(SQLRowAccessor r) {
                this.dureeMois.getDocument().removeDocumentListener(this.listenerMois);
                this.dureeHebdo.getDocument().removeDocumentListener(this.listenerHebdo);

                super.select(r);

                this.dureeMois.getDocument().addDocumentListener(this.listenerMois);
                this.dureeHebdo.getDocument().addDocumentListener(this.listenerHebdo);
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".info";
    }
}
