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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.view.EditPanel;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class VisualisationPayeFrame extends JFrame {

    private boolean ok = false;
    private EditPanel panel;
    private static final String frameTitle = "Visualisation d'une fiche de paye";

    public VisualisationPayeFrame(final Semaphore semaphore) {

        super("Visualisation d'une fiche de paye");

        final SQLElement eltFichePaye = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                VisualisationPayeFrame.this.panel = new EditPanel(eltFichePaye, EditPanel.READONLY);
                VisualisationPayeFrame.this.panel.disableCancel();

                VisualisationPayeFrame.this.setLayout(new GridBagLayout());
                Container content = VisualisationPayeFrame.this.getContentPane();

                GridBagConstraints c = new GridBagConstraints();
                c.gridheight = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1;
                c.weighty = 1;
                c.insets = new Insets(2, 2, 1, 2);
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.BOTH;

                content.add(VisualisationPayeFrame.this.panel, c);

                JButton buttonAnnuler = new JButton("Annuler");
                buttonAnnuler.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ok = false;
                        semaphore.release();
                        VisualisationPayeFrame.this.setVisible(false);
                        // synchronized (t) {
                        // System.err.println("Notify");
                        // t.notifyAll();
                        // }

                    }
                });

                JButton buttonContinue = new JButton("Continuer");
                buttonContinue.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ok = true;
                        panel.modifier();
                        semaphore.release();
                        VisualisationPayeFrame.this.setVisible(false);

                        // synchronized (t) {
                        // System.err.println("Notify");
                        // t.notifyAll();
                        // }

                    }
                });

                c.gridy++;
                c.gridx = GridBagConstraints.RELATIVE;
                c.gridwidth = 1;
                c.weightx = 1;
                c.weighty = 0;
                c.anchor = GridBagConstraints.EAST;
                c.fill = GridBagConstraints.NONE;
                content.add(buttonContinue, c);
                c.weightx = 0;
                content.add(buttonAnnuler, c);

            }
        });

    }

    public boolean getAnswer() {
        return this.ok;
    }

    /**
     * Selection de la fiche de paye à afficher
     * 
     * @param id
     */
    public void setSelectedFichePaye(int id) {
        this.panel.selectionId(id, 1);

        SQLElement eltFichePaye = Configuration.getInstance().getDirectory().getElement("FICHE_PAYE");
        SQLElement eltSalarie = Configuration.getInstance().getDirectory().getElement("SALARIE");
        SQLRow rowFiche = eltFichePaye.getTable().getRow(id);
        SQLRow rowSal = eltSalarie.getTable().getRow(rowFiche.getInt("ID_SALARIE"));
        this.setTitle(frameTitle + " [" + rowSal.getString("CODE") + " " + rowSal.getString("NOM") + " " + rowSal.getString("PRENOM"));
    }
}
