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
 
 package org.openconcerto.erp.core.supplychain.receipt.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;

public class ListeDesBonsReceptionsAction extends CreateFrameAbstractAction {

    public ListeDesBonsReceptionsAction() {
        super();
        this.putValue(Action.NAME, "Liste des bons de réceptions");
    }

    public JFrame createFrame() {
        final IListFrame frame = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("BON_RECEPTION")));
        frame.getPanel().getListe().getJTable().addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent mouseEvent) {

                if (mouseEvent.getButton() == MouseEvent.BUTTON3 && frame.getPanel().getListe().getSelectedId() > 1) {
                    System.err.println("Display Menu");
                    JPopupMenu menuDroit = new JPopupMenu();

                    final SQLRow rowCmd = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("BON_RECEPTION").getRow(frame.getPanel().getListe().getSelectedId());

                    // Transfert vers facture
                    AbstractAction factureAction = (new AbstractAction("Transfert vers facture") {
                        public void actionPerformed(ActionEvent e) {
                            transfertFactureFournisseur(rowCmd);
                        }
                    });

                    menuDroit.add(factureAction);

                    menuDroit.pack();
                    menuDroit.show(mouseEvent.getComponent(), mouseEvent.getPoint().x, mouseEvent.getPoint().y);
                    menuDroit.setVisible(true);
                }
            }
        });

        return frame;
    }

    /**
     * Transfert en Facture
     * 
     * @param row
     */
    private void transfertFactureFournisseur(SQLRow row) {
        BonReceptionSQLElement elt = (BonReceptionSQLElement) Configuration.getInstance().getDirectory().getElement("BON_RECEPTION");
        elt.transfertFacture(row.getID());
    }
}
