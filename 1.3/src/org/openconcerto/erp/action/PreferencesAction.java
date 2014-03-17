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

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.finance.accounting.ui.ComptaPrefTreeNode;
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.preferences.PreferenceFrame;
import org.openconcerto.ui.state.WindowStateManager;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

public class PreferencesAction extends AbstractAction {

    public PreferencesAction() {
        super();
        this.putValue(Action.NAME, "Préférences");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final JFrame frame = new PreferenceFrame(new ComptaPrefTreeNode());
        frame.setIconImages(Gestion.getFrameIcon());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        // Evite les saute de Layout
        frame.setMinimumSize(new Dimension(frame.getSize()));
        frame.setLocationRelativeTo(null);
        // Charge la taille et les dimensions sauvegardées
        final String fileName = "Configuration" + File.separator + "Frame" + File.separator + this.getValue(Action.NAME).toString() + ".xml";
        final WindowStateManager stateManager = new WindowStateManager(frame, new File(Configuration.getInstance().getConfDir(), fileName), true);
        stateManager.loadState();
        frame.setVisible(true);
    }
}
