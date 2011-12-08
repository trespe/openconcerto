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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.erp.utils.FileUtility;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TemplatePreferencePanel extends DefaultPreferencePanel {

    private JTextField textTemplate;
    private JFileChooser fileChooser = null;
    public static String MULTIMOD = "MultiModele";
    JCheckBox boxMultiMod = new JCheckBox("Activer la gestion multimodèle");

    public TemplatePreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints cPanel = new DefaultGridBagConstraints();
        cPanel.weighty = 0;
        cPanel.anchor = GridBagConstraints.WEST;
        /*******************************************************************************************
         * Emplacement
         ******************************************************************************************/
        this.add(new JLabel("Emplacement des modèles"), cPanel);
        cPanel.gridx++;
        cPanel.weightx = 1;
        this.textTemplate = new JTextField();
        this.add(this.textTemplate, cPanel);

        final JButton buttonTemplate = new JButton("...");
        cPanel.gridx++;
        cPanel.weightx = 0;
        cPanel.fill = GridBagConstraints.NONE;
        this.add(buttonTemplate, cPanel);

        cPanel.gridy++;
        cPanel.gridx = 0;
        this.add(boxMultiMod, cPanel);

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        cPanel.gridy++;
        cPanel.weighty = 1;
        this.add(spacer, cPanel);

        buttonTemplate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose("template");
            }
        });
        this.textTemplate.setEditable(false);

        setValues();
    }

    public void storeValues() {

        final File z = new File(".");
        final File f = new File(this.textTemplate.getText());
        try {
            TemplateNXProps.getInstance().setProperty("LocationTemplate", FileUtility.getPrimaryPath(z.getCanonicalFile(), f));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TemplateNXProps.getInstance().setProperty(MULTIMOD, String.valueOf(this.boxMultiMod.isSelected()));

        TemplateNXProps.getInstance().store();
    }

    public void restoreToDefaults() {
        this.boxMultiMod.setSelected(false);
    }

    public String getTitleName() {
        return "Emplacement des modèles";
    }

    private void setValues() {
        try {
            final File f = new File(TemplateNXProps.getInstance().getStringProperty("LocationTemplate"));
            if (f.exists()) {
                this.textTemplate.setForeground(UIManager.getColor("TextField.foreground"));
            } else {
                this.textTemplate.setForeground(Color.RED);
            }
            this.textTemplate.setText(f.getCanonicalPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
        this.boxMultiMod.setSelected(TemplateNXProps.getInstance().getBooleanValue(MULTIMOD, false));
    }

    private void directoryChoose(final String type) {

        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        this.fileChooser.setCurrentDirectory(new File(TemplatePreferencePanel.this.textTemplate.getText()));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (TemplatePreferencePanel.this.fileChooser.showDialog(TemplatePreferencePanel.this, "Sélectionner") == JFileChooser.APPROVE_OPTION) {

                    if (type.equalsIgnoreCase("template")) {
                        File selectedFile = TemplatePreferencePanel.this.fileChooser.getSelectedFile();
                        if (selectedFile.exists()) {
                            TemplatePreferencePanel.this.textTemplate.setForeground(UIManager.getColor("TextField.foreground"));
                        } else {
                            TemplatePreferencePanel.this.textTemplate.setForeground(Color.RED);
                        }
                        TemplatePreferencePanel.this.textTemplate.setText(selectedFile.getPath());
                    }
                }
            }
        });
    }

}
