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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ModuleFrame extends JFrame {
    ModuleFrame() {

        this.setTitle("Modules");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Modules installés", new InstalledModulesPanel());
        tabbedPane.addTab("Modules disponibles", new AvailableModulesPanel());
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        p.add(tabbedPane, c);
        final JButton closeButton = new JButton("Fermer");
        c.gridy++;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        p.add(closeButton, c);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ModuleFrame.this.dispose();
            }
        });
        this.setContentPane(p);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ModuleFrame frame = new ModuleFrame();
                frame.setMinimumSize(new Dimension(480, 640));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            }
        });

    }
}
