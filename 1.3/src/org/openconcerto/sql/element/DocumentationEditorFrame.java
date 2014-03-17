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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class DocumentationEditorFrame extends JFrame {

    public DocumentationEditorFrame(final BaseSQLComponent sqlComponent, final String itemName) {
        this.setTitle("Documentation");
        final JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        addLine(p, c, "Identifiant", new JLabel(itemName));

        final RowItemDesc rivDesc = sqlComponent.getRIVDesc(itemName);

        // Label Editor
        final JTextField labelTxt = new JTextField();
        labelTxt.setText(rivDesc.getLabel());
        addLine(p, c, "Nom", labelTxt);

        // Title Editor
        final JTextField titleTxt = new JTextField();
        titleTxt.setText(rivDesc.getTitleLabel());
        addLine(p, c, "Titre de colonne", titleTxt);

        // Editor
        final JTextArea docTxt = new JTextArea();
        docTxt.setFont(new JTextField().getFont());
        c.weighty = 1;
        docTxt.setText(rivDesc.getDocumentation());
        final JScrollPane comp = new JScrollPane(docTxt);
        comp.setMinimumSize(new Dimension(400, 300));
        comp.setPreferredSize(new Dimension(400, 300));
        addLine(p, c, "Documentation", comp);

        // Ok, Cancel
        final JPanel btnPanel = new JPanel();
        c.gridx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        JButton bOk = new JButton("Valider");
        btnPanel.add(bOk);

        JButton bCancel = new JButton("Annuler");
        btnPanel.add(bCancel);

        p.add(btnPanel, c);

        this.setContentPane(p);
        // Listeners
        bOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlComponent.setRIVDesc(itemName, new RowItemDesc(labelTxt.getText(), titleTxt.getText(), docTxt.getText()));
                dispose();
            }
        });
        bCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void addLine(final JPanel p, final GridBagConstraints c, final String label, final JComponent comp) {
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(comp, c);
        c.gridx = 0;
        c.gridy++;
    }
}
