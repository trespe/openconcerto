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
 
 package org.openconcerto.erp.core.supplychain.credit.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.DeviseNiceTableCellRenderer;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.view.IListFrame;

import java.math.BigInteger;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;


public class ListeDesAvoirsFournisseurAction extends CreateFrameAbstractAction {

    public ListeDesAvoirsFournisseurAction() {
        super();
        this.putValue(Action.NAME, "Liste des avoirs fournisseurs");
    }

    public JFrame createFrame() {
        final IListFrame frame = new IListFrame(new ListeGestCommEltPanel(Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR")));

        frame.getPanel().getListe().getJTable().addMouseListener(new MouseSheetXmlListeListener(frame.getPanel().getListe(), AvoirFournisseurXmlSheet.class));

        // Renderer pour les devises
        DeviseNiceTableCellRenderer rend = new DeviseNiceTableCellRenderer();
        JTable table = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnClass(i) == Long.class || table.getColumnClass(i) == BigInteger.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }
        frame.getPanel().getListe().setSQLEditable(false);
        return frame;
    }
}
