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
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public class LocalInstalledModulesPanel extends JPanel {

    private final LocalInstalledModuleTableModel tm;
    final ModuleFrame moduleFrame;

    LocalInstalledModulesPanel(final ModuleFrame moduleFrame) {
        this.moduleFrame = moduleFrame;

        final IFactory<List<ModuleReference>> rowSource = new IFactory<List<ModuleReference>>() {
            @Override
            public List<ModuleReference> createChecked() {

                final ModuleManager mngr = ModuleManager.getInstance();
                Set<ModuleReference> moduleRefs = new HashSet<ModuleReference>();
                moduleRefs.addAll(mngr.getModulesInstalledLocally());

                return new ArrayList<ModuleReference>(moduleRefs);
            }
        };

        this.tm = new LocalInstalledModuleTableModel(rowSource);

        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Toolbar
        c.weightx = 0;
        c.weighty = 0;

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        final JButton startButton = new JButton(new StartAction());
        startButton.setOpaque(false);
        this.add(startButton, c);
        c.gridx++;
        final JButton stopButton = new JButton(new StopAction());
        stopButton.setOpaque(false);
        this.add(stopButton, c);
        c.gridx++;
        final JButton uninstallButton = new JButton(new UninstallAction());
        uninstallButton.setOpaque(false);
        this.add(uninstallButton, c);
        // Rows

        final JTable t = new JTable(this.tm);
        t.setShowGrid(false);
        t.setShowVerticalLines(false);
        t.setFocusable(false);
        t.setRowSelectionAllowed(false);
        t.setColumnSelectionAllowed(false);
        t.setCellSelectionEnabled(false);
        t.getColumnModel().getColumn(0).setMinWidth(24);
        t.getColumnModel().getColumn(0).setPreferredWidth(24);
        t.getColumnModel().getColumn(0).setMaxWidth(24);
        t.getColumnModel().getColumn(0).setResizable(false);
        t.getColumnModel().getColumn(1).setMinWidth(148);
        // Version
        t.getColumnModel().getColumn(2).setMinWidth(48);
        t.getColumnModel().getColumn(2).setMaxWidth(48);
        // Etat
        t.getColumnModel().getColumn(3).setMinWidth(48);
        t.getColumnModel().getColumn(3).setPreferredWidth(48);
        t.getColumnModel().getColumn(3).setMaxWidth(200);
        //
        t.getColumnModel().getColumn(4).setMinWidth(80);
        t.getColumnModel().getColumn(4).setPreferredWidth(80);
        t.getColumnModel().getColumn(5).setMinWidth(128);
        t.getColumnModel().getColumn(5).setPreferredWidth(128);
        t.getTableHeader().setReorderingAllowed(false);
        // allow InstalledModuleTableModel to be gc'd
        t.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    if (!e.getChanged().isDisplayable()) {
                        t.setModel(new DefaultTableModel());
                    } else if (t.getModel() != LocalInstalledModulesPanel.this.tm) {
                        t.setModel(LocalInstalledModulesPanel.this.tm);
                    }
                }
            }
        });

        final JScrollPane scroll = new JScrollPane(t);
        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        this.add(scroll, c);

        ModuleManager.getInstance().dump(System.err);
    }

    public final void reload() {
        this.tm.reload();
    }

    private final class StartAction extends AbstractAction {
        private final Preferences reqPrefs;

        public StartAction() {
            super("Démarrer");
            this.reqPrefs = tm.getPrefs();
            this.putValue(Action.SHORT_DESCRIPTION, "Démarrer le(s) module(s), maintenir CTRL pour rendre obligatoire le démarrage");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final ModuleManager mngr = ModuleManager.getInstance();
            final Collection<ModuleReference> checkedRows = LocalInstalledModulesPanel.this.tm.getCheckedRows();
            try {
                for (final ModuleReference f : checkedRows) {
                    mngr.startModule(f.getId(), true);
                    // on Ubuntu ALT-Click is used to move windows
                    final boolean changeRequired = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0;
                    if (changeRequired) {
                        this.reqPrefs.put(f.getId(), "");
                    }
                }
                this.reqPrefs.sync();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(LocalInstalledModulesPanel.this, "Impossible d'activer les modules");
            }
            // only need to reload us
            reload();
        }
    }

    private final class StopAction extends AbstractAction {
        private final Preferences reqPrefs;

        public StopAction() {
            super("Arrêter");
            this.reqPrefs = tm.getPrefs();
            this.putValue(Action.SHORT_DESCRIPTION, "Arrête le(s) module(s), maintenir CTRL pour rendre facultatif le démarrage");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final ModuleManager mngr = ModuleManager.getInstance();
            final Collection<ModuleReference> checkedRows = LocalInstalledModulesPanel.this.tm.getCheckedRows();
            try {
                for (final ModuleReference f : checkedRows) {
                    mngr.stopModuleRecursively(f.getId());
                    // on Ubuntu ALT-Click is used to move windows
                    final boolean changeRequired = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0;
                    if (changeRequired) {
                        this.reqPrefs.remove(f.getId());
                    }
                }
                this.reqPrefs.sync();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(LocalInstalledModulesPanel.this, "Impossible de désactiver les modules");
            }
            // only need to reload us
            reload();
        }
    }

    private final class UninstallAction extends AbstractAction {
        UninstallAction() {
            super("Désinstaller du poste");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final Collection<ModuleReference> checkedRows = LocalInstalledModulesPanel.this.tm.getCheckedRows();
            if (checkedRows.isEmpty()) {
                JOptionPane.showMessageDialog(LocalInstalledModulesPanel.this, "Aucune ligne cochée");
                return;
            }

            final ModuleManager mngr = ModuleManager.getInstance();
            final int answer = JOptionPane.showConfirmDialog(LocalInstalledModulesPanel.this,
                    "Êtes-vous sûr de vouloir désinstaller ces modules sur ce poste?\nToutes les données locales seront irrémédiablement effacées.", "Désinstallation de modules",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.NO_OPTION)
                return;

            try {
                final Set<String> ids = new HashSet<String>();
                for (final ModuleReference f : checkedRows) {
                    ids.add(f.getId());
                }
                final Collection<String> dependentModules = mngr.getDependentModulesRecursively(ids);
                if (!dependentModules.isEmpty()) {
                    final int selectAnswer = JOptionPane.showConfirmDialog(LocalInstalledModulesPanel.this, "Des modules non sélectionnés ont besoin de la sélection : " + dependentModules
                            + ".\nVoulez-vous également désinstaller ces modules ?", "Désinstallation de modules", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (selectAnswer == JOptionPane.NO_OPTION)
                        return;
                }
                final boolean force = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0;

                final JDialog dialog = AvailableModulesPanel.displayDialog(LocalInstalledModulesPanel.this,
                        "Désinstallation " + AvailableModulesPanel.MODULE_FMT.format(new Object[] { checkedRows.size() }));
                new SwingWorker<Object, Object>() {
                    protected Object doInBackground() throws Exception {
                        // Local uninstall
                        mngr.uninstall(ids, true, force, true);
                        return null;
                    }

                    protected void done() {
                        try {
                            this.get();
                        } catch (Exception e) {
                            ExceptionHandler.handle(LocalInstalledModulesPanel.this, "Impossible de désinstaller les modules", e);
                        }
                        moduleFrame.reload();
                        dialog.dispose();
                    }
                }.execute();
            } catch (Exception e) {
                ExceptionHandler.handle(LocalInstalledModulesPanel.this, "Impossible de trouver les modules à désinstaller", e);
            }
        }

    }

}
