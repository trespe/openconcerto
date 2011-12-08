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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GenerationDeclarationDocPreferencePanel extends DefaultPreferencePanel {

    private JTextField text2033APDF, text2033BPDF, text2033CPDF;

    private static final String formatPDF = "Format PDF : ";
    private static final String parcourir = "...";
    private JFileChooser fileChooser = null;

    public GenerationDeclarationDocPreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;

        final GridBagConstraints cPanel = new DefaultGridBagConstraints();

        /*******************************************************************************************
         * 2033A, 2033B, 2033C
         ******************************************************************************************/
        JPanel panel2033A = new JPanel();
        panel2033A.setOpaque(false);
        panel2033A.setBorder(BorderFactory.createTitledBorder("2033-A"));
        panel2033A.setLayout(new GridBagLayout());

        JPanel panel2033B = new JPanel();
        panel2033B.setOpaque(false);
        panel2033B.setBorder(BorderFactory.createTitledBorder("2033-B"));
        panel2033B.setLayout(new GridBagLayout());

        JPanel panel2033C = new JPanel();
        panel2033C.setOpaque(false);
        panel2033C.setBorder(BorderFactory.createTitledBorder("2033-C"));
        panel2033C.setLayout(new GridBagLayout());

        // Emplacement PDF
        cPanel.fill = GridBagConstraints.HORIZONTAL;
        cPanel.weightx = 0;
        cPanel.gridy++;
        cPanel.gridx = 0;
        cPanel.gridwidth = 1;
        panel2033A.add(new JLabel(formatPDF), cPanel);
        panel2033B.add(new JLabel(formatPDF), cPanel);
        panel2033C.add(new JLabel(formatPDF), cPanel);
        cPanel.gridx++;
        cPanel.weightx = 1;
        this.text2033APDF = new JTextField();
        this.text2033BPDF = new JTextField();
        this.text2033CPDF = new JTextField();
        panel2033A.add(this.text2033APDF, cPanel);
        panel2033B.add(this.text2033BPDF, cPanel);
        panel2033C.add(this.text2033CPDF, cPanel);

        JButton button2033APDF = new JButton(parcourir);
        JButton button2033BPDF = new JButton(parcourir);
        JButton button2033CPDF = new JButton(parcourir);
        cPanel.gridx++;
        cPanel.weightx = 0;
        cPanel.fill = GridBagConstraints.NONE;
        panel2033A.add(button2033APDF, cPanel);
        panel2033B.add(button2033BPDF, cPanel);
        panel2033C.add(button2033CPDF, cPanel);

        this.add(panel2033A, c);
        c.gridy++;
        this.add(panel2033B, c);
        c.gridy++;
        c.weighty = 1;
        this.add(panel2033C, c);

        button2033APDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose(GenerationDeclarationDocPreferencePanel.this.text2033APDF);
            }
        });
        button2033BPDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose(GenerationDeclarationDocPreferencePanel.this.text2033BPDF);
            }
        });
        button2033CPDF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose(GenerationDeclarationDocPreferencePanel.this.text2033CPDF);
            }
        });

        this.text2033APDF.setEditable(false);
        this.text2033BPDF.setEditable(false);
        this.text2033CPDF.setEditable(false);

        setValues();
    }

    public void storeValues() {

        File f2033APDF = new File(this.text2033APDF.getText());
        File f2033BPDF = new File(this.text2033BPDF.getText());
        File f2033CPDF = new File(this.text2033CPDF.getText());
        File z = new File(".");

        try {
            TemplateNXProps.getInstance().setProperty("Location2033APDF", FileUtility.getPrimaryPath(z.getCanonicalFile(), f2033APDF));
            TemplateNXProps.getInstance().setProperty("Location2033BPDF", FileUtility.getPrimaryPath(z.getCanonicalFile(), f2033BPDF));
            TemplateNXProps.getInstance().setProperty("Location2033CPDF", FileUtility.getPrimaryPath(z.getCanonicalFile(), f2033CPDF));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TemplateNXProps.getInstance().store();
    }

    public void restoreToDefaults() {
    }

    public String getTitleName() {
        return "Destination des documents générés";
    }

    private void setValues() {

        File f2033AOO = new File(TemplateNXProps.getInstance().getStringProperty("Location2033APDF"));
        File f2033BPDF = new File(TemplateNXProps.getInstance().getStringProperty("Location2033BPDF"));
        File f2033COO = new File(TemplateNXProps.getInstance().getStringProperty("Location2033CPDF"));

        try {
            if (f2033AOO.exists()) {
                this.text2033APDF.setForeground(UIManager.getColor("TextField.foreground"));
            } else {
                this.text2033APDF.setForeground(Color.RED);
            }
            if (f2033BPDF.exists()) {
                this.text2033BPDF.setForeground(UIManager.getColor("TextField.foreground"));
            } else {
                this.text2033BPDF.setForeground(Color.RED);
            }
            if (f2033COO.exists()) {
                this.text2033CPDF.setForeground(UIManager.getColor("TextField.foreground"));
            } else {
                this.text2033CPDF.setForeground(Color.RED);
            }
            this.text2033APDF.setText(f2033AOO.getCanonicalPath());
            this.text2033BPDF.setText(f2033BPDF.getCanonicalPath());
            this.text2033CPDF.setText(f2033COO.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void directoryChoose(final JTextField field) {

        if (this.fileChooser == null) {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        this.fileChooser.setCurrentDirectory(new File(field.getText()));

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (GenerationDeclarationDocPreferencePanel.this.fileChooser.showDialog(GenerationDeclarationDocPreferencePanel.this, "Sélectionner") == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = GenerationDeclarationDocPreferencePanel.this.fileChooser.getSelectedFile();
                    if (selectedFile.exists()) {
                        field.setForeground(UIManager.getColor("TextField.foreground"));
                    } else {
                        field.setForeground(Color.RED);
                    }
                    field.setText(selectedFile.getAbsolutePath());
                }
            }
        });
    }
}
