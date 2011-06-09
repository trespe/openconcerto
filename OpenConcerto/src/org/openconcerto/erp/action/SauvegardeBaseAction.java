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
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.preferences.BackupNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.utils.BackupPanel;

import java.io.File;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.JFrame;

public class SauvegardeBaseAction extends CreateFrameAbstractAction {

    public SauvegardeBaseAction() {
        super();
        this.putValue(Action.NAME, "Sauvegarde de la base");
    }

    @Override
    public JFrame createFrame() {
        final JFrame frame = new JFrame();
        frame.setContentPane(new BackupPanel(Arrays.asList("Common", ((ComptaPropsConfiguration) Configuration.getInstance()).getSocieteBaseName()), Arrays.asList(new File("./2010"),
                ((ComptaPropsConfiguration) Configuration.getInstance()).getDataDir()), false, BackupNXProps.getInstance()));
        frame.setTitle("Sauvegarde des données");
        // so that the application can exit
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setMinimumSize(frame.getSize());

        frame.setIconImages(Gestion.getFrameIcon());
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        return frame;
    }
}
