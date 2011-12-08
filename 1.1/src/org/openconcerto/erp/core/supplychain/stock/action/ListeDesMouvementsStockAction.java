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
 
 package org.openconcerto.erp.core.supplychain.stock.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTable;


public class ListeDesMouvementsStockAction extends CreateFrameAbstractAction {

    public ListeDesMouvementsStockAction() {
        super();
        this.putValue(Action.NAME, "Liste des mouvements de stock");
    }

    public JFrame createFrame() {

        final IListFrame frame = new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK")));

        JTable table = frame.getPanel().getListe().getJTable();

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menuDroit = new JPopupMenu();
                    menuDroit.add(new AbstractAction("Voir la source") {
                        public void actionPerformed(ActionEvent e) {
                            MouvementStockSQLElement.showSource(frame.getPanel().getListe().getSelectedId());
                        }
                    });
                    menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                }
            }
        });
        return frame;
    }
}
