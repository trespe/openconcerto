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
 
 package org.openconcerto.erp.core.humanresources.payroll.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.humanresources.payroll.ui.HistoriqueFichePayePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;

public class NouvelHistoriqueFichePayeAction extends CreateFrameAbstractAction {
    public NouvelHistoriqueFichePayeAction() {
        super();
        this.putValue(Action.NAME, "Liste des fiches de paye");
    }

    public JFrame createFrame() {
        SQLBase b = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        List<String> l = new ArrayList<String>();
        l.add("FICHE_PAYE");

        // final HistoriqueFournBilanPanel panelBilan = new HistoriqueFournBilanPanel();
        // final ListeHistoriquePanel listHistoriquePanel = new ListeHistoriquePanel("Salariés",
        // b.getTable("SALARIE"), l, null, null);

        return new PanelFrame(new HistoriqueFichePayePanel(), "Liste des fiches paye");
    }
}
