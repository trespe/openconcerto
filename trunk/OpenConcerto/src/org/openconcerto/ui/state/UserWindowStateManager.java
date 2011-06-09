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
 
 package org.openconcerto.ui.state;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Delete state if user close (windowClosing) but save state if the program dispose (windowClosed).
 * 
 * @author Sylvain CUAZ
 */
public class UserWindowStateManager extends WindowStateManager {

    public UserWindowStateManager(Window w, File f) {
        super(w, f, true);
    }

    protected WindowAdapter createListener() {
        return new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                // programmatic close
                save();
            }

            public void windowClosing(WindowEvent e) {
                // user close
                deleteState();
                endAutoSave();
            }
        };
    }

}
