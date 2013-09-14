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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.element.EcritureSQLElement;
import org.openconcerto.erp.core.finance.accounting.element.MouvementSQLElement;
import org.openconcerto.erp.rights.ComptaUserRight;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.IListener;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JTable;

public class ListPanelEcritures extends ListeAddPanel {

    public ListPanelEcritures() {
        this(Configuration.getInstance().getDirectory().getElement("ECRITURE"), (Where) null);
    }

    public ListPanelEcritures(final SQLElement element, final Where w) {
        this(element, new IListe(element.createTableSource(w)));
    }

    public ListPanelEcritures(final SQLElement element, final IListe l) {
        super(element, l);

        this.buttonAjouter.setVisible(false);
        this.getListe().setSQLEditable(false);

        // TODO verifier que ca fonctionne, si selection d'une ecriture valide alors bouton disable
        this.getListe().addIListener(new IListener() {
            public void selectionId(int id, int field) {

                System.out.println("Selection Changed");
                SQLRow row = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("ECRITURE").getRow(id);

                if (row != null) {
                    setButtonEnabled(!row.getBoolean("VALIDE"));
                }
            }
        });

        if (!UserManager.getInstance().getCurrentUser().getRights().haveRight(ComptaUserRight.ACCES_NOT_RESCTRICTED_TO_411)) {
            // TODO Show Restricted acces in UI
            getListe().getRequest().setWhere(new Where(getElement().getTable().getField("COMPTE_NUMERO"), "LIKE", "411%"));
        }

        this.getListe().getSelectedId();
    }

    public JTable getJTable() {
        return this.getListe().getJTable();
    }

    /***********************************************************************************************
     * TODO : Action Ajout, suppression modification en fonction du mouvement pour respecter
     * l'équilibre, Ajout --> saisie au Km
     */
    protected void handleAction(JButton source, ActionEvent e) {
        if (source == this.buttonModifier) {
            final SQLRow ecritureRow = new EcritureSQLElement().getTable().getRow(this.getListe().getSelectedId());
            MouvementSQLElement.showSource(ecritureRow.getInt("ID_MOUVEMENT"));
        } else if (source == this.buttonEffacer) {
            SQLRow row = this.getListe().getSelectedRow();

            PanelFrame frame = new PanelFrame(new SuppressionEcrituresPanel(row.getInt("ID_MOUVEMENT")), "Suppression");
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        } else {
            super.handleAction(source, e);
        }
    }

    public void setModificationVisible(boolean b) {
        this.buttonModifier.setVisible(b);
    }

    public void setAjoutVisible(boolean b) {
        this.buttonAjouter.setVisible(b);
    }

    public void setSuppressionVisible(boolean b) {
        this.buttonEffacer.setVisible(b);
    }

    private void setButtonEnabled(boolean b) {
        this.buttonAjouter.setEnabled(b);
        this.buttonModifier.setEnabled(b);
        this.buttonEffacer.setEnabled(b);
    }

}
