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
 
 package org.openconcerto.erp.core.supplychain.supplier.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.reports.history.ui.ListeHistoriquePanel;
import org.openconcerto.erp.core.supplychain.supplier.ui.HistoriqueFournBilanPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.users.rights.JListSQLTablePanel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NouvelHistoriqueListeFournAction extends CreateFrameAbstractAction {
    public NouvelHistoriqueListeFournAction() {
        super();
        this.putValue(Action.NAME, "Historique fournisseurs");
    }

    public JFrame createFrame() {
        SQLBase b = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        // List<String> l = new ArrayList<String>();
        // l.add("SAISIE_ACHAT");
        // l.add("CHEQUE_FOURNISSEUR");
        Map<String, List<String>> mapList = new HashMap<String, List<String>>();
        mapList.put("Achats", Arrays.asList("SAISIE_ACHAT"));
        mapList.put("Chèques émis", Arrays.asList("CHEQUE_FOURNISSEUR"));

        final HistoriqueFournBilanPanel panelBilan = new HistoriqueFournBilanPanel();
        final ListeHistoriquePanel listHistoriquePanel = new ListeHistoriquePanel("Fournisseurs", JListSQLTablePanel.createComboRequest(
                Configuration.getInstance().getDirectory().getElement(b.getTable("FOURNISSEUR")), true), mapList, panelBilan, null, null);

        listHistoriquePanel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                SQLRowAccessor row = listHistoriquePanel.getSelectedRow();
                if (row != null) {
                    panelBilan.updateData(row.getID());
                }
            }
        });

        return new PanelFrame(listHistoriquePanel, "Historique fournisseurs");
    }
}
