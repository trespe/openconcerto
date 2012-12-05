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
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.generationDoc.gestcomm.CommandeXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

public class ListeDesCommandesAction extends CreateFrameAbstractAction {
    public ListeDesCommandesAction() {
        super();
        this.putValue(Action.NAME, "Liste des commandes fournisseurs");
    }

    public JFrame createFrame() {
        final IListFrame frame = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("COMMANDE")));

        frame.getPanel().getListe().addIListeActions(new MouseSheetXmlListeListener(CommandeXmlSheet.class) {
            @Override
            public List<RowAction> addToMenu() {
                // Transfert vers BR
                PredicateRowAction bonAction = new PredicateRowAction(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        transfertBonReceptionClient(IListe.get(e).getSelectedRow());
                    }
                }, false, "supplychain.order.create.receipt");

                bonAction.setPredicate(IListeEvent.getSingleSelectionPredicate());

                List<RowAction> l = new ArrayList<RowAction>();
                l.add(bonAction);
                return l;
            }
        }.getRowActions());

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

}
