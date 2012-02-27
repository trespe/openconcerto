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
import org.openconcerto.erp.core.finance.accounting.report.GrandLivreSheet;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ImpressionGrandLivrePanel extends JPanel implements SpreadSheetGeneratorListener {

    private final JDate dateDeb, dateEnd;
    private JButton valid;
    private JButton annul;
    private JCheckBox checkImpr;
    private JCheckBox checkVisu;
    private JTextField compteDeb, compteEnd;
    private int mode = GrandLivreSheet.MODEALL;
    private JProgressBar bar = new JProgressBar(0, 3);

    public ImpressionGrandLivrePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));

        this.dateDeb = new JDate();
        this.dateEnd = new JDate();

        // Période
        c.weightx = 0;
        this.add(new JLabel("Période du", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateDeb, c);
        // Chargement des valeurs par défaut
        String valueDateDeb = DefaultNXProps.getInstance().getStringProperty("GrandLivreDateDeb");
        if (valueDateDeb.trim().length() > 0) {
            Long l = new Long(valueDateDeb);
            this.dateDeb.setValue(new Date(l.longValue()));
        } else {
            this.dateDeb.setValue((Date) rowExercice.getObject("DATE_DEB"));
        }

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("au"), c);
        c.gridx++;

        c.fill = GridBagConstraints.NONE;
        this.add(this.dateEnd, c);
        // Chargement des valeurs par défaut
        String valueDateEnd = DefaultNXProps.getInstance().getStringProperty("GrandLivreDateEnd");
        if (valueDateEnd.trim().length() > 0) {
            Long l = new Long(valueDateEnd);
            this.dateEnd.setValue(new Date(l.longValue()));
        } else {
            this.dateEnd.setValue((Date) rowExercice.getObject("DATE_FIN"));
        }

        // Compte
        this.compteDeb = new JTextField(8);
        this.compteEnd = new JTextField(8);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Du compte", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.NONE;
        JPanel pCompte = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        pCompte.add(this.compteDeb);
        pCompte.add(new JLabel(" au compte "));
        pCompte.add(this.compteEnd);

        this.add(pCompte, c);

        c.gridx = 0;
        c.gridwidth = 4;
        c.gridy++;
        this.add(new JLabelBold("Options"), c);
        final JCheckBox boxCumulsAnts = new JCheckBox("Cumuls antérieurs");
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        this.add(boxCumulsAnts, c);
        boxCumulsAnts.setSelected(true);

        final JCheckBox boxCentralClient = new JCheckBox("Centralisation comptes clients");
        c.gridx += 2;
        c.gridwidth = 2;
        this.add(boxCentralClient, c);

        final JCheckBox boxCompteSolde = new JCheckBox("Inclure les comptes soldés");
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        this.add(boxCompteSolde, c);
        boxCompteSolde.setSelected(true);

        final JCheckBox boxCentralFourn = new JCheckBox("Centralisation comptes fournisseurs");
        c.gridx += 2;
        c.gridwidth = 2;
        this.add(boxCentralFourn, c);

        // Journal à exclure
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        this.add(new JLabel("Exclure le journal", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 3;
        final ElementComboBox comboJrnl = new ElementComboBox(true);
        comboJrnl.init(Configuration.getInstance().getDirectory().getElement("JOURNAL"));
        this.add(comboJrnl, c);

        // Radio mode
        JRadioButton radioAll = new JRadioButton(new AbstractAction("Toutes") {
            public void actionPerformed(ActionEvent e) {
                mode = GrandLivreSheet.MODEALL;
            }
        });

        JRadioButton radioLettree = new JRadioButton(new AbstractAction("Lettrées") {
            public void actionPerformed(ActionEvent e) {
                mode = GrandLivreSheet.MODELETTREE;
            }
        });

        JRadioButton radioNonLettree = new JRadioButton(new AbstractAction("Non lettrées") {
            public void actionPerformed(ActionEvent e) {
                mode = GrandLivreSheet.MODENONLETTREE;
            }
        });

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.add(new JLabelBold("Ecritures à inclure"), c);
        JPanel panelMode = new JPanel();
        panelMode.add(radioAll);
        panelMode.add(radioLettree);
        panelMode.add(radioNonLettree);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        ButtonGroup group = new ButtonGroup();
        group.add(radioAll);
        group.add(radioLettree);
        group.add(radioNonLettree);
        radioAll.setSelected(true);

        this.add(panelMode, c);

        // Progress bar
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.gridx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.add(new JLabelBold("Progression de la creation du grand livre"), c);
        c.gridy++;
        this.bar.setStringPainted(true);
        this.add(this.bar, c);

        this.valid = new JButton("Valider");
        this.annul = new JButton("Fermer");
        this.checkImpr = new JCheckBox("Impression");
        this.checkVisu = new JCheckBox("Visualisation");

        // Print & View
        final JPanel panelPrintView = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));

        panelPrintView.add(this.checkImpr);
        panelPrintView.add(this.checkVisu);
        this.checkImpr.setSelected(true);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 4;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        this.add(panelPrintView, c);

        // OK, Cancel
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        JPanel panelOkCancel = new JPanel();
        panelOkCancel.add(this.valid);
        panelOkCancel.add(this.annul);
        this.add(panelOkCancel, c);
        this.checkValidity();
        this.valid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                valid.setEnabled(false);
                bar.setString(null);
                bar.setValue(1);
                new Thread(new Runnable() {
                    public void run() {
                        GrandLivreSheet bSheet = new GrandLivreSheet(dateDeb.getDate(), dateEnd.getDate(), compteDeb.getText().trim(), compteEnd.getText().trim(), mode, boxCumulsAnts.isSelected(),
                                !boxCompteSolde.isSelected(), boxCentralClient.isSelected(), boxCentralFourn.isSelected(), comboJrnl.getSelectedId());
                        if (bSheet.getSize() == 0) {
                            JOptionPane.showMessageDialog(ImpressionGrandLivrePanel.this, "Aucune écriture trouvée");
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    taskEnd();
                                }
                            });
                        } else {
                            final SpreadSheetGeneratorCompta generator = new SpreadSheetGeneratorCompta(bSheet, "GrandLivre" + Calendar.getInstance().getTimeInMillis(), checkImpr.isSelected(),
                                    checkVisu.isSelected(), false);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    bar.setValue(2);
                                    generator.addGenerateListener(ImpressionGrandLivrePanel.this);
                                }
                            });
                        }

                    }
                }).start();

            }
        });
        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(ImpressionGrandLivrePanel.this)).dispose();
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

        DocumentListener d = new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                checkValidity();
            }

            public void removeUpdate(DocumentEvent e) {
                checkValidity();
            }

            public void changedUpdate(DocumentEvent e) {
                checkValidity();
            }
        };

        this.compteDeb.getDocument().addDocumentListener(d);
        // Chargement des valeurs par défaut
        String valueCompteDeb = DefaultNXProps.getInstance().getStringProperty("GrandLivreCompteDeb");
        if (valueCompteDeb.trim().length() > 0) {
            this.compteDeb.setText(valueCompteDeb);
        } else {
            this.compteDeb.setText("1");
        }

        this.compteEnd.getDocument().addDocumentListener(d);
        // Chargement des valeurs par défaut
        String valueCompteEnd = DefaultNXProps.getInstance().getStringProperty("GrandLivreCompteEnd");
        if (valueCompteEnd.trim().length() > 0) {
            this.compteEnd.setText(valueCompteEnd);
        } else {
            this.compteEnd.setText("8");
        }
    }

    private void checkValidity() {

        Date beginDate = this.dateDeb.getDate();
        Date endDate = this.dateEnd.getDate();

        // System.err.println("Check validity between ");
        this.valid.setEnabled(true);
        if (beginDate == null || endDate == null) {
            this.valid.setEnabled(false);
        } else {
            if (this.compteDeb.getText().trim().length() == 0 || this.compteEnd.getText().trim().length() == 0) {
                this.valid.setEnabled(false);
            } else {
                if (this.compteDeb.getText().trim().compareToIgnoreCase(this.compteEnd.getText().trim()) > 0) {
                    this.valid.setEnabled(false);
                } else {
                    if (beginDate.after(endDate)) {
                        this.valid.setEnabled(false);
                    }
                }
            }
        }
    }

    private void storeValue() {

        // Set date debut
        Date d = this.dateDeb.getDate();
        if (d != null) {
            DefaultNXProps.getInstance().setProperty("GrandLivreDateDeb", String.valueOf(d.getTime()));
        }

        // Set date Fin
        Date dFin = this.dateEnd.getDate();
        if (dFin != null) {
            DefaultNXProps.getInstance().setProperty("GrandLivreDateEnd", String.valueOf(dFin.getTime()));
        }

        DefaultNXProps.getInstance().setProperty("GrandLivreCompteDeb", this.compteDeb.getText());
        DefaultNXProps.getInstance().setProperty("GrandLivreCompteEnd", this.compteEnd.getText());

        DefaultNXProps.getInstance().store();
    }

    public void taskEnd() {
        bar.setValue(3);
        bar.setString("Terminée");
        valid.setEnabled(true);
    }
}
