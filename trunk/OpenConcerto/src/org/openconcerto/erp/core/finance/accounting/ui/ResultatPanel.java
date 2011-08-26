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
import org.openconcerto.erp.core.finance.accounting.report.Map2033B;
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

public class ResultatPanel extends JPanel {

    public ResultatPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(new JLabel("Vous allez générer le fichier result_2033B.pdf contenant le résultat."), c);
        c.gridy++;
        try {
            this.add(new JLabel("Il se trouvera dans le dossier " + new File(TemplateNXProps.getInstance().getStringProperty("Location2033BPDF")).getCanonicalPath()), c);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        final JButton buttonFermer = new JButton("Fermer");
        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                ((JFrame) SwingUtilities.getRoot(ResultatPanel.this)).dispose();
            }
        });

        final JButton buttonOuvrir = new JButton("Ouvrir dossier");
        buttonOuvrir.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                final String file = TemplateNXProps.getInstance().getStringProperty("Location2033BPDF");
                File f = new File(file);
                FileUtils.browseFile(f);
            }
        });

        // FIXME impossible de générer si le fichier est ouvert
        final JButton buttonGenerer = new JButton("Générer");
        buttonGenerer.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                final Map2033B map = new Map2033B(new JProgressBar());
                map.generateMap();
            }
        });

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(buttonOuvrir, c);

        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        buttonGenerer.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(buttonGenerer, c);
        c.weightx = 0;
        this.add(buttonFermer, c);
    }

    public static long getResultatValue() {

        long soldeVente = 0;
        long soldeAchat = 0;

        /*******************************************************************************************
         * CALCUL DU RESULTAT PRODUITS - CHARGES
         ******************************************************************************************/

        // Récupération des ecritures du journal avec le total
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable compteTable = base.getTable("COMPTE_PCE");
        final SQLTable ecritureTable = base.getTable("ECRITURE");

        final SQLSelect selAchat = new SQLSelect(base);
        final SQLSelect selVente = new SQLSelect(base);

        selAchat.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        selAchat.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        selVente.addSelect(ecritureTable.getField("DEBIT"), "SUM");
        selVente.addSelect(ecritureTable.getField("CREDIT"), "SUM");

        final Where where = new Where(ecritureTable.getField("ID_COMPTE_PCE"), "=", compteTable.getField("ID"));
        final Where wVente = new Where(compteTable.getField("NUMERO"), "LIKE", "7%");
        final Where wAchat = new Where(compteTable.getField("NUMERO"), "LIKE", "6%");

        selAchat.setWhere(where.and(wAchat));
        selVente.setWhere(where.and(wVente));

        final String reqAchat = selAchat.asString();
        final String reqVente = selVente.asString();

        final Object obAchat = base.getDataSource().execute(reqAchat, new ArrayListHandler());
        final Object obVente = base.getDataSource().execute(reqVente, new ArrayListHandler());

        // FIXME supprimer la boucle une seule ligne retournée
        final List<Object[]> myListAchat = (List<Object[]>) obAchat;
        final int achatsCount = myListAchat.size();
        for (int i = 0; i < achatsCount; i++) {
            final Object[] objTmp = myListAchat.get(i);
            soldeAchat += ((Long) objTmp[0]).longValue() - ((Long) objTmp[1]).longValue();
        }

        final List<Object[]> myListVente = (List<Object[]>) obVente;
        final int ventesCount = myListVente.size();
        for (int i = 0; i < ventesCount; i++) {
            final Object[] objTmp = myListVente.get(i);
            soldeVente += ((Long) objTmp[1]).longValue() - ((Long) objTmp[0]).longValue();
        }

        return (soldeVente - soldeAchat);
    }
}
