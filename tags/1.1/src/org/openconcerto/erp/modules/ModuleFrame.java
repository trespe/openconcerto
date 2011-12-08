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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ModuleFrame extends JFrame {
    private final InstalledModulesPanel tab1;
    private final AvailableModulesPanel tab2;

    public ModuleFrame() {
        this.setTitle("Modules");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final JTabbedPane tabbedPane = new JTabbedPane();
        this.tab1 = new InstalledModulesPanel(this);
        tabbedPane.addTab("Modules installés", this.tab1);
        this.tab2 = new AvailableModulesPanel(this);
        tabbedPane.addTab("Modules disponibles", this.tab2);
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

    public void reload() {
        this.tab1.reload();
        this.tab2.reload();
    }
}
