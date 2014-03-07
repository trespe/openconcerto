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

import org.openconcerto.erp.modules.ModuleManager.ModuleAction;
import org.openconcerto.erp.modules.ModuleManager.NoChoicePredicate;
import org.openconcerto.erp.modules.ModuleTableModel.Columns;
import org.openconcerto.erp.modules.ModuleTableModel.ModuleRow;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.TableCellRendererDecorator.TableCellRendererDecoratorUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.JImage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ModulePanel extends JPanel {

    private final ModuleTableModel tm;

    ModulePanel(final ModuleFrame moduleFrame, final boolean onlyInstall) {
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        this.tm = new ModuleTableModel();
        final JTable t = new JTable(this.tm) {
            // only force JTable size above its minimal size (i.e. otherwise use its preferred size
            // and display scroll bars accordingly). Without this, some columns can be invisible
            // since the JTable always respect columns minimum size.
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // ScrollPaneLayout.layoutContainer() set the size of the view port and then call
                // Scrollable methods
                final Container viewPort = SwingUtilities.getAncestorOfClass(JViewport.class, this);
                return viewPort.getSize().width >= this.getMinimumSize().width;
            }
        };
        // perhaps pass custom sorter to be able to unsort
        t.setAutoCreateRowSorter(true);
        t.setShowGrid(false);
        t.setShowVerticalLines(false);
        t.setFocusable(false);
        t.setRowSelectionAllowed(false);
        t.setColumnSelectionAllowed(false);
        t.setCellSelectionEnabled(false);
        final TableColumnModel columnModel = t.getColumnModel();
        final TableCellRenderer headerDefaultRenderer = t.getTableHeader().getDefaultRenderer();
        final EnumSet<Columns> booleanCols = EnumSet.of(ModuleTableModel.Columns.LOCAL, ModuleTableModel.Columns.REMOTE, ModuleTableModel.Columns.DB_REQUIRED, ModuleTableModel.Columns.ADMIN_REQUIRED);
        final JImage trueComp = new JImage(TableCellRendererDecoratorUtils.class.getResource("okay.png"));
        trueComp.setBackground(Color.WHITE);
        trueComp.setCenterImage(true);
        final JComponent falseComp = new JPanel();
        falseComp.setBackground(Color.WHITE);
        final TableCellRenderer booleanRenderer = new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, final Object value, final boolean isSelected, boolean hasFocus, int row, int column) {
                return ((Boolean) value) ? trueComp : falseComp;
            }
        };

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            final TableColumn col = columnModel.getColumn(i);
            col.setIdentifier(ModuleTableModel.Columns.values()[i]);

            final int minCellWidth = this.tm.getColumnClass(i) == Boolean.class ? 32 : 48;
            col.setMinWidth(minCellWidth);
            final int prefCellWidth;
            if (col.getIdentifier() == ModuleTableModel.Columns.NAME || col.getIdentifier() == ModuleTableModel.Columns.STATE)
                prefCellWidth = 192;
            else
                prefCellWidth = minCellWidth;
            // makes sure the column can display its label
            final Component headerComp = headerDefaultRenderer.getTableCellRendererComponent(null, col.getHeaderValue(), false, false, 0, 0);
            col.setPreferredWidth(Math.max(headerComp.getMinimumSize().width, prefCellWidth));
            if (col.getIdentifier() == ModuleTableModel.Columns.CB) {
                col.setMaxWidth(minCellWidth);
            } else {
                col.setMaxWidth(Integer.MAX_VALUE);
            }
            col.setWidth(col.getPreferredWidth());
            if (booleanCols.contains(col.getIdentifier())) {
                col.setCellRenderer(new AlternateTableCellRenderer(booleanRenderer));
            } else {
                AlternateTableCellRenderer.setRenderer(col);
            }
        }
        // JTable returns by default a hard coded dimension (e.g. the table can be too small and its
        // column labels truncated)
        t.setPreferredScrollableViewportSize(new Dimension(t.getPreferredSize().width, 400));
        t.getTableHeader().setReorderingAllowed(false);
        // Better look
        t.setShowHorizontalLines(false);
        t.setGridColor(new Color(230, 230, 230));
        t.setRowHeight(t.getRowHeight() + 4);
        // allow the table model to be gc'd
        t.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    // keep the table model to keep our renderers and maybe later the selection
                    // (though we could overload createDefaultColumnsFromModel() to set the
                    // renderers)
                    if (!e.getChanged().isDisplayable()) {
                        ModulePanel.this.tm.clear();
                    } else {
                        reload();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(t);
        c.weighty = 1;
        c.weightx = 1;
        // 4 buttons + space
        c.gridheight = 5;
        c.fill = GridBagConstraints.BOTH;
        this.add(scroll, c);
        // Right column
        c.weightx = 0;
        c.weighty = 0;
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        c.gridheight = 1;

        final JButton installButton = new JButton(AvailableModulesPanel.createInstallAction(this, onlyInstall));
        installButton.setOpaque(false);
        this.add(installButton, c);

        if (!onlyInstall) {
            c.gridy++;
            JButton activateButton = new JButton(new StartStopAction(ModuleAction.START));
            activateButton.setOpaque(false);
            this.add(activateButton, c);
            JButton desactivateButton = new JButton(new StartStopAction(ModuleAction.STOP));
            desactivateButton.setOpaque(false);
            c.gridy++;
            this.add(desactivateButton, c);
        }
        c.insets = new Insets(20, 3, 2, 2);
        JButton uninstallButton = new JButton(new AbstractAction("Désinstaller") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final String dialogTitle = "Désinstallation de modules";
                final Collection<ModuleRow> checkedRows = getSelection();
                if (checkedRows.isEmpty()) {
                    JOptionPane.showMessageDialog(ModulePanel.this, "Aucune ligne cochée", dialogTitle, JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                final ModuleManager mngr = ModuleManager.getInstance();
                final boolean forceUninstall = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0 && mngr.currentUserIsAdmin();
                try {
                    final Set<ModuleReference> ids = new HashSet<ModuleReference>();
                    final Set<ModuleReference> deniedRefs = new HashSet<ModuleReference>();
                    for (final ModuleRow f : checkedRows) {
                        if (!mngr.canCurrentUser(ModuleAction.UNINSTALL, f))
                            deniedRefs.add(f.getRef());
                        else
                            ids.add(f.getRef());
                    }

                    final String denied = deniedRefs.size() == 0 ? "" : "Désinstallation refusée pour les modules :\n" + formatList(deniedRefs) + ".\n";
                    if (ids.size() == 0) {
                        // since checkedRows is not empty
                        assert denied.length() > 0;
                        JOptionPane.showMessageDialog(ModulePanel.this, denied, dialogTitle, JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    assert ids.size() > 0;
                    final int answer = JOptionPane.showConfirmDialog(ModulePanel.this, "Êtes-vous sûr de vouloir désinstaller ces modules "
                            + (forceUninstall ? "*en ce passant des modules si nécessaire* " : "") + "?\n" + denied + "Toutes les données seront irrémédiablement effacées.", dialogTitle,
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (answer == JOptionPane.NO_OPTION)
                        return;

                    final JDialog dialog = AvailableModulesPanel.displayDialog(ModulePanel.this, "Calcul des dépendences");
                    new SwingWorker<ModulesStateChange, Object>() {
                        protected ModulesStateChange doInBackground() throws Exception {
                            return mngr.getUninstallSolution(ids, true, forceUninstall);
                        }

                        protected void done() {
                            dialog.dispose();
                            try {
                                final ModulesStateChange solution = this.get();

                                final Set<ModuleReference> dependentModules = new HashSet<ModuleReference>(solution.getReferencesToRemove());
                                dependentModules.removeAll(ids);
                                if (!dependentModules.isEmpty()) {
                                    deniedRefs.clear();
                                    // ids have already been checked
                                    for (final ModuleReference depModule : dependentModules) {
                                        if (!mngr.canCurrentUserInstall(ModuleAction.UNINSTALL, depModule, solution.getInstallState())) {
                                            deniedRefs.add(depModule);
                                        }
                                    }

                                    if (deniedRefs.size() > 0) {
                                        JOptionPane
                                                .showMessageDialog(ModulePanel.this, "Désinstallation refusée pour les modules :" + formatList(deniedRefs), dialogTitle, JOptionPane.WARNING_MESSAGE);
                                        return;
                                    }

                                    final int selectAnswer = JOptionPane.showConfirmDialog(ModulePanel.this, "Les modules suivants doivent être désinstallés : \n" + formatList(dependentModules)
                                            + ".\nVoulez-vous également désinstaller ces modules ?", dialogTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                                    if (selectAnswer == JOptionPane.NO_OPTION)
                                        return;
                                }

                                AvailableModulesPanel.applySolution(mngr, ModulePanel.this, solution, onlyInstall);
                            } catch (Exception e) {
                                ExceptionHandler.handle(ModulePanel.this, "Impossible de désinstaller les modules", e);
                            }
                        }
                    }.execute();
                } catch (Exception e) {
                    ExceptionHandler.handle(ModulePanel.this, "Impossible de trouver les modules à désinstaller", e);
                }
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

        this.tm.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                installButton.setEnabled(ModulePanel.this.tm.isValid());
                // uninstall to make it valid
            }
        });
        this.setTransferHandler(AvailableModulesPanel.createTransferHandler(this));
    }

    public final void reload() {
        try {
            this.tm.reload();
        } catch (Exception e) {
            ExceptionHandler.handle(ModulePanel.this, "Impossible de recharger la liste des modules", e);
        }
    }

    protected final Collection<ModuleRow> getSelection() {
        // don't use native selection since users have some difficulty to select multiple rows
        // NOTE: since we use a RowSorter this also frees us from converting indexes
        return this.tm.getCheckedRows();
    }

    // this doesn't change the installation state, only start/stop
    private final class StartStopAction extends AbstractAction {
        private final boolean start;
        private final ModuleAction action;

        public StartStopAction(final ModuleAction action) {
            super(action == ModuleAction.START ? "Activer" : "Désactiver");
            if (action != ModuleAction.START && action != ModuleAction.STOP)
                throw new IllegalArgumentException(action + " is neither START nor STOP");
            this.action = action;
            this.start = action == ModuleAction.START;
            this.putValue(Action.SHORT_DESCRIPTION, this.start ? "Démarrer le(s) module(s), maintenir CTRL pour rendre obligatoire le démarrage"
                    : "Arrête le(s) module(s), maintenir CTRL pour rendre facultatif le démarrage");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final ModuleManager mngr = ModuleManager.getInstance();
            final String dialogTitle = this.start ? "Démarrage de modules" : "Arrêt de modules";
            final Collection<ModuleRow> checkedRows = getSelection();
            if (checkedRows.isEmpty()) {
                JOptionPane.showMessageDialog(ModulePanel.this, "Aucune ligne cochée", dialogTitle, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            final boolean userIsAdmin = mngr.currentUserIsAdmin();
            // on Ubuntu ALT-Click is used to move windows
            final boolean changeRequired = (evt.getModifiers() & ActionEvent.CTRL_MASK) != 0 && userIsAdmin;
            final Set<ModuleReference> adminRequired = changeRequired ? new HashSet<ModuleReference>() : Collections.<ModuleReference> emptySet();
            try {
                final Set<ModuleReference> refs = new HashSet<ModuleReference>();
                final Set<ModuleReference> deniedRefs = new HashSet<ModuleReference>();
                for (final ModuleRow f : checkedRows) {
                    if (!mngr.canCurrentUser(this.action, f)) {
                        deniedRefs.add(f.getRef());
                    } else {
                        refs.add(f.getRef());
                    }
                    if (changeRequired) {
                        adminRequired.add(f.getRef());
                    }
                }
                if (deniedRefs.size() > 0) {
                    final String msg = getDeniedMessage(deniedRefs);
                    if (AvailableModulesPanel.displayDenied(ModulePanel.this, dialogTitle, msg, refs.size() == 0))
                        return;
                }
                assert refs.size() > 0;
                if (this.start) {
                    // pass NO_CHANGE so that we don't block the EDT, use install button to install
                    // (resolve dependencies) and start modules
                    final Set<ModuleReference> notStarted = mngr.startModules(refs, NoChoicePredicate.NO_CHANGE, true);
                    if (notStarted.size() > 0)
                        JOptionPane.showMessageDialog(ModulePanel.this, getDeniedMessage(notStarted) + "\nEssayer d'abord de les installer", dialogTitle, JOptionPane.WARNING_MESSAGE);
                } else {
                    for (final ModuleReference ref : refs) {
                        final List<ModuleReference> runningDepModules = mngr.getRunningDependentModulesRecursively(ref.getID());
                        deniedRefs.clear();
                        for (final ModuleReference runningDepModule : runningDepModules) {
                            if (!mngr.canCurrentUser(this.action, ModulePanel.this.tm.getRow(runningDepModule))) {
                                deniedRefs.add(runningDepModule);
                            }
                        }
                        if (!deniedRefs.isEmpty()) {
                            JOptionPane.showMessageDialog(ModulePanel.this, getDeniedMessage(deniedRefs), dialogTitle, JOptionPane.WARNING_MESSAGE);
                        } else {
                            for (final ModuleReference runningDepModule : runningDepModules) {
                                mngr.stopModule(runningDepModule.getID());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.handle(ModulePanel.this, "Impossible " + (this.start ? "d'activer" : "de désactiver") + " les modules", e);
            } finally {
                try {
                    mngr.setAdminRequiredModules(adminRequired, this.start);
                } catch (BackingStoreException e1) {
                    ExceptionHandler.handle(ModulePanel.this, "Impossible de rendre " + (this.start ? "obligatoires" : "facultatifs") + " les modules", e1);
                }
                reload();
            }
        }

        private final String getDeniedMessage(final Set<ModuleReference> deniedRefs) {
            return "Les modules suivants ne peuvent être " + (this.start ? "démarrés" : "arrêtés") + " : \n" + formatList(deniedRefs);
        }

    }

    public static String formatList(Collection<ModuleReference> refs) {
        String str = "";
        final List<ModuleReference> lInstall = new ArrayList<ModuleReference>(refs);
        Collections.sort(lInstall, ModuleReference.COMP_ID_ASC_VERSION_DESC);
        for (ModuleReference moduleReference : refs) {
            str += "- " + format(moduleReference);
        }
        return str;
    }

    public static String format(ModuleReference moduleReference) {
        String str = moduleReference.getID();
        if (moduleReference.getVersion() != null) {
            str += " (" + moduleReference.getVersion().toString() + ")";
        }
        return str;
    }
}
