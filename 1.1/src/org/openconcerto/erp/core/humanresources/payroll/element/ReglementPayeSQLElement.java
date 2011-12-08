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
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.model.ISQLCompteSelector;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.ui.RadioButtons;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ReglementPayeSQLElement extends ComptaSQLConfElement {

    public ReglementPayeSQLElement() {
        super("REGLEMENT_PAYE", "un règlement de paye", "règlements de paye");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM_BANQUE");
        l.add("RIB");
        l.add("ID_COMPTE_PCE");
        l.add("ID_MODE_REGLEMENT_PAYE");
        l.add("LE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM_BANQUE");
        l.add("RIB");
        return l;
    }

    /*
     * protected List getPrivateFields() { final List l = new ArrayList();
     * l.add("ID_MODE_REGLEMENT"); return l; }
     */

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

                /***********************************************************************************
                 * COORDONNEES DE LA BANQUE
                 **********************************************************************************/
                JPanel panelBanque = new JPanel();
                panelBanque.setOpaque(false);
                panelBanque.setBorder(BorderFactory.createTitledBorder("Coordonnées bancaires"));
                panelBanque.setLayout(new GridBagLayout());

                // Nom Banque
                JLabel labelNomBq = new JLabel(getLabelFor("NOM_BANQUE"));
                labelNomBq.setHorizontalAlignment(SwingConstants.RIGHT);
                SQLTextCombo textNomBq = new SQLTextCombo();

                panelBanque.add(labelNomBq, c);
                c.gridx++;
                c.weightx = 1;
                panelBanque.add(textNomBq, c);
                c.weightx = 0;

                // RIB
                JLabel labelRIB = new JLabel(getLabelFor("RIB"));
                labelRIB.setHorizontalAlignment(SwingConstants.RIGHT);
                JTextField textRIB = new JTextField();

                c.gridy++;
                c.gridx = 0;
                panelBanque.add(labelRIB, c);
                c.gridx++;
                c.weightx = 1;
                panelBanque.add(textRIB, c);
                c.weightx = 0;

                c.gridx = 0;
                c.gridy = 0;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.weightx = 1;
                this.add(panelBanque, c);
                c.weightx = 0;
                c.anchor = GridBagConstraints.WEST;

                /***********************************************************************************
                 * Mode de reglement
                 **********************************************************************************/
                JPanel panelReglement = new JPanel();
                // panelReglement.setOpaque(false);
                panelReglement.setBorder(BorderFactory.createTitledBorder(getLabelFor("ID_MODE_REGLEMENT_PAYE")));
                panelReglement.setLayout(new GridBagLayout());

                // Mode
                RadioButtons typeRegl = new RadioButtons("NOM");
                typeRegl.setLayout(new FlowLayout(FlowLayout.LEFT));
                c.gridy = 0;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                panelReglement.add(typeRegl, c);

                // Date de paiement
                final JRadioButton radioLe = new JRadioButton("Le");
                final JTextField textLe = new JTextField(2);
                JLabel labelDu = new JLabel("du mois.");
                final JRadioButton radioFin = new JRadioButton("Fin de mois");

                c.gridwidth = 1;
                c.weightx = 0;
                c.gridy++;
                panelReglement.add(radioLe, c);
                c.gridx++;
                panelReglement.add(textLe, c);
                textLe.setText("31");
                c.gridx++;
                c.weightx = 1;
                panelReglement.add(labelDu, c);

                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panelReglement.add(radioFin, c);
                radioFin.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textLe.setText("31");
                        textLe.setEditable(false);
                        textLe.setEnabled(false);
                    }
                });

                radioLe.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textLe.setEditable(true);
                        textLe.setEnabled(true);
                    }
                });
                ButtonGroup groupDate = new ButtonGroup();
                groupDate.add(radioLe);
                groupDate.add(radioFin);
                radioFin.setSelected(true);
                textLe.setEditable(false);
                textLe.setEnabled(false);

                c.weightx = 0;
                c.gridwidth = 1;

                c.gridx = 1;
                c.gridy = 0;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(panelReglement, c);
                c.anchor = GridBagConstraints.WEST;

                /***********************************************************************************
                 * Comptabilite
                 **********************************************************************************/
                JPanel panelCompta = new JPanel();
                panelCompta.setOpaque(false);
                panelCompta.setBorder(BorderFactory.createTitledBorder("Comptabilité"));
                panelCompta.setLayout(new GridBagLayout());

                // Compte du salarié
                JLabel labelCompteSal = new JLabel(getLabelFor("ID_COMPTE_PCE"));
                labelCompteSal.setHorizontalAlignment(SwingConstants.RIGHT);
                ISQLCompteSelector compteSal = new ISQLCompteSelector();

                c.gridx = 0;
                c.gridy = 0;
                c.gridwidth = 1;
                panelCompta.add(labelCompteSal, c);
                c.gridx++;
                c.weightx = 1;
                panelCompta.add(compteSal, c);

                c.gridx = 0;
                c.gridy = 1;
                c.weighty = 1;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(panelCompta, c);

                this.addSQLObject(textNomBq, "NOM_BANQUE");
                this.addSQLObject(textRIB, "RIB");
                this.addRequiredSQLObject(typeRegl, "ID_MODE_REGLEMENT_PAYE");
                this.addRequiredSQLObject(textLe, "LE");
                this.addRequiredSQLObject(compteSal, "ID_COMPTE_PCE");
                // compteSal.init();
            }

            protected SQLRowValues createDefaults() {

                SQLRowValues rowVals = new SQLRowValues(getTable());
                rowVals.put("ID_MODE_REGLEMENT_PAYE", Integer.valueOf(2));
                rowVals.put("ID_COMPTE_PCE", ComptePCESQLElement.getId("421"));
                rowVals.put("LE", 31);
                return rowVals;
            }
        };
    }
}
