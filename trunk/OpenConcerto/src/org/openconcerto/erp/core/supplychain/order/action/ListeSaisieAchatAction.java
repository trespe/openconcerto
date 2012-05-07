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
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JFrame;

public class ListeSaisieAchatAction extends CreateFrameAbstractAction {

    public ListeSaisieAchatAction() {
        super();
        this.putValue(Action.NAME, "Liste des saisies d'achats");
    }

    public JFrame createFrame() {
        SQLElement element = Configuration.getInstance().getDirectory().getElement("SAISIE_ACHAT");
        ListeGestCommEltPanel panel = new ListeGestCommEltPanel(element);
        panel.setAddVisible(true);
        IListFrame frame = new IListFrame(panel);
        IListTotalPanel total = new IListTotalPanel(frame.getPanel().getListe(), Arrays.asList(element.getTable().getField("MONTANT_HT"), element.getTable().getField("MONTANT_TTC")));
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        frame.getPanel().add(total, c);

        // Date panel
        Map<IListe, SQLField> map = new HashMap<IListe, SQLField>();
        map.put(frame.getPanel().getListe(), element.getTable().getField("DATE"));

        IListFilterDatePanel datePanel = new IListFilterDatePanel(map, IListFilterDatePanel.getDefaultMap());
        c.gridy = 4;
        c.anchor = GridBagConstraints.CENTER;
        c.weighty = 0;
        datePanel.setFilterOnDefault();
        frame.getPanel().add(datePanel, c);
        return frame;
    }
}
