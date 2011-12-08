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

public class JComponentAnimator {
    protected JComponent chk;
    // public static final int[] a = { 0, 0, 64, 80, 160, 255, 255, 160, 64, 1, 1, 64, 80, 160, 255,
    // 218, 160, 64, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] a = { 0, 30, 60, 90, 120, 150, 180, 210, 240, 255, 255, 255, 240, 210, 180, 150, 120, 90, 60, 0 };
    static protected Color[] yellowBG = new Color[a.length];
    static protected Color[] yellowFG = new Color[a.length];;
    static {
        for (int i = 0; i < a.length; i++) {
            final Color bgcolor = new Color(255, 255- (a[i] / 30), 255 - (a[i] / 5));
            yellowBG[i] = bgcolor;
            final Color fgcolor = new Color(a[i], a[i]/2, 0);
            yellowFG[i] = fgcolor;
        }
    }

    protected int i = 0;
    protected int wait = 0;

    public JComponentAnimator(JComponent f) {
        chk = f;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JComponentAnimator) {
            return (((JComponentAnimator) obj).chk == chk);
        }
        return super.equals(obj);
    }

}
