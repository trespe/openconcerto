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

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.modules.ModuleManager.ModuleAction;
import org.openconcerto.erp.modules.ModuleManager.ModuleState;
import org.openconcerto.erp.modules.ModuleTableModel.ModuleRow;
import org.openconcerto.erp.panel.UserExitConf;
import org.openconcerto.sql.view.AbstractFileTransfertHandler;
import org.openconcerto.ui.component.WaitIndeterminatePanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

public class AvailableModulesPanel {

    static final MessageFormat MODULE_FMT;

    static {
        final ChoiceFormat choiceForm = new ChoiceFormat(new double[] { 1, 2 }, new String[] { "d'un module", "de {0} modules" });
        MODULE_FMT = new MessageFormat("{0}");
        MODULE_FMT.setFormatByArgumentIndex(0, choiceForm);
    }

    // prevent the user from interacting when un/installing modules
    static JDialog displayDialog(JComponent parent, final String text) {
        final WaitIndeterminatePanel panel = new WaitIndeterminatePanel(text);
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(true);
            }
        });
        return dialog;
    }

    // return true if the user cancels
    static boolean displayDenied(final JComponent panel, final String dialogTitle, final String deniedMsg, final boolean noneAllowed) {
        final boolean done;
        if (noneAllowed) {
            JOptionPane.showMessageDialog(panel, deniedMsg, dialogTitle, JOptionPane.WARNING_MESSAGE);
            done = true;
        } else {
            final int answer = JOptionPane.showConfirmDialog(panel, deniedMsg, dialogTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            done = answer == JOptionPane.CANCEL_OPTION;
        }
        return done;
    }

    static void applySolution(final ModuleManager mngr, final ModulePanel panel, final ModulesStateChange chosen, final boolean onlyInstall) {
        final String dialogTitle = "Gestion des modules";
        final ModuleState targetState = onlyInstall ? ModuleState.INSTALLED : ModuleState.STARTED;
        final int installSize = chosen.getReferencesToInstall().size();
        final int uninstallSize = chosen.getReferencesToRemove().size();
        if (installSize == 0 && uninstallSize == 0) {
            JOptionPane.showMessageDialog(panel, "Aucun changement à apporter", dialogTitle, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final StringBuilder sb = new StringBuilder(128);
        if (uninstallSize > 0) {
            sb.append("Désinstallation ");
            sb.append(MODULE_FMT.format(new Object[] { uninstallSize }));
        }
        if (installSize > 0) {
            if (sb.length() > 0)
                sb.append(". ");
            sb.append("Installation ");
            sb.append(MODULE_FMT.format(new Object[] { installSize }));
        }
        sb.append(".");
        final String msg = sb.toString();
        if (mngr.needExit(chosen)) {
            Gestion.askForExit(new UserExitConf(msg, true) {
                @Override
                protected void afterWindowsClosed() throws Exception {
                    // since windows are closed this should minimise uninstall
                    // problems. As the other branch of the if, start modules
                    mngr.applyChange(chosen, targetState, true);
                };
            });
        } else {
            final JDialog dialog = displayDialog(panel, msg);
            new SwingWorker<ModulesStateChangeResult, Object>() {
                @Override
                protected ModulesStateChangeResult doInBackground() throws Exception {
                    return mngr.applyChange(chosen, ModuleState.REGISTERED);
                }

                @Override
                protected void done() {
                    try {
                        final ModulesStateChangeResult res = this.get();
                        if (res.getNotCreated().size() > 0)
                            JOptionPane.showMessageDialog(panel, "Certains modules n'ont pu être créés : " + res.getNotCreated(), dialogTitle, JOptionPane.WARNING_MESSAGE);

                        // start inside the EDT, that way when we return, the
                        // modules are completely started. Further if any exception
                        // is thrown we can catch it here.
                        if (targetState.compareTo(ModuleState.STARTED) >= 0) {
                            try {
                                // pass all modules to start() since start/stop status might have
                                // changed since doInBackground()
                                mngr.startFactories(res.getGraph().flatten());
                                mngr.setPersistentModules(chosen.getUserReferencesToInstall());
                            } catch (Exception e) {
                                ExceptionHandler.handle(panel, "Impossible de démarrer les modules", e);
                            }
                        }
                    } catch (Exception e) {
                        ExceptionHandler.handle(panel, "Impossible d'appliquer les changements", e);
                    }
                    panel.reload();
                    dialog.dispose();
                }
            }.execute();
        }
    }

    static final Action createInstallAction(final ModulePanel panel, final boolean onlyInstall) {
        return new AbstractAction("Installer") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                final String dialogTitle = "Installation de modules";
                final Collection<ModuleRow> checkedRows = panel.getSelection();
                if (checkedRows.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "Aucune ligne cochée", dialogTitle, JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                final ModuleManager mngr = ModuleManager.getInstance();
                final Set<ModuleReference> refs = new HashSet<ModuleReference>();
                final Set<ModuleReference> deniedRefs = new HashSet<ModuleReference>();
                for (final ModuleRow f : checkedRows) {
                    if (!mngr.canCurrentUser(ModuleAction.INSTALL, f))
                        deniedRefs.add(f.getRef());
                    else
                        refs.add(f.getRef());
                }
                if (deniedRefs.size() > 0) {
                    if (displayDenied(panel, dialogTitle, "Ces modules ne peuvent être installés : " + deniedRefs, refs.size() == 0))
                        return;
                }
                assert refs.size() > 0;
                final JDialog depDialog = displayDialog(panel, "Calcul des dépendences");
                new SwingWorker<Solutions, Object>() {
                    @Override
                    protected Solutions doInBackground() throws Exception {
                        // MAYBE present the user with the reason references couldn't be installed
                        return mngr.getSolutions(refs, 5);
                    }

                    @Override
                    protected void done() {
                        depDialog.dispose();
                        try {
                            final Solutions res = this.get();
                            if (res.getSolutions().size() == 0) {
                                JOptionPane.showMessageDialog(panel, "Aucune solution trouvée", "Installation de modules", JOptionPane.WARNING_MESSAGE);
                            } else {
                                final DepSolverResultChooserPanel cPanel = new DepSolverResultChooserPanel(res.getSolutions());
                                cPanel.setRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        applySolution(mngr, panel, cPanel.getSolutionToApply(), onlyInstall);
                                    }
                                });
                                final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(panel), ModalityType.APPLICATION_MODAL);
                                dialog.add(cPanel);
                                dialog.pack();
                                dialog.setLocationRelativeTo(panel);
                                dialog.setVisible(true);

                            }
                        } catch (Exception e) {
                            ExceptionHandler.handle(panel, "Erreur lors de la recherche de solutions", e);
                        }
                    }

                }.execute();
            }
        };
    }

    static final TransferHandler createTransferHandler(final ModulePanel panel) {
        return new AbstractFileTransfertHandler() {

            @Override
            public void handleFile(File f) {
                if (!f.getName().endsWith(".jar")) {
                    JOptionPane.showMessageDialog(panel, "Impossible d'installer le module. Le fichier n'est pas un module.");
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
                        JOptionPane.showMessageDialog(panel, "Impossible d'installer le module.\n" + f.getAbsolutePath() + " vers " + dir.getAbsolutePath());
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(panel, "Impossible d'installer le module.\nVous devez disposer des droits en écriture sur le dossier:\n" + dir.getAbsolutePath());
                    return;
                }
                try {
                    ModuleManager.getInstance().addFactory(new JarModuleFactory(out));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(panel, "Impossible d'intégrer le module.\n" + e.getMessage());
                    return;
                }
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        panel.reload();
                    }
                });

            }
        };
    }
}
