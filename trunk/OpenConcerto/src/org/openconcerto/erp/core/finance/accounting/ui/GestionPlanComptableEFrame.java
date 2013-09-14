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
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.model.PlanComptableEModel;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;

public class GestionPlanComptableEFrame extends JFrame {

    private JButton boutonAjout = new JButton("Ajout");
    private JButton boutonSuppr = new JButton("Supprimer");
    private JButton boutonAjoutPCG = new JButton("Ajout depuis le PCG");
    private EditFrame edit = null;
    private AjouterComptePCGtoPCEFrame ajoutCptFrame = null;

    private SQLTable compteTable = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("COMPTE_PCE");
    private PlanComptableEPanel panelPCE;

    public GestionPlanComptableEFrame() {
        super("Gérer le plan comptable entreprise");
        Container container = this.getContentPane();

        // instanciation du panel et du menu click droit associé
        Vector<AbstractAction> actionClickDroitTable = new Vector<AbstractAction>();

        actionClickDroitTable.add(new AbstractAction("Ajouter un compte") {

            public void actionPerformed(ActionEvent e) {
                if (GestionPlanComptableEFrame.this.edit == null) {
                    GestionPlanComptableEFrame.this.edit = new EditFrame(Configuration.getInstance().getDirectory().getElement("COMPTE_PCE"));
                    GestionPlanComptableEFrame.this.edit.pack();
                }
                GestionPlanComptableEFrame.this.edit.setVisible(true);
            }
        });

        this.panelPCE = new PlanComptableEPanel(actionClickDroitTable);

        container.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        container.add(this.boutonAjout, c);

        this.boutonAjout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (GestionPlanComptableEFrame.this.edit == null) {
                    GestionPlanComptableEFrame.this.edit = new EditFrame(Configuration.getInstance().getDirectory().getElement("COMPTE_PCE"));
                    GestionPlanComptableEFrame.this.edit.pack();
                }
                GestionPlanComptableEFrame.this.edit.setVisible(true);
            }
        });

        c.gridx++;
        container.add(this.boutonSuppr, c);
        this.boutonSuppr.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                supprimerCompte();
            }
        });

        c.gridx++;
        container.add(this.boutonAjoutPCG);
        this.boutonAjoutPCG.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (GestionPlanComptableEFrame.this.ajoutCptFrame == null) {
                    GestionPlanComptableEFrame.this.ajoutCptFrame = new AjouterComptePCGtoPCEFrame();
                    GestionPlanComptableEFrame.this.ajoutCptFrame.pack();
                }
                GestionPlanComptableEFrame.this.ajoutCptFrame.setVisible(true);
            }
        });

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;

        container.add(this.panelPCE, c);

        this.compteTable.addTableListener(new SQLTableListener() {

            public void rowModified(SQLTable table, int id) {

                SQLRow row = table.getRow(id);
                GestionPlanComptableEFrame.this.panelPCE.fireModificationCompte(new Compte(id, row.getString("NUMERO"), row.getString("NOM")));
            }

            public void rowAdded(SQLTable table, int id) {

                SQLRow row = table.getRow(id);
                GestionPlanComptableEFrame.this.panelPCE.fireModificationCompte(new Compte(id, row.getString("NUMERO"), row.getString("NOM")));
            }

            public void rowDeleted(SQLTable table, int id) {

                SQLRow row = table.getRow(id);
                GestionPlanComptableEFrame.this.panelPCE.fireModificationCompte(new Compte(id, row.getString("NUMERO"), row.getString("NOM")));
            }
        });
    }

    private void supprimerCompte() {

        JTable tableTmp = (JTable) (this.panelPCE.getTables().get(this.panelPCE.getSelectedIndex()));

        PlanComptableEModel model = (PlanComptableEModel) tableTmp.getModel();

        int[] selectedRows = tableTmp.getSelectedRows();

        if (selectedRows.length == 0) {
            return;
        }

        SQLElement eltComptePCE = Configuration.getInstance().getDirectory().getElement("COMPTE_PCE");

        for (int i = 0; i < selectedRows.length; i++) {

            // Numéro du compte à supprimer
            // String numero = tableTmp.getValueAt(selectedRows[i], 0).toString();
            // int id = ComptePCESQLElement.getId(numero);
            int id = model.getId(selectedRows[i]);
            try {
                eltComptePCE.archive(id);
            } catch (SQLException e) {
                e.printStackTrace();
                ExceptionHandler.handle("Erreur lors de la suppression du compte.");
            }
            System.out.println("Compte Supprimé");
        }
    }

    public PlanComptableEPanel getPanelPCE() {
        return this.panelPCE;
    }
}
