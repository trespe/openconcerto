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

import org.openconcerto.sql.view.AbstractFileTransfertHandler;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class AvailableModulesPanel extends JPanel {
    private final AvailableModuleTableModel tm;

    AvailableModulesPanel(final ModuleFrame moduleFrame) {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        // Left Column
        this.tm = new AvailableModuleTableModel();
        JTable t = new JTable(this.tm);
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
        t.getColumnModel().getColumn(1).setMinWidth(100);
        t.getColumnModel().getColumn(2).setMinWidth(48);
        t.getColumnModel().getColumn(2).setPreferredWidth(48);
        t.getColumnModel().getColumn(3).setMinWidth(48);
        t.getColumnModel().getColumn(3).setPreferredWidth(200);
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
        final JButton activateButton = new JButton(new AbstractAction("Installer") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final Collection<ModuleFactory> checkedRows = AvailableModulesPanel.this.tm.getCheckedRows();
                final ModuleManager mngr = ModuleManager.getInstance();
                try {
                    // TODO install out of EDT
                    for (final ModuleFactory f : checkedRows) {
                        mngr.startModule(f.getID(), true);
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle(AvailableModulesPanel.this, "Impossible de démarrer les modules", e);
                }
                // some might have started
                moduleFrame.reload();
            }
        });
        activateButton.setOpaque(false);
        this.add(activateButton, c);

        JPanel space = new JPanel();
        space.setOpaque(false);
        c.weighty = 1;
        c.gridy++;
        this.add(space, c);
        this.setTransferHandler(new AbstractFileTransfertHandler() {

            @Override
            public void handleFile(File f) {
                if (!f.getName().endsWith(".jar")) {
                    JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'installer le module. Le fichier n'est pas un module.");
                    return;
                }
                File dir = new File("Modules");
                dir.mkdir();
                File out = null;
                if (dir.canWrite()) {
                    try {
                        out = new File(dir, f.getName());
                        FileUtils.copyFile(f, out);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'installer le module.\n" + f.getAbsolutePath() + " vers " + dir.getAbsolutePath());
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'installer le module.\nVous devez disposer des droits en écriture sur le dossier:\n" + dir.getAbsolutePath());
                    return;
                }
                try {
                    ModuleManager.getInstance().addFactory(new JarModuleFactory(out));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'intégrer le module.\n" + e.getMessage());
                    return;
                }
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        reload();
                    }
                });

            }
        });
    }

    public void reload() {
        this.tm.reload();
    }
}
