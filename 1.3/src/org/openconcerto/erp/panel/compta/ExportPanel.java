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
 
 package org.openconcerto.erp.panel.compta;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ExportPanel extends JPanel {
    static enum ExportType {
        FEC("Fichier des écritures comptables (FEC)") {
            @Override
            public AbstractExport createExport(DBRoot root) {
                return new ExportFEC(root);
            }
        },
        RelationExpert("Relation expert (Coala)") {
            @Override
            public AbstractExport createExport(DBRoot root) {
                return new ExportRelationExpertPanel(root);
            }
        },
        EBP_OL("EBP Open Line") {
            @Override
            public AbstractExport createExport(DBRoot root) {
                return new ExportEBP_OL(root);
            }
        },
        EBP("EBP Compta Pro") {
            @Override
            public AbstractExport createExport(DBRoot root) {
                return new ExportEBP_ComptaPro(root);
            }
        };

        private final String label;

        private ExportType(final String label) {
            this.label = label;
        }

        public final String getLabel() {
            return this.label;
        }

        public abstract AbstractExport createExport(final DBRoot root);
    }

    private final JDate du, au;
    private final JButton buttonGen = new JButton("Exporter");
    private final JFileChooser fileChooser = new JFileChooser();
    private final JTextField textDestination = new JTextField();
    private final ElementComboBox boxJournal = new ElementComboBox(true);
    private final JCheckBox boxExport = new JCheckBox("Seulement les nouvelles ecritures");

    public ExportPanel() {
        super(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        c.gridwidth = 4;
        this.add(new JLabelBold("Export des écritures comptables"), c);

        // Fichier de destination
        final JPanel panelFichier = new JPanel(new GridBagLayout());
        final GridBagConstraints cFic = new DefaultGridBagConstraints();
        cFic.insets.left = 0;
        cFic.insets.bottom = 0;
        cFic.gridx++;
        cFic.weightx = 1;
        panelFichier.add(this.textDestination, cFic);

        final JButton buttonChoose = new JButton("...");
        cFic.gridx++;
        cFic.weightx = 0;
        cFic.fill = GridBagConstraints.NONE;
        panelFichier.add(buttonChoose, cFic);

        this.buttonGen.setEnabled(false);
        this.textDestination.setEditable(false);

        this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        buttonChoose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int answer = ExportPanel.this.fileChooser.showSaveDialog(ExportPanel.this);
                if (answer == JFileChooser.APPROVE_OPTION) {
                    ExportPanel.this.textDestination.setText(ExportPanel.this.fileChooser.getSelectedFile().getAbsolutePath());
                }
                ExportPanel.this.buttonGen.setEnabled(answer == JFileChooser.APPROVE_OPTION);
            }
        });
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        this.add(new JLabel("Dossier de destination", SwingUtilities.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 4;
        this.add(panelFichier, c);

        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel("Période du", SwingUtilities.RIGHT), c);

        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        this.du = new JDate(true);
        this.add(this.du, c);

        c.gridx++;
        this.add(new JLabel("au"), c);

        c.gridx++;
        this.au = new JDate(true);
        this.add(this.au, c);

        this.boxExport.setSelected(true);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        this.add(this.boxExport, c);

        final SQLElement elt = Configuration.getInstance().getDirectory().getElement(JournalSQLElement.class);
        final ComboSQLRequest comboRequest = elt.getComboRequest(true);
        comboRequest.setUndefLabel("Tous");
        this.boxJournal.init(elt, comboRequest);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Ecritures du journal", SwingUtilities.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.NONE;
        this.boxJournal.setValue(JournalSQLElement.VENTES);
        this.add(this.boxJournal, c);

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Type d'export", SwingUtilities.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 4;
        final JComboBox comboType = new JComboBox(ExportType.values());
        comboType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((ExportType) value).getLabel(), index, isSelected, cellHasFocus);
            }
        });
        c.fill = GridBagConstraints.NONE;
        comboType.setSelectedIndex(0);
        this.add(comboType, c);

        c.gridy++;
        c.weightx = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        final JPanel panelButton = new JPanel();
        panelButton.add(this.buttonGen);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.weightx = 0;
        c.weighty = 1;
        this.add(panelButton, c);
        this.buttonGen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                export((ExportType) comboType.getSelectedItem());
            }
        });
    }

    protected final void export(ExportType type) {
        try {
            final Tuple2<File, Number> res = type.createExport(ComptaPropsConfiguration.getInstanceCompta().getRootSociete()).export(this.fileChooser.getSelectedFile(), this.du.getDate(),
                    this.au.getDate(), this.boxJournal.getSelectedRow(), this.boxExport.isSelected());
            if (res.get1().intValue() == 0) {
                JOptionPane.showMessageDialog(this, "Aucune écriture trouvée. La période est-elle correcte ?");
            } else {
                // Notify
                JOptionPane.showMessageDialog(this, "L'export des " + res.get1() + " écritures est terminé.\nLe fichier créé est " + res.get0().getAbsolutePath());
                // Close frame
                SwingUtilities.getWindowAncestor(this).dispose();
            }
        } catch (final FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Création du fichier impossible : " + ex.getMessage());
        } catch (Exception e) {
            ExceptionHandler.handle(this, "Erreur d'export", e);
        }
    }
}
