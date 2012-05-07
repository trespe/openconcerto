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
 
 package org.openconcerto.map.ui;

import org.openconcerto.map.model.Ville;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class VilleEditorPanel extends JPanel implements ActionListener {

    final JTextField textVille = new JTextField(20);
    final JTextField textCodePostal = new JTextField(8);
    final JButton buttonAdd = new JButton("Ajouter");
    final JButton buttonCancel = new JButton("Annuler");

    public VilleEditorPanel(String s) {
        if (s == null)
            s = "";
        s = s.trim();

        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Ville
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        this.add(new JLabel("Ville", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        this.add(textVille, c);
        // Code postal
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel("Code postal", SwingConstants.RIGHT), c);
        c.gridx++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        this.add(textCodePostal, c);
        // Buttons
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        this.add(buttonAdd, c);
        c.gridwidth = 0;
        c.gridx++;
        c.weightx = 0;
        this.add(buttonCancel, c);

        if (!s.isEmpty()) {
            if (Character.isDigit(s.charAt(0))) {
                int index = s.indexOf(' ');
                if (index > 0) {
                    this.textCodePostal.setText(s.substring(0, index));
                    this.textVille.setText(s.substring(index).trim());
                } else {
                    this.textCodePostal.setText(s);
                }
            } else {
                this.textVille.setText(s);
            }
        }

        buttonAdd.addActionListener(this);
        buttonCancel.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this.buttonAdd)) {
            String name = this.textVille.getText().trim();
            String code = this.textCodePostal.getText().trim();
            if (!name.isEmpty()) {
                Ville.addVille(new Ville(name, 0, 0, 0, code));
            }
        }
        final Container c = SwingUtilities.getAncestorOfClass(Window.class, this);
        if (c != null) {
            ((Window) c).setVisible(false);
            ((Window) c).dispose();
        }
    }
}
