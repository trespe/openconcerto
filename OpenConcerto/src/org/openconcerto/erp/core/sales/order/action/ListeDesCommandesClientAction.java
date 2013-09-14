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
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.component.TransfertBaseSQLComponent;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.order.report.CommandeClientXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ListeDesCommandesClientAction extends CreateFrameAbstractAction {

    public ListeDesCommandesClientAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes clients");
    }

    public JFrame createFrame() {
        final JFrame frame = new JFrame("Commandes clients");
        // Actions
        // Transfert vers facture
        PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertBonLivraisonClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.deliverynote");

        // Transfert vers facture
        PredicateRowAction factureAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                transfertFactureClient(IListe.get(e).copySelectedRows());
            }
        }, false, "sales.order.create.invoice");

        // Transfert vers commande
        PredicateRowAction cmdAction = new PredicateRowAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final int selectedId = IListe.get(e).getSelectedId();
                ComptaPropsConfiguration.getInstanceCompta().getNonInteractiveSQLExecutor().execute(new Runnable() {

                    @Override
                    public void run() {
                        final CommandeClientSQLElement elt = (CommandeClientSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
                        elt.transfertCommande(selectedId);

                    }
                });

            }

        }, false, "sales.order.create.supplier.order");

        cmdAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        factureAction.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());

        // Tabs
        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Commandes non facturées", createOrderWithoutInvoicePanel(cmdAction, factureAction, bonAction));
        tabs.addTab("Commandes facturées", createOrderWithInvoicePanel(cmdAction, bonAction));
        tabs.addTab("Toutes les commandes", createAllOrderPanel(cmdAction, factureAction, bonAction));
        frame.setContentPane(tabs);

        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        FrameUtil.setBounds(frame);
        final File file = IListFrame.getConfigFile(eltCmd, frame.getClass());
        if (file != null)
            new WindowStateManager(frame, file).loadState();
        return frame;
    }

    JPanel createAllOrderPanel(final PredicateRowAction cmdAction, final PredicateRowAction factureAction, final PredicateRowAction bonAction) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        allowedActions.add(bonAction);
        allowedActions.add(factureAction);
        allowedActions.add(cmdAction);
        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    private ListeAddPanel getPanel(final SQLElement eltCmd, final SQLTableModelSourceOnline tableSource, final List<RowAction> allowedActions) {
        final ListeAddPanel panel = new ListeAddPanel(eltCmd, new IListe(tableSource));

        final List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(eltCmd.getTable().getField("T_HT"));
        final IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, "Total des commandes de la liste");

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;

        // Date panel
        final IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), eltCmd.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());

        panel.getListe().addIListeActions(new MouseSheetXmlListeListener(CommandeClientXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                return allowedActions;
            }
        }.getRowActions());

        datePanel.setFilterOnDefault();

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setOpaque(false);
        final GridBagConstraints c2 = new DefaultGridBagConstraints();
        c2.fill = GridBagConstraints.NONE;
        c2.weightx = 1;
        bottomPanel.add(datePanel, c2);

        c2.gridx++;
        c2.weightx = 0;
        c2.anchor = GridBagConstraints.EAST;
        bottomPanel.add(totalPanel, c2);

        panel.add(bottomPanel, c);
        return panel;
    }

    JPanel createOrderWithInvoicePanel(PredicateRowAction cmdAction, PredicateRowAction bonAction) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        allowedActions.add(bonAction);
        allowedActions.add(cmdAction);

        // Filter on transfered
        final SQLInjector injector = SQLInjector.getInjector(eltCmd.getTable(), eltCmd.getTable().getTable("SAISIE_VENTE_FACTURE"));
        injector.setOnlyTransfered(tableSource);

        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    JPanel createOrderWithoutInvoicePanel(PredicateRowAction cmdAction, PredicateRowAction factureAction, PredicateRowAction bonAction) {
        final SQLElement eltCmd = Configuration.getInstance().getDirectory().getElement("COMMANDE_CLIENT");
        final SQLTableModelSourceOnline tableSource = eltCmd.getTableSource(true);
        final List<RowAction> allowedActions = new ArrayList<RowAction>();
        allowedActions.add(bonAction);
        allowedActions.add(factureAction);
        allowedActions.add(cmdAction);

        // Filter on not transfered
        final SQLInjector injector = SQLInjector.getInjector(eltCmd.getTable(), eltCmd.getTable().getTable("SAISIE_VENTE_FACTURE"));
        injector.setOnlyNotTransfered(tableSource);

        final ListeAddPanel panel = getPanel(eltCmd, tableSource, allowedActions);
        return panel;
    }

    /**
     * Transfert en BL
     * 
     * @param row
     */
    private void transfertBonLivraisonClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "BON_DE_LIVRAISON");
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureClient(List<SQLRowValues> rows) {
        TransfertBaseSQLComponent.openTransfertFrame(rows, "SAISIE_VENTE_FACTURE");

    }
}
