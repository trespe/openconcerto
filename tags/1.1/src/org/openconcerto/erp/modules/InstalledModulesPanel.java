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
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.IFactory;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class InstalledModulesPanel extends JPanel {

    private final ModuleTableModel tm;

    InstalledModulesPanel(final ModuleFrame moduleFrame) {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        final IFactory<List<ModuleFactory>> rowSource = new IFactory<List<ModuleFactory>>() {
            @Override
            public List<ModuleFactory> createChecked() {
                final List<ModuleFactory> l = new ArrayList<ModuleFactory>();
                final ModuleManager mngr = ModuleManager.getInstance();
                for (final String id : mngr.getModulesInstalledLocally()) {
                    final ModuleFactory factory = mngr.getFactories().get(id);
                    if (factory == null) {
                        System.err.println("Erreur: pas de factory pour le module " + id);
                    } else {
                        l.add(factory);
                    }
                }
                return l;
            }
        };

        this.tm = new ModuleTableModel(rowSource);
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
        JButton activateButton = new JButton(new StartStopAction(true));
        activateButton.setOpaque(false);
        this.add(activateButton, c);
        JButton desactivateButton = new JButton(new StartStopAction(false));
        desactivateButton.setOpaque(false);
        c.gridy++;
        this.add(desactivateButton, c);
        c.insets = new Insets(20, 3, 2, 2);
        JButton uninstallButton = new JButton(new AbstractAction("Désinstaller") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final ModuleManager mngr = ModuleManager.getInstance();
                final Collection<ModuleFactory> checkedRows = InstalledModulesPanel.this.tm.getCheckedRows();
                final int answer = JOptionPane.showConfirmDialog(InstalledModulesPanel.this,
                        "Êtes-vous sûr de vouloir désinstaller ces modules ?\nToutes les données seront irrémédiablement effacées.", "Désinstallation de modules", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.NO_OPTION)
                    return;

                try {
                    // TODO uninstall out of EDT
                    final Set<String> ids = new HashSet<String>();
                    for (final ModuleFactory f : checkedRows) {
                        ids.add(f.getID());
                    }
                    final Collection<String> dependentModules = mngr.getDependentModulesRecursively(ids);
                    if (!dependentModules.isEmpty()) {
                        final int selectAnswer = JOptionPane.showConfirmDialog(InstalledModulesPanel.this, "Des modules non sélectionnés ont besoin de la sélection : " + dependentModules
                                + ".\nVoulez-vous également désinstaller ces modules ?", "Désinstallation de modules", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (selectAnswer == JOptionPane.NO_OPTION)
                            return;
                    }
                    mngr.uninstall(ids, true);
                } catch (Exception e) {
                    ExceptionHandler.handle(InstalledModulesPanel.this, "Impossible de désinstaller les modules", e);
                }
                // some might have started
                moduleFrame.reload();
            }
        });
        uninstallButton.setOpaque(false);
        c.gridy++;
        this.add(uninstallButton, c);

        JPanel space = new JPanel();
        space.setOpaque(false);
        c.weighty = 1;
        c.gridy++;
        this.add(space, c);
    }

    public final void reload() {
        this.tm.reload();
    }

    private final class StartStopAction extends AbstractAction {
        private final boolean start;

        public StartStopAction(final boolean start) {
            super(start ? "Activer" : "Désactiver");
            this.start = start;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final ModuleManager mngr = ModuleManager.getInstance();
            final Collection<ModuleFactory> checkedRows = InstalledModulesPanel.this.tm.getCheckedRows();
            try {
                for (final ModuleFactory f : checkedRows) {
                    if (this.start)
                        mngr.startModule(f.getID(), true);
                    else
                        mngr.stopModuleRecursively(f.getID());
                }
            } catch (Exception e) {
                ExceptionHandler.handle(InstalledModulesPanel.this, "Impossible " + (this.start ? "d'activer" : "de désactiver") + " les modules", e);
            }
            // only need to reload us
            reload();
        }
    }
}
