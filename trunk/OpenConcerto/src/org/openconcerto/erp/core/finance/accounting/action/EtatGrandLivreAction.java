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
 
 package org.openconcerto.erp.core.finance.accounting.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.ui.GrandLivrePanel;
import org.openconcerto.erp.model.LoadingTableListener;

import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.JFrame;

public class EtatGrandLivreAction extends CreateFrameAbstractAction {

    public EtatGrandLivreAction() {
        super();
        this.putValue(Action.NAME, "Grand livre");
    }

    public JFrame createFrame() {
        final GrandLivrePanel p = new GrandLivrePanel();
        final PanelFrame panelFrame = new PanelFrame(p, "Grand livre");
        p.addLoadingListener(new LoadingTableListener() {

            @Override
            public void isLoading(boolean b) {
                String title = "Grand livre";
                if (b) {
                    title += " (chargement en cours)";
                }
                panelFrame.setTitle(title);
            }
        });
        p.loadAsynchronous();
        final Dimension preferredSize = new Dimension(550, 400);
        p.setMinimumSize(preferredSize);
        p.setPreferredSize(preferredSize);
        return panelFrame;
    }
}
