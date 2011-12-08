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
 
 package org.openconcerto.ui.coreanimation;

import java.awt.Color;

import javax.swing.JComponent;

public class JComponentForegroundAnimator extends JComponentAnimator implements Pulse {
    private final Color fgColor;

    public JComponentForegroundAnimator(final JComponent f) {
        super(f);
        fgColor = f.getForeground();
    }

    public void pulse() {
        if (wait > 0) {
            if (wait > 4) {
                wait = -1;
            }
            wait++;
        } else {
            chk.setForeground(yellowFG[i]);
            i++;
            if (i >= JComponentBackGroundAnimator.a.length) {
                i = 0;
                wait++;
            }
        }
    }

    @Override
    public String toString() {
        return "FGA:" + this.chk.getClass();
    }

    public void resetState() {
        chk.setForeground(fgColor);
    }
}
