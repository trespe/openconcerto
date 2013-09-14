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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.customerrelationship.customer.element.RelanceSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.finance.payment.component.EncaisserMontantSQLComponent;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.erp.rights.NXRights;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ListeDesEcheancesClientsPanel extends JPanel {

    private ListPanelEcheancesClients panelEcheances;
    private EditFrame editEncaisse = null;
    private EditFrame editRelance = null;
    private JButton relancer, encaisser;

    public ListeDesEcheancesClientsPanel() {
        this(false);
    }

    // TODO GEstion Relance (??? loi NRE pour le calcul des penalites)
    public ListeDesEcheancesClientsPanel(boolean onlyOld) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.panelEcheances = new ListPanelEcheancesClients(onlyOld);

        // PANEL AVEC LA LISTE DES ECHEANCES
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
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

        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        // Bouton Relancer
        this.relancer = new JButton("Relancer");
        this.relancer.setHorizontalAlignment(SwingConstants.RIGHT);
        if (UserManager.getInstance().getCurrentUser().getRights().haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {
            c.gridx++;
            this.add(this.relancer, c);
            this.relancer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    relanceClient();
                }
            });
        }

        // Bouton Encaisser
        this.encaisser = new JButton("Encaisser");
        this.encaisser.setHorizontalAlignment(SwingConstants.RIGHT);
        c.gridx++;
        c.weightx = 0;
        if (UserManager.getInstance().getCurrentUser().getRights().haveRight(NXRights.GESTION_ENCAISSEMENT.getCode())) {

            this.add(this.encaisser, c);
        }
        this.encaisser.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                List<Integer> selectedIds = ListeDesEcheancesClientsPanel.this.panelEcheances.getListe().getSelection().getSelectedIDs();
                List<SQLRow> selectedRows = new ArrayList<SQLRow>(selectedIds.size());
                int idCpt = -1;
                int idClient = -1;
                boolean showMessage = false;

                String numeroFact = "";
                for (Integer integer : selectedIds) {
                    final SQLRow row = ListeDesEcheancesClientsPanel.this.panelEcheances.getListe().getSource().getPrimaryTable().getRow(integer);
                    // System.err.println("ListeDesEcheancesClientsPanel.ListeDesEcheancesClientsPanel().new ActionListener() {...}.actionPerformed()"
                    // + row);
                    selectedRows.add(row);

                    String nom = row.getForeignRow("ID_MOUVEMENT").getForeignRow("ID_PIECE").getString("NOM");
                    numeroFact += " " + nom;

                    SQLRow rowClient = row.getForeignRow("ID_CLIENT");
                    int idTmp = rowClient.getInt("ID_COMPTE_PCE");
                    int idCliTmp = rowClient.getID();
                    if (idCpt > -1 && idCpt != idTmp) {
                        JOptionPane.showMessageDialog(null, "Impossible d'effectuer un encaissement sur plusieurs factures ayant des clients avec des comptes différents.");
                        return;
                    } else {
                        idCpt = idTmp;
                    }

                    if (idClient > -1 && idClient != idCliTmp) {
                        showMessage = true;
                    } else {
                        idClient = idCliTmp;
                    }
                }
                if (showMessage) {
                    int answer = JOptionPane.showConfirmDialog(null, "Attention vous avez sélectionné des factures ayant des clients différents. Voulez vous continuer?");
                    if (answer != JOptionPane.YES_OPTION) {
                        return;
                    }

                }
                SQLElement encaisseElt = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
                if (ListeDesEcheancesClientsPanel.this.editEncaisse == null) {
                    ListeDesEcheancesClientsPanel.this.editEncaisse = new EditFrame(encaisseElt);
                    ListeDesEcheancesClientsPanel.this.editEncaisse.setIconImages(Gestion.getFrameIcon());
                }

                SQLRowValues rowVals = new SQLRowValues(encaisseElt.getTable());

                rowVals.put("ID_CLIENT", idClient);
                rowVals.put("NOM", numeroFact);

                final EncaisserMontantSQLComponent sqlComponent = (EncaisserMontantSQLComponent) ListeDesEcheancesClientsPanel.this.editEncaisse.getSQLComponent();

                sqlComponent.resetValue();
                sqlComponent.select(rowVals);
                sqlComponent.loadEcheancesFromRows(selectedRows);
                ListeDesEcheancesClientsPanel.this.editEncaisse.pack();
                ListeDesEcheancesClientsPanel.this.editEncaisse.setVisible(true);
            }
        });

        // Bouton Fermer
        c.gridx++;
        c.weightx = 0;
        JButton fermer = new JButton("fermer");
        this.add(fermer, c);
        fermer.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JFrame tmpF = (JFrame) SwingUtilities.getRoot(ListeDesEcheancesClientsPanel.this);
                tmpF.setVisible(false);
                tmpF.dispose();

            }
        });

        // Gestion de la souris
        this.panelEcheances.getJTable().addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent mE) {

                // Mise à jour de l'echeance sur la frame de reglement
                // si cette derniere est cree
                final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                final SQLRow row = panelEcheances.getListe().getSelectedRow();
                if (mE.getButton() == MouseEvent.BUTTON1) {

                    if (ListeDesEcheancesClientsPanel.this.editEncaisse != null) {
                        final SQLRowValues rowVals = new SQLRowValues(base.getTable("ENCAISSER_MONTANT"));
                        rowVals.put("ID_ECHEANCE_CLIENT", row.getID());

                        ListeDesEcheancesClientsPanel.this.editEncaisse.getSQLComponent().select(rowVals);
                        ListeDesEcheancesClientsPanel.this.editEncaisse.pack();
                    }
                }

            }
        });

        liste.addIListener(new IListener() {
            public void selectionId(int id, int field) {
                if (id > 1) {
                    final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                    final SQLTable tableEch = base.getTable("ECHEANCE_CLIENT");
                    final SQLRow rowEch = tableEch.getRow(id);

                    int idMvtSource = MouvementSQLElement.getSourceId(rowEch.getInt("ID_MOUVEMENT"));
                    SQLRow rowMvtSource = base.getTable("MOUVEMENT").getRow(idMvtSource);

                    if (!rowMvtSource.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                        ListeDesEcheancesClientsPanel.this.relancer.setEnabled(false);
                    } else {
                        ListeDesEcheancesClientsPanel.this.relancer.setEnabled(true);
                    }
                    ListeDesEcheancesClientsPanel.this.encaisser.setEnabled(true);
                } else {
                    ListeDesEcheancesClientsPanel.this.relancer.setEnabled(false);

                    ListeDesEcheancesClientsPanel.this.encaisser.setEnabled(false);
                }

            }
        });
        this.relancer.setEnabled(false);
        this.encaisser.setEnabled(false);

    }

    private SQLRow rowSource;

    private void relanceClient() {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLElement relanceElt = Configuration.getInstance().getDirectory().getElement("RELANCE");

        this.rowSource = this.panelEcheances.getListe().getSelectedRow();

        if (this.rowSource != null) {
            int idMvtSource = MouvementSQLElement.getSourceId(rowSource.getInt("ID_MOUVEMENT"));
            SQLRow rowMvtSource = base.getTable("MOUVEMENT").getRow(idMvtSource);

            if (!rowMvtSource.getString("SOURCE").equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
                this.relancer.setEnabled(false);
                return;
            }

            if (this.editRelance == null) {
                this.editRelance = new EditFrame(relanceElt);
                this.editRelance.setIconImages(Gestion.getFrameIcon());
                this.editRelance.addEditPanelListener(new EditPanelListener() {

                    public void cancelled() {
                    }

                    public void modified() {
                    }

                    public void deleted() {
                    }

                    public void inserted(int id) {
                        System.err.println("INSERTED " + id + " -- " + rowSource.getID());
                        int nbRelance = rowSource.getInt("NOMBRE_RELANCE");
                        nbRelance++;

                        SQLRowValues rowValsEch = new SQLRowValues(rowSource.getTable());
                        rowValsEch.put("NOMBRE_RELANCE", nbRelance);
                        rowValsEch.put("DATE_LAST_RELANCE", new Date());

                        try {
                            rowValsEch.update(rowSource.getID());
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }

            SQLRowValues rowVals = new SQLRowValues(relanceElt.getTable());
            rowVals.put("ID_SAISIE_VENTE_FACTURE", rowMvtSource.getInt("IDSOURCE"));
            rowVals.put("MONTANT", rowSource.getObject("MONTANT"));
            rowVals.put("ID_CLIENT", rowSource.getInt("ID_CLIENT"));
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(RelanceSQLElement.class, new Date()));
            this.editRelance.getSQLComponent().select(rowVals);

            this.editRelance.pack();
            this.editRelance.setVisible(true);
        } else {
            Thread.dumpStack();
        }
    }

    public IListPanel getListPanelEcheancesClients() {
        return this.panelEcheances;
    }
}
