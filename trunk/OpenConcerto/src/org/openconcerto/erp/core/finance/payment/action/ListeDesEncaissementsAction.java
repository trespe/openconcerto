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

import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;

import javax.swing.Action;
import javax.swing.JFrame;

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
        IListFrame frame = new IListFrame(new ListeViewPanel(elementEchClient));

        List<SQLField> fields = new ArrayList<SQLField>(2);
        fields.add(elementEchClient.getTable().getField("MONTANT"));

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
        IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(), elementEchClient.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        frame.getPanel().add(datePanel, c);

        return frame;
    }
}
