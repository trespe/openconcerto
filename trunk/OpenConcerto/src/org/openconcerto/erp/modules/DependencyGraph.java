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
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DirectedMultigraph;

// needs multigraph (the same module can satisfy more than one dependency) and loops (avoidable but
// harmless)
class DependencyGraph extends DirectedMultigraph<ModuleFactory, DepLink> {

    private final Set<String> ids;

    public DependencyGraph() {
        super(DepLink.class);
        this.ids = new HashSet<String>();
    }

    public DependencyGraph(final DependencyGraph g) {
        this();
        for (final ModuleFactory f : g.vertexSet()) {
            this.addVertex(f);
        }
        // share links since they're immutable
        for (final DepLink l : g.edgeSet()) {
            this.addEdge(l.getSource(), l.getTarget(), l);
        }
    }

    @Override
    public boolean addVertex(ModuleFactory v) {
        final String id = v.getID();
        // simplify removeVertex()
        if (this.ids.contains(id))
            throw new IllegalStateException("ID already exists : " + v);
        final boolean res = super.addVertex(v);
        this.ids.add(id);
        return res;
    }

    @Override
    public boolean removeVertex(ModuleFactory v) {
        final boolean res = super.removeVertex(v);
        this.ids.remove(v.getID());
        return res;
    }

    public final Set<String> idSet() {
        return Collections.unmodifiableSet(this.ids);
    }

    public boolean addEdge(ModuleFactory sourceVertex, final Object depID, ModuleFactory targetVertex) {
        return super.addEdge(sourceVertex, targetVertex, new DepLink(sourceVertex, depID, targetVertex));
    }

    // will be available in 0.8.4, see https://github.com/jgrapht/jgrapht/wiki/EqualsAndHashCode

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.vertexSet().hashCode();
        result = prime * result + this.edgeSet().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DependencyGraph other = (DependencyGraph) obj;
        return this.vertexSet().equals(other.vertexSet()) && this.edgeSet().equals(other.edgeSet());
    }
}
