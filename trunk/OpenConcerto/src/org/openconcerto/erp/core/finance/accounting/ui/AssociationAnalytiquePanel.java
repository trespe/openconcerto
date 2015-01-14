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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class AssociationAnalytiquePanel extends JPanel {

    public AssociationAnalytiquePanel(final SQLRowAccessor rowEcr) {
        super(new GridBagLayout());

        final long debit = rowEcr.getObject("DEBIT") == null ? 0 : rowEcr.getLong("DEBIT");
        final long credit = rowEcr.getObject("CREDIT") == null ? 0 : rowEcr.getLong("CREDIT");
        final long solde = debit - credit;

        final GridBagConstraints c = new DefaultGridBagConstraints();
        final AnalytiqueItemTable table = new AnalytiqueItemTable();
        final SQLRequestComboBox box = new SQLRequestComboBox();
        box.uiInit(Configuration.getInstance().getDirectory().getElement("ECRITURE").getComboRequest());
        box.setEnabled(false);
        box.setValue(rowEcr);

        c.weightx = 0;
        this.add(new JLabel("Ecriture associée"), c);
        c.weightx = 1;
        c.gridx++;
        this.add(box, c);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.add(table, c);

        final JLabel labelTotal = new JLabelBold("Total réparti : 0/" + solde);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        this.add(labelTotal, c);

        table.insertFrom("ID_ECRITURE", rowEcr.getID());

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        final JButton buttonApply = new JButton("Enregistrer les modifications");
        JButton buttonClose = new JButton("Annuler");
        JPanel panelButton = new JPanel();
        panelButton.add(buttonApply);
        panelButton.add(buttonClose);
        this.add(panelButton, c);

        buttonClose.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(AssociationAnalytiquePanel.this)).dispose();

            }
        });
        buttonApply.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                table.updateField("ID_ECRITURE", rowEcr.getID());
                ((JFrame) SwingUtilities.getRoot(AssociationAnalytiquePanel.this)).dispose();
            }
        });

        final RowValuesTableModel model = table.getModel();
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {

                final int count = model.getRowCount();
                long soldeRows = 0;
                for (int i = 0; i < count; i++) {
                    SQLRowValues rowVals = model.getRowValuesAt(i);
                    soldeRows += rowVals.getLong("MONTANT");
                }
                buttonApply.setEnabled(soldeRows == solde);
                if (!buttonApply.isEnabled()) {
                    buttonApply.setToolTipText("La répartition analytique de l'écriture n'est pas totale!");
                } else {
                    buttonApply.setToolTipText(null);
                }
                labelTotal.setText("Répartition totale : " + GestionDevise.currencyToString(soldeRows) + "/" + GestionDevise.currencyToString(solde));
            }
        });
    }
}
