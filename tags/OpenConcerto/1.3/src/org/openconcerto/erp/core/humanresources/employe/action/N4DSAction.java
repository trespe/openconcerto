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
 
 package org.openconcerto.erp.core.humanresources.employe.action;

import org.openconcerto.erp.core.humanresources.employe.report.N4DS;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

public class N4DSAction extends AbstractAction {

    public N4DSAction() {
        super();
        this.putValue(Action.NAME, "Déclaration N4DS 2012");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File f = new N4DS().createDocument();
        JOptionPane.showMessageDialog(null, "La déclaration N4DS a été généré dans le fichier " + f.getAbsolutePath());
    }
}
