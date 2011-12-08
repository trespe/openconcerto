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
 
 package org.openconcerto.utils;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Permet de disposer des champs sur 1 ou 2 colonnes. Exemple :
 * 
 * <pre>
 * 
 *  
 *   
 *    	Nom |____________|   Prenom |__________|
 *    	Rue |__________________________________|
 *    
 *   
 *  
 * </pre>
 * 
 * Les champs sont placés grace aux add*(). Il est possible de personnaliser le layout avec
 * getConstraints.
 * 
 * @author ILM Informatique 2 sept. 2004
 */
public class AutoLayouter {
    //	TODO refactor
    //	TODO extends GridBagLayout

    // les contraintes
    private GridBagConstraints constraints;
    private int x, y;
    private final JComponent co;

    public AutoLayouter(JComponent co) {
        this.x = 0;
        this.y = 0;
        this.co = co;
        this.setupConstraints();
        co.setLayout(new GridBagLayout());
    }

    /**
     * Ajout un composant sur une ligne avec la description passee en parametre Si comp est null, un
     * titre est créé
     * 
     * @param desc le label du champ
     * @param comp le composant graphique d'edition ou null si titre
     */

    public void add(String desc, JComponent comp) {
        GridBagConstraints c = this.createConstraints();

        if (comp != null) {
            JLabel lab = new JLabel(desc);
            co.add(lab, c);

            c.gridx++;
            c.weightx = 1;
            // span two columns
            c.gridwidth = 3;

            co.add(comp, c);
                        
        } else {
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 4;
            JLabel lab = new JLabel(desc);
            lab.setFont(lab.getFont().deriveFont(Font.BOLD, 11));
            co.add(lab, c);
        }
        this.newLine();
    }

    // next line
    private final void newLine() {
        this.y++;
        this.x = 0;
    }

    public void addLeft(String desc, JComponent comp) {
        if (x != 0) {
            this.newLine();
        }
        GridBagConstraints c = this.createConstraints();
        co.add(new JLabel(desc), c);
        c.weightx = 1;
        c.gridx++;
        c.gridwidth = 1;

        co.add(comp, c);

        x = 2;
    }

    public void addRight(String desc, JComponent comp) {
        GridBagConstraints c = this.createConstraints();
        co.add(new JLabel(desc), c);
        c.weightx = 1;
        c.gridx++;
        c.gridwidth = 1;

        co.add(comp, c);

        this.newLine();
    }

    public void add(JButton btn) {
        this.addRight("", btn);
    }

    private GridBagConstraints createConstraints() {
        GridBagConstraints c = (GridBagConstraints) this.constraints.clone();
        c.gridx = this.x;
        c.gridy = this.y;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        return c;
    }

    private void setupConstraints() {
        this.constraints = new GridBagConstraints();
        constraints.ipadx = 8;
        constraints.insets = new Insets(2, 5, 0, 0);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 1;
        constraints.weightx = 0;
    }

    /**
     * Makes the constraints available for customizing.
     * 
     * @return the constraints that will be used for laying out components.
     */
    public GridBagConstraints getConstraints() {
        return constraints;
    }

    public final JComponent getComponent() {
        return this.co;
    }
}
