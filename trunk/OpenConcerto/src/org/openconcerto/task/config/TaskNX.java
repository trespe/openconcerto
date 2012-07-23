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
 
 package org.openconcerto.task.config;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.Exiter;
import org.openconcerto.task.ModelStateListener;
import org.openconcerto.task.TodoListPanel;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.ui.state.WindowStateManager;
import org.openconcerto.utils.JImage;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TaskNX extends JFrame implements ModelStateListener {
    private static TrayIcon trayIcon = null;

    public TaskNX() {
        this.setTitle("Task NX");
        TodoListPanel panel = new TodoListPanel();
        panel.addModelStateListener(this);
        this.setContentPane(panel);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        final JMenuBar menuBar2 = new JMenuBar();
        final JMenu menu = new JMenu("Fichier");
        final Action min = new AbstractAction("Minimiser") {

            public void actionPerformed(ActionEvent e) {
                setState(JFrame.ICONIFIED);

            }
        };
        // don't listen on windowClosing since we want to iconify
        final Exiter closeAll = new Exiter(this, false);
        final Action quit = new AbstractAction("Quitter") {

            public void actionPerformed(ActionEvent e) {
                closeAll.closeAll();
            }
        };

        menu.add(new JMenuItem(min));
        menu.add(new JMenuItem(quit));

        menuBar2.add(menu);
        this.setJMenuBar(menuBar2);

        // add state listener before, otherwise System.exit() prevents the save from happening
        final WindowStateManager m = new WindowStateManager(this, new File(Configuration.getInstance().getConfDir(), this.getClass().getSimpleName() + "-window.xml"));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setState(JFrame.ICONIFIED);
            }
        });

        addTray(quit);

        this.setLocation(10, 10);
        this.pack();
        m.loadState();
    }

    public static void main(String[] args) {
        System.setProperty(SQLBase.STRUCTURE_USE_XML, "true");
        Configuration.setInstance(TaskPropsConfiguration.create());
        final Runnable r = new Runnable() {

            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        TaskNX f = new TaskNX();
                        f.setVisible(true);
                    }
                });
            }
        };
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Toolkit.getDefaultToolkit().setDynamicLayout(true);
                System.setProperty("org.openconcerto.editpanel.noborder", "true");
                System.setProperty("org.openconcerto.editpanel.separator", "true");
                System.setProperty("org.openconcerto.listpanel.simpleui", "true");

                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                }
                final JImage imageLogo = new JImage(TaskNX.class.getResource("logo.png"));
                // on machines with a shared session, avoid that a user stores his password and then
                // another one connects as him
                final boolean sharedSession = "inspecteur".equals(System.getProperty("user.name"));
                PanelFrame f = new PanelFrame(new ConnexionPanel(r, imageLogo, false, !sharedSession), "Connexion");

                f.pack();
                f.setResizable(false);
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                // Centre la fenetre
                f.setLocationRelativeTo(null);

                FrameUtil.show(f);
            }
        });

    }

    private void addTray(final Action quit) {
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            Image image = new ImageIcon(TodoListPanel.class.getResource("low.png")).getImage();

            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "TaskNX");
            final PopupMenu popup = new PopupMenu();
            popup.add(new MenuItem((String) quit.getValue(Action.NAME)));
            popup.addActionListener(quit);
            trayIcon.setPopupMenu(popup);

            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    public void stateChanged(int state) {
        if (state == ModelStateListener.CONTENT_MODIFIED) {
            if (this.getState() == JFrame.ICONIFIED) {
                if (trayIcon != null) {
                    // Si on supporte les popup
                    trayIcon.displayMessage("TaskNX", "Votre liste des tâches vient d'être modifiée", MessageType.INFO);
                }
            }
        }
    }

}
