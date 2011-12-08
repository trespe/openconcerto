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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.report.BalanceSheet;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ImpressionBalancePanel extends JPanel implements SpreadSheetGeneratorListener {

    private final JDate dateDeb, dateEnd;
    private JButton valid;
    private JButton annul;
    private JCheckBox checkImpr;
    private JCheckBox checkVisu;
    private JCheckBox checkClientCentral;
    private JCheckBox checkFournCentral;
    private JProgressBar bar = new JProgressBar(0, 3);
    private JTextField compteDeb, compteEnd;

    public ImpressionBalancePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));

        this.dateDeb = new JDate();
        this.dateEnd = new JDate();

        // Période
        this.add(new JLabel("Période du"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateDeb, c);
        // Chargement des valeurs par défaut
        String valueDateDeb = DefaultNXProps.getInstance().getStringProperty("BalanceDateDeb");
        if (valueDateDeb.trim().length() > 0) {
            Long l = new Long(valueDateDeb);
            this.dateDeb.setValue(new Date(l.longValue()));
        } else {
            this.dateDeb.setValue((Date) rowExercice.getObject("DATE_DEB"));
        }

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Au"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateEnd, c);
        // Chargement des valeurs par défaut
        String valueDateEnd = DefaultNXProps.getInstance().getStringProperty("BalanceDateEnd");
        if (valueDateEnd.trim().length() > 0) {
            Long l = new Long(valueDateEnd);
            this.dateEnd.setValue(new Date(l.longValue()));
        } else {
            this.dateEnd.setValue((Date) rowExercice.getObject("DATE_FIN"));
        }

        // Compte
        this.compteDeb = new JTextField();
        this.compteEnd = new JTextField();
        c.gridy++;
        c.gridx = 0;
        this.add(new JLabel("Du compte "), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.compteDeb, c);
        this.compteDeb.setText("1");
        this.compteEnd.setText("9");

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Au"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.compteEnd, c);

        // Centralisation Clients
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        this.checkClientCentral = new JCheckBox("Centralisation des comptes clients");
        this.add(this.checkClientCentral, c);

        // Centralisation Fournisseurs
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        this.checkFournCentral = new JCheckBox("Centralisation des comptes fournisseurs");
        this.add(this.checkFournCentral, c);

        // Progress bar
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.bar.setStringPainted(true);
        this.add(this.bar, c);

        this.valid = new JButton("Valider");
        this.annul = new JButton("Fermer");
        this.checkImpr = new JCheckBox("Impression");
        this.checkVisu = new JCheckBox("Visualisation");

        c.gridwidth = 2;
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        this.add(this.checkImpr, c);
        this.checkImpr.setSelected(true);
        c.gridx += 2;
        this.add(this.checkVisu, c);

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        this.add(this.valid, c);
        c.gridx += 2;
        this.add(this.annul, c);
        checkValidity();
        this.valid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                valid.setEnabled(false);

                bar.setString(null);
                bar.setValue(1);
                new Thread(new Runnable() {
                    public void run() {
                        BalanceSheet bSheet = new BalanceSheet(dateDeb.getDate(), dateEnd.getDate(), compteDeb.getText(), compteEnd.getText(), checkClientCentral.isSelected(), checkFournCentral
                                .isSelected());
                        final SpreadSheetGeneratorCompta generator = new SpreadSheetGeneratorCompta(bSheet, "Balance" + new Date().getTime(), checkImpr.isSelected(), checkVisu.isSelected());

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                bar.setValue(2);
                                generator.addGenerateListener(ImpressionBalancePanel.this);
                            }
                        });
                    }
                }).start();

            }
        });
        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(ImpressionBalancePanel.this)).dispose();
            }
        });

        this.dateDeb.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                checkValidity();
                storeValue();
            }
        });
        this.dateEnd.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                checkValidity();
                storeValue();
            }
        });
    }

    private void checkValidity() {

        Date beginDate = this.dateDeb.getDate();
        Date endDate = this.dateEnd.getDate();

        // System.err.println("Check validity between ");
        if (beginDate == null || endDate == null) {
            this.valid.setEnabled(false);
        } else {
            if (beginDate.after(endDate)) {
                this.valid.setEnabled(false);
            } else {
                this.valid.setEnabled(true);
            }
        }
    }

    private void storeValue() {

        // Set date debut
        Date d = this.dateDeb.getDate();
        if (d != null) {
            DefaultNXProps.getInstance().setProperty("BalanceDateDeb", String.valueOf(d.getTime()));
        }

        // Set date Fin
        Date dFin = this.dateEnd.getDate();
        if (dFin != null) {
            DefaultNXProps.getInstance().setProperty("BalanceDateEnd", String.valueOf(dFin.getTime()));
        }

        DefaultNXProps.getInstance().store();
    }

    public void taskEnd() {
        bar.setValue(3);
        bar.setString("Terminée");
        valid.setEnabled(true);
    }
}
