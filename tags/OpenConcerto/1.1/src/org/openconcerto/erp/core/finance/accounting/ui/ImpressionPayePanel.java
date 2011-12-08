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

import org.openconcerto.erp.core.common.element.MoisSQLElement;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class ImpressionPayePanel extends JPanel implements ActionListener {

    private JButton valid;
    private JButton annul;
    protected JCheckBox checkImpr;
    protected JCheckBox checkVisu;
    protected ElementComboBox selMoisDeb, selMoisEnd;
    protected JTextField textAnnee;
    DateFormat format = new SimpleDateFormat("yyyy");

    public ImpressionPayePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        Calendar cal = Calendar.getInstance();
        // Période
        final MoisSQLElement moisElt = new MoisSQLElement();
        this.selMoisDeb = new ElementComboBox(true, 25);
        this.selMoisDeb.init(moisElt);
        this.selMoisDeb.setButtonsVisible(false);

        this.selMoisDeb.setAddIconVisible(false);

        this.selMoisEnd = new ElementComboBox(true, 25);
        this.selMoisEnd.init(moisElt);
        this.selMoisEnd.setButtonsVisible(false);

        this.selMoisEnd.setAddIconVisible(false);

        this.add(new JLabel("Période de"), c);
        c.gridx++;
        c.weightx = 0;
        this.add(this.selMoisDeb, c);
        this.selMoisDeb.setValue(2);

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("à"), c);
        c.gridx++;
        c.weightx = 0;
        this.add(this.selMoisEnd, c);
        this.selMoisEnd.setValue(cal.get(Calendar.MONTH) + 2);

        this.textAnnee = new JTextField(5);
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Année"), c);
        c.gridx++;
        c.weightx = 1;
        this.textAnnee.setText(this.format.format(cal.getTime()));
        this.add(this.textAnnee, c);

        this.valid = new JButton("Valider");
        this.annul = new JButton("Fermer");
        this.checkImpr = new JCheckBox("Impression");
        this.checkVisu = new JCheckBox("Visualisation");

        // Check impression visu
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        JPanel panelCheck = new JPanel();
        panelCheck.add(this.checkImpr);
        panelCheck.add(this.checkVisu);
        this.checkVisu.setSelected(true);
        this.add(panelCheck, c);

        // Button
        c.gridy++;
        c.gridx = 0;
        JPanel panelButton = new JPanel();
        panelButton.add(this.valid);
        panelButton.add(this.annul);
        this.add(panelButton, c);

        this.valid.addActionListener(this);

        this.annul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(ImpressionPayePanel.this)).dispose();
            }
        });
        this.selMoisDeb.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                checkValidity();
            }
        });
        this.selMoisEnd.addValueListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                checkValidity();
            }
        });
        this.textAnnee.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                checkValidity();
            }

            public void insertUpdate(DocumentEvent e) {
                checkValidity();
            }

            public void removeUpdate(DocumentEvent e) {
                checkValidity();
            }
        });
    }

    private void checkValidity() {
        if (this.selMoisDeb.getSelectedId() > 1 && this.selMoisEnd.getSelectedId() > 1 && this.textAnnee.getText().trim().length() > 0) {
            if (this.selMoisDeb.getSelectedId() < this.selMoisEnd.getSelectedId()) {
                this.valid.setEnabled(this.checkImpr.isSelected() || this.checkVisu.isSelected());
                return;
            }
        }
        this.valid.setEnabled(false);
    }
}
