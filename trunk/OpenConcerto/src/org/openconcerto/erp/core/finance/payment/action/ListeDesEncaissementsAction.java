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
 
 package org.openconcerto.erp.core.finance.payment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;

import java.math.BigInteger;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;


public class ListeDesEncaissementsAction extends CreateFrameAbstractAction {

    public ListeDesEncaissementsAction() {
        super();
        this.putValue(Action.NAME, "Liste des encaissements");
    }

    public JFrame createFrame() {

        final SQLElement elementEchClient = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
        final SQLElement elementModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        Where w = new Where(elementEchClient.getTable().getField("ID_MODE_REGLEMENT"), "=", elementModeRegl.getTable().getKey());
        Where w2 = new Where(elementModeRegl.getTable().getField("AJOURS"), "=", 0).and(new Where(elementModeRegl.getTable().getField("LENJOUR"), "=", 0));
        ListSQLRequest req = ListSQLRequest.copy(elementEchClient.getListRequest(), null);
        IListe liste = new IListe(req);
        IListFrame frame = new IListFrame(new ListeViewPanel(elementEchClient, liste));

        DeviseNiceTableCellRenderer rend = new DeviseNiceTableCellRenderer();
        JTable table = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) == BigInteger.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }
        return frame;
    }
}
