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
import org.openconcerto.sql.utils.Exiter;
import org.openconcerto.task.ui.UserRightsPrefPanel;

import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TaskAdminNX {

    public static void main(String[] args) {
        System.setProperty("org.openconcerto.sql.structure.useXML", "true");
        Configuration.setInstance(TaskPropsConfiguration.create());
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Toolkit.getDefaultToolkit().setDynamicLayout(true);
                final JFrame fLogin = new JFrame("Accès à la gestion des utilisateurs");
                JPanel pass = new JPanel();
                final JPasswordField pField = new JPasswordField(20);
                pField.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {

                        String p = new String(pField.getPassword()).trim();
                        if (p.length() == 3) {
                            if (p.substring(0, 1).equalsIgnoreCase("j") && p.substring(1, 2).equalsIgnoreCase("d") && p.substring(2, 3).equalsIgnoreCase("t")) {
                                JFrame f = new JFrame();
                                new Exiter(f, true);
                                f.setTitle("Gestion des autorisations concernant les tâches");
                                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                f.setContentPane(new UserRightsPrefPanel());
                                f.pack();
                                f.setLocation(20, 20);
                                f.setSize(800, 600);
                                f.setVisible(true);
                            }
                            fLogin.dispose();
                        } else {
                            System.exit(0);
                        }
                    }
                });

                pass.setLayout(new FlowLayout());
                pass.add(new JLabel("Entrez le mot de passe super utilisateur:"));
                pass.add(pField);

                fLogin.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                fLogin.setContentPane(pass);
                fLogin.pack();
                fLogin.setLocation(20, 20);
                // fLogin.setSize(800, 600);
                fLogin.setVisible(true);

            }
        });
    }
}
