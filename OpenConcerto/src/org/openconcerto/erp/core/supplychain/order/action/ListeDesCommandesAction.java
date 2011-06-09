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
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;

import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;

public class ListeDesCommandesAction extends CreateFrameAbstractAction {
    public ListeDesCommandesAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes fournisseurs");
    }

    public JFrame createFrame() {
        final IListFrame frame = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("COMMANDE")));

        DeviseNiceTableCellRenderer rend = new DeviseNiceTableCellRenderer();
        JTable table = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) == BigInteger.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }

        frame.getPanel().getListe().getJTable().addMouseListener(new MouseSheetXmlListeListener(frame.getPanel().getListe(), CommandeXmlSheet.class) {
            @Override
            public List<AbstractAction> addToMenu() {
                // Transfert vers BR
                AbstractAction bonAction = (new AbstractAction("Transfert vers BR") {
                    public void actionPerformed(ActionEvent e) {
                        transfertBonReceptionClient(frame.getPanel().getListe().getSelectedRow());
                    }
                });

                // Transfert vers facture
                AbstractAction factureAction = (new AbstractAction("Transfert vers facture") {
                    public void actionPerformed(ActionEvent e) {
                        transfertFactureFournisseur(frame.getPanel().getListe().getSelectedRow());
                    }
                });

                List<AbstractAction> l = new ArrayList<AbstractAction>();
                l.add(bonAction);
                l.add(factureAction);
                return l;
            }
        });

        return frame;
    }

    /**
     * Transfert en BR
     * 
     * @param row
     */
    private void transfertBonReceptionClient(SQLRow row) {
        CommandeSQLElement elt = (CommandeSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE");
        elt.transfertBR(row.getID());
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureFournisseur(SQLRow row) {
        CommandeSQLElement elt = (CommandeSQLElement) Configuration.getInstance().getDirectory().getElement("COMMANDE");
        elt.transfertFacture(row.getID());
    }

}
