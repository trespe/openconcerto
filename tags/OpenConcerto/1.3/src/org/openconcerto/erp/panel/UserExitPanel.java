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
// import org.openconcerto.erp.config.GestionLauncher;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.preferences.BackupNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.utils.BackupPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.UserExit;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ProcessStreams;
import org.openconcerto.utils.ProcessStreams.Action;
import org.openconcerto.utils.prog.VMLauncher;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class UserExitPanel extends JPanel {

    static private final AtomicBoolean CLOSING = new AtomicBoolean(false);

    static public final boolean isClosing() {
        return CLOSING.get();
    }

    private final UserExitConf conf;

    public UserExitPanel(final UserExitConf conf) {
        if (conf == null)
            throw new NullPointerException("Null conf");
        this.conf = conf;
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 2, 5, 2);
        c.weightx = 1;
        c.gridwidth = 2;

        String text = "Voulez-vous vraiment " + (this.conf.shouldRestart() ? "relancer " : "quitter ") + Configuration.getInstance().getAppName() + " ?";
        if (this.conf.getMessage() != null) {
            text = this.conf.getMessage() + "<br/>" + text;
        }
        final JLabel labelQuit = new JLabel("<html>" + text + "</html>");
        JButton buttonCancel = new JButton("Annuler");
        final String verb = this.conf.shouldRestart() ? "Relancer" : "Quitter";
        JButton buttonBackup = new JButton("Sauvegarder et " + verb.toLowerCase());
        JButton buttonExit = new JButton(verb);
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
        assert SwingUtilities.isEventDispatchThread();
        if (!CLOSING.compareAndSet(false, true)) {
            // already closing
            return;
        }
        UserExit.closeAllWindows(null);
        try {
            final UserExitConf c = this.conf;
            // add shutdown hook first, since the user already agreed to restart
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        // MAYBE only run beforeShutdown() if afterWindowsClosed() is successful
                        c.beforeShutdown();

                        // if (c.shouldRestart())
                        // VMLauncher.restart(ProcessStreams.Action.CLOSE, GestionLauncher.class);
                    } catch (Exception e) {
                        // in shutdown sequence : don't use the GUI
                        e.printStackTrace();
                    }
                }
            });
            c.afterWindowsClosed();
        } catch (Exception exn) {
            // all windows already closed
            ExceptionHandler.handle("Erreur lors de la fermeture", exn);
        }
        ModuleManager.tearDown();
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
                        ProcessStreams.handle(p, Action.REDIRECT);
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
                    // 5 s d'attente
                    // TODO make ModuleManager.install() atomic
                    Thread.sleep(5 * 1000);
                    System.err.println("Warning: Forcing exit");
                    Frame[] l = Frame.getFrames();
                    for (int i = 0; i < l.length; i++) {
                        Frame f = l[i];
                        System.err.println(f.getName() + " " + f + " Displayable: " + f.isDisplayable() + " Valid: " + f.isValid() + " Active: " + f.isActive());
                    }
                    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                    for (Thread thread : threadSet) {
                        if (!thread.isDaemon()) {
                            System.err.println(thread.getName() + " " + thread.getId() + " not daemon");
                        }
                    }

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Configuration.getInstance().destroy();
                        }
                    });
                    t.setDaemon(true);
                    t.setName("Configuration destroy");
                    t.start();
                    Thread.sleep(5 * 1000);
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
