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

import org.openconcerto.erp.generationDoc.DocumentLocalStorageManager;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;

public abstract class AbstractGenerationDocumentPreferencePanel extends DefaultPreferencePanel {

    protected Map<Tuple2<String, String>, String> mapKeyLabel = new HashMap<Tuple2<String, String>, String>();
    private final Map<Tuple2<String, String>, JTextField> mapKeyTextOO = new HashMap<Tuple2<String, String>, JTextField>();
    private final Map<Tuple2<String, String>, JTextField> mapKeyTextPDF = new HashMap<Tuple2<String, String>, JTextField>();

    private JFileChooser fileChooser;

    private static final String formatOO = "Format Open Office : ";
    private static final String formatPDF = "Format PDF : ";
    private static final String parcourir = "...";
    private static final String defaut = "Définir l'emplacement suivant pour tous";

    public static String getLabelFromTable(String tableName) {
        String pluralName = Configuration.getInstance().getDirectory().getElement(tableName).getPluralName();
        pluralName = StringUtils.firstUp(pluralName);
        return pluralName;
    }

    public AbstractGenerationDocumentPreferencePanel() {
        super();
    }

    public void uiInit() {
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Emplacement par défaut
        final GridBagConstraints cDefault = new DefaultGridBagConstraints();
        final JLabel labelDefaut = new JLabel(defaut);
        final JTextField textDefault = new JTextField();
        textDefault.setEditable(false);
        final JButton buttonDefault = new JButton(parcourir);
        buttonDefault.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                directoryChoose(textDefault);
            }
        });

        final JButton buttonSetDefaut = new JButton("Appliquer");
        buttonSetDefaut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                final String folder = textDefault.getText();

                for (final Entry<Tuple2<String, String>, JTextField> entry : AbstractGenerationDocumentPreferencePanel.this.mapKeyTextOO.entrySet()) {
                    entry.getValue().setText(folder + File.separator + AbstractGenerationDocumentPreferencePanel.this.mapKeyLabel.get(entry.getKey()));
                }

                for (final Entry<Tuple2<String, String>, JTextField> entry : AbstractGenerationDocumentPreferencePanel.this.mapKeyTextPDF.entrySet()) {
                    entry.getValue().setText(folder + File.separator + AbstractGenerationDocumentPreferencePanel.this.mapKeyLabel.get(entry.getKey()));
                }
            }
        });

        textDefault.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(final DocumentEvent e) {
                buttonSetDefaut.setEnabled(textDefault.getText().trim().length() > 0);
            }
        });
        buttonSetDefaut.setEnabled(false);
        cDefault.gridx = GridBagConstraints.RELATIVE;
        final JPanel panelDefaut = new JPanel(new GridBagLayout());
        panelDefaut.add(labelDefaut, cDefault);
        cDefault.weightx = 1;
        panelDefaut.add(textDefault, cDefault);
        cDefault.weightx = 0;
        panelDefaut.add(buttonDefault, cDefault);
        panelDefaut.add(buttonSetDefaut, cDefault);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;

        this.add(panelDefaut, c);

        final List<Tuple2<String, String>> list = new ArrayList<Tuple2<String, String>>(this.mapKeyLabel.keySet());
        Collections.sort(list, new Comparator<Tuple2<String, String>>() {
            @Override
            public int compare(final Tuple2<String, String> o1, final Tuple2<String, String> o2) {
                return o1.get0().compareTo(o2.get0());
            }
        });

        final JPanel panelGlobal = new JPanel(new GridBagLayout());
        final GridBagConstraints cGlobal = new DefaultGridBagConstraints();

        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final Tuple2<String, String> key = list.get(i);
            cGlobal.gridwidth = 1;
            cGlobal.weightx = 1;
            cGlobal.gridy++;
            cGlobal.gridx = 0;
            cGlobal.anchor = GridBagConstraints.NORTHWEST;

            if (i == size - 1) {
                cGlobal.weighty = 1;
            }

            final JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createTitledBorder(this.mapKeyLabel.get(key)));
            panel.setLayout(new GridBagLayout());
            final GridBagConstraints cPanel = new DefaultGridBagConstraints();

            // Emplacement SXC

            panel.add(new JLabel(formatOO), cPanel);
            cPanel.gridx++;
            cPanel.weightx = 1;
            final JTextField text = new JTextField();
            text.setEditable(false);
            this.mapKeyTextOO.put(key, text);
            panel.add(text, cPanel);

            final JButton button = new JButton(parcourir);
            cPanel.gridx++;
            cPanel.weightx = 0;
            cPanel.fill = GridBagConstraints.NONE;
            panel.add(button, cPanel);

            // Emplacement PDF
            cPanel.fill = GridBagConstraints.HORIZONTAL;
            cPanel.weightx = 0;
            cPanel.gridy++;
            cPanel.gridx = 0;
            cPanel.gridwidth = 1;
            panel.add(new JLabel(formatPDF), cPanel);
            cPanel.gridx++;
            cPanel.weightx = 1;
            final JTextField textPDF = new JTextField();
            textPDF.setEditable(false);
            this.mapKeyTextPDF.put(key, textPDF);
            panel.add(textPDF, cPanel);

            final JButton buttonPDF = new JButton(parcourir);
            cPanel.gridx++;
            cPanel.weightx = 0;
            cPanel.fill = GridBagConstraints.NONE;
            panel.add(buttonPDF, cPanel);

            panelGlobal.add(panel, cGlobal);

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    directoryChoose(AbstractGenerationDocumentPreferencePanel.this.mapKeyTextOO.get(key));
                }
            });
            buttonPDF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    directoryChoose(AbstractGenerationDocumentPreferencePanel.this.mapKeyTextPDF.get(key));
                }
            });

        }

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(panelGlobal), c);

        setValues();
    }

    @Override
    public void storeValues() {

        try {
            final File z = new File(".");
            for (final Entry<Tuple2<String, String>, JTextField> entry : this.mapKeyTextOO.entrySet()) {
                final File f = new File(entry.getValue().getText());
                TemplateNXProps.getInstance().setProperty(entry.getKey().get1() + "OO", FileUtils.relative(z, f));
                DocumentLocalStorageManager.getInstance().addDocumentDirectory(entry.getKey().get0(), new File(FileUtils.relative(z, f)));
            }

            for (final Entry<Tuple2<String, String>, JTextField> entry : this.mapKeyTextPDF.entrySet()) {
                final File f = new File(entry.getValue().getText());
                TemplateNXProps.getInstance().setProperty(entry.getKey().get1() + "PDF", FileUtils.relative(z, f));
                DocumentLocalStorageManager.getInstance().addPDFDirectory(entry.getKey().get0(), new File(FileUtils.relative(z, f)));
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }

        TemplateNXProps.getInstance().store();
    }

    @Override
    public void restoreToDefaults() {
        final Color foregroundColor = UIManager.getColor("TextField.foreground");

        for (final Entry<Tuple2<String, String>, JTextField> entry : this.mapKeyTextOO.entrySet()) {
            final File f = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory("Default");
            final JTextField textField = entry.getValue();
            if (f.exists()) {
                textField.setForeground(foregroundColor);
            } else {
                textField.setForeground(Color.RED);
            }
            try {
                textField.setText(f.getCanonicalPath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        for (final Entry<Tuple2<String, String>, JTextField> entry : this.mapKeyTextPDF.entrySet()) {
            final File f = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory("Default");
            final JTextField textField = entry.getValue();
            if (f.exists()) {
                textField.setForeground(foregroundColor);
            } else {
                textField.setForeground(Color.RED);
            }
            try {
                textField.setText(f.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setValues() {
        try {
            updateTextFields(this.mapKeyTextOO, "OO");
            updateTextFields(this.mapKeyTextPDF, "PDF");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void updateTextFields(final Map<Tuple2<String, String>, JTextField> map, final String format) throws IOException {
        final Color foregroundColor = UIManager.getColor("TextField.foreground");
        final DocumentLocalStorageManager storage = DocumentLocalStorageManager.getInstance();
        for (final Entry<Tuple2<String, String>, JTextField> entry : map.entrySet()) {
            final File f;
            if (format.equalsIgnoreCase("PDF")) {
                f = storage.getPDFOutputDirectory(entry.getKey().get0());
            } else {
                f = storage.getDocumentOutputDirectory(entry.getKey().get0());
            }
            final JTextField textField = entry.getValue();
            if (f.exists()) {
                textField.setForeground(foregroundColor);
            } else {
                textField.setForeground(Color.RED);
            }
            textField.setText(f.getCanonicalPath());
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

                if (AbstractGenerationDocumentPreferencePanel.this.fileChooser.showDialog(AbstractGenerationDocumentPreferencePanel.this, "Sélectionner") == JFileChooser.APPROVE_OPTION) {
                    final File selectedFile = AbstractGenerationDocumentPreferencePanel.this.fileChooser.getSelectedFile();
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
