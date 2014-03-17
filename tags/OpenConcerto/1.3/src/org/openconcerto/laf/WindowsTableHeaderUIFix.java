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
 
 package org.openconcerto.laf;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.JTableHeader;

import com.sun.java.swing.plaf.windows.WindowsTableHeaderUI;

public class WindowsTableHeaderUIFix extends WindowsTableHeaderUI {
    // Fix for http://bugs.sun.com/view_bug.do?bug_id=6516888
    // ie null header when switching L&F, resuming from locked sessions, or opening/closing RDP
    // sessions on Windows...

    public static ComponentUI createUI(JComponent h) {
        return new WindowsTableHeaderUIFix();
    }

    @Override
    public void uninstallUI(JComponent c) {
        JTableHeader old = header;
        super.uninstallUI(c);
        header = old;
    }

}
