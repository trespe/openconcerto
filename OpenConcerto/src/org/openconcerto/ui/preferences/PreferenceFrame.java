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
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.ui.TM;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;

public class PreferenceFrame extends JFrame {

    MainPrefPanel mainPrefPanel;

    public PreferenceFrame(DefaultMutableTreeNode root) {
        super();
        this.setTitle(TM.tr("prefs.title"));
        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        this.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 1;

        PrefTree prefTree = new PrefTree(root);
        prefTree.setMinimumSize(new Dimension(250, 200));
        prefTree.setPreferredSize(new Dimension(250, 200));
        this.getContentPane().add(prefTree, c);

        c.gridx++;
        c.weightx = 0;
        this.getContentPane().add(new JSeparator(JSeparator.VERTICAL), c);

        c.gridx++;
        c.weightx = 1;
        this.mainPrefPanel = new MainPrefPanel(prefTree);
        this.getContentPane().add(this.mainPrefPanel, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 3;
        this.getContentPane().add(new JSeparator(JSeparator.HORIZONTAL), c);

        JPanel p1 = new JPanel();
        JButton buttonClose = new JButton(TM.tr("toClose"));
        p1.setOpaque(true);
        p1.add(buttonClose);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        this.getContentPane().add(p1, c);
        this.setBackground(p1.getBackground());
        this.getContentPane().setBackground(p1.getBackground());
        this.setMinimumSize(new Dimension(800, 660));
        this.setPreferredSize(new Dimension(800, 660));
        prefTree.addTreeSelectionListener(this.mainPrefPanel);
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doOnClose();
                PreferenceFrame.this.dispose();
            }
        });

        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                doOnClose();
            };
        });
    }

    private void doOnClose() {
        PreferencePanel currentPanel = this.mainPrefPanel.getCurrentPanel();
        if (currentPanel.isModified()) {
            if (JOptionPane.showConfirmDialog(null, TM.tr("prefs.applyModif"), TM.tr("prefs.applyModif.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentPanel.apply();
            }
        }
    }

}
