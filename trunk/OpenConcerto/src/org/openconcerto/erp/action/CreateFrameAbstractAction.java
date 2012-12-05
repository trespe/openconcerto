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
import org.openconcerto.sql.Configuration;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.state.WindowStateManager;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

public abstract class CreateFrameAbstractAction extends AbstractAction {
    protected boolean mustLoadState = true;

    protected CreateFrameAbstractAction() {
        super();
    }

    protected CreateFrameAbstractAction(String name) {
        super(name);
    }

    public void actionPerformed(ActionEvent e) {
        final JFrame frame = createFrame();
        frame.setIconImages(Gestion.getFrameIcon());

        final Object name = this.getValue(Action.NAME);
        WindowStateManager stateManager = null;
        if (name != null) {
            stateManager = new WindowStateManager(frame, new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator + name.toString() + ".xml"),
                    true);
        } else {
            System.err.println("Warning: no action name for action " + this + ", unable to use a window state manager.");
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(d.width - 100, frame.getWidth());
        int h = Math.min(d.height - 100, frame.getHeight());
        frame.setMinimumSize(new Dimension(w, h));
        frame.setLocationRelativeTo(null);
        if (mustLoadState && stateManager != null) {
            stateManager.loadState();
        }
        FrameUtil.show(frame);

    }

    abstract public JFrame createFrame();
}
