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
 
 package org.openconcerto.erp.core.sales.pos.ui;

import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.RemoteShell;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.utils.ClassPathLoader;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

public class CaisseFrame extends JFrame {
    CaissePanel t;

    CaisseFrame() {
        t = new CaissePanel(this);

        setContentPane(t);
        setFocusable(true);
    }

    public static void main(String[] args) {
        try {
            System.out.println("Lancement du module de caisse");
            ToolTipManager.sharedInstance().setInitialDelay(0);
            RemoteShell.startDefaultInstance();

            System.setProperty(PropsConfiguration.REDIRECT_TO_FILE, "true");
            System.setProperty(SQLBase.ALLOW_OBJECT_REMOVAL, "true");

            ExceptionHandler.setForceUI(true);
            ExceptionHandler.setForumURL("http://www.openconcerto.org/forum");

            // SpeedUp Linux
            System.setProperty("sun.java2d.pmoffscreen", "false");
            System.setProperty(SQLBase.STRUCTURE_USE_XML, "true");
            System.setProperty(PropsConfiguration.REDIRECT_TO_FILE, "true");
            if (Caisse.isUsingJPos()) {
                ClassPathLoader c = ClassPathLoader.getInstance();
                try {
                    final String posDirectory = Caisse.getJPosDirectory();
                    if (posDirectory != null && !posDirectory.trim().isEmpty()) {
                        c.addJarFromDirectory(new File(posDirectory.trim()));
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                c.load();

            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        System.setProperty("awt.useSystemAAFontSettings", "on");
                        System.setProperty("swing.aatext", "true");
                        System.setProperty(ElementComboBox.CAN_MODIFY, "true");

                        Caisse.createConnexion();
                        CaisseFrame f = new CaisseFrame();
                        f.setUndecorated(true);
                        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                        f.pack();
                        f.setLocation(0, 0);
                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        f.setSize(screenSize);

                        System.out.println("Affichage de l'interface");
                        f.setVisible(true);
                        if (screenSize.getWidth() < 1280 || screenSize.getHeight() < 720) {
                            JOptionPane.showMessageDialog(f,
                                    "La résolution de votre écran est trop faible.\nLa largeur doit être au minium de 1280 pixels.\nLa hauteur doit être au minium de 720 pixels.");
                        }

                    } catch (Exception e) {
                        ExceptionHandler.handle("Erreur d'initialisation de la caisse (main)", e);
                    }

                }
            });
        } catch (Exception e) {
            ExceptionHandler.handle("Erreur d'initialisation de la caisse", e);
        }
    }

    public void showMenu() {
        System.out.println("CaisseFrame.showMenu()");
        this.invalidate();
        this.setContentPane(new CaisseMenuPanel(this));
        this.validate();
        this.repaint();

    }

    public void showCaisse() {
        System.out.println("CaisseFrame.showCaisse()");
        this.invalidate();
        this.setContentPane(this.t);
        this.validate();
        this.repaint();

    }

    public void showTickets(Ticket t) {
        System.out.println("CaisseFrame.showMenu()");
        this.invalidate();
        ListeDesTicketsPanel panel = new ListeDesTicketsPanel(this);
        panel.setSelectedTicket(t);
        this.setContentPane(panel);
        this.validate();
        this.repaint();

    }

}
