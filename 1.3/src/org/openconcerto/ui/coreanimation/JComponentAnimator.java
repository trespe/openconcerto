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

import javax.swing.JComponent;

public abstract class JComponentAnimator {
    protected static final int[] a = { 0, 30, 60, 90, 120, 150, 180, 210, 240, 255, 255, 255, 240, 210, 180, 150, 120, 90, 60, 0 };

    protected final JComponent chk;
    private int i = 0;
    private int wait = 0;

    public JComponentAnimator(JComponent f) {
        this.chk = f;
    }

    public final void pulse() {
        if (this.wait > 0) {
            if (this.wait > 4) {
                this.wait = -1;
            }
            this.wait++;
        } else {
            setColor(this.i);
            this.i++;
            if (this.i >= a.length) {
                this.i = 0;
                this.wait++;
            }
        }
    }

    abstract protected void setColor(int i);

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JComponentAnimator) {
            return (((JComponentAnimator) obj).chk == this.chk);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.chk.hashCode();
    }
}
