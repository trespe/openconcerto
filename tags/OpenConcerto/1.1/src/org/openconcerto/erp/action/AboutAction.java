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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.SystemInfoPanel;
import org.openconcerto.utils.ProductInfo;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class AboutAction extends AbstractAction {

    static private final AboutAction instance = new AboutAction();

    public static AboutAction getInstance() {
        return instance;
    }

    private AboutAction() {
        super("Informations");

    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        final JFrame frame = new JFrame((String) this.getValue(Action.NAME));
        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        panel.add(createTitle("Logiciel"), c);
        c.gridy++;
        panel.add(createSoftwareInfoPanel(), c);
        c.gridy++;
        panel.add(createTitle("Informations système"), c);
        c.gridy++;
        panel.add(new SystemInfoPanel(), c);

        final JScrollPane contentPane = new JScrollPane(panel);
        frame.setContentPane(contentPane);
        frame.pack();

        final Dimension size = frame.getSize();

        final Dimension maxSize = new Dimension(size.width, 700);
        if (size.height > maxSize.height) {
            frame.setMinimumSize(maxSize);
            frame.setPreferredSize(maxSize);
            frame.setSize(maxSize);
        } else {
            frame.setMinimumSize(size);
            frame.setPreferredSize(size);
            frame.setSize(size);
        }
        final Dimension maximumSize = maxSize;
        frame.setMaximumSize(maximumSize);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    private JLabel createTitle(final String text) {
        final JLabel res = new JLabel(text);
        final Font font = res.getFont();
        res.setFont(font.deriveFont(font.getSize2D() * 1.2f).deriveFont(Font.BOLD));
        return res;
    }

    private JPanel createSoftwareInfoPanel() {
        final JPanel res = new JPanel();
        final FormLayouter lay = new FormLayouter(res, 1);
        final ComptaPropsConfiguration conf = (ComptaPropsConfiguration) ComptaPropsConfiguration.getInstance();
        lay.add("Nom de l'application", new JLabel(conf.getAppName()));
        String version = "Version inconnue";
        try {
            version = ProductInfo.getInstance().getProps().getProperty("VERSION", version);
        } catch (Exception e) {
            System.err.println("Error reading product.properties");
        }
        lay.add("Version de l'application", new JLabel(version));
        if (conf.isUsingSSH()) {
            lay.add("Liaison sécurisée", new JLabel(conf.getWanHostAndPort()));
        }
        lay.add("URL de base de données", new JLabel(Configuration.getInstance().getSystemRoot().getDataSource().getUrl()));

        return res;
    }
}
