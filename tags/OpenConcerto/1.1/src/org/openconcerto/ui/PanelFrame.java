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
 
 package org.openconcerto.ui;

import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;

public class PanelFrame extends JFrame {

    public PanelFrame(Container p, String titre) {
        this(p, titre, false);
    }

    public PanelFrame(Container p, String titre, final boolean setBounds) {
        super();

        this.setContentPane(p);
        this.setTitle(titre);
        if (setBounds)
            this.setBounds();
        this.pack();
    }

    final protected void setBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final Rectangle dm = new Rectangle(ge.getMaximumWindowBounds());
        // don't use ge.getDefaultScreenDevice().getDisplayMode();
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6921661

        // if there's room, don't maximize
        if (dm.getWidth() > 800 && dm.getHeight() > 600) {
            dm.x += 10;
            dm.width -= 50;
            dm.height -= 20;
        }
        // on Ubuntu getMaximumWindowBounds() ignores menu bars
        final int topOffset = 50;
        dm.height -= topOffset;
        dm.y += topOffset;

        this.setBounds(dm);
    }
}
