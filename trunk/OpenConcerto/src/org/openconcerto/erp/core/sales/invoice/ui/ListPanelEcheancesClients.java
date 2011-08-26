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
 
 package org.openconcerto.erp.core.sales.invoice.ui;

import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSource;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class ListPanelEcheancesClients extends ListeAddPanel {

    // TODO Ajouter une légende dans liste echeance panel pour les couleurs du renderer

    private EditFrame editFrame;
    private boolean showRegCompta = false;

    public ListPanelEcheancesClients() {
        this(Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT"));
    }

    private ListPanelEcheancesClients(final SQLElement elem) {
        super(elem, new IListe(elem.getTableSource(true)));
        setListe();
    }

    public JTable getJTable() {

        return this.getListe().getJTable();
    }

    public void setShowRegCompta(boolean b) {
        this.showRegCompta = b;
        setListe();
    }

    protected void handleAction(JButton source, ActionEvent e) {
        if (source == this.buttonModifier) {

            if (this.editFrame == null) {
                this.editFrame = new EditFrame(this.element, EditFrame.MODIFICATION);
            }
            this.editFrame.selectionId(this.getListe().getSelectedId());
            this.editFrame.pack();
            this.editFrame.setVisible(true);

            SQLRow ecritureRow = new EcritureSQLElement().getTable().getRow(this.getListe().getSelectedId());

            MouvementSQLElement.showSource(ecritureRow.getInt("ID_MOUVEMENT"));

        } else {

            super.handleAction(source, e);
        }
    }

    private void setListe() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                final SQLTable elementEchT = getListe().getSource().getPrimaryTable();
                Where wNotRegle = new Where(elementEchT.getField("REGLE"), "=", Boolean.FALSE);
                if (!showRegCompta) {
                    wNotRegle = wNotRegle.and(new Where(elementEchT.getField("REG_COMPTA"), "=", Boolean.FALSE));
                }
                getListe().getRequest().setWhere(wNotRegle);

                // this.buttonAjouter.setVisible(false);
                final ListEcheanceClientRenderer rend = new ListEcheanceClientRenderer();
                for (int i = 0; i < ListPanelEcheancesClients.this.getListe().getJTable().getColumnCount(); i++) {
                    if (ListPanelEcheancesClients.this.getListe().getJTable().getColumnClass(i) != Boolean.class) {

                        ListPanelEcheancesClients.this.getListe().getJTable().getColumnModel().getColumn(i).setCellRenderer(rend);
                    }
                }
                // this.getListe().setSQLEditable(false);
                ListPanelEcheancesClients.this.buttonAjouter.setVisible(false);
                ListPanelEcheancesClients.this.buttonEffacer.setVisible(false);
                ListPanelEcheancesClients.this.buttonModifier.setVisible(false);

                final SQLTableModelSource src = ListPanelEcheancesClients.this.getListe().getSource();

                ListPanelEcheancesClients.this.getListe().setSQLEditable(true);

                for (SQLTableModelColumn column : src.getColumns()) {
                    if (column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                        ((SQLTableModelColumnPath) column).setEditable(false);
                    }
                }

                ((SQLTableModelColumnPath) src.getColumns(getElement().getTable().getField("INFOS")).iterator().next()).setEditable(true);

            }
        });
    }

    @Override
    public SQLComponent getModifComp() {
        // TODO Auto-generated method stub
        return null;
    }
}
