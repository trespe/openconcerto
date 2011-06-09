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

import org.openconcerto.erp.core.finance.accounting.report.Map2033E;
import org.openconcerto.erp.core.finance.accounting.report.PdfGenerator_2033E;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.FileUtils;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Formulaire2033EPanel extends JPanel {

    private final static String dossierPath = "/Configuration/Template/PDF/";

    public Formulaire2033EPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(new JLabel("Vous allez générer le fichier result_2033E.pdf."), c);
        c.gridy++;
        this.add(new JLabel("Il se trouvera dans le dossier " + Formulaire2033EPanel.dossierPath), c);

        /*
         * PdfGenerator_2033A p = new PdfGenerator_2033A(); p.generateFrom(new
         * Map2033A().getMap2033A());
         */

        JButton buttonFermer = new JButton("Fermer");
        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(Formulaire2033EPanel.this)).dispose();
            }
        });

        JButton buttonOuvrirDossier = new JButton("Ouvrir dossier");
        buttonOuvrirDossier.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File f = new File(dossierPath);
                FileUtils.browseFile(f);
            };
        });

        // FIXME impossible de générer si le fichier est ouvert prevenir l'utilisateur
        JButton buttonGenerer = new JButton("Générer");
        buttonGenerer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PdfGenerator_2033E p = new PdfGenerator_2033E();
                p.generateFrom(new Map2033E().getMap2033E());
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
}
