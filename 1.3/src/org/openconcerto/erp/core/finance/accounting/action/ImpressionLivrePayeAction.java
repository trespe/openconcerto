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
import org.openconcerto.erp.core.finance.accounting.ui.ImpressionLivrePayePanel;

import javax.swing.Action;
import javax.swing.JFrame;

public class ImpressionLivrePayeAction extends CreateFrameAbstractAction {

    public ImpressionLivrePayeAction() {
        super();
        this.putValue(Action.NAME, "Livre paye ...");
    }

    @Override
    public JFrame createFrame() {
        final PanelFrame panelFrame = new PanelFrame(new ImpressionLivrePayePanel(), "Impression du livre de paye");
        panelFrame.pack();
        panelFrame.setResizable(false);
        return panelFrame;
    }
}
