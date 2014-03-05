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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

@Immutable
final class ModulesStateChangeResult {

    private static final ModulesStateChangeResult EMPTY = onlyRemoved(Collections.<ModuleReference> emptySet());

    // no modules needed to be removed or created
    static final ModulesStateChangeResult empty() {
        return EMPTY;
    }

    static final ModulesStateChangeResult noneCreated(Set<ModuleReference> notCreated) {
        return new ModulesStateChangeResult(Collections.<ModuleReference> emptySet(), notCreated, null, Collections.<ModuleReference, AbstractModule> emptyMap());
    }

    static final ModulesStateChangeResult onlyRemoved(Set<ModuleReference> removed) {
        return new ModulesStateChangeResult(removed, Collections.<ModuleReference> emptySet(), null, Collections.<ModuleReference, AbstractModule> emptyMap());
    }

    private final Set<ModuleReference> removed, notCreated;
    private final DepSolverGraph graph;
    private final Map<ModuleReference, AbstractModule> created;

    ModulesStateChangeResult(Set<ModuleReference> removed, Set<ModuleReference> notCreated, DepSolverGraph graph, Map<ModuleReference, AbstractModule> created) {
        super();
        this.removed = Collections.unmodifiableSet(removed);
        this.notCreated = Collections.unmodifiableSet(notCreated);
        this.graph = graph;
        if (this.graph != null)
            this.graph.freeze();
        this.created = Collections.unmodifiableMap(created);
    }

    public final Set<ModuleReference> getRemoved() {
        return this.removed;
    }

    /**
     * The asked references that couldn't be created.
     * 
     * @return the references that couldn't be created (e.g. runtime error while creating the
     *         module...).
     */
    public final Set<ModuleReference> getNotCreated() {
        return this.notCreated;
    }

    /**
     * The graph solving the dependencies for the asked references.
     * 
     * @return the graph, can be <code>null</code> (i.e. nothing to create or no solution).
     */
    public final DepSolverGraph getGraph() {
        return this.graph;
    }

    /**
     * The modules that were just created.
     * 
     * @return the created modules.
     */
    public final Map<ModuleReference, AbstractModule> getCreated() {
        return this.created;
    }
}
