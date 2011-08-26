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
 
 package org.openconcerto.ui.tips;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.JImage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

public class TipsFrame extends JFrame {
    private static final int H = 330;
    private final List<Tip> tips = new ArrayList<Tip>();
    private int currentIndex;
    private JComponent currentPanel = new JPanel();
    private GridBagConstraints constraintPanel;

    public TipsFrame(boolean checked) {
        this.setTitle("Astuces");
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        JImage img = new JImage(TipsFrame.class.getResource("bg_tips.png"));
        // Colonne 1
        c.gridheight = 2;
        c.anchor = GridBagConstraints.NORTHEAST;
        panel.add(img, c);

        // Colonne 1
        c.gridheight = 2;
        c.gridx++;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHEAST;
        panel.add(new JSeparator(JSeparator.VERTICAL), c);

        // Colonne 2
        c.gridx++;
        c.insets = new Insets(5, 5, 5, 5);
        final JLabel comp = new JLabel("Le saviez-vous ?");
        c.gridheight = 1;
        comp.setFont(comp.getFont().deriveFont(18f));
        panel.add(comp, c);
        // Panel
        c.gridy++;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        currentPanel.setBackground(Color.WHITE);
        currentPanel.setMinimumSize(new Dimension(480, H));
        currentPanel.setPreferredSize(new Dimension(480, H));
        constraintPanel = (GridBagConstraints) c.clone();
        panel.add(currentPanel, c);

        // Derniere Ligne
        c.gridx = 0;
        c.gridy += 2;
        c.gridwidth = 3;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        panel.add(sep, c);
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;

        final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JCheckBox checkBox = new JCheckBox("Afficher les astuces au démarrage");
        checkBox.setSelected(checked);

        toolbar.add(checkBox);
        final JButton buttonPrecedent = new JButton("Précédent");
        toolbar.add(buttonPrecedent);
        buttonPrecedent.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int c = (currentIndex - 1) % tips.size();
                setCurrentTip(c);

            }
        });

        final JButton buttonSuivant = new JButton("Suivant");
        buttonSuivant.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int c = (currentIndex + 1) % tips.size();
                setCurrentTip(c);

            }
        });
        toolbar.add(buttonSuivant);
        panel.add(toolbar, c);

        this.setContentPane(panel);

        this.setResizable(false);
        this.pack();
        checkBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                checkBoxModified(checkBox.isSelected());

            }
        });

    }

    protected void checkBoxModified(boolean selected) {
        //
    }

    public void addTip(Tip t) {
        this.tips.add(t);
    }

    public void setCurrentTip(int i) {
        if (i < 0) {
            i = this.tips.size() - 1;
        }
        currentIndex = i % this.tips.size();
        // Le panel du tips doit faire 640,480;
        synchronized (getTreeLock()) {
            this.remove(currentPanel);
            this.invalidate();
            currentPanel = this.tips.get(i).getPanel();
            currentPanel.setMinimumSize(new Dimension(480, H));
            currentPanel.setPreferredSize(new Dimension(480, H));
            this.add(currentPanel, constraintPanel);
            this.validateTree();
        }
        this.repaint();
    }

    public static void main(String[] args) {
        Tip t1 = new Tip();
        t1.addText("Les fonctions de gestion courante se trouvent dans le menu 'Saisie'.");
        t1.addText("Vous y trouverez les interfaces de création :");
        t1.addText("- des devis et factures");
        t1.addText("- des achats, livraisons et mouvements stocks");
        t1.addText("- des commandes et bons de réception");

        Tip t2 = new Tip();
        t1.addText("Le logiciel intègre un module de cartographie.");

        TipsFrame f = new TipsFrame(true);
        f.addTip(t1);
        f.setCurrentTip(0);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
