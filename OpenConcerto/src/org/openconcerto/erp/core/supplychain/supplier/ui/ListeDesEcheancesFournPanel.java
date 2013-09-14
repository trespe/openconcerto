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

import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.supplychain.order.ui.ListPanelEcheancesFourn;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class ListeDesEcheancesFournPanel extends JPanel implements TableModelListener {

    private ListPanelEcheancesFourn panelEcheances;
    private EditFrame edit = null;
    private JButton regler;
    private SQLElement eltRegler = Configuration.getInstance().getDirectory().getElement("REGLER_MONTANT");

    public ListeDesEcheancesFournPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.panelEcheances = new ListPanelEcheancesFourn();

        // PANEL AVEC LA LISTE DES ECHEANCES
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        this.add(this.panelEcheances, c);

        // Bouton Encaisser
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        this.regler = new JButton("Régler");
        this.regler.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(this.regler, c);

        this.regler.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                if (ListeDesEcheancesFournPanel.this.edit == null) {

                    ListeDesEcheancesFournPanel.this.edit = new EditFrame(eltRegler);
                }

                SQLRowValues rowVals = new SQLRowValues(eltRegler.getTable());

                rowVals.put("ID_ECHEANCE_FOURNISSEUR", ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSelectedId());

                ListeDesEcheancesFournPanel.this.edit.getSQLComponent().select(rowVals);
                ListeDesEcheancesFournPanel.this.edit.pack();
                ListeDesEcheancesFournPanel.this.edit.setVisible(true);
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
        // Gestion de la souris
        this.panelEcheances.getJTable().addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent mE) {

                // Mise à jour de l'echeance sur la frame de reglement
                // si cette derniere est cree
                if (mE.getButton() == MouseEvent.BUTTON1) {

                    if (ListeDesEcheancesFournPanel.this.edit != null) {
                        SQLRowValues rowVals = new SQLRowValues(eltRegler.getTable());

                        int id = ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSelectedId();

                        rowVals.put("ID_ECHEANCE_FOURNISSEUR", id);

                        ListeDesEcheancesFournPanel.this.edit.getSQLComponent().select(rowVals);
                        ListeDesEcheancesFournPanel.this.edit.pack();
                    }
                }

                // Gestion du clic droit
                if (mE.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menuDroit = new JPopupMenu();

                    menuDroit.add(new AbstractAction("Voir la source") {

                        public void actionPerformed(ActionEvent e) {
                            SQLRow row = ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSelectedRow();
                            MouvementSQLElement.showSource(row.getInt("ID_MOUVEMENT"));
                        }
                    });

                    /*
                     * menuDroit.add(new AbstractAction("Encaisser") {
                     * 
                     * public void actionPerformed(ActionEvent e) { if (edit == null) {
                     * ReglerMontantSQLElement encaisseElt = new ReglerMontantSQLElement(); edit =
                     * new EditFrame(encaisseElt); }
                     * 
                     * SQLRowValues rowVals = new SQLRowValues(new
                     * ReglerMontantSQLElement().getTable());
                     * 
                     * int id =
                     * ListeDesEcheancesFournPanel.this.panelEcheances.getListe().getSelectedId();
                     * 
                     * rowVals.put("ID_ECHEANCE_FOURNISSEUR", id);
                     * 
                     * edit.getSQLComponent().select(rowVals); edit.pack(); edit.setVisible(true); }
                     * });
                     */

                    menuDroit.show(mE.getComponent(), mE.getX(), mE.getY());
                }
            }
        });

    }

    public ListPanelEcheancesFourn getListPanelEcheanceFourn() {
        return this.panelEcheances;
    }

    public void tableChanged(TableModelEvent e) {

        this.regler.setEnabled(this.panelEcheances.getJTable().getRowCount() > 0);
    }

}
