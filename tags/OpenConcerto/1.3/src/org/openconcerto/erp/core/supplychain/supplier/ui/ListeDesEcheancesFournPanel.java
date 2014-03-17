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
 
 package org.openconcerto.erp.core.supplychain.supplier.ui;

import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.finance.payment.element.ReglerMontantSQLComponent;
import org.openconcerto.erp.core.supplychain.order.ui.ListPanelEcheancesFourn;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class ListeDesEcheancesFournPanel extends JPanel implements TableModelListener {

    private ListPanelEcheancesFourn panelEcheances;
    private JButton regler;
    private SQLElement eltRegler = Configuration.getInstance().getDirectory().getElement("REGLER_MONTANT");

    public ListeDesEcheancesFournPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.panelEcheances = new ListPanelEcheancesFourn();

        // PANEL AVEC LA LISTE DES ECHEANCES
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.panelEcheances, c);

        final IListe liste = this.panelEcheances.getListe();
        IListFilterDatePanel datePanel = new IListFilterDatePanel(liste, liste.getRequest().getPrimaryTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());

        c.weighty = 0;
        c.gridy++;
        c.weightx = 1;
        this.add(datePanel, c);

        IListTotalPanel totalPanel = new IListTotalPanel(liste, Arrays.asList(this.panelEcheances.getElement().getTable().getField("MONTANT")));

        c.weighty = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(totalPanel, c);

        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;

        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        final JCheckBox checkRegCompta = new JCheckBox("Voir les régularisations de comptabilité");
        if (UserRightsManager.getCurrentUserRights().haveRight(ComptaUserRight.MENU)) {
            this.add(checkRegCompta, c);
        }

        checkRegCompta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelEcheances.setShowRegCompta(checkRegCompta.isSelected());

            }
        });

        // Bouton Encaisser
        c.anchor = GridBagConstraints.EAST;

        c.gridx++;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.regler = new JButton("Régler");
        this.regler.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.regler, c);

        this.regler.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                List<Integer> selectedIds = ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSelection().getSelectedIDs();
                List<SQLRow> selectedRows = new ArrayList<SQLRow>(selectedIds.size());
                int idCpt = -1;
                int idFournisseur = -1;
                boolean showMessage = false;

                String numeroFact = "";
                for (Integer integer : selectedIds) {
                    final SQLRow row = ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSource().getPrimaryTable().getRow(integer);
                    selectedRows.add(row);

                    String nom = row.getForeignRow("ID_MOUVEMENT").getForeignRow("ID_PIECE").getString("NOM");
                    numeroFact += " " + nom;

                    SQLRow rowFournisseur = row.getForeignRow("ID_FOURNISSEUR");
                    int idTmp = rowFournisseur.getInt("ID_COMPTE_PCE");
                    int idCliTmp = rowFournisseur.getID();
                    if (idCpt > -1 && idCpt != idTmp) {
                        JOptionPane.showMessageDialog(null, "Impossible d'effectuer un encaissement sur plusieurs factures ayant des fournisseurs avec des comptes différents.");
                        return;
                    } else {
                        idCpt = idTmp;
                    }

                    if (idFournisseur > -1 && idFournisseur != idCliTmp) {
                        showMessage = true;
                    } else {
                        idFournisseur = idCliTmp;
                    }
                }
                if (showMessage) {
                    int answer = JOptionPane.showConfirmDialog(null, "Attention vous avez sélectionné des factures ayant des fournisseurs différents. Voulez vous continuer?");
                    if (answer != JOptionPane.YES_OPTION) {
                        return;
                    }

                }
                SQLElement encaisseElt = Configuration.getInstance().getDirectory().getElement("REGLER_MONTANT");
                EditFrame edit = new EditFrame(encaisseElt);

                SQLRowValues rowVals = new SQLRowValues(encaisseElt.getTable());

                rowVals.put("ID_FOURNISSEUR", idFournisseur);
                rowVals.put("NOM", numeroFact);

                final ReglerMontantSQLComponent sqlComponent = (ReglerMontantSQLComponent) edit.getSQLComponent();

                sqlComponent.resetValue();
                sqlComponent.select(rowVals);
                sqlComponent.loadEcheancesFromRows(selectedRows);
                edit.pack();
                edit.setVisible(true);
            }
        });

        // Bouton Fermer
        c.gridx++;
        c.weightx = 0;
        JButton fermer = new JButton("fermer");
        this.add(fermer, c);
        fermer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JFrame tmpF = (JFrame) SwingUtilities.getRoot(ListeDesEcheancesFournPanel.this);
                tmpF.setVisible(false);
                tmpF.dispose();

            }
        });

        this.panelEcheances.getListe().addListener(this);
    }

    public ListPanelEcheancesFourn getListPanelEcheanceFourn() {
        return this.panelEcheances;
    }

    public void tableChanged(TableModelEvent e) {

        this.regler.setEnabled(this.panelEcheances.getJTable().getRowCount() > 0);
    }

}
