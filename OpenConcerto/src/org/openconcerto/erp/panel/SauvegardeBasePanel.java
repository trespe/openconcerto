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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.utils.Copy;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SauvegardeBasePanel extends JPanel {

    private File saveFile = new File(".", "Sauvegarde.sql");
    private JTextField textLocation;
    private JFileChooser fileChooser = null;

    public SauvegardeBasePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        JLabel label = new JLabel("Sauvegarde de la base de données de la société " + ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete().getString("NOM"));
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(label, c);

        c.gridwidth = 1;
        JLabel labelEmplacement = new JLabel("Emplacement du fichier de sauvegarde");
        c.gridy++;
        this.add(labelEmplacement, c);

        c.fill = GridBagConstraints.NONE;
        JButton buttonSearch = new JButton("...");
        c.gridx++;
        this.add(buttonSearch, c);

        // Emplacement du fichier de sauvegarde
        c.fill = GridBagConstraints.HORIZONTAL;
        this.textLocation = new JTextField();
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0;
        this.add(this.textLocation, c);
        this.textLocation.setEditable(false);
        // this.textLocation.setEnabled(false);

        // Bouton Sauver, Fermer
        JButton buttonSave = new JButton("Sauver");
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.weighty = 1;
        c.weightx = 0;
        c.gridy++;
        this.add(buttonSave, c);

        JButton buttonClose = new JButton("Fermer");
        c.gridx++;
        this.add(buttonClose, c);

        // Emplacement par defaut
        try {
            this.textLocation.setText(this.saveFile.getCanonicalPath());
        } catch (IOException e2) {
            e2.printStackTrace();
        }

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(SauvegardeBasePanel.this)).dispose();
            }
        });

        buttonSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // DumpMysqlDB.dumpMySQLDB(saveFile);

                // if
                // (Configuration.getInstance().getBase().getServer().getSystem().equalsIgnoreCase("postgresql"))
                // {
                // DumpPostgresqlDB.dumpPostgresqlDB(saveFile);
                // } else {
                // DumpMysqlDB.dumpMySQLDB(saveFile);
                // }
                try {
                    Copy c = new Copy(true, SauvegardeBasePanel.this.saveFile, Configuration.getInstance().getSystemRoot(), false, false);
                    c.applyTo(Configuration.getInstance().getRoot().getName(), null);

                } catch (SQLException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                ((JFrame) SwingUtilities.getRoot(SauvegardeBasePanel.this)).dispose();
            }
        });

        buttonSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        if (SauvegardeBasePanel.this.fileChooser == null) {
                            SauvegardeBasePanel.this.fileChooser = new JFileChooser();
                        }

                        if (SauvegardeBasePanel.this.fileChooser.showSaveDialog(SauvegardeBasePanel.this) == JFileChooser.APPROVE_OPTION) {

                            SauvegardeBasePanel.this.saveFile = SauvegardeBasePanel.this.fileChooser.getSelectedFile();
                            try {
                                SauvegardeBasePanel.this.textLocation.setText(SauvegardeBasePanel.this.saveFile.getCanonicalPath());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });

            }
        });
    }

}
