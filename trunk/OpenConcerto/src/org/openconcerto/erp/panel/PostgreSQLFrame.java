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

import org.openconcerto.utils.JImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PostgreSQLFrame extends JFrame {
    public PostgreSQLFrame(String title) {
        this.setUndecorated(true);
        this.setBackground(Color.white);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.white);
        p.add(new JImage(PostgreSQLFrame.class.getResource("logo_postgresql.png")), BorderLayout.CENTER);
        p.add(new JLabel(title), BorderLayout.SOUTH);
        this.setContentPane(p);
        this.pack();
        this.setResizable(false);
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        // Get the current screen size
        Dimension scrnsize = toolkit.getScreenSize();
        this.setLocation(scrnsize.width - this.getWidth(), 0);
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dispose();
            }
        });
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public static void main(String[] args) {
        PostgreSQLFrame f = new PostgreSQLFrame("Démarrage");
        f.setVisible(true);
    }
}
