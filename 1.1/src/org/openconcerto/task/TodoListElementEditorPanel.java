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
 
 package org.openconcerto.task;

import org.openconcerto.sql.users.UserManager;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TodoListElementEditorPanel extends JPanel {
    private transient TodoListElement element;
    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");

    TodoListElementEditorPanel(TodoListElement e) {
        this.element = e;
        System.out.println(e);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Ligne 1 =====================================
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        JLabel l = new JLabel("Résumé:");
        this.add(l, c);
        //
        c.gridx++;
        c.weightx = 1;
        final JTextField f = new JTextField();
        f.setText(e.getName());

        this.add(f, c);

        // Ligne 1 bis =====================================
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 0, 0);
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Ligne 2 =====================================
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JTextArea fComment = new JTextArea();
        fComment.setFont(f.getFont());
        fComment.setText(e.getComment());
        this.add(fComment, c);
        // Ligne 2 bis =====================================
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 0;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);

        // Ligne 3 =====================================
        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        c.gridwidth = 2;
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel label = new JLabel("A réaliser pour le " + simpleDateFormat.format(e.getExpectedDate()) + " par " + UserManager.getInstance().getUser(e.getUserId()).getFullName());
        this.add(label, c);
        // Ligne 4 =====================================
        JButton bOk = new JButton("Ok");
        bOk.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                element.setName(f.getText());
                element.setComment(fComment.getText());
                element.commitChanges();
                SwingUtilities.getWindowAncestor(TodoListElementEditorPanel.this).dispose();
            }
        });
        JButton bAnnuler = new JButton("Annuler");
        bAnnuler.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                SwingUtilities.getWindowAncestor(TodoListElementEditorPanel.this).dispose();
            }
        });

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        p.add(bOk);
        p.add(bAnnuler);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.add(p, c);

    }
}
