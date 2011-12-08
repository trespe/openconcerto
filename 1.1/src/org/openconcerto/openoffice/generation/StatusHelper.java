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
 * Help generators to signal their progression.
 * 
 * @author Sylvain
 */
public class StatusHelper {

    private final DocumentGenerator<?> g;
    private final int count;
    private int done;

    /**
     * Create a new helper for the passed generator.
     * 
     * @param g the generator.
     * @param count the total number of work unit.
     */
    public StatusHelper(final DocumentGenerator<?> g, final int count) {
        this.g = g;
        this.count = count;
        this.done = 0;
    }

    public int workDone() {
        return this.workDone(1);
    }

    /**
     * Set the status of our generator.
     * 
     * @param size the number of work unit completed.
     * @return the new total number of work unit completed.
     */
    synchronized public int workDone(int size) {
        this.done += size;
        this.g.fireStatusChange(100 * this.done / this.count);
        return this.done;
    }

}
