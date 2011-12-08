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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextCombo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class EcheancePanel extends JPanel {
    private JTextField aNJours = new JTextField();
    private JTextField leNJour = new JTextField();
    private JTextField textAJours = new JTextField("30", 4);
    private JTextField textLeJours = new JTextField("15", 4);
    private ITextCombo textComboAJours = new SQLTextCombo(true);
    private JRadioButton checkLeJour;
    private JRadioButton checkFinDeMois;
    private JRadioButton checkDateFacture;
    private JRadioButton checkComptant;
    private JRadioButton checkNJours;
    private SQLTextCombo textCustom;

    public EcheancePanel() {
        uiInit();
    }

    private void uiInit() {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 1, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;

        this.textLeJours = new JTextField("15", 3);
        this.textAJours = new JTextField("30", 3);

            this.checkComptant = new JRadioButton("paiement comptant");
        this.checkComptant.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setComptant(true);
            }

        });

        c.gridwidth = GridBagConstraints.REMAINDER;
        final JLabel label = new JLabel("Echéance : ");
        this.add(label);
        c.gridx++;
        this.textCustom = new SQLTextCombo();
        this.add(this.textCustom, c);
            label.setVisible(false);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        this.add(this.checkComptant, c);

        // Date de depot
        JLabel labelDate = new JLabel("A déposer après le");
        JDate dateDepot = new JDate(true);
        c.gridx += 3;
        c.gridwidth = 1;
        this.add(labelDate, c);
        c.gridx++;
        this.add(dateDepot, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        //
        this.checkNJours = new JRadioButton("paiement à");

        this.checkNJours.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setComptant(false);
            }
        });
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        this.add(this.checkNJours, c);

            c.gridx++;
            c.weightx = 1;
            this.add(this.textAJours, c);
        this.textAJours.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {

                setComptant(false);
            }
        });

        JLabel labelAJours = new JLabel("jours, ");
        c.weightx = 0;
        c.gridx++;
        this.add(labelAJours, c);

        this.checkFinDeMois = new JRadioButton("fin de mois");
        this.checkFinDeMois.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setComptant(false);

            }

        });
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 1;
        this.add(this.checkFinDeMois, c);

        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 1;
        JPanel pane = new JPanel(new GridLayout(1, 3));
        this.checkLeJour = new JRadioButton("le");
        this.checkLeJour.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setComptant(false);

            }

        });
        pane.add(this.checkLeJour);
        pane.add(this.textLeJours);

        this.textLeJours.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {

                // calculDate();
                setComptant(false);
            }
        });

        JLabel labelDuMois = new JLabel("du mois");
        pane.setBorder(null);
        pane.add(labelDuMois);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.add(pane, c);

        // Date de facture
        this.checkDateFacture = new JRadioButton("Date de facturation");
        this.checkDateFacture.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setComptant(false);
            }

        });
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 1;
        this.add(this.checkDateFacture, c);

        ButtonGroup grp1 = new ButtonGroup();
        grp1.add(this.checkComptant);
        grp1.add(this.checkNJours);

        ButtonGroup grp2 = new ButtonGroup();
        grp2.add(this.checkFinDeMois);
        grp2.add(this.checkDateFacture);
        grp2.add(this.checkLeJour);
        this.checkFinDeMois.setSelected(true);
        this.leNJour.setText("31");
        this.aNJours.setText("30");

    }

    public void setComptant(boolean b) {

        if (b) {
            this.checkComptant.setSelected(true);
            this.aNJours.setText("0");
            this.textComboAJours.setValue("0");
            this.leNJour.setText("0");
            setEnableEcheance(false);
        } else {

            this.aNJours.setText(this.textAJours.getText());
            if (this.checkLeJour.isSelected()) {
                this.leNJour.setText(this.textLeJours.getText());
            } else {
                if (this.checkDateFacture.isSelected()) {
                    this.leNJour.setText("0");
                } else {
                    this.leNJour.setText("31");
                }
            }
            setEnableEcheance(true);
        }

    }

    public void fixComptant(boolean b) {
        System.err.println("Fix comptant " + b);
        if (b) {
            this.checkComptant.setSelected(true);
            setComptant(true);
            this.checkNJours.setEnabled(false);
        } else {
            this.checkNJours.setSelected(true);
            this.checkNJours.setEnabled(true);
            setComptant(false);
        }
    }

    public void setComptantEnable(boolean b) {
        this.checkComptant.setEnabled(b);
    }

    private void setEnableEcheance(boolean b) {
        this.textAJours.setEditable(b);
        this.textComboAJours.setEnabled(b);
        this.textLeJours.setEditable(b);
        this.checkFinDeMois.setEnabled(b);
        this.checkDateFacture.setEnabled(b);
        this.checkLeJour.setEnabled(b);
    }

    public void setValue(int aJour, int nJour) {

        this.textAJours.setText(String.valueOf(aJour));
        this.textComboAJours.setValue(String.valueOf(aJour));
        this.textLeJours.setText(String.valueOf(nJour));

        if ((aJour == 0) && (nJour == 0)) {
            setComptant(true);
        } else {

            this.checkNJours.setEnabled(true);
            this.checkLeJour.setSelected(nJour != 31);
            this.checkNJours.setSelected(true);
            setComptant(false);
        }
    }

    public void setEnabled(boolean b) {

        this.textAJours.setEnabled(b);
        this.textComboAJours.setEnabled(b);
        this.textLeJours.setEnabled(b);
        this.checkLeJour.setEnabled(b);
        this.checkFinDeMois.setEnabled(b);
        this.checkComptant.setEnabled(b);
        this.checkNJours.setEnabled(b);
        this.checkDateFacture.setEnabled(b);
    }

    public JComponent getANjours() {
        return this.aNJours;
    }

    public JTextField getLeNjours() {
        return this.leNJour;
    }

    public JRadioButton getCheckNJours() {
        return this.checkNJours;
    }

    public SQLTextCombo getTextCustom() {
        return this.textCustom;
    }

}
