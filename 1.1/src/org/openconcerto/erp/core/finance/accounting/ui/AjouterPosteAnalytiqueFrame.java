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

import org.openconcerto.erp.core.finance.accounting.model.AnalytiqueModel;
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

public class AjouterPosteAnalytiqueFrame extends JFrame {

    private JTextField textNom = new JTextField();
    private JButton boutonAjout = new JButton("Ajout");
    private JButton boutonSuppr = new JButton("Annuler");

    // private JFrame f;

    public AjouterPosteAnalytiqueFrame(final AnalytiqueModel m, final RepartitionAxeAnalytiquePanel repPanel) {
        super("Ajouter un poste");
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        final Container content = this.getContentPane();
        content.add(new JLabel("Libellé"), c);

        c.weightx = 1;
        c.gridx++;
        content.add(this.textNom, c);

        this.textNom.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent arg0) {

                if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {

                    repPanel.disableAffichageColonne();
                    m.addPoste(textNom.getText());
                    repPanel.setAffichageColonne();
                    setVisible(false);
                    dispose();
                }
            }
        });

        c.gridy++;
        c.gridx = 0;

        content.add(this.boutonAjout, c);

        this.boutonAjout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                repPanel.disableAffichageColonne();
                m.addPoste(textNom.getText());
                repPanel.setAffichageColonne();
                setVisible(false);
                dispose();
            }
        });

        content.add(this.boutonAjout, c);

        this.boutonSuppr.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                setVisible(false);
                dispose();
            }
        });

        c.gridx++;
        content.add(this.boutonSuppr, c);
    }
}
