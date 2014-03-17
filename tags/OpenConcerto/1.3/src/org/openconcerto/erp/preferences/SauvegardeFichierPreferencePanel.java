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
 
 package org.openconcerto.erp.preferences;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.preferences.DefaultPreferencePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SauvegardeFichierPreferencePanel extends DefaultPreferencePanel {
    public SauvegardeFichierPreferencePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = 2;
        c.weightx = 1;
        final JCheckBox b = new JCheckBox("Activer la sauvegarde fichier");
        this.add(b, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        final JLabel label = new JLabel("Répertoire de sauvegarde:");
        this.add(label, c);
        c.gridx++;
        c.weightx = 1;
        final JTextField file = new JTextField("C:\\");
        this.add(file, c);
        c.weightx = 1;
        c.weighty = 1;
        this.add(new JPanel(), c);
    }

    public void storeValues() {
    }

    public void restoreToDefaults() {
    }

    public String getTitleName() {
        return "Sauvegarde fichier";
    }
}
