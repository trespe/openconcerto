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
 
 package org.openconcerto.erp.core.sales.order.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.core.sales.order.ui.CommandeClientRenderer;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
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

public class ListeDesCommandesClientAction extends CreateFrameAbstractAction {

    public ListeDesCommandesClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes clients");
    }

    public JFrame createFrame() {
        SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        ListeAddPanel listeAddPanel = new ListeAddPanel(eltCmd, new IListe(eltCmd.getTableSource(true))) {
            @Override
            protected GridBagConstraints createConstraints() {
                // TODO Auto-generated method stub
                GridBagConstraints c = super.createConstraints();
                c.gridy++;
                return c;
            }
        };

        List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(eltCmd.getTable().getField("T_HT"));
        // fields.add(eltCmd.getTable().getField("T_TTC"));
        IListTotalPanel totalPanel = new IListTotalPanel(listeAddPanel.getListe(), fields, "Total Global");

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        listeAddPanel.add(totalPanel, c);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(listeAddPanel.getListe(), eltCmd.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        listeAddPanel.add(datePanel, c);

        final IListFrame frame = new IListFrame(listeAddPanel);

        final CommandeClientRenderer rend = CommandeClientRenderer.getInstance();
        c = new DefaultGridBagConstraints();
        final JPanel legendePanel = rend.getLegendePanel();
        legendePanel.setBorder(BorderFactory.createTitledBorder("Légende"));
        legendePanel.setOpaque(true);
        c.fill = GridBagConstraints.NONE;
        frame.getPanel().add(legendePanel, c);

        frame.getPanel().getListe().addIListeActions(new MouseSheetXmlListeListener(CommandeClientXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                // Transfert vers facture
                PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        transfertBonLivraisonClient(IListe.get(e).getSelectedRow());
                    }
                }, false, "sales.order.create.deliverynote");

                // Transfert vers facture
                PredicateRowAction factureAction = new PredicateRowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        transfertFactureClient(IListe.get(e).getSelectedRow());
                    }
                }, false, "sales.order.create.invoice");

                // Transfert vers commande
                PredicateRowAction cmdAction = new PredicateRowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
                        elt.transfertCommande(IListe.get(e).getSelectedId());
                    }

                }, false, "sales.order.create.supplier.order");

                cmdAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
                factureAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
                bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
                List<RowAction> l = new ArrayList<RowAction>();
                l.add(bonAction);
                l.add(factureAction);
                l.add(cmdAction);
                return l;
            }
        }.getRowActions());

        datePanel.setFilterOnDefault();

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
