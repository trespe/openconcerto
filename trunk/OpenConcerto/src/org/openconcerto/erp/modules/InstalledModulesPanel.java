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
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class InstalledModulesPanel extends JPanel {
    InstalledModulesPanel() {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        final List<ModuleFactory> l = new ArrayList<ModuleFactory>();
        final ModuleManager mngr = ModuleManager.getInstance();
        for (final String id : mngr.getModulesInstalledLocally()) {
            l.add(mngr.getFactories().get(id));
        }

        // Left Column
        JTable t = new JTable(new ModuleTableModel(l));
        t.setShowGrid(false);
        t.setShowVerticalLines(false);
        t.setFocusable(false);
        t.setRowSelectionAllowed(false);
        t.setColumnSelectionAllowed(false);
        t.setCellSelectionEnabled(false);
        t.getColumnModel().getColumn(0).setWidth(24);
        t.getColumnModel().getColumn(0).setPreferredWidth(24);
        t.getColumnModel().getColumn(0).setMaxWidth(24);
        t.getColumnModel().getColumn(0).setResizable(false);
        t.getColumnModel().getColumn(1).setMinWidth(148);
        t.getColumnModel().getColumn(2).setMinWidth(48);
        t.getColumnModel().getColumn(2).setPreferredWidth(48);
        t.getColumnModel().getColumn(3).setMinWidth(48);
        t.getColumnModel().getColumn(3).setPreferredWidth(48);
        t.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(t);
        c.weighty = 1;
        c.weightx = 1;
        c.gridheight = 4;
        c.fill = GridBagConstraints.BOTH;
        this.add(scroll, c);
        // Right column
        c.weightx = 0;
        c.weighty = 0;
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.gridheight = 1;
        JButton activateButton = new JButton("Activer");
        activateButton.setOpaque(false);
        this.add(activateButton, c);
        JButton desactivateButton = new JButton("Désactiver");
        desactivateButton.setOpaque(false);
        c.gridy++;
        this.add(desactivateButton, c);
        c.insets = new Insets(20, 3, 2, 2);
        JButton uninstallButton = new JButton("Désinstaller");
        uninstallButton.setOpaque(false);
        c.gridy++;
        this.add(uninstallButton, c);

        JPanel space = new JPanel();
        space.setOpaque(false);
        c.weighty = 1;
        c.gridy++;
        this.add(space, c);
    }
}
