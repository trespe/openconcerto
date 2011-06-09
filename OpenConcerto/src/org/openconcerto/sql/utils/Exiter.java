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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.Configuration;

import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Close all windows and exit.
 * 
 * @author Sylvain CUAZ
 */
public class Exiter {

    private final Window main;

    /**
     * Create a new instance.
     * 
     * @param main the window that will be closed last.
     * @param onClosing <code>true</code> if the {@link WindowListener#windowClosing(WindowEvent)
     *        closing} of <code>main</code> should call {@link #closeAll()}.
     */
    public Exiter(final Window main, final boolean onClosing) {
        super();
        this.main = main;
        this.main.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (SystemTray.isSupported()) {
                    final SystemTray tray = SystemTray.getSystemTray();
                    for (final TrayIcon icon : tray.getTrayIcons())
                        tray.remove(icon);
                }

                Configuration.getInstance().destroy();

                // shouldn't be needed but just in case
                System.exit(0);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (onClosing)
                    closeAll();
            }
        });
    }

    public void closeAll() {
        // dispose of every window (this will call windowClosed())
        for (final Window f : Frame.getWindows()) {
            if (f != this.main)
                f.dispose();
        }
        // this is last to exit
        this.main.dispose();
    }
}
