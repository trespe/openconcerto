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

import org.openconcerto.erp.core.finance.accounting.element.AnalytiqueSQLElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ValiderSuppressionAxeFrame extends JFrame {

    private JButton boutonValid = new JButton("Valider");
    private JButton boutonAnnul = new JButton("Annuler");

    public ValiderSuppressionAxeFrame(final AnalytiqueSQLElement aM, final int idaxe) {

        super("Répartition associée à un compte !");

        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(12, 12, 12, 12);
        c.gridwidth = 4;
        Container container = this.getContentPane();
        container.add(new JLabel("Voulez-vous réellement supprimer l'axe sélectionné?"), c);

        c.insets = new Insets(2, 2, 1, 2);
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 1;

        container.add(this.boutonValid, c);

        this.boutonValid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                aM.deleteAxe(idaxe);
                setVisible(false);
                dispose();
            }
        });

        c.gridx++;
        container.add(this.boutonAnnul, c);

        this.boutonAnnul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
                dispose();
            }
        });
    }
}
