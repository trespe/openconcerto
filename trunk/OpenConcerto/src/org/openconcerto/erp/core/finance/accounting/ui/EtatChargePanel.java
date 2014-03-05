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
import org.openconcerto.erp.core.common.element.MoisSQLElement;
import org.openconcerto.erp.core.finance.accounting.model.EtatChargeModel;
import org.openconcerto.erp.core.humanresources.payroll.element.CaisseCotisationSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.report.EtatChargesPayeSheet;
import org.openconcerto.erp.generationDoc.SpreadSheetGeneratorCompta;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.state.JTableStateManager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class EtatChargePanel extends JPanel {

    private ElementComboBox selMoisDu;
    private ElementComboBox selMoisAu;
    private JTextField textAnnee;
    private EtatChargeModel[] model;
    private static final NumberFormat numberFormat = new DecimalFormat("0.00");

    public EtatChargePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Periode
        final SQLElement eltMois = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(MoisSQLElement.class);
        this.selMoisDu = new ElementComboBox(false, 15);
        this.selMoisDu.init(eltMois);
        this.selMoisDu.setButtonsVisible(false);
        this.selMoisAu = new ElementComboBox(false, 15);

        this.selMoisAu.init(eltMois);
        this.selMoisAu.setButtonsVisible(false);

        JLabel labelAnnee = new JLabel("pour l'année");
        this.textAnnee = new JTextField(5);

        JLabel labelMoisDu = new JLabel("Période de");
        JLabel labelMoisAu = new JLabel("à");
        {
            JPanel pDate = new JPanel();
            pDate.setOpaque(false);
            pDate.add(labelMoisDu);
            pDate.add(this.selMoisDu);
            this.selMoisDu.setValue(DefaultNXProps.getInstance().getIntProperty("EtatChargeSelMoisDu"));
            pDate.add(labelMoisAu);
            pDate.add(this.selMoisAu);
            final int intProperty = DefaultNXProps.getInstance().getIntProperty("EtatChargeSelMoisAu");
            if (intProperty > 1) {
                this.selMoisAu.setValue(intProperty);
            } else {
                int month = Calendar.getInstance().get(Calendar.MONTH);

                this.selMoisAu.setValue(month + 2);
            }
            pDate.add(labelAnnee);
            pDate.add(this.textAnnee);
            String s = DefaultNXProps.getInstance().getStringProperty("EtatChargeAnnee");
            if (s == null || s.length() == 0) {
                DateFormat format = new SimpleDateFormat("yyyy");
                s = format.format(new Date());
            }
            this.textAnnee.setText(s);

            c.weightx = 1;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            this.add(pDate, c);
        }

        // Elt

        JTabbedPane tabbedPane = new JTabbedPane();
        List<SQLRow> l = CaisseCotisationSQLElement.getCaisseCotisation();
        this.model = new EtatChargeModel[l.size()];
        for (int i = 0; i < l.size(); i++) {
            SQLRow row = l.get(i);
            tabbedPane.add(row.getString("NOM"), createTabbedPanel(i, row.getID()));
        }

        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(tabbedPane, c);

        JButton buttonImpression = new JButton("Impression");
        JButton buttonClose = new JButton("Fermer");
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(buttonImpression, c);
        c.gridx++;
        c.weightx = 0;
        this.add(buttonClose, c);

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(EtatChargePanel.this)).dispose();
            };
        });
        buttonImpression.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                EtatChargesPayeSheet bSheet = new EtatChargesPayeSheet(selMoisDu.getSelectedId(), selMoisAu.getSelectedId(), textAnnee.getText());
                new SpreadSheetGeneratorCompta(bSheet, "EtatChargesPaye", false, true);
            }
        });

        this.selMoisAu.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println("Selection Changed");
                refreshModel();
                storeValue();
            }
        });

        this.selMoisDu.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println("Selection Changed");
                refreshModel();
                storeValue();
            }
        });

        this.textAnnee.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {

                refreshModel();
                storeValue();
            }

            public void insertUpdate(DocumentEvent e) {
                refreshModel();
                storeValue();
            };

            public void removeUpdate(DocumentEvent e) {

                refreshModel();
                storeValue();
            }
        });
    }

    private void storeValue() {

        System.err.println("___STORE VALUES");
        DefaultNXProps.getInstance().setProperty("EtatChargeSelMoisDu", String.valueOf(selMoisDu.getSelectedId()));
        DefaultNXProps.getInstance().setProperty("EtatChargeSelMoisAu", String.valueOf(selMoisAu.getSelectedId()));
        DefaultNXProps.getInstance().setProperty("EtatChargeAnnee", String.valueOf(textAnnee.getText()));
        DefaultNXProps.getInstance().store();
    }

    private JPanel createTabbedPanel(final int i, final int idCaisse) {

        final JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.model[i] = new EtatChargeModel(0, 0, 0, idCaisse);
        final JTable tableEltLivre = new JTable(this.model[i]);
        JTableStateManager s = new JTableStateManager(tableEltLivre, new File(Configuration.getInstance().getConfDir(), "state-" + this.getClass().getSimpleName() + "_" + idCaisse + ".xml"), true);
        s.loadState();
        tableEltLivre.setDefaultRenderer(String.class, new EtatChargeRenderer());
        tableEltLivre.setDefaultRenderer(Float.class, new EtatChargeRenderer());

        final JScrollPane scrollPane = new JScrollPane(tableEltLivre);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);

        panel.add(scrollPane, c);

        // cotisation salariale
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.weighty = 0;

        final JPanel panelTotal = new JPanel();
        panelTotal.setOpaque(false);

        final JLabel labelCotSal = new JLabel("Cotisations salariales");
        panelTotal.add(labelCotSal);
        final JTextField textCotS = new JTextField(10);
        panelTotal.add(textCotS);
        textCotS.setEditable(false);
        textCotS.setEnabled(false);

        // cotisation patronale
        final JLabel labelCotPat = new JLabel("Cotisations patronales");
        panelTotal.add(labelCotPat);
        final JTextField textCotP = new JTextField(10);
        panelTotal.add(textCotP);
        textCotP.setEditable(false);
        textCotP.setEnabled(false);
        c.gridy++;
        panel.add(panelTotal, c);

        this.model[i].addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                textCotS.setText(numberFormat.format(model[i].getTotalCotisationSal()));
                textCotP.setText(numberFormat.format(model[i].getTotalCotisationPat()));
            }
        });

        return panel;
    }

    private void refreshModel() {
        for (int i = 0; i < this.model.length; i++) {
            if (this.selMoisDu.getSelectedId() <= this.selMoisAu.getSelectedId()) {

                System.err.println("LOAD");
                String yearS = this.textAnnee.getText();
                int yearI = (yearS.trim().length() == 0) ? 0 : Integer.parseInt(yearS);
                this.model[i].reload(this.selMoisDu.getSelectedId(), this.selMoisAu.getSelectedId(), yearI);
            }
        }
    }
}
