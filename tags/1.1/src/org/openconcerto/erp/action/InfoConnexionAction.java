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
 
 package org.openconcerto.erp.action;

import org.openconcerto.sql.Configuration;

import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InfoConnexionAction extends CreateFrameAbstractAction {

    public InfoConnexionAction() {
        super();
        this.putValue(Action.NAME, "Informations de connexion");
    }

    public JFrame createFrame() {
        JPanel panel = new JPanel();
        String s = "";

        try {
            InetAddress in = InetAddress.getLocalHost();
            InetAddress[] all = InetAddress.getAllByName(in.getHostName());
            for (int i = 0; i < all.length; i++) {
                s += ("Adresse IP: " + all[i] + "\n");
            }
        } catch (UnknownHostException e) {
        }

        s += "Dossier de configuration: ";
        s += Configuration.getInstance().getConfDir() + "\n";
        s += "Base: ";
        s += Configuration.getInstance().getBase().getName() + "\n";
        s += "Utilisateur de base de donnée: ";
        s += Configuration.getInstance().getBase().getDataSource().getUsername() + "\n";
        s += "URL JDBC: ";
        s += Configuration.getInstance().getBase().getDataSource().getUrl();

        final JTextArea textArea = new JTextArea(s);
        textArea.setFont(new JLabel().getFont());
        textArea.setEditable(false);
        textArea.setBackground(panel.getBackground());
        panel.setLayout(new GridLayout(1, 1));
        panel.add(new JScrollPane(textArea));
        final JFrame f = new JFrame("Informations de connexion");
        f.setContentPane(panel);
        return f;
    }
}
