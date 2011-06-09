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

import java.awt.Component;
import java.awt.Frame;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Utility class for asking the user to exit an application. If the answer is no, nothing happen,
 * otherwise a runnable is run.
 * 
 * @author Sylvain
 */
public final class UserExit {

    private final Component comp;
    private final Runnable r;

    /**
     * Creates a new instance whose runnable will dispose every frame.
     * 
     * @param comp the parentComponent of the dialog.
     */
    public UserExit(final Component comp) {
        this(comp, new Runnable() {
            public void run() {
                for (final Frame f : JFrame.getFrames()) {
                    if (f != null)
                        f.dispose();
                }
            }
        });
    }

    /**
     * Creates a new instance.
     * 
     * @param comp the parentComponent of the dialog.
     * @param r what to do when the user exits.
     */
    public UserExit(final Component comp, final Runnable r) {
        super();
        this.comp = comp;
        this.r = r;
    }

    public final void ask() {
        final int res = JOptionPane.showConfirmDialog(this.comp, "Voulez-vous vraiment quitter ?", "Quitter", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            this.r.run();
        }
    }
}
