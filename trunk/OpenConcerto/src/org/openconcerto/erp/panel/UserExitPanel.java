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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.preferences.BackupNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.utils.BackupPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.UserExit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class UserExitPanel extends JPanel {

    public UserExitPanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 2, 5, 2);
        c.weightx = 1;
        c.gridwidth = 2;

        final JLabel labelQuit = new JLabel("Voulez-vous vraiment quitter " + Configuration.getInstance().getAppName() + " ?");
        JButton buttonCancel = new JButton("Annuler");
        JButton buttonBackup = new JButton("Sauvegarder et quitter");
        JButton buttonExit = new JButton("Quitter");
        this.add(labelQuit, c);

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        this.add(buttonExit, c);
        final ComptaPropsConfiguration comptaPropsConfiguration = (ComptaPropsConfiguration) Configuration.getInstance();
            if (comptaPropsConfiguration.getSocieteID() > 1) {
                c.gridx++;
                this.add(buttonBackup, c);
            }
        c.gridx++;
        this.add(buttonCancel, c);
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // not use getRoot if frame is Jdialog modal, not return the jdialog but the root of
                // jdialog
                SwingUtilities.getWindowAncestor(UserExitPanel.this).dispose();
            }
        });

        final ActionListener listenerExit = new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                closeGNx();
            }

        };

        buttonBackup.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog frame = new JDialog();
                frame.setContentPane(new BackupPanel(Arrays.asList("Common", ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName()), Arrays.asList(new File("./2010"),
                        ((ComptaPropsConfiguration) Configuration.getInstance()).getDataDir()), false, BackupNXProps.getInstance()) {
                    @Override
                    public void doOnClose() {

                        super.doOnClose();
                        closeGNx();
                    }

                });
                frame.setTitle("Sauvegarde des données");
                // so that the application can exit
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                // ATTN works because BackupPanel do not open a new window when doing backup
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        startTimeOut();
                    }
                });
                frame.setLocationRelativeTo(null);
                frame.pack();
                frame.setMinimumSize(frame.getSize());

                ((Window) SwingUtilities.getRoot(UserExitPanel.this)).dispose();
                frame.setModal(true);
                frame.setIconImages(Gestion.getFrameIcon());
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
            }
        });
        buttonExit.addActionListener(listenerExit);
    }

    PostgreSQLFrame pgFrame = null;

    private void closeGNx() {
        UserExit.closeAllWindows(null);
        ComptaPropsConfiguration.closeOOConnexion();
        // ((JFrame) SwingUtilities.getRoot(UserExitingPanel.this)).dispose();
        if (Gestion.pgFrameStart != null) {
            Gestion.pgFrameStart.setVisible(false);
        }
        new Thread() {
            public void run() {

                try {

                    Runtime runtime = Runtime.getRuntime();

                    File f = new File(".\\PostgreSQL\\bin\\");
                    if (f.exists()) {
                        final Process p = runtime.exec(new String[] { "cmd.exe", "/C", "stopPostGres.bat" }, null, f);
                        // Consommation de la sortie standard de l'application externe dans
                        // un
                        // Thread
                        // separe
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {

                                try {
                                    pgFrame = new PostgreSQLFrame("Arrêt en cours");
                                    pgFrame.setVisible(true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        System.err.println("Execute end postgres");
                        new Thread() {
                            public void run() {
                                try {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                    String line = "";
                                    try {
                                        while ((line = reader.readLine()) != null) {
                                            System.out.println(line);
                                        }
                                    } finally {
                                        reader.close();
                                    }
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }.start();

                        // Consommation de la sortie d'erreur de l'application externe dans
                        // un
                        // Thread
                        // separe
                        new Thread() {
                            public void run() {
                                try {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                                    String line = "";
                                    try {
                                        while ((line = reader.readLine()) != null) {
                                            System.err.println(line);
                                        }
                                    } finally {
                                        reader.close();
                                    }
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        }.start();
                        p.waitFor();

                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (pgFrame != null) {
                                pgFrame.dispose();
                            }
                        }
                    });
                    startTimeOut();
                    // System.exit(0);
                }
            }
        }.start();
    }

    // (especially important with embedded h2, since the database is locked)
    private final void startTimeOut() {
        final Thread timeOut = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 2 minutes d'attente
                    Thread.sleep(2 * 60 * 1000);
                    System.err.println("Warning: Forcing exit");
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Configuration.getInstance().destroy();
                        }
                    });
                    t.setDaemon(true);
                    t.setName("Configuration destroy");
                    t.start();
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Dans tous les cas, on arrete le programme
                System.exit(1);
            }
        });
        timeOut.setName("TimeOut Thread");
        timeOut.setDaemon(true);
        timeOut.start();
    }
}
