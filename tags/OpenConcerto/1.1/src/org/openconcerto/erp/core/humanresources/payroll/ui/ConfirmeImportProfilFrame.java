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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.erp.model.FichePayeModel;
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

public class ConfirmeImportProfilFrame extends JFrame {

    public ConfirmeImportProfilFrame(final int idProfil, final FichePayeModel model) {

        super("Import d'un profil");

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(12, 12, 12, 12);
        c.gridwidth = GridBagConstraints.REMAINDER;

        final Container container = this.getContentPane();

        container.add(new JLabel("L'import d'un profil effacera les éléments de la paye actuelle."), c);

        c.insets = new Insets(2, 2, 1, 2);
        c.gridy++;
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        final JButton boutonValid = new JButton("Valider");
        container.add(boutonValid, c);

        boutonValid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                model.loadFromProfil(idProfil);
                setVisible(false);
                dispose();
            }
        });

        c.gridx++;
        final JButton boutonAnnul = new JButton("Annuler");
        container.add(boutonAnnul, c);

        boutonAnnul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
                dispose();
            }
        });
    }
}
