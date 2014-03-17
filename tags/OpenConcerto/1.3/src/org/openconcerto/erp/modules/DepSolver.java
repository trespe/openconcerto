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

import org.openconcerto.erp.modules.DepSolverGraph.NodeState;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.IPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;

final class DepSolver {

    static private final Logger L = ModuleManager.L;

    static private final class Work extends Tuple3<DepSolverResult, FactoriesByID, DepSolverGraph> {

        public Work(DepSolverResult a, FactoriesByID b, DepSolverGraph c) {
            super(a, b, c);
        }

    }

    private int maxSuccess;
    private DepSolverResult.Factory resultFactory;
    private IPredicate<? super DepSolverResult> resultPred;

    @GuardedBy("this")
    private final Deque<Work> toTry = new LinkedList<Work>();
    @GuardedBy("this")
    private final LinkedHashMap<FactoriesByID, DepSolverResult> tried = new LinkedHashMap<FactoriesByID, DepSolverResult>();

    public DepSolver() {
        this.maxSuccess = 1;
        this.setResultFactory(null);
        this.setResultPredicate(null);
    }

    public final DepSolver setMaxSuccess(int maxSuccess) {
        if (maxSuccess < 1 || maxSuccess > 100)
            throw new IllegalArgumentException("Max success : " + maxSuccess);
        this.maxSuccess = maxSuccess;
        return this;
    }

    public final int getMaxSuccess() {
        return this.maxSuccess;
    }

    public final DepSolver setResultPredicate(final IPredicate<? super DepSolverResult> resultPred) {
        // eclipse bug, cannot use ternary operator
        if (resultPred == null)
            this.resultPred = IPredicate.<DepSolverResult> truePredicate();
        else
            this.resultPred = resultPred;
        return this;
    }

    public final DepSolver setResultFactory(final DepSolverResult.Factory resultFactory) {
        this.resultFactory = resultFactory;
        return this;
    }

    private final DepSolverResult createResult(DepSolverResult parent, int tryCount, String error, DepSolverGraph graph) {
        if (this.resultFactory == null)
            return new DepSolverResult(parent, tryCount, error, graph);
        else
            return this.resultFactory.create(parent, tryCount, error, graph);
    }

    final List<DepSolverResult> solve(final FactoriesByID pool, final DependencyGraph currentGraph, final List<ModuleReference> refs) {
        final List<Dependency> deps = new ArrayList<Dependency>(refs.size());
        for (final ModuleReference ref : refs) {
            deps.add(Dependency.createFromReference(ref));
        }
        final ModuleFactory virtualFactory = new VirtualModuleFactory(new ModuleReference(this.getClass().getName() + ".virtual", ModuleVersion.MIN), this.getClass().getName(), deps);
        return this.solve(pool, currentGraph, virtualFactory, true);
    }

    final List<DepSolverResult> solve(final FactoriesByID pool, final DependencyGraph currentGraph, final ModuleFactory factory) {
        return this.solve(pool, currentGraph, factory, false);
    }

    /**
     * Find solutions for the passed factory.
     * 
     * @param pool the available factories.
     * @param currentGraph the current already solved state.
     * @param factory the factory to solve.
     * @param virtualRoot <code>true</code> if <code>factory</code> is just there to allow solving
     *        multiple factories at once or to solve a module without a specific version.
     * @return the solutions, between 0 and {@link #getMaxSuccess()}.
     */
    synchronized final List<DepSolverResult> solve(final FactoriesByID pool, final DependencyGraph currentGraph, final ModuleFactory factory, final boolean virtualRoot) {
        if (factory == null)
            throw new NullPointerException("Null factory");
        if (this.maxSuccess == 0)
            return Collections.emptyList();
        if (!virtualRoot && !pool.contains(factory))
            throw new IllegalArgumentException("Factory not in pool : " + factory);

        this.toTry.clear();
        this.toTry.add(new Work(null, pool, new DepSolverGraph(factory, virtualRoot, currentGraph)));
        this.tried.clear();
        final LinkedHashMap<DepSolverGraph, DepSolverResult> res = new LinkedHashMap<DepSolverGraph, DepSolverResult>();
        final int tryCount = 15000;
        Work current;
        while (res.size() < this.maxSuccess && this.tried.size() < tryCount && (current = this.toTry.poll()) != null) {
            final FactoriesByID factories = current.get1();
            if (!this.tried.containsKey(factories)) {
                final DepSolverResult parentRes = current.get0();
                if (L.isLoggable(Level.FINER)) {
                    final String reason = parentRes == null ? "" : "Because of " + parentRes + "\n";
                    L.finer(reason + "Trying " + factories + "\nwith " + current.get2());
                }
                final DepSolverResult solve = solve(parentRes, this.tried.size(), factories, current.get2(), null, null, factory);
                solve.getGraph().freeze();
                // different pools can yield the same result
                if (solve.getError() == null && this.resultPred.evaluateChecked(solve) && !res.containsKey(solve.getGraph()))
                    res.put(solve.getGraph(), solve);
                this.tried.put(factories, solve);
            }
        }
        return new ArrayList<DepSolverResult>(res.values());
    }

    /**
     * The total number of tries before returning from solve().
     * 
     * @return the number of tries.
     */
    synchronized final int getTriedCount() {
        return this.tried.size();
    }

    private final void addWork(final DepSolverResult parent, FactoriesByID factories, final DepSolverGraph graph, final Set<ModuleFactory> toRemove) {
        if (!this.tried.containsKey(factories)) {
            factories = new FactoriesByID(factories);
            factories.removeAll(toRemove);
            this.toTry.add(new Work(parent, factories, graph.copyAndRemove(toRemove)));
        }
    }

    private final DepSolverResult solve(final DepSolverResult parent, int tryCount, final FactoriesByID factories, final DepSolverGraph graph, final ModuleFactory requiredByFactory,
            final Object requiredByDep, final ModuleFactory f) {
        final NodeState state = graph.getState(f);
        if (state == NodeState.SOLVED) {
            if (requiredByFactory != null)
                graph.addDependency(requiredByFactory, requiredByDep, f);
            return createResult(parent, tryCount, null, graph);
        } else if (state == NodeState.SOLVING) {
            final DepSolverResult cycleRes = createResult(parent, tryCount, "cycle", graph);
            // for each of the factories causing the cycle, retry without it
            ModuleFactory current = requiredByFactory;
            while (current != f) {
                this.addWork(cycleRes, factories, graph, Collections.singleton(current));
                current = graph.getPreviousSolving(current);
            }
            this.addWork(cycleRes, factories, graph, Collections.singleton(current));
            return cycleRes;
        }
        assert state == NodeState.NOT_SOLVING;
        final Set<ModuleFactory> conflicts = graph.getConflicts(f);
        if (conflicts.size() > 0) {
            final DepSolverResult conflictRes = createResult(parent, tryCount, "conflict", graph);

            // first try to keep existing nodes (they might already be installed)

            // keep conflicts and remove their conflicts
            final Set<ModuleFactory> toRemove = new HashSet<ModuleFactory>();
            for (final SortedMap<ModuleVersion, ModuleFactory> m : factories.getMap().values()) {
                for (final ModuleFactory mf : m.values()) {
                    boolean conflictFound = false;
                    final Iterator<ModuleFactory> iter = conflicts.iterator();
                    while (iter.hasNext() && !conflictFound) {
                        final ModuleFactory conflict = iter.next();
                        conflictFound = conflict.conflictsWith(mf);
                    }
                    if (conflictFound)
                        toRemove.add(mf);
                }
            }
            assert toRemove.contains(f);
            this.addWork(conflictRes, factories, graph, toRemove);

            // keep f and remove its conflicts
            toRemove.clear();
            for (final SortedMap<ModuleVersion, ModuleFactory> m : factories.getMap().values()) {
                for (final ModuleFactory mf : m.values()) {
                    if (f.conflictsWith(mf))
                        toRemove.add(mf);
                }
            }
            assert !toRemove.contains(f);
            this.addWork(conflictRes, factories, graph, toRemove);

            // for each conflict remove it
            this.addWork(conflictRes, factories, graph, Collections.singleton(f));
            for (final ModuleFactory conflict : conflicts) {
                this.addWork(conflictRes, factories, graph, Collections.singleton(conflict));
            }

            // remove conflicts and f
            toRemove.clear();
            toRemove.addAll(conflicts);
            toRemove.add(f);
            this.addWork(conflictRes, factories, graph, toRemove);

            return conflictRes;
        }
        graph.addSolving(requiredByFactory, requiredByDep, f);
        for (final Entry<Object, Dependency> e : f.getDependencies().entrySet()) {
            final Object depID = e.getKey();
            final Dependency dep = e.getValue();
            final ModuleFactory dependency = graph.getDependency(f, depID);
            assert dependency == null || graph.getState(dependency) == NodeState.SOLVED;
            // check that we didn't already solve it previously
            if (dependency == null) {

                // try first with IDs already in graph
                final Set<String> graphIDs = graph.getIDs();
                // or explicitly asked
                final Set<String> rootIDs = new HashSet<String>(graph.getRootIDs());

                final List<String> inGraph = new LinkedList<String>();
                final List<String> inRoots = new LinkedList<String>();
                final List<String> nowhere = new LinkedList<String>();
                for (final String requiredID : dep.getRequiredIDs()) {
                    final List<String> l;
                    if (graphIDs.contains(requiredID))
                        l = inGraph;
                    else if (rootIDs.contains(requiredID))
                        l = inRoots;
                    else
                        l = nowhere;
                    l.add(requiredID);
                }
                final List<String> requiredIDs = inGraph;
                requiredIDs.addAll(inRoots);
                requiredIDs.addAll(nowhere);

                ModuleFactory found = null;
                for (final String reqID : requiredIDs) {
                    final Deque<ModuleFactory> candidates = new LinkedList<ModuleFactory>(factories.getVersions(reqID).values());
                    while (found == null && candidates.size() > 0) {
                        final ModuleFactory current = candidates.remove();
                        assert current != null;
                        if (dep.isRequiredFactoryOK(current)) {
                            final DepSolverResult recRes = solve(parent, tryCount, factories, graph, f, depID, current);
                            if (recRes.getError() == null)
                                found = current;
                        }
                    }
                    if (found != null)
                        break;
                }
                if (found == null)
                    return createResult(parent, tryCount, "missing dependency", graph);
            }
            assert graph.getState(graph.getDependency(f, depID)) == NodeState.SOLVED;
        }
        graph.setSolved(f);
        return createResult(parent, tryCount, null, graph);
    }
}
