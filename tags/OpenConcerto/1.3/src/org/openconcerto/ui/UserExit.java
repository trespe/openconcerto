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
import java.awt.Window;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Utility class for asking the user to exit an application. If the answer is no, nothing happen,
 * otherwise a runnable is run.
 * 
 * @author Sylvain
 */
public final class UserExit {

    static public void closeAllWindows(final Window last) {
        // dispose of every window (this will call windowClosed())
        for (final Window f : Window.getWindows()) {
            // check isDisplayable() to avoid triggering a second HierarchyEvent with the same
            // displayability which can confuse some listeners
            if (f != last && f.isDisplayable())
                f.dispose();
        }
        // this is last to exit
        if (last != null && last.isDisplayable())
            last.dispose();
    }

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
                closeAllWindows(comp == null ? null : SwingUtilities.getWindowAncestor(comp));
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
        final int res = JOptionPane.showConfirmDialog(this.comp, TM.tr("userExit.question"), TM.tr("userExit.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            this.r.run();
        }
    }
}
