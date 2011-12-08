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

import java.awt.event.ActionEvent;

public class ContinuousActionEvent extends ActionEvent {

    private int timesFired;

    public ContinuousActionEvent(ContinuousButtonModel model, int action_performed, String actionCommand, long mostRecentEventTime, int modifiers) {
        super(model, action_performed, actionCommand, mostRecentEventTime, modifiers);
        this.timesFired = 0;
    }

    public void fired() {
        this.timesFired++;
    }

    /**
     * Get the number of times this event has been fired.
     * 
     * @return the number of times, 0 if never has been used.
     */
    public int getTimesFired() {
        return this.timesFired;
    }

    /**
     * Returns getTimesFired(). This is the only way to transmit information through an action
     * event, since this is not this instance that will actually be delivered to the listeners but
     * rather a plain ActionEvent.
     * 
     * @return number of times this has been fired, 0 for never.
     * @see #getTimesFired()
     */
    public long getWhen() {
        return this.getTimesFired();
    }

}
