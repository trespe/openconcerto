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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How each dependency is solved.
 * 
 * @author Sylvain
 * @see #getDependencies(ModuleFactory)
 * @see #flatten()
 */
public final class DepSolverGraph {

    public static enum NodeState {
        NOT_SOLVING, SOLVING, SOLVED
    };

    private final ModuleFactory root;
    private final boolean virtualRoot;
    private final List<String> rootIDs;
    private final DependencyGraph graph;
    private final Set<ModuleFactory> solving;
    private final Set<ModuleFactory> solved;

    private boolean frozen;

    {
        // by definition a new instance isn't solving anything
        this.solving = new HashSet<ModuleFactory>();
        // a new instance is always mutable
        this.frozen = false;
    }

    DepSolverGraph(final ModuleFactory root, final boolean virtualRoot, final DependencyGraph currentGraph) {
        if (root == null)
            throw new NullPointerException("Null root");
        this.root = root;
        this.virtualRoot = virtualRoot;
        if (!virtualRoot) {
            this.rootIDs = Collections.singletonList(this.root.getID());
        } else {
            final List<String> l = new ArrayList<String>();
            for (final Dependency d : this.root.getDependencies().values()) {
                l.addAll(d.getRequiredIDs());
            }
            this.rootIDs = Collections.unmodifiableList(l);
        }

        this.graph = new DependencyGraph(currentGraph);
        this.solved = new HashSet<ModuleFactory>(this.graph.vertexSet());
    }

    private DepSolverGraph(final DepSolverGraph o) {
        this.root = o.root;
        this.virtualRoot = o.virtualRoot;
        this.rootIDs = o.rootIDs;

        this.graph = new DependencyGraph(o.graph);
        this.solved = new HashSet<ModuleFactory>(o.solved);

        // since this.solving is empty, remove them from our graph
        for (final ModuleFactory f : o.solving) {
            // at start this.graph contains all o.solving but removeRec is recursive
            if (this.graph.vertexSet().contains(f))
                this.removeRec(f);
        }
    }

    final DepSolverGraph copyAndRemove(final Set<ModuleFactory> toRemove) {
        final DepSolverGraph res = new DepSolverGraph(this);
        for (final ModuleFactory f : toRemove) {
            if (res.graph.vertexSet().contains(f))
                res.removeRec(f);
        }
        return res;
    }

    private void removeRec(ModuleFactory f) {
        this.checkFrozen();
        this.solved.remove(f);
        this.solving.remove(f);
        // copy live view
        final Set<DepLink> incomingEdges = new HashSet<DepLink>(this.graph.incomingEdgesOf(f));
        this.graph.removeVertex(f);
        for (final DepLink l : incomingEdges) {
            final ModuleFactory src = l.getSource();
            assert l.getTarget() == f;
            this.removeRec(src);
        }
    }

    public void freeze() {
        this.frozen = true;
    }

    public final boolean isFrozen() {
        return this.frozen;
    }

    private final void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("this has been frozen: " + this);
    }

    public final List<String> getRootIDs() {
        return this.rootIDs;
    }

    public final Set<String> getIDs() {
        return this.graph.idSet();
    }

    public final void addSolving(final ModuleFactory src, final Object depID, final ModuleFactory target) {
        this.checkFrozen();

        if (src != null && this.getState(src) != NodeState.SOLVING)
            throw new IllegalArgumentException("Source isn't solving : " + src);
        if (this.getState(target) != NodeState.NOT_SOLVING)
            throw new IllegalArgumentException("Target isn't NOT_SOLVING : " + target);
        this.solving.add(target);
        this.graph.addVertex(target);
        if (src != null) {
            this.graph.addEdge(src, depID, target);
        } else {
            assert target == this.root;
        }
    }

    public final void setSolved(final ModuleFactory f) {
        this.checkFrozen();

        if (this.getState(f) != NodeState.SOLVING)
            throw new IllegalArgumentException("Not SOLVING : " + f);
        this.solving.remove(f);
        this.solved.add(f);
    }

    public final void addDependency(final ModuleFactory src, final Object depID, final ModuleFactory target) {
        this.checkFrozen();

        if (this.getState(src) != NodeState.SOLVING)
            throw new IllegalArgumentException("Source isn't solving : " + src);
        if (this.getState(target) != NodeState.SOLVED)
            throw new IllegalArgumentException("Target isn't SOLVED : " + target);
        this.graph.addEdge(src, depID, target);
    }

    public final NodeState getState(ModuleFactory f) {
        if (f == null)
            throw new NullPointerException();
        if (!this.graph.containsVertex(f))
            return NodeState.NOT_SOLVING;
        else if (this.solved.contains(f))
            return NodeState.SOLVED;
        else if (this.solving.contains(f))
            return NodeState.SOLVING;
        else
            throw new IllegalStateException("Factory is in graph but neither solving nor solved : " + f);
    }

    /**
     * Whether all nodes are solved.
     * 
     * @return <code>true</code> if all nodes are {@link NodeState#SOLVED}.
     * @see #getState(ModuleFactory)
     */
    public final boolean isSolved() {
        return this.graph.vertexSet().size() == this.solved.size();
    }

    public final Set<ModuleFactory> getConflicts(ModuleFactory f) {
        final Set<ModuleFactory> res = new HashSet<ModuleFactory>();
        for (final ModuleFactory in : this.graph.vertexSet()) {
            if (f.conflictsWith(in))
                res.add(in);
        }
        return res;
    }

    public final ModuleFactory getPreviousSolving(final ModuleFactory f) {
        if (this.getState(f) != NodeState.SOLVING)
            throw new IllegalArgumentException("Not SOLVING : " + f);
        final Set<DepLink> incomingEdges = this.graph.incomingEdgesOf(f);
        if (incomingEdges.size() != 1)
            throw new IllegalStateException("Not 1 previous : " + incomingEdges);
        final ModuleFactory res = incomingEdges.iterator().next().getSource();
        return this.getState(res) == NodeState.SOLVING ? res : null;
    }

    public ModuleFactory getDependency(final ModuleFactory f, final Object id) {
        for (final DepLink l : this.graph.outgoingEdgesOf(f)) {
            if (l.getDepID().equals(id))
                return l.getTarget();
        }
        return null;
    }

    /**
     * Return factories by dependencies ID.
     * 
     * @param f a factory.
     * @return its current dependencies.
     */
    public final Map<Object, ModuleFactory> getDependencies(final ModuleFactory f) {
        return this.getDependencies(f, null);
    }

    public final Map<Object, ModuleFactory> getDependencies(final ModuleFactory f, final NodeState state) {
        final Set<DepLink> outgoingEdges = this.graph.outgoingEdgesOf(f);
        final Map<Object, ModuleFactory> res = new HashMap<Object, ModuleFactory>(outgoingEdges.size());
        for (final DepLink l : outgoingEdges) {
            if (state == null || getState(l.getTarget()) == state)
                res.put(l.getDepID(), l.getTarget());
        }
        return res;
    }

    /**
     * Flatten the graph to installation order.
     * 
     * @return all factories in installation order.
     * @throws IllegalStateException if the graph isn't all {@link #isSolved() solved}.
     */
    public List<ModuleFactory> flatten() throws IllegalStateException {
        if (!this.isSolved())
            throw new IllegalStateException("Not all solved");
        final List<ModuleFactory> res = new ArrayList<ModuleFactory>(this.solved.size());
        this.walk(this.root, res);
        assert this.root == res.get(res.size() - 1);
        if (this.virtualRoot)
            res.remove(res.size() - 1);
        return res;
    }

    private void walk(final ModuleFactory node, final List<ModuleFactory> res) {
        if (!res.contains(node)) {
            for (final DepLink l : this.graph.outgoingEdgesOf(node)) {
                this.walk(l.getTarget(), res);
            }
            res.add(node);
        }
    }

    public final Set<ModuleFactory> getFactories() {
        return getFactories(false);
    }

    public final Set<ModuleFactory> getFactories(final boolean includeVirtual) {
        Set<ModuleFactory> res = this.graph.vertexSet();
        if (this.virtualRoot && !includeVirtual) {
            res = new HashSet<ModuleFactory>(res);
            res.remove(this.root);
        }
        return Collections.unmodifiableSet(res);
    }

    @Override
    public String toString() {
        final String rootS = this.root + (this.virtualRoot ? " (virtual)" : "");
        final String solvingS = "\nSolving  " + this.solving;
        final String solvedS = "\nSolved : " + this.solved;
        final String graphS = "\nGraph : " + this.graph;
        return this.getClass().getSimpleName() + " " + rootS + solvedS + solvingS + graphS;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.graph.hashCode();
        result = prime * result + this.root.hashCode();
        result = prime * result + (this.virtualRoot ? 1231 : 1237);
        result = prime * result + this.solved.hashCode();
        // don't bother with solving, it's transient
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
        final DepSolverGraph other = (DepSolverGraph) obj;
        return this.root.equals(other.root) && this.virtualRoot == other.virtualRoot && this.solved.equals(other.solved) && this.solving.equals(other.solving) && this.graph.equals(other.graph);
    }
}
