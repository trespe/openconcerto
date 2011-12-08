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

import org.openconcerto.erp.core.finance.accounting.report.Map2033A;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.FileUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class BilanPanel extends JPanel {

    public BilanPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(new JLabel("Vous allez générer le fichier result_2033A.pdf contenant le bilan simplifié."), c);
        c.gridy++;
        try {
            this.add(new JLabel("Il se trouvera dans le dossier " + new File(TemplateNXProps.getInstance().getStringProperty("Location2033APDF")).getCanonicalPath()), c);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        /*
         * PdfGenerator_2033A p = new PdfGenerator_2033A(); p.generateFrom(new
         * Map2033A().getMap2033A());
         */

        JButton buttonFermer = new JButton("Fermer");
        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(BilanPanel.this)).dispose();
            }
        });

        JButton buttonOuvrirDossier = new JButton("Ouvrir dossier");
        buttonOuvrirDossier.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String file = TemplateNXProps.getInstance().getStringProperty("Location2033APDF");
                File f = new File(file);
                FileUtils.browseFile(f);
            };
        });

        // FIXME impossible de générer si le fichier est ouvert
        JButton buttonGenerer = new JButton("Générer");
        buttonGenerer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Map2033A map = new Map2033A(new JProgressBar());
                map.generateMap2033A();
            };
        });

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(buttonOuvrirDossier, c);

        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        buttonGenerer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(buttonGenerer, c);
        c.weightx = 0;
        this.add(buttonFermer, c);
    }

    public void getBilan() {

        long soldeClient, soldeFourn, soldeBanq;
        soldeClient = 0;
        soldeBanq = 0;
        soldeFourn = 0;

        /*******************************************************************************************
         * CALCUL DU BILAN CREANCES(CLIENTS BANQUE) - DETTES (FOURNISSEURS BENEF)
         ******************************************************************************************/
        // Récupération des ecritures du journal avec le total
        SQLBase base = Configuration.getInstance().getBase();
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLTable ecritureTable = base.getTable("ECRITURE");

        SQLSelect selClient = new SQLSelect(base);
        SQLSelect selFourn = new SQLSelect(base);
        SQLSelect selBanq = new SQLSelect(base);

        // sel.addSelect(ecritureTable.getField("DATE"), "YEAR");
        // sel.addSelect(ecritureTable.getField("DATE"), "MONTH");
        selClient.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        selClient.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        selFourn.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        selFourn.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        selBanq.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        selBanq.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        Where w = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getField("ID"));
        Where wClient = new Where(compteTable.getField("NUMERO"), "LIKE", "41%");
        Where wFourn = new Where(compteTable.getField("NUMERO"), "LIKE", "40%");
        Where wBanq = new Where(compteTable.getField("NUMERO"), "LIKE", "5%");

        selBanq.setWhere(w.and(wBanq));
        selFourn.setWhere(w.and(wFourn));
        selClient.setWhere(w.and(wClient));

        // sel.setDistinct(true);
        // sel.addRawOrder("NUMERO");
        String reqClient = selClient.asString();
        String reqFourn = selFourn.asString();
        String reqBanq = selBanq.asString();
        System.out.println(reqClient);
        System.out.println(reqFourn);
        System.out.println(reqBanq);

        Object obClient = base.getDataSource().execute(reqClient, new ArrayListHandler());
        Object obFourn = base.getDataSource().execute(reqFourn, new ArrayListHandler());
        Object obBanq = base.getDataSource().execute(reqBanq, new ArrayListHandler());

        List myListClient = (List) obClient;

        if (myListClient.size() != 0) {

            for (int i = 0; i < myListClient.size(); i++) {

                Object[] objTmp = (Object[]) myListClient.get(i);
                soldeClient = ((Long) objTmp[0]).longValue() - ((Long) objTmp[1]).longValue();
            }
        }

        List myListBanq = (List) obBanq;
        if (myListBanq.size() != 0) {

            for (int i = 0; i < myListBanq.size(); i++) {

                Object[] objTmp = (Object[]) myListBanq.get(i);
                soldeBanq = ((Long) objTmp[0]).longValue() - ((Long) objTmp[1]).longValue();
            }
        }

        List myListFourn = (List) obFourn;

        if (myListFourn.size() != 0) {

            for (int i = 0; i < myListFourn.size(); i++) {

                Object[] objTmp = (Object[]) myListFourn.get(i);
                soldeFourn = -((Long) objTmp[0]).longValue() + ((Long) objTmp[1]).longValue();
            }
        }
        this.add(new JLabel("Clients : " + soldeClient + " Banque : " + soldeBanq + " solde Fourn : " + soldeFourn + " Résultat " + ResultatPanel.getResultatValue() + " Bilan = "
                + (soldeClient + soldeBanq) + " <<>> " + (soldeFourn - ResultatPanel.getResultatValue())));
    }
}
