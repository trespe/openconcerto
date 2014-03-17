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
 
 package org.openconcerto.erp.modules;

public class DepSolverResult {

    static public interface Factory {
        DepSolverResult create(DepSolverResult parent, int tryCount, String error, DepSolverGraph graph);
    }

    private final DepSolverResult parent;
    private final int triesCount;
    private final String error;
    private final DepSolverGraph graph;

    public DepSolverResult(DepSolverResult parent, int tryCount, String error, DepSolverGraph graph) {
        super();
        this.parent = parent;
        this.triesCount = tryCount;
        this.error = error;
        this.graph = graph;
    }

    /**
     * The result that led to this one. E.g. there could have been a missing dependency for the
     * latest version of a module and thus this result contains an older version of the module.
     * 
     * @return the parent result.
     */
    public final DepSolverResult getParent() {
        return this.parent;
    }

    /**
     * The number of tries before obtaining this result.
     * 
     * @return the tries count, starts at 0.
     */
    public final int getTriesCount() {
        return this.triesCount;
    }

    public final String getError() {
        return this.error;
    }

    public final DepSolverGraph getGraph() {
        return this.graph;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + (this.getError() == null ? " no errors" : (" error : " + this.getError()));
    }
}
