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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.VFlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class DepSolverResultPanel extends JPanel {

    public DepSolverResultPanel(DepSolverResultMM depSolverResult) {
        this.setLayout(new VFlowLayout());
        final List<ModuleReference> lInstall = new ArrayList<ModuleReference>(depSolverResult.getReferencesToInstall());
        Collections.sort(lInstall, ModuleReference.COMP_ID_ASC_VERSION_DESC);
        if (lInstall.size() > 0) {
            if (lInstall.size() == 1) {
                this.add(new JLabelBold("Installation du module " + ModulePanel.format(lInstall.get(0))));
            } else {
                this.add(new JLabelBold("Modules à installer :"));
                for (ModuleReference moduleReference : lInstall) {
                    this.add(new JLabel("- " + ModulePanel.format(moduleReference)));
                }
            }

        }

        final List<ModuleReference> lRemove = new ArrayList<ModuleReference>(depSolverResult.getReferencesToRemove());
        Collections.sort(lRemove, ModuleReference.COMP_ID_ASC_VERSION_DESC);
        if (lRemove.size() > 0) {
            if (lRemove.size() == 1) {
                this.add(new JLabelBold("Désinstallation du module " + ModulePanel.format(lInstall.get(0))));
            } else {
                this.add(new JLabelBold("Modules à désinstaller :"));
                for (ModuleReference moduleReference : lRemove) {
                    this.add(new JLabel("- " + ModulePanel.format(moduleReference)));
                }
            }

        }

    }

}
