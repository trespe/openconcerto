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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.erp.panel.ITreeSelection;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

public class FamilleArticlePanel extends JPanel {
    private final SQLElement elt;
    private final JCheckBox box = new JCheckBox("Masquer les articles obsoletes");
    private final ITreeSelection treeSel;

    public FamilleArticlePanel(SQLElement elt) {
        super(new GridBagLayout());

        this.elt = elt;
        this.treeSel = new ITreeSelection(elt);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 0, 0, 0);
        treeSel.init(null);
        this.add(new JScrollPane(treeSel), c);
        // Separateur
        c.weighty = 0;
        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Ligne de boutons
        c.insets = new Insets(2, 2, 1, 2);
        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 0;
        c.weighty = 0;
        final JButton add = new JButton("Ajouter");
        this.add(add, c);
        c.gridx++;
        final JButton remove = new JButton("Supprimer");
        this.add(remove, c);
        c.gridx++;
        final JButton modifier = new JButton("Modifier");
        this.add(modifier, c);

        // Checkbox pour masquer les produits obsoletes
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(box, c);

        // Etat

        box.setSelected(true);

        // Listeners
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                treeSel.addElement(treeSel.getSelectedID());
            }
        });

        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                treeSel.removeElement(treeSel.getSelectedID());
            }
        });

        modifier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                treeSel.modifyElement(treeSel.getSelectedID());
            }
        });
    }

    public ITreeSelection getFamilleTree() {
        return this.treeSel;
    }

    public JCheckBox getCheckObsolete() {
        return this.box;
    }
}
