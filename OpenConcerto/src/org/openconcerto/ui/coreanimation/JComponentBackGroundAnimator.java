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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;

public class JComponentBackGroundAnimator extends JComponentAnimator implements Pulse {

    static private final Color[] COLORS = new Color[a.length];
    static {
        for (int i = 0; i < a.length; i++) {
            COLORS[i] = new Color(255, 255 - (a[i] / 30), 255 - (a[i] / 5));
        }
    }

    private final Color bgColor;
    private final Boolean opaque;

    public JComponentBackGroundAnimator(JComponent f) {
        this(f, false);
    }

    public JComponentBackGroundAnimator(JComponent f, final boolean setOpaque) {
        super(f);
        // FIXME the background might not be what we expect, e.g. here the component is disabled and
        // return grey, but in resetState() is enabled and should be white
        this.bgColor = f.isBackgroundSet() ? f.getBackground() : null;
        if (setOpaque) {
            this.opaque = f.isOpaque();
            f.setOpaque(true);
        } else {
            this.opaque = null;
        }
    }

    @Override
    protected void setColor(int i) {
        this.chk.setBackground(COLORS[i]);
    }

    @Override
    public String toString() {
        return "BGA:" + this.chk.getClass();
    }

    @Override
    public void resetState() {
        // FIXME see constructor, here reset the possibly UIResource background
        this.chk.setBackground(this.bgColor);

        boolean hadFocus = this.chk.hasFocus();
        // then force refresh, so that ComponentUI has a chance to set the correct background
        if (this.chk.isEnabled()) {
            this.chk.setEnabled(false);
            this.chk.setEnabled(true);
        } else {
            this.chk.setEnabled(true);
            this.chk.setEnabled(false);
        }
        if (this.opaque != null)
            this.chk.setOpaque(this.opaque);

        // MAYBE use setEditable()
        if (hadFocus) {
            // Megatrick to avoid focus listeners to do bad things like selecting text...
            final FocusListener[] listeners = this.chk.getFocusListeners();
            for (int i = 0; i < listeners.length; i++) {
                FocusListener focusListener = listeners[i];
                this.chk.removeFocusListener(focusListener);
            }

            this.chk.addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent e) {

                }

                @Override
                public void focusGained(FocusEvent e) {
                    JComponentBackGroundAnimator.this.chk.removeFocusListener(this);
                    for (int i = 0; i < listeners.length; i++) {
                        FocusListener focusListener = listeners[i];
                        JComponentBackGroundAnimator.this.chk.addFocusListener(focusListener);
                    }

                }
            });
            this.chk.requestFocus();

        }
    }
}
