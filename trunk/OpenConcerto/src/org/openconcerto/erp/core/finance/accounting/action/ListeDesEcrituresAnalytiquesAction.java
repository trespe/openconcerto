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
 
 package org.openconcerto.erp.core.finance.accounting.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.common.ui.IListTotalPanel;
import org.openconcerto.erp.core.common.ui.ListeViewPanel;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.ui.AssociationAnalytiquePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.Tuple2;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;

public class ListeDesEcrituresAnalytiquesAction extends CreateFrameAbstractAction {

    public ListeDesEcrituresAnalytiquesAction() {
        super();
        this.putValue(Action.NAME, "Liste des écritures analytiques");
    }

    public JFrame createFrame() {

        final SQLElement analytique = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE");

        final ListeViewPanel panel = new ListeViewPanel(analytique) {

            @Override
            protected GridBagConstraints createConstraints() {
                GridBagConstraints c = super.createConstraints();
                c.gridwidth = 2;
                return c;
            }

            @Override
            protected void handleAction(JButton source, ActionEvent evt) {
                if (source == this.buttonModifier) {

                    PanelFrame frameAssoc = new PanelFrame(new AssociationAnalytiquePanel(getListe().getSelectedRow().getForeign("ID_ECRITURE").asRow()), "Association analytique");
                    frameAssoc.setVisible(true);
                }
            }
        };
        panel.setTextButton("Gérer");
        final IListFrame frame = new IListFrame(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getPanel().setSearchFullMode(true);

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 1;

        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getInt("ID_EXERCICE_COMMON"));

        final SQLTable ecritureTable = analytique.getTable().getForeignTable("ID_ECRITURE");
        final IListFilterDatePanel comp = new IListFilterDatePanel(frame.getPanel().getListe(), ecritureTable.getField("DATE"), IListFilterDatePanel.getDefaultMap());
        comp.setDateDu((Date) rowExercice.getObject("DATE_DEB"));
        c.weightx = 1;
        frame.getPanel().add(comp, c);

        List<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>> fields = new ArrayList<Tuple2<? extends SQLTableModelColumn, IListTotalPanel.Type>>(2);
        List<SQLTableModelColumn> l = frame.getPanel().getListe().getSource().getColumns();
        fields.add(Tuple2.create(l.get(l.size() - 2), IListTotalPanel.Type.SOMME));
        fields.add(Tuple2.create(l.get(l.size() - 1), IListTotalPanel.Type.SOMME));
        // fields.add(eltCmd.getTable().getField("T_TTC"));
        IListTotalPanel comp2 = new IListTotalPanel(frame.getPanel().getListe(), fields, null, "Total");

        c.gridx++;
        c.weightx = 0;
        frame.getPanel().add(comp2, c);

        return frame;
    }

}
