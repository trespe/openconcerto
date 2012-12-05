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
 
 package org.openconcerto.erp.core.sales.shipment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.sales.shipment.report.BonLivraisonXmlSheet;
import org.openconcerto.erp.core.sales.shipment.ui.BonLivraisionRenderer;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowAccessor;
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
import javax.swing.JTable;

public class ListeDesBonsDeLivraisonAction extends CreateFrameAbstractAction {

    public ListeDesBonsDeLivraisonAction() {
        super();
        this.putValue(Action.NAME, "Liste des bons de livraison");
    }

    public JFrame createFrame() {
        final IListFrame edit1 = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON")) {
            @Override
            protected GridBagConstraints createConstraints() {
                // TODO Auto-generated method stub
                GridBagConstraints c = super.createConstraints();
                c.gridy++;
                return c;
            }
        });

        JTable table = edit1.getPanel().getListe().getJTable();

        final BonLivraisionRenderer rend = BonLivraisionRenderer.getInstance();
        GridBagConstraints c = new DefaultGridBagConstraints();
        final JPanel legendePanel = rend.getLegendePanel();
        legendePanel.setBorder(BorderFactory.createTitledBorder("Légende"));
        legendePanel.setOpaque(true);
        c.fill = GridBagConstraints.NONE;
        edit1.getPanel().add(legendePanel, c);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rend);
        }

        edit1.getPanel().getListe().addIListeActions(new MouseSheetXmlListeListener(BonLivraisonXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                PredicateRowAction actionTransfertFacture = new PredicateRowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent ev) {
                        transfertFactureClient(IListe.get(ev).getSelectedRows());
                    }
                }, false, "sales.shipment.create.invoice");
                actionTransfertFacture.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
                List<RowAction> l = new ArrayList<RowAction>();
                l.add(actionTransfertFacture);

                return l;
            }
        }.getRowActions());

        return edit1;
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureClient(List<SQLRowAccessor> rows) {
        BonDeLivraisonSQLElement elt = (BonDeLivraisonSQLElement) Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON");
        elt.transfertFacture(rows);
    }
}
