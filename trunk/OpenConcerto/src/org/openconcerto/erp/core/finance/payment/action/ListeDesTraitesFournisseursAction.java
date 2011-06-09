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
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.IListFrame;

import java.math.BigInteger;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;

public class ListeDesTraitesFournisseursAction extends CreateFrameAbstractAction {

    public ListeDesTraitesFournisseursAction() {
        super();
        this.putValue(Action.NAME, "Liste des traites, virements founisseurs");
    }

    public JFrame createFrame() {
        SQLTable table = Configuration.getInstance().getBase().getTable("REGLER_MONTANT");
        SQLElement elt = Configuration.getInstance().getDirectory().getElement(table);
        SQLElement eltMode = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");

        IListFrame frame = new IListFrame(new ListeViewPanel(elt));
        frame.getPanel().setAddVisible(false);

        Where wPrev = new Where(elt.getTable().getField("ID_MODE_REGLEMENT"), "=", eltMode.getTable().getKey());
        wPrev = wPrev.and(new Where(eltMode.getTable().getField("ID_TYPE_REGLEMENT"), ">=", TypeReglementSQLElement.TRAITE));
        wPrev = wPrev.and(new Where(elt.getTable().getField("ID_ECHEANCE_FOURNISSEUR"), ">", 1));
        frame.getPanel().getListe().setRequest(ListSQLRequest.copy(elt.getListRequest(), wPrev));

        frame.getPanel().getListe().setSQLEditable(false);

        // Renderer
        DeviseNiceTableCellRenderer rend = new DeviseNiceTableCellRenderer();
        JTable jTable = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < jTable.getColumnCount(); i++) {
            if (jTable.getColumnClass(i) == Long.class || jTable.getColumnClass(i) == BigInteger.class) {
                jTable.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }

        return frame;
    }
}
