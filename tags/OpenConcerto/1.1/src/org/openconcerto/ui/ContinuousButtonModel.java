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

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

import javax.swing.DefaultButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A model that continuously fire action event, while armed. It behaves like the knobs of a
 * scrollbar.
 * 
 * @author Sylvain
 */
public class ContinuousButtonModel extends DefaultButtonModel {

    public static final ContinuousButtonModel createAccel(int delay) {
        return createAccel(delay, (short) 8);
    }

    public static final ContinuousButtonModel createAccel(int delay, short factor) {
        return new ContinuousButtonModel(delay, delay / factor, delay / factor);
    }

    private Thread thread;

    public ContinuousButtonModel() {
        this(200);
    }

    public ContinuousButtonModel(int delay) {
        this(delay, 0, delay);
    }

    /**
     * Create a new instance.
     * 
     * @param delay the initial delay between repetition.
     * @param accel the delay will be diminished by this value at each repetition.
     * @param min the minimum delay.
     */
    public ContinuousButtonModel(final int delay, final int accel, final int min) {
        super();
        this.thread = null;
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                // the state comprises more than just armed & pressed
                // so this isn't just called when these change (eg also when rollover changes)
                if (!isArmed()) {
                    stop();
                }
                if (isArmed() && isPressed()) {
                    launch(delay, accel, min);
                }
            }
        });
    }

    private ContinuousActionEvent createEvent() {
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent) currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent) currentEvent).getModifiers();
        }
        return new ContinuousActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(), EventQueue.getMostRecentEventTime(), modifiers);
    }

    /**
     * Overload to prevent an event from being fired.
     * 
     * @param b true to set the button to "pressed"
     * @see javax.swing.ButtonModel#setPressed(boolean)
     */
    public void setPressed(boolean b) {
        if ((isPressed() == b) || !isEnabled()) {
            return;
        }

        if (b) {
            this.stateMask |= PRESSED;
        } else {
            this.stateMask &= ~PRESSED;
        }

        fireStateChanged();
    }

    protected synchronized final void launch(int delay, int accel, int min) {
        if (this.thread != null) {
            // see stateChanged() as to why this can be called when already launched
            Log.get().finest("previous thread was not stopped: " + this.thread);
            return;
        }
        // don't create event each time
        final ContinuousActionEvent evt = createEvent();
        this.thread = new DelayTimer(new Runnable() {
            public void run() {
                evt.fired();
                fireActionPerformed(evt);
            }
        }, delay, accel, min);
        this.thread.start();
    }

    protected synchronized final void stop() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
            // SWINGFIXME de-press otherwise it might stay pressed : if you activate one button with
            // the space bar and at the same time click on another one, the first one will remain
            // pressed even after you release the mouse.
            this.setPressed(false);
        }
    }

}
