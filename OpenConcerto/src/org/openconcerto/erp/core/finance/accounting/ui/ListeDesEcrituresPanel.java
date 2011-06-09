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

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.GestionDevise;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class ListeDesEcrituresPanel extends JPanel {

    private ListPanelEcritures panelEcritures;
    private JPanel panelTotal;
    private JPanel panelLegende;
    private JLabel montantDebit;
    private JLabel montantCredit;
    private JLabel montantSolde;

    public ListeDesEcrituresPanel() {

        this.panelEcritures = new ListPanelEcritures();
        this.montantDebit = new JLabel("0.0");
        this.montantCredit = new JLabel("0.0");
        this.montantSolde = new JLabel("0.0");

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.panelEcritures, c);

        /* Panel Legende */
        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        this.panelLegende = new JPanel();
        this.panelLegende.setLayout(new GridBagLayout());
        this.panelLegende.setBorder(BorderFactory.createTitledBorder("Légende"));

        c.insets = new Insets(0, 0, 0, 0);
        JPanel panelValide = new JPanel();
        panelValide.setLayout(new GridBagLayout());
        panelValide.add(new JLabel("Ecritures validées"));
        panelValide.setBackground(Color.WHITE);
        // panelValide.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.panelLegende.add(panelValide, c);

        JPanel panelNonValide = new JPanel();
        panelNonValide.setLayout(new GridBagLayout());
        panelNonValide.add(new JLabel("Ecritures non validées"));
        panelNonValide.setBackground(ListEcritureRenderer.GetCouleurEcritureNonValide());
        // panelNonValide.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.panelLegende.add(panelNonValide, c);

        JPanel panelNonValideToDay = new JPanel();
        panelNonValideToDay.setLayout(new GridBagLayout());
        panelNonValideToDay.add(new JLabel("Ecritures non validées du jour"));
        panelNonValideToDay.setBackground(ListEcritureRenderer.getCouleurEcritureToDay());
        // panelNonValideToDay.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.panelLegende.add(panelNonValideToDay, c);

        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(2, 2, 1, 2);
        this.add(this.panelLegende, c);

        /* Panel Total */
        c.gridx = 0;
        c.gridy = 0;
        this.panelTotal = new JPanel();
        this.panelTotal.setLayout(new GridBagLayout());
        this.panelTotal.setBorder(BorderFactory.createTitledBorder("Totaux"));

        JLabel labelDebit = new JLabel("Débit");
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.weighty = 0;
        this.panelTotal.add(labelDebit, c);

        c.gridx++;
        this.panelTotal.add(this.montantDebit, c);

        JLabel labelCredit = new JLabel("Crédit");

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.panelTotal.add(labelCredit, c);

        c.gridx++;
        this.panelTotal.add(this.montantCredit, c);

        JLabel labelSolde = new JLabel("Solde");

        c.weightx = 0;
        c.gridx = 0;
        c.gridy++;
        this.panelTotal.add(labelSolde, c);

        c.gridx++;
        this.panelTotal.add(this.montantSolde, c);

        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(this.panelTotal, c);

        // Mise à jour des totaux Solde, débit, crédit
        this.panelEcritures.getListe().addListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {

                long totalDebit = 0;
                long totalCredit = 0;
                TableModel tableModel = ListeDesEcrituresPanel.this.panelEcritures.getListe().getTableModel();

                if (tableModel instanceof ITableModel) {
                    final ITableModel model = ListeDesEcrituresPanel.this.panelEcritures.getListe().getModel();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        // no need to handle sorter since we don't care about order
                        final SQLRowValues ecritureRow = model.getRow(i).getRow();

                        totalDebit += ((Long) ecritureRow.getObject("DEBIT")).longValue();
                        totalCredit += ((Long) ecritureRow.getObject("CREDIT")).longValue();
                    }

                    ListeDesEcrituresPanel.this.montantDebit.setText(GestionDevise.currencyToString(totalDebit));
                    ListeDesEcrituresPanel.this.montantCredit.setText(GestionDevise.currencyToString(totalCredit));
                    ListeDesEcrituresPanel.this.montantSolde.setText(GestionDevise.currencyToString(totalDebit - totalCredit));
                }
            }
        });
    }

    public ListPanelEcritures getListPanelEcritures() {
        return this.panelEcritures;
    }
}
