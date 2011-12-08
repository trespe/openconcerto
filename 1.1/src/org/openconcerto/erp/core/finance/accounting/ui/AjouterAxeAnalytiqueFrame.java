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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class AjouterAxeAnalytiqueFrame extends JFrame {

    private final JTextField textNom = new JTextField(20);
    private final JButton boutonAjout = new JButton("Ajouter");
    private final JButton boutonAnnuler = new JButton("Annuler");

    public AjouterAxeAnalytiqueFrame(final AnalytiqueSQLElement a) {
        super("Ajouter un axe");
        Container container = this.getContentPane();

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;

        // Libellé de l'axe
        container.add(new JLabel("Libellé"), c);
        c.weightx = 1;
        c.gridx++;
        container.add(this.textNom, c);
        this.textNom.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent arg0) {
                if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
                    ajouterAddAndClose(a);
                }
            }
        });

        // Ajouter
        c.gridy++;
        c.gridx = 0;
        container.add(this.boutonAjout, c);
        this.boutonAjout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                ajouterAddAndClose(a);
            }
        });
        container.add(this.boutonAjout, c);

        // Annuler
        this.boutonAnnuler.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
                dispose();
            }
        });
        c.gridx++;
        container.add(this.boutonAnnuler, c);

    }

    /**
     * @param a
     */
    private void ajouterAddAndClose(final AnalytiqueSQLElement a) {
        a.ajouterAxe(this.textNom.getText());
        setVisible(false);
        dispose();
    }
}
