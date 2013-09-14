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
 
 package org.openconcerto.erp.core.sales.credit.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirClientXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;

public class ListeDesAvoirsClientsAction extends CreateFrameAbstractAction {

    public ListeDesAvoirsClientsAction() {
        super();
        this.putValue(Action.NAME, "Liste des avoirs client");
    }

    public JFrame createFrame() {
        final SQLElement element = Configuration.getInstance().getDirectory().getElement("AVOIR_CLIENT");
        ListeGestCommEltPanel panel = new ListeGestCommEltPanel(element);
        panel.setAddVisible(true);
        final IListFrame frame = new IListFrame(panel);

        List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(element.getTable().getField("MONTANT_HT"));
        fields.add(element.getTable().getField("MONTANT_TTC"));
        IListTotalPanel totalPanel = new IListTotalPanel(frame.getPanel().getListe(), fields, "Total Global");
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;

        // Total panel
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridy = 4;
        frame.getPanel().add(totalPanel, c);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(), element.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        datePanel.setFilterOnDefault();
        frame.getPanel().add(datePanel, c);

        frame.getPanel().getListe().addIListeActions(new MouseSheetXmlListeListener(AvoirClientXmlSheet.class).getRowActions());

        frame.getPanel().getListe().setSQLEditable(false);
        return frame;
    }
}
