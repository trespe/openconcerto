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
import org.openconcerto.erp.core.finance.accounting.model.PlanComptableGModel;
import org.openconcerto.erp.element.objet.Compte;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.apache.commons.dbutils.handlers.ArrayListHandler;




public class AjouterComptePCGtoPCEFrame extends JFrame {

    private JButton boutonAjout = new JButton("Ajout");
    private JButton boutonClose = new JButton("Fermer");
    // private JFrame frame;
    private PlanComptableGPanel planPanel;

    public AjouterComptePCGtoPCEFrame() {

        super("Ajouter un compte du plan comptable général");
        Container f = this.getContentPane();

        // instanciation du panel et du menu click droit associé
        Vector<AbstractAction> actionClickDroitTable = new Vector<AbstractAction>();

        actionClickDroitTable.add(new AbstractAction("Ajouter au PCE") {

            public void actionPerformed(ActionEvent e) {
                ajoutCompteSelected();
            }
        });

        this.planPanel = new PlanComptableGPanel(actionClickDroitTable);

        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(12, 2, 12, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = 2;
        c.gridheight = 1;
        JLabel label = new JLabel("Choississez le ou les comptes à ajouter au Plan Comptable Entreprise");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        f.add(label, c);

        /*******************************************************************************************
         * * Affichage du plan comptable entreprise
         ******************************************************************************************/
        c.insets = new Insets(0, 0, 0, 0);
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        f.add(this.planPanel, c);

        /*******************************************************************************************
         * * Bouton ajout / fermer
         ******************************************************************************************/
        c.insets = new Insets(2, 2, 1, 2);
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridy++;
        c.gridx = 0;
        c.anchor = GridBagConstraints.SOUTHEAST;

        f.add(this.boutonAjout, c);

        this.boutonAjout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                ajoutCompteSelected();

            }
        });

        c.gridx++;
        f.add(this.boutonClose, c);
        this.boutonClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                AjouterComptePCGtoPCEFrame.this.setVisible(false);
                AjouterComptePCGtoPCEFrame.this.dispose();
            }
        });
        /*
         * this.pack(); this.setVisible(true);
         */
    }

    private void ajoutCompteSelected() {

        SQLTable compteTable = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("COMPTE_PCE");

        JTable tabTmp = (JTable) (this.planPanel.getTables().get(this.planPanel.getSelectedIndex()));
        PlanComptableGModel modelTmp = (PlanComptableGModel) tabTmp.getModel();

        int[] selectRows = tabTmp.getSelectedRows();

        if (selectRows.length == 0) {
            return;
        }

        // On verifie que les compte n'existent pas deja
        SQLSelect selCompte = new SQLSelect(compteTable.getBase());
        selCompte.addSelect(compteTable.getField("NUMERO"));

        Where w = new Where(compteTable.getField("NUMERO"), "=", tabTmp.getValueAt(selectRows[0], 0));

        for (int i = 1; i < selectRows.length; i++) {
            w.or(new Where(compteTable.getField("NUMERO"), "=", tabTmp.getValueAt(selectRows[i], 0)));
        }

        String reqCompte = selCompte.asString();
        Object obRep = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getDataSource().execute(reqCompte, new ArrayListHandler());

        List tmpCpt = (List) obRep;

        // Map qui contient les comptes existants
        Map<String, Integer> mCompte;
        if (tmpCpt.size() == 0) {
            mCompte = null;
        } else {
            mCompte = new HashMap<String, Integer>();
            for (int i = 0; i < tmpCpt.size(); i++) {
                Object[] tmp = (Object[]) tmpCpt.get(i);
                mCompte.put(tmp[0].toString().trim(), new Integer(0));
            }
        }

        // on ajoute les comptes si ils n'existent pas
        for (int i = 0; i < selectRows.length; i++) {
            if ((mCompte != null) && (mCompte.get(tabTmp.getValueAt(selectRows[i], 0).toString().trim()) == null)) {

                System.out.println("Ajout du compte" + tabTmp.getValueAt(selectRows[i], 0) + "  " + tabTmp.getValueAt(selectRows[i], 1));
                SQLRowValues val = new SQLRowValues(compteTable);
                val.put("NUMERO", tabTmp.getValueAt(selectRows[i], 0));
                val.put("NOM", tabTmp.getValueAt(selectRows[i], 1));
                val.put("INFOS", ((Compte) modelTmp.getComptes().get(selectRows[i])).getInfos());

                try {
                    val.insert();
                } catch (SQLException sqlE) {
                    System.err.println("Error insert row in table COMPTE_PCE");
                    sqlE.printStackTrace();
                }
            }
        }
    }
}
