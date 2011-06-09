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
 
 package org.openconcerto.erp.core.supplychain.order.ui;

import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.core.supplychain.supplier.ui.ListEcheanceFournRenderer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.ListeAddPanel;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JTable;

public class ListPanelEcheancesFourn extends ListeAddPanel implements SQLTableListener {

    private EditFrame editFrame;

    public ListPanelEcheancesFourn() {

        super(Configuration.getInstance().getDirectory().getElement("ECHEANCE_FOURNISSEUR"));

        // this.buttonAjouter.setVisible(false);
        setListe();
        SQLElement elementEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_FOURNISSEUR");
        elementEch.getTable().addTableListener(this);
    }

    public JTable getJTable() {

        return this.getListe().getJTable();
    }

    protected void handleAction(JButton source, ActionEvent e) {
        if (source == this.buttonModifier) {

            if (this.editFrame == null) {
                this.editFrame = new EditFrame(this.element, EditPanel.MODIFICATION);
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
        SQLElement elementEch = Configuration.getInstance().getDirectory().getElement("ECHEANCE_FOURNISSEUR");
        Where wNotRegle = new Where(elementEch.getTable().getField("REGLE"), "=", Boolean.FALSE);
        this.setRequest(ListSQLRequest.copy(elementEch.getListRequest(), wNotRegle));

        this.getListe().setSQLEditable(false);

        final JTable jTable = this.getListe().getJTable();
        final int columnCount = jTable.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            jTable.getColumnModel().getColumn(i).setCellRenderer(new ListEcheanceFournRenderer());
        }
        this.setAddVisible(false);
        this.setModifyVisible(false);
        this.setDeleteVisible(false);
    }

    @Override
    public SQLComponent getModifComp() {
        // TODO Auto-generated method stub
        return null;
    }
}
