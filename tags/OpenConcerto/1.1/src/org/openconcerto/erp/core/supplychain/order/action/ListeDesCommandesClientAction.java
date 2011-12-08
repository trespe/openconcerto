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
 
 package org.openconcerto.erp.core.supplychain.order.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.order.ui.CommandeClientRenderer;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;

public class ListeDesCommandesClientAction extends CreateFrameAbstractAction {

    public ListeDesCommandesClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes clients");
    }

    public JFrame createFrame() {
        final IListFrame frame = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT")) {
            @Override
            protected GridBagConstraints createConstraints() {
                // TODO Auto-generated method stub
                GridBagConstraints c = super.createConstraints();
                c.gridy++;
                return c;
            }
        });

        final CommandeClientRenderer rend = CommandeClientRenderer.getInstance();
        GridBagConstraints c = new DefaultGridBagConstraints();
        final JPanel legendePanel = rend.getLegendePanel();
        legendePanel.setBorder(BorderFactory.createTitledBorder("Légende"));
        legendePanel.setOpaque(true);
        c.fill = GridBagConstraints.NONE;
        frame.getPanel().add(legendePanel, c);
        JTable table = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            // if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) ==
            // BigInteger.class) {
            table.getColumnModel().getColumn(i).setCellRenderer(rend);
            // }
        }

        frame.getPanel().getListe().getJTable().addMouseListener(new MouseSheetXmlListeListener(frame.getPanel().getListe(), CommandeClientXmlSheet.class) {
            @Override
            public List<AbstractAction> addToMenu() {
                // Transfert vers facture
                AbstractAction bonAction = (new AbstractAction("Transfert vers BL") {
                    public void actionPerformed(ActionEvent e) {
                        transfertBonLivraisonClient(frame.getPanel().getListe().getSelectedRow());
                    }
                });

                // Transfert vers facture
                AbstractAction factureAction = (new AbstractAction("Transfert vers facture") {
                    public void actionPerformed(ActionEvent e) {
                        transfertFactureClient(frame.getPanel().getListe().getSelectedRow());
                    }
                });

                // Transfert vers commande
                AbstractAction cmdAction = (new AbstractAction("Transfert vers commande fournisseur") {
                    public void actionPerformed(ActionEvent e) {
                        CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
                        elt.transfertCommande(frame.getPanel().getListe().getSelectedRow().getID());
                    }
                });
                List<AbstractAction> l = new ArrayList<AbstractAction>();
                l.add(bonAction);
                l.add(factureAction);
                l.add(cmdAction);
                return l;
            }
        });

        return frame;
    }

    /**
     * Transfert en BL
     * 
     * @param row
     */
    private void transfertBonLivraisonClient(SQLRow row) {
        CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        elt.transfertBonLivraison(row.getID());
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureClient(SQLRow row) {
        CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        elt.transfertFacture(row.getID());
    }
}
