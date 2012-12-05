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
import org.openconcerto.ui.component.WaitIndeterminatePanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.awt.Dialog.ModalityType;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class AvailableModulesPanel extends JPanel {

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

    private final AvailableModuleTableModel tm;
    private final ModuleFrame moduleFrame;

    AvailableModulesPanel(final ModuleFrame moduleFrame) {
        this.moduleFrame = moduleFrame;
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        // Toolbar
        c.weightx = 0;
        c.weighty = 0;

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        final JButton startButton = new JButton(new InstallAction());
        startButton.setOpaque(false);
        this.add(startButton, c);
        c.gridx++;
        final JButton importButton = new JButton(new ImportAction());
        importButton.setOpaque(false);
        this.add(importButton, c);

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
        // Version
        t.getColumnModel().getColumn(2).setMinWidth(48);
        t.getColumnModel().getColumn(2).setMaxWidth(48);

        t.getColumnModel().getColumn(3).setMinWidth(48);
        t.getColumnModel().getColumn(3).setPreferredWidth(200);
        t.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(t);
        c.weighty = 1;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;

        c.fill = GridBagConstraints.BOTH;
        this.add(scroll, c);

        this.setTransferHandler(new AbstractFileTransfertHandler() {

            @Override
            public void handleFile(File f) {
                installModule(f);
            }
        });
    }

    protected void installModule(File f) {
        if (!f.getName().endsWith(".jar")) {
            JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'installer le module. Le fichier n'est pas un module.");
            return;
        }
        File dir = new File("Modules");
        dir.mkdir();
        if (!dir.exists()) {
            JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Impossible d'installer le module.\nLe dossier ne peut pas être créer:\n" + dir.getAbsolutePath());
            return;
        }
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

    public void reload() {
        this.tm.reload();
    }

    private final class InstallAction extends AbstractAction {

        InstallAction() {
            super("Installer");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final Set<ModuleReference> checkedRows = AvailableModulesPanel.this.tm.getCheckedRows();
            if (checkedRows.isEmpty()) {
                JOptionPane.showMessageDialog(AvailableModulesPanel.this, "Aucune ligne cochée");
                return;
            }

            final JDialog dialog = displayDialog(AvailableModulesPanel.this, "Installation " + MODULE_FMT.format(new Object[] { checkedRows.size() }));
            final ModuleManager mngr = ModuleManager.getInstance();
            new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {

                    mngr.installModulesOnServer(checkedRows);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        this.get();
                        int r = JOptionPane.showConfirmDialog(AvailableModulesPanel.this, "Installation sur le serveur reussie.\nVoulez-vous également installer les modules sur le poste?");
                        if (r == JOptionPane.OK_OPTION) {
                            mngr.installModulesLocally(checkedRows);
                        }
                    } catch (Exception e) {
                        ExceptionHandler.handle(AvailableModulesPanel.this, "Impossible d'installer les modules", e);
                    }
                    // some might have started
                    moduleFrame.reload();
                    dialog.dispose();
                }
            }.execute();
        }
    }

    private final class ImportAction extends AbstractAction {

        ImportAction() {
            super("Importer");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            FileDialog fileDialog = new FileDialog(moduleFrame, "Module à importer", FileDialog.LOAD);
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null) {
                installModule(new File(fileDialog.getFile()));
            }

        }
    }

}
