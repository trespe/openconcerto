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
import org.openconcerto.erp.core.finance.accounting.model.SelectJournauxModel;
import org.openconcerto.erp.core.finance.accounting.report.GrandLivreSheet;
import org.openconcerto.erp.core.finance.accounting.report.JournauxMoisSheet;
import org.openconcerto.erp.core.finance.accounting.report.JournauxSheet;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorListener;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.TitledSeparator;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

public class ImpressionJournauxPanel extends JPanel implements SpreadSheetGeneratorListener {

    private final JDate dateDeb, dateEnd;
    private JTable tableJrnl;
    // private boolean isValidated;
    private JButton valid;
    private JButton annul;
    private JCheckBox checkImpr;
    private JCheckBox checkVisu;
    private JCheckBox checkCentralMois;
    private JTextField compteDeb, compteEnd;
    private int mode = GrandLivreSheet.MODEALL;
    private JProgressBar bar = new JProgressBar(0, 3);

    public ImpressionJournauxPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));

        // this.isValidated = false;
        this.dateDeb = new JDate();
        this.dateEnd = new JDate();
        this.tableJrnl = new JTable(new SelectJournauxModel());

        this.add(new JLabel("Période du"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateDeb, c);
        // Chargement des valeurs par défaut
        String valueDateDeb = DefaultNXProps.getInstance().getStringProperty("JournauxDateDeb");
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
        String valueDateEnd = DefaultNXProps.getInstance().getStringProperty("JournauxDateEnd");
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

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.add(new TitledSeparator("Sélection des journaux"), c);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 1;

        JScrollPane scroll = new JScrollPane(this.tableJrnl);
        Dimension d;
        if (this.tableJrnl.getPreferredSize().height > 200) {
            d = new Dimension(scroll.getPreferredSize().width, 200);
        } else {
            d = new Dimension(scroll.getPreferredSize().width, this.tableJrnl.getPreferredSize().height + 30);
        }
        scroll.setPreferredSize(d);

        this.add(scroll, c);

        this.valid = new JButton("Valider");
        this.annul = new JButton("Fermer");
        this.checkImpr = new JCheckBox("Impression");
        this.checkVisu = new JCheckBox("Visualisation");

        // Radio mode
        JRadioButton radioAll = new JRadioButton(new AbstractAction("Toutes") {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                mode = GrandLivreSheet.MODEALL;
            }
        });

        JRadioButton radioLettree = new JRadioButton(new AbstractAction("Lettrées") {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                mode = GrandLivreSheet.MODELETTREE;
            }
        });

        JRadioButton radioNonLettree = new JRadioButton(new AbstractAction("Non lettrées") {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                mode = GrandLivreSheet.MODENONLETTREE;
            }
        });
        JPanel panelMode = new JPanel();
        panelMode.add(radioAll);
        panelMode.add(radioLettree);
        panelMode.add(radioNonLettree);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.gridwidth = 2;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        ButtonGroup group = new ButtonGroup();
        group.add(radioAll);
        group.add(radioLettree);
        group.add(radioNonLettree);
        radioAll.setSelected(true);
        panelMode.setBorder(BorderFactory.createTitledBorder("Ecritures"));
        this.add(panelMode, c);

        // Centralisation par mois
        this.checkCentralMois = new JCheckBox("Centralisation par mois");
        c.gridx += 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(this.checkCentralMois, c);

        // Progress bar
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.bar.setStringPainted(true);
        this.add(this.bar, c);

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
                bar.setString(null);
                bar.setValue(1);
                valid.setEnabled(false);
                new Thread(new Runnable() {
                    public void run() {
                        int[] idS = ((SelectJournauxModel) tableJrnl.getModel()).getSelectedIds(tableJrnl.getSelectedRows());
                        JournauxSheet bSheet;
                        if (checkCentralMois.isSelected()) {
                            bSheet = new JournauxMoisSheet(idS, dateDeb.getDate(), dateEnd.getDate(), mode);
                        } else {
                            bSheet = new JournauxSheet(idS, dateDeb.getDate(), dateEnd.getDate(), mode, compteDeb.getText().trim(), compteEnd.getText().trim());
                        }

                        final SpreadSheetGeneratorCompta generator = new SpreadSheetGeneratorCompta(bSheet, "Journal_" + Calendar.getInstance().getTimeInMillis(), checkImpr.isSelected(), checkVisu
                                .isSelected());
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                bar.setValue(2);
                                generator.addGenerateListener(ImpressionJournauxPanel.this);
                            }
                        });
                    }
                }).start();
            }
        });
        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(ImpressionJournauxPanel.this)).dispose();
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
        this.tableJrnl.addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                checkValidity();
            }
        });
        this.tableJrnl.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                checkValidity();
            }
        });

        SimpleDocumentListener doc = new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {

                checkValidity();
            }
        };

        this.compteDeb.getDocument().addDocumentListener(doc);
        // Chargement des valeurs par défaut

        this.compteDeb.setText("1");

        this.compteEnd.getDocument().addDocumentListener(doc);
        // Chargement des valeurs par défaut

        this.compteEnd.setText("8");

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
                if (this.tableJrnl.getSelectedRows().length == 0) {
                    this.valid.setEnabled(false);
                } else {
                    this.valid.setEnabled(true);
                }
            }
        }

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

    private void storeValue() {

        // Set date debut
        Date d = this.dateDeb.getDate();
        if (d != null) {
            DefaultNXProps.getInstance().setProperty("JournauxDateDeb", String.valueOf(d.getTime()));
        }

        // Set date Fin
        Date dFin = this.dateEnd.getDate();
        if (dFin != null) {
            DefaultNXProps.getInstance().setProperty("JournauxDateEnd", String.valueOf(dFin.getTime()));
        }

        DefaultNXProps.getInstance().store();
    }

    public void taskEnd() {
        bar.setValue(3);
        bar.setString("Terminée");
        valid.setEnabled(true);
    }
}
