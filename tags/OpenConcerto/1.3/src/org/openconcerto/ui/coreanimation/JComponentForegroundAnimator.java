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

    static private final Color[] COLORS = new Color[a.length];
    static {
        for (int i = 0; i < a.length; i++) {
            COLORS[i] = new Color(a[i], a[i] / 2, 0);
        }
    }

    private final Color fgColor;

    public JComponentForegroundAnimator(final JComponent f) {
        super(f);
        this.fgColor = f.isForegroundSet() ? f.getForeground() : null;
    }

    @Override
    protected void setColor(int i) {
        this.chk.setForeground(COLORS[i]);
    }

    @Override
    public String toString() {
        return "FGA:" + this.chk.getClass();
    }

    @Override
    public void resetState() {
        this.chk.setForeground(this.fgColor);
    }
}
