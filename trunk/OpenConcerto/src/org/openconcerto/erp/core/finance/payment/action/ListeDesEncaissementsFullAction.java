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
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.IListFrame;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.Where;

public class ListeDesEncaissementsFullAction extends CreateFrameAbstractAction {

    public ListeDesEncaissementsFullAction() {
        super();
        this.putValue(Action.NAME, "Liste des encaissements");
    }

    // SELECT * FROM
    // "GestionNX84"."SAISIE_VENTE_FACTURE" "f"
    // JOIN "GestionNX84"."MOUVEMENT" "mvt_f" on "f"."ID_MOUVEMENT" = "mvt_f"."ID"
    // LEFT JOIN ( "GestionNX84"."ECHEANCE_CLIENT" "e"
    // JOIN "GestionNX84"."MOUVEMENT" "mvt_e" on "e"."ID_MOUVEMENT" = "mvt_e"."ID" ) "ee"
    // on "mvt_f"."ID_PIECE" = "ee"."ID_PIECE" ORDER BY "f"."ID"

    public JFrame createFrame() {
        final SQLElement elementFacture = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE");
        final SQLElement elementEchClient = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT");
        final SQLElement elementModeRegl = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        final SQLElement elementMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT");

        IListFrame frame = new IListFrame(new ListeViewPanel(elementFacture));

        DeviseNiceTableCellRenderer rend = new DeviseNiceTableCellRenderer();
        JTable table = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) == BigInteger.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }

        Where w = new Where(elementFacture.getTable().getField("ID_MODE_REGLEMENT"), "=", elementModeRegl.getTable().getKey());

        Where w2 = new Where(elementModeRegl.getTable().getField("AJOURS"), "=", 0).and(new Where(elementModeRegl.getTable().getField("LENJOUR"), "=", 0));
        Where w6 = new Where(elementEchClient.getTable().getField("ID_MOUVEMENT"), "=", elementMvt.getTable().getKey());
        Where w3 = new Where(elementMvt.getTable().getField("SOURCE"), "=", elementFacture.getTable().getName());
        Where w4 = new Where(elementMvt.getTable().getField("IDSOURCE"), "=", elementFacture.getTable().getKey());
        Where w5 = new Where(elementEchClient.getTable().getField("REGLE"), "=", Boolean.TRUE);

        List<String> l = new ArrayList<String>();
        l.add("NUMERO");
        l.add("DATE");
        l.add("ID_CLIENT");
        l.add("ID_MODE_REGLEMENT");

        Where wFinal = w.and(w2.or(w6.and(w3.and(w4.and(w5)))));

        System.err.println(wFinal.getClause());

        frame.getPanel().getListe().setRequest(new ListSQLRequest(elementFacture.getTable(), l, wFinal));
        return frame;
    }
}
