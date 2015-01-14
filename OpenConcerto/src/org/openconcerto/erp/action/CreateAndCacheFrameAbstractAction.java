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
import org.openconcerto.utils.FileUtils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;

public abstract class CreateAndCacheFrameAbstractAction extends AbstractAction {

    private JFrame frame;
    WindowStateManager stateManager;

    public void actionPerformed(ActionEvent e) {
        if (this.frame == null) {
            this.frame = createFrame();
            this.frame.setLocationRelativeTo(null);
            this.stateManager = new WindowStateManager(this.frame, new File(Configuration.getInstance().getConfDir(), "Configuration" + File.separator + "Frame" + File.separator
                    + FileUtils.sanitize(this.getValue(Action.NAME).toString()) + ".xml"));
            this.frame.setIconImages(Gestion.getFrameIcon());
            this.frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
        this.frame.pack();
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(d.width - 100, this.frame.getWidth());
        int h = Math.min(d.height - 100, this.frame.getHeight());
        this.frame.setMinimumSize(new Dimension(w, h));
        FrameUtil.show(this.frame);
        this.stateManager.loadState();
    }

    abstract public JFrame createFrame();
}
