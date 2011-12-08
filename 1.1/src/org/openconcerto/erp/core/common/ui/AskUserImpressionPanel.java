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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// FIXME si OO est lancé et que l'utilisateur ferme le document avant de lancer l'impression alors
// Exception --> le bridge n'existe plus entre le doc et OO
public class AskUserImpressionPanel extends JPanel {

    public AskUserImpressionPanel(final String docURL, final String printerName, final Map<String, Object> propsVal) {
        super();
        final File f = new File(docURL);

        final JLabel labelConfirm = new JLabel("Impression du document " + f.getName() + " sur " + printerName);
        final JButton buttonLancer = new JButton("Lancer");
        final JButton buttonAnnul = new JButton("Annuler");

        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());

        c.gridy = 0;
        c.gridx = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 2, 1, 2);
        c.anchor = GridBagConstraints.CENTER;
        this.add(labelConfirm, c);

        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        JPanel panel = new JPanel();
        panel.add(buttonLancer);
        panel.add(buttonAnnul, c);
        this.add(panel, c);

        buttonAnnul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                ((JFrame) SwingUtilities.getRoot(AskUserImpressionPanel.this)).dispose();
            }
        });

        buttonLancer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                    if (ooConnexion == null) {
                        return;
                    }
                    final Component doc = ooConnexion.loadDocument(f, true);
                    // doc.launchPrint(propsVal, null);
                    doc.close();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                ((JFrame) SwingUtilities.getRoot(AskUserImpressionPanel.this)).dispose();
            }
        });

    }
}
