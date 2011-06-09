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

import org.openconcerto.erp.core.finance.accounting.action.ImpressionBalanceAction;
import org.openconcerto.erp.core.finance.accounting.model.BalanceModel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.state.JTableStateManager;
import org.openconcerto.utils.GestionDevise;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/***************************************************************************************************
 * Panel Balance --> liste des comptes avec leurs soldes
 **************************************************************************************************/
public class BalancePanel extends JPanel {

    private long totalDebit, totalCredit;
    private JLabel labelTotalDebit, labelTotalCredit;

    // TODO Clic droit consulter le compte
    public BalancePanel() {

        super();
        this.setLayout(new GridBagLayout());

        this.totalDebit = 0;
        this.totalCredit = 0;

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;

        final BalanceModel model = new BalanceModel();
        final JTable table = new JTable(model);
        new SwingWorker<String, Object>() {

            @Override
            protected String doInBackground() throws Exception {
                model.getBalance();

                return null;
            }

            @Override
            protected void done() {
                Component c = SwingUtilities.getRoot(BalancePanel.this);
                if (c != null) {
                    ((JFrame) c).setTitle("Balance");
                }
                totalCredit = model.getTotalCredit();
                totalDebit = model.getTotalDebit();

                labelTotalCredit.setText(GestionDevise.currencyToString(totalCredit));
                labelTotalDebit.setText(GestionDevise.currencyToString(totalDebit));
                model.fireTableDataChanged();
            }
        }.execute();

        JTableStateManager s = new JTableStateManager(table, new File(Configuration.getInstance().getConfDir(), "state-" + this.getClass().getSimpleName() + ".xml"), true);
        s.loadState();
        table.getTableHeader().setReorderingAllowed(false);
        this.add(new JScrollPane(table), c);

        BalanceCellRenderer rend = new BalanceCellRenderer(0);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rend);
        }

        JPanel panelTotal = new JPanel();
        panelTotal.setLayout(new GridBagLayout());
        panelTotal.setBorder(BorderFactory.createTitledBorder("Totaux"));

        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;

        this.totalCredit = model.getTotalCredit();
        this.totalDebit = model.getTotalDebit();

        this.labelTotalCredit = new JLabel(GestionDevise.currencyToString(this.totalCredit));
        this.labelTotalDebit = new JLabel(GestionDevise.currencyToString(this.totalDebit));
        panelTotal.add(new JLabel("Débit"), c);
        c.gridx++;
        panelTotal.add(this.labelTotalDebit, c);

        c.gridx = 0;
        c.gridy++;
        panelTotal.add(new JLabel("Crédit"), c);
        c.gridx++;
        panelTotal.add(this.labelTotalCredit, c);

        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 2;
        this.add(panelTotal, c);

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
                ((JFrame) SwingUtilities.getRoot(BalancePanel.this)).dispose();
            };
        });
        buttonImpression.addActionListener(new ImpressionBalanceAction());
    }
}
