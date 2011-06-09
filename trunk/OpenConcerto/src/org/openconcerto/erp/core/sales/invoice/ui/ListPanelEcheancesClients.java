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
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class ListPanelEcheancesClients extends ListeAddPanel implements SQLTableListener {

    // TODO Ajouter une légende dans liste echeance panel pour les couleurs du renderer

    private EditFrame editFrame;
    private boolean showRegCompta = false;

    public ListPanelEcheancesClients() {

        super(Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT"));

        setListe();
        SQLElement elementEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT");
        elementEch.getTable().addTableListener(this);

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
            this.editFrame.selectionId(this.getListe().getSelectedId(), -1);
            this.editFrame.pack();
            this.editFrame.setVisible(true);

            SQLRow ecritureRow = new EcritureSQLElement().getTable().getRow(this.getListe().getSelectedId());

            MouvementSQLElement.showSource(ecritureRow.getInt("ID_MOUVEMENT"));

        } else {

            super.handleAction(source, e);
        }
    }

    public void rowAdded(SQLTable table, int id) {
        // TODO Auto-generated method stub

    }

    public void rowDeleted(SQLTable table, int id) {
        // TODO Auto-generated method stub

    }

    public void rowModified(SQLTable table, int id) {
        setListe();
    }

    private void setListe() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                final SQLElement elementEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_CLIENT");
                Where wNotRegle = new Where(elementEch.getTable().getField("REGLE"), "=", Boolean.FALSE);
                if (!showRegCompta) {
                    wNotRegle = wNotRegle.and(new Where(elementEch.getTable().getField("REG_COMPTA"), "=", Boolean.FALSE));
                }
                ListPanelEcheancesClients.this.setRequest(ListSQLRequest.copy(elementEch.getListRequest(), wNotRegle));

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

                final SQLTableModelSourceOnline src = (SQLTableModelSourceOnline) ListPanelEcheancesClients.this.getListe().getModel().getReq();

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
