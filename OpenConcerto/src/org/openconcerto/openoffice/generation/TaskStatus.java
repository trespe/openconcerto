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
 
 package org.openconcerto.openoffice.generation;

/**
 * The status of a task, it can be not (yet) started, working and done ; additionaly it contains a
 * degree of completion. To retrieve an instance either use the constants for the "bounds" or use
 * {@link #create(float)}.
 * 
 * @author Sylvain
 */
public final class TaskStatus {

    public static enum State {
        NOT_STARTED, WORKING, DONE
    }

    public static final TaskStatus NOT_STARTED = new TaskStatus(State.NOT_STARTED, 0);
    public static final TaskStatus STARTED = new TaskStatus(State.WORKING, 0);
    public static final TaskStatus DONE = new TaskStatus(State.DONE, 1);

    /**
     * Creates a new working status with the passed completion.
     * 
     * @param completion a float between 0 and 1, 1 meaning "done".
     * @return a new status.
     */
    static TaskStatus create(float completion) {
        return new TaskStatus(State.WORKING, completion);
    }

    private final State s;
    private final float completion;

    private TaskStatus(State s, float completion) {
        this.s = s;
        if (completion < 0 || completion > 1)
            throw new IllegalArgumentException(completion + " is not between 0 and 1");
        this.completion = completion;
    }

    public final float getCompletion() {
        return this.completion;
    }

    public final State getState() {
        return this.s;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaskStatus) {
            final TaskStatus t = (TaskStatus) obj;
            return t.getState().equals(this.getState()) && t.getCompletion() == this.completion;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.s.hashCode() + (int) (this.completion * 100);
    }

    @Override
    public String toString() {
        return this.s + " " + this.completion;
    }
}
