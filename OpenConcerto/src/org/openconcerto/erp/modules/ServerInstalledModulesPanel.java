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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class ServerInstalledModulesPanel extends JPanel {

    private final ServerInstalledModuleTableModel tm;
    private ModuleFrame moduleFrame;

    ServerInstalledModulesPanel(final ModuleFrame moduleFrame) {
        this.moduleFrame = moduleFrame;
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        // Toolbar
        c.weightx = 0;
        c.weighty = 0;

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        final JButton activateButton = new JButton(new LocalInstallAction());
        activateButton.setOpaque(false);
        this.add(activateButton, c);
        c.gridx++;
        final JButton desactivateButton = new JButton(new UninstallAction());
        desactivateButton.setOpaque(false);
        this.add(desactivateButton, c);

        // Rows

        final IFactory<List<ModuleReference>> rowSource = new IFactory<List<ModuleReference>>() {
            @Override
            public List<ModuleReference> createChecked() {
                return ModuleManager.getInstance().getRemoteInstalledModules();
            }
        };

        this.tm = new ServerInstalledModuleTableModel(rowSource);
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
        t.getColumnModel().getColumn(3).setMinWidth(80);
        t.getColumnModel().getColumn(3).setPreferredWidth(148);
        t.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ModuleReference ref = (ModuleReference) tm.getModuleReferenceAt(row);
                if (!ModuleManager.getInstance().isModuleInstalledLocally(ref) && ModuleManager.getInstance().isModuleRequiredLocally(ref)) {
                    c.setBackground(Color.ORANGE);
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        });
        // Installation
        t.getColumnModel().getColumn(4).setMinWidth(80);
        t.getColumnModel().getColumn(4).setPreferredWidth(80);
        // Info
        t.getColumnModel().getColumn(5).setMinWidth(160);
        t.getColumnModel().getColumn(5).setPreferredWidth(128);
        t.getTableHeader().setReorderingAllowed(false);
        // allow InstalledModuleTableModel to be gc'd
        t.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    if (!e.getChanged().isDisplayable()) {
                        t.setModel(new DefaultTableModel());
                    } else if (t.getModel() != ServerInstalledModulesPanel.this.tm) {
                        t.setModel(ServerInstalledModulesPanel.this.tm);
                    }
                }
            }
        });

        final JScrollPane scroll = new JScrollPane(t);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;

        this.add(scroll, c);

        ModuleManager.getInstance().dump(System.err);
    }

    public final void reload() {
        this.tm.reload();
    }

    private final class LocalInstallAction extends AbstractAction {
        LocalInstallAction() {
            super("Installer sur le poste");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final Set<ModuleReference> checkedRows = ServerInstalledModulesPanel.this.tm.getCheckedRows();
            if (checkedRows.isEmpty()) {
                JOptionPane.showMessageDialog(ServerInstalledModulesPanel.this, "Aucune ligne cochée");
                return;
            }
            // Modules are already installed on server
            try {
                ModuleManager.getInstance().installModulesLocally(checkedRows);
            } catch (Exception e) {
                ExceptionHandler.handle(ServerInstalledModulesPanel.this, "Impossible d'installer les modules sur le poste", e);
            }
            reload();
        }
    }

    private final class UninstallAction extends AbstractAction {

        public UninstallAction() {
            super("Désinstaller du serveur");

        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final Collection<ModuleReference> checkedRows = ServerInstalledModulesPanel.this.tm.getCheckedRows();
            if (checkedRows.isEmpty()) {
                JOptionPane.showMessageDialog(ServerInstalledModulesPanel.this, "Aucune ligne cochée");
                return;
            }

            final ModuleManager mngr = ModuleManager.getInstance();
            final int answer = JOptionPane.showConfirmDialog(ServerInstalledModulesPanel.this,
                    "Êtes-vous sûr de vouloir désinstaller ces modules ?\nToutes les données seront irrémédiablement effacées.", "Désinstallation de modules", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.NO_OPTION)
                return;

            try {
                final Set<String> ids = new HashSet<String>();
                for (final ModuleReference f : checkedRows) {
                    ids.add(f.getId());
                }
                final Collection<String> dependentModules = mngr.getDependentModulesRecursively(ids);
                if (!dependentModules.isEmpty()) {
                    final int selectAnswer = JOptionPane.showConfirmDialog(ServerInstalledModulesPanel.this, "Des modules non sélectionnés ont besoin de la sélection : " + dependentModules
                            + ".\nVoulez-vous également désinstaller ces modules ?", "Désinstallation de modules", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (selectAnswer == JOptionPane.NO_OPTION)
                        return;
                }

                final boolean force = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0;

                final JDialog dialog = AvailableModulesPanel.displayDialog(ServerInstalledModulesPanel.this,
                        "Désinstallation " + AvailableModulesPanel.MODULE_FMT.format(new Object[] { checkedRows.size() }));
                new SwingWorker<Object, Object>() {
                    protected Object doInBackground() throws Exception {
                        // Uninstall on server and locally
                        mngr.uninstall(ids, true, force, false);
                        return null;
                    }

                    protected void done() {
                        try {
                            this.get();
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(ServerInstalledModulesPanel.this, "Impossible de désinstaller les modules");
                        }
                        moduleFrame.reload();
                        dialog.dispose();
                    }
                }.execute();
            } catch (Exception e) {
                ExceptionHandler.handle(ServerInstalledModulesPanel.this, "Impossible de trouver les modules à désinstaller", e);
            }

            // only need to reload us
            reload();
        }
    }
}
