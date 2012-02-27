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
 
 package org.openconcerto.erp.config;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.NetUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class CloudPanel extends JPanel {

    private static final String CLOUD_URL = "https://cloud.openconcerto.org";

    CloudPanel(final ServerFinderPanel serverFinderPanel) {
        if (CLOUD_URL.contains("127.0.0.1")) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                } };

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.setOpaque(false);
        this.setLayout(new GridBagLayout());
        uiInit(serverFinderPanel);
    }

    private void uiInit(final ServerFinderPanel serverFinderPanel) {
        GridBagConstraints c = new DefaultGridBagConstraints();

        final String token = serverFinderPanel.getToken();
        if (token == null) {
            c.gridwidth = 2;
            this.add(new JLabel("Connexion de ce poste au cloud "), c);
            //
            c.gridwidth = 1;
            c.gridx = 0;
            c.gridy++;
            JLabel labelEmail = new JLabel("Email", SwingConstants.RIGHT);
            this.add(labelEmail, c);
            c.gridx++;
            c.weightx = 1;
            final JTextField textEmail = new JTextField();
            this.add(textEmail, c);
            //
            c.weightx = 0;
            c.gridx = 0;
            c.gridy++;
            JLabel labelPassword = new JLabel("Mot de passe", SwingConstants.RIGHT);
            this.add(labelPassword, c);
            final JPasswordField textPassword = new JPasswordField();
            c.gridx++;
            this.add(textPassword, c);
            //
            c.gridy++;
            c.fill = GridBagConstraints.NONE;
            JButton connect = new JButton("Se connecter au cloud");
            connect.setOpaque(false);
            this.add(connect, c);
            connect.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    String result = NetUtils.getHTTPContent(CLOUD_URL + "/getAuthToken?email=" + textEmail.getText() + "&password=" + new String(textPassword.getPassword()), false);
                    if (result != null && !result.contains("ERROR")) {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Connexion activée");
                        serverFinderPanel.setToken(result);
                        serverFinderPanel.saveConfigFile();
                        invalidate();
                        removeAll();
                        uiInit(serverFinderPanel);
                        revalidate();
                        repaint();
                    } else if (result != null && result.contains("not paid")) {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Compte non crédité");
                    } else {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Email ou identifiant incorrect");
                    }

                }
            });

        } else {
            c.weightx = 1;
            this.add(new JLabelBold("Ce poste est configuré pour utiliser le cloud"), c);
            c.gridy++;
            this.add(new JLabel("Id de connexion:" + token.substring(0, 16)), c);
            c.gridy++;
            c.fill = GridBagConstraints.NONE;

            final JButton buttonTest = new JButton("Tester la connexion");
            buttonTest.setOpaque(false);
            c.gridy++;
            this.add(buttonTest, c);
            buttonTest.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String result = NetUtils.getHTTPContent(CLOUD_URL + "/getAuthInfo?token=" + token, false);
                    if (result != null && !result.contains("ERROR")) {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Connexion opérationnelle");
                    } else if (result != null && result.contains("not paid")) {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Compte non crédité");
                    } else {
                        JOptionPane.showMessageDialog(CloudPanel.this, "Connexion impossible");
                    }
                }
            });
            c.gridy++;
            this.add(new JLabel(" "), c);
            c.gridy++;
            this.add(new JLabel("Désactivation de l'accès au cloud pour ce poste"), c);
            c.gridy++;
            final JButton buttonRevoke = new JButton("Désactiver la connexion au cloud");
            buttonRevoke.setOpaque(false);
            this.add(buttonRevoke, c);
            buttonRevoke.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int res = JOptionPane.showConfirmDialog(CloudPanel.this, "Souhaitez vous effacer la configuration de connexion au cloud pour ce poste?");
                    if (res == JOptionPane.YES_OPTION) {
                        serverFinderPanel.setToken(null);
                        serverFinderPanel.saveConfigFile();
                        invalidate();
                        removeAll();
                        uiInit(serverFinderPanel);
                        revalidate();
                        repaint();
                    }
                }
            });

        }
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        this.add(spacer, c);

    }

}
