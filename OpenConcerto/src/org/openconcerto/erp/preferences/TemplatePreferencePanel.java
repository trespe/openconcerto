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

import org.openconcerto.erp.generationDoc.AbstractLocalTemplateProvider;
import org.openconcerto.erp.generationDoc.DefaultLocalTemplateProvider;
import org.openconcerto.erp.generationDoc.TemplateManager;
import org.openconcerto.erp.generationDoc.TemplateProvider;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;
import org.openconcerto.ui.table.TablePopupMouseListener;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

public class TemplatePreferencePanel extends DefaultPreferencePanel {

    private JTextField textTemplate;
    private JFileChooser fileChooser = null;

    private JButton bModify;
    private JButton bUndo;
    private JButton bSync;

    public TemplatePreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints cPanel = new DefaultGridBagConstraints();
        cPanel.weighty = 0;
        cPanel.anchor = GridBagConstraints.WEST;
        /*******************************************************************************************
         * Emplacement
         ******************************************************************************************/
        this.add(new JLabel("Modèles des documents"), cPanel);
        cPanel.gridx++;
        cPanel.weightx = 1;
        this.textTemplate = new JTextField();
        this.add(this.textTemplate, cPanel);

        final JButton buttonTemplate = new JButton("...");
        cPanel.gridx++;
        cPanel.weightx = 0;
        cPanel.fill = GridBagConstraints.NONE;
        this.add(buttonTemplate, cPanel);

        final JPanel templates = createTemplateList();
        templates.setOpaque(false);
        cPanel.gridy++;
        cPanel.fill = GridBagConstraints.BOTH;
        cPanel.weightx = 1;
        cPanel.weighty = 1;
        cPanel.gridwidth = 3;
        cPanel.gridx = 0;
        this.add(templates, cPanel);

        buttonTemplate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                directoryChoose("template");
            }
        });
        this.textTemplate.setEditable(false);

        setValues();
    }

    private JPanel createTemplateList() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        bModify = new JButton("Modifier");
        bModify.setEnabled(false);
        bModify.setOpaque(false);
        p.add(bModify, c);

        bUndo = new JButton("Rétablir");
        bUndo.setEnabled(false);
        bUndo.setOpaque(false);
        c.gridx++;
        p.add(bUndo, c);
        bSync = new JButton("Synchroniser");
        bSync.setEnabled(false);
        bSync.setOpaque(false);
        c.gridx++;
        p.add(bSync, c);
        final TemplateTableModel dm = new TemplateTableModel();
        final JTable table = new JTable(dm);
        TablePopupMouseListener.add(table, new ITransformer<MouseEvent, JPopupMenu>() {
            @Override
            public JPopupMenu transformChecked(MouseEvent input) {
                final JPopupMenu res = new JPopupMenu();
                final int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    final JMenuItem menuItem = new JMenuItem(dm.getTemplateId(selectedRow));
                    res.add(menuItem);
                    menuItem.setEnabled(false);
                    res.addSeparator();

                    res.add(new AbstractAction("Modifier le modèle") {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            modifyTemplate(dm, table);

                        }
                    });
                    res.add(new AbstractAction("Modifier la configuration du modèle") {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            modifyTemplateXML(dm, table);

                        }
                    });
                    res.add(new AbstractAction("Modifier les paramètres d'impression") {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            modifyTemplateODSP(dm, table);

                        }
                    });
                }
                return res;
            }
        });
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                TemplateProvider provider = dm.getTemplateProvider(row);
                if (provider instanceof AbstractLocalTemplateProvider) {
                    final String templateId = dm.getTemplateId(row);
                    File f = ((AbstractLocalTemplateProvider) provider).getFileTemplate(templateId, null, null);
                    if (f == null || !f.exists()) {
                        tableCellRendererComponent.setBackground(Color.ORANGE);
                        if (f != null) {
                            setToolTipText(f.getAbsolutePath() + " not found");
                        } else {
                            setToolTipText("no file for template " + templateId);
                        }
                    } else {
                        if (isSelected) {
                            tableCellRendererComponent.setBackground(table.getSelectionBackground());
                        } else {
                            tableCellRendererComponent.setBackground(table.getBackground());
                        }
                        setToolTipText(f.getAbsolutePath());
                    }
                }
                return tableCellRendererComponent;
            }
        });
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        p.add(new JScrollPane(table), c);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    bModify.setEnabled(false);
                    bSync.setEnabled(false);
                    bUndo.setEnabled(false);
                    return;
                }
                boolean b = dm.isSynced(row);
                if (b) {
                    bModify.setEnabled(true);
                    bSync.setEnabled(false);
                    bUndo.setEnabled(false);
                } else {
                    bModify.setEnabled(true);
                    bSync.setEnabled(true);
                    bUndo.setEnabled(true);
                }

            }
        });
        bModify.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTemplate(dm, table);
            }
        });
        bSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                dm.sync(row);
            }
        });

        bUndo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                dm.restore(row);

            }
        });

        return p;
    }

    public void storeValues() {

        final File z = new File(".");
        final File f = new File(this.textTemplate.getText());
        try {
            TemplateNXProps.getInstance().setProperty("LocationTemplate", FileUtils.relative(z, f));
            final DefaultLocalTemplateProvider provider = new DefaultLocalTemplateProvider();

            provider.setBaseDirectory(new File(FileUtils.relative(z, f)));

            TemplateManager.getInstance().setDefaultProvider(provider);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TemplateNXProps.getInstance().store();
    }

    public void restoreToDefaults() {

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

    public void modifyTemplate(final TemplateTableModel dm, final JTable table) {
        int row = table.getSelectedRow();
        dm.unsync(row);
        // modify on OO
        final TemplateProvider templateProvider = dm.getTemplateProvider(row);
        if (templateProvider instanceof AbstractLocalTemplateProvider) {
            final String templateId = dm.getTemplateId(row);

            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        final File fileTemplate = ((AbstractLocalTemplateProvider) templateProvider).getFileTemplate(templateId, null, null);
                        if (fileTemplate != null && fileTemplate.exists()) {
                            FileUtils.openFile(fileTemplate);
                        }
                    } catch (IOException e1) {
                        ExceptionHandler.handle("Impossible d'ouvrir le modèle", e1);
                    }
                }
            });
            t.setName("TemplatePreferencePanel: open with OO");
            t.setDaemon(true);
            t.start();

        }
    }

    public void modifyTemplateXML(final TemplateTableModel dm, final JTable table) {
        int row = table.getSelectedRow();
        dm.unsync(row);
        // modify on OO
        final TemplateProvider templateProvider = dm.getTemplateProvider(row);
        if (templateProvider instanceof AbstractLocalTemplateProvider) {
            final String templateId = dm.getTemplateId(row);

            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        final File fileTemplateConfiguration = ((AbstractLocalTemplateProvider) templateProvider).getFileTemplateConfiguration(templateId, null, null);
                        if (fileTemplateConfiguration != null && fileTemplateConfiguration.exists()) {
                            FileUtils.openFile(fileTemplateConfiguration);
                        } else {
                            JOptionPane.showMessageDialog(TemplatePreferencePanel.this, "Pas de fichier de configuration associé");
                        }
                    } catch (IOException e1) {
                        ExceptionHandler.handle("Impossible d'ouvrir la configuration du modèle", e1);
                    }
                }
            });
            t.setName("TemplatePreferencePanel: open with OO");
            t.setDaemon(true);
            t.start();

        }

    }

    public void modifyTemplateODSP(final TemplateTableModel dm, final JTable table) {
        int row = table.getSelectedRow();
        dm.unsync(row);
        // modify on OO
        final TemplateProvider templateProvider = dm.getTemplateProvider(row);
        if (templateProvider instanceof AbstractLocalTemplateProvider) {
            final String templateId = dm.getTemplateId(row);

            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        final File fileTemplatePrintConfiguration = ((AbstractLocalTemplateProvider) templateProvider).getFileTemplatePrintConfiguration(templateId, null, null);
                        if (fileTemplatePrintConfiguration != null && fileTemplatePrintConfiguration.exists()) {
                            FileUtils.openFile(fileTemplatePrintConfiguration);
                        }
                    } catch (IOException e1) {
                        ExceptionHandler.handle("Impossible d'ouvrir la configuration d'impression", e1);
                    }
                }
            });
            t.setName("TemplatePreferencePanel: open with OO");
            t.setDaemon(true);
            t.start();

        }

    }
}
