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

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.finance.accounting.ui.ListeGestCommEltPanel;
import org.openconcerto.erp.generationDoc.gestcomm.AvoirFournisseurXmlSheet;
import org.openconcerto.erp.model.MouseSheetXmlListeListener;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;

import javax.swing.Action;
import javax.swing.JFrame;

public class ListeDesAvoirsFournisseurAction extends CreateFrameAbstractAction {

    public ListeDesAvoirsFournisseurAction() {
        super();
        this.putValue(Action.NAME, "Liste des avoirs fournisseurs");
    }

    public JFrame createFrame() {
        SQLElement element = Configuration.getInstance().getDirectory().getElement("AVOIR_FOURNISSEUR");
        ListeGestCommEltPanel panel = new ListeGestCommEltPanel(element);

        List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(element.getTable().getField("MONTANT_HT"));
        fields.add(element.getTable().getField("MONTANT_TTC"));
        IListTotalPanel totalPanel = new IListTotalPanel(panel.getListe(), fields, "Total Global");
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
        panel.add(totalPanel, c);

        // Date panel
        IListFilterDatePanel datePanel = new IListFilterDatePanel(panel.getListe(), element.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        datePanel.setFilterOnDefault();
        panel.add(datePanel, c);

        final IListFrame frame = new IListFrame(panel);
        frame.getPanel().setAddVisible(true);
        frame.getPanel().getListe().addIListeActions(new MouseSheetXmlListeListener(AvoirFournisseurXmlSheet.class).getRowActions());
        frame.getPanel().getListe().setSQLEditable(false);

        return frame;
    }
}
