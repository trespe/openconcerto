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

import org.openconcerto.ui.ReloadPanel;

import java.awt.FlowLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class InProgressFrame extends JDialog {
    public InProgressFrame() {

    }

    public void show(String title) {
        setTitle("Veuillez patienter");
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
        final ReloadPanel rlPanel = new ReloadPanel();
        rlPanel.setMode(ReloadPanel.MODE_ROTATE);
        p.add(new JLabel(title));
        p.add(rlPanel);
        setContentPane(p);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
