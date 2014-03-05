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

import org.openconcerto.erp.modules.ModuleManager.InvalidRef;
import org.openconcerto.utils.SetMap;

import java.util.List;

public final class Solutions {

    // different than no solution (i.e. null versus empty list)
    static public final Solutions EMPTY = new Solutions(null, null, null);

    private final SetMap<InvalidRef, ModuleReference> notSolvedReferences;
    private final List<ModuleReference> solvedReferences;
    private final List<DepSolverResult> solutions;

    Solutions(SetMap<InvalidRef, ModuleReference> notSolvedReferences, List<ModuleReference> solvedReferences, List<DepSolverResult> solutions) {
        super();
        this.notSolvedReferences = notSolvedReferences;
        this.solvedReferences = solvedReferences;
        this.solutions = solutions;
    }

    public final SetMap<InvalidRef, ModuleReference> getNotSolvedReferences() {
        return this.notSolvedReferences;
    }

    public final List<ModuleReference> getSolvedReferences() {
        return this.solvedReferences;
    }

    public final List<DepSolverResult> getSolutions() {
        return this.solutions;
    }
}
