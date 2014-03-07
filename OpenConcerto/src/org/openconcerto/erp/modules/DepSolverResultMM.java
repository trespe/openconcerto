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

import org.openconcerto.erp.modules.ModuleManager.ModuleAction;
import org.openconcerto.erp.modules.ModuleManager.NoChoicePredicate;
import org.openconcerto.utils.cc.IPredicate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

/**
 * A result that caches the modules to add and remove for a module manager.
 * 
 * @author Sylvain
 * @see #init(ModuleManager, InstallationState, NoChoicePredicate, Collection)
 */
@ThreadSafe
public class DepSolverResultMM extends DepSolverResult implements ModulesStateChange {

    // true only if current user can install and uninstall
    static final IPredicate<DepSolverResult> VALID_PRED = new IPredicate<DepSolverResult>() {
        @Override
        public boolean evaluateChecked(DepSolverResult input) {
            final DepSolverResultMM res = (DepSolverResultMM) input;
            return res.getReferencesToInstall() != null && res.getReferencesToRemove() != null;
        }
    };

    private ModuleManager mngr;
    private InstallationState installState;
    private NoChoicePredicate noChoice;
    private Set<ModuleReference> refs;

    private Set<ModuleReference> toAdd, toRemove;
    private boolean toAddComputed, toRemoveComputed;

    public DepSolverResultMM(DepSolverResultMM parent, int tryCount, String error, DepSolverGraph graph) {
        super(parent, tryCount, error, graph);
        this.installState = null;
        this.noChoice = null;

        this.toAddComputed = this.toRemoveComputed = false;
    }

    final void init(final ModuleManager mngr, final InstallationState installState) {
        this.init(mngr, installState, null, null);
    }

    /**
     * Initialise this instance.
     * 
     * @param mngr the manager.
     * @param installState its installation state (avoid asking it from the manager for each
     *        result).
     * @param s the choice solution, <code>null</code> if any solution is OK.
     * @param refs the references that were passed (to check <code>s</code>), can be
     *        <code>null</code> if <code>s</code> is.
     */
    synchronized final void init(final ModuleManager mngr, final InstallationState installState, final NoChoicePredicate s, final Collection<ModuleReference> refs) {
        if (this.isInited())
            throw new IllegalStateException("Already set : " + this.installState);
        if (mngr == null)
            throw new NullPointerException("Null manager");
        this.mngr = mngr;
        this.setInstallState(installState);
        this.noChoice = s;
        this.refs = refs == null ? Collections.<ModuleReference> emptySet() : Collections.unmodifiableSet(new HashSet<ModuleReference>(refs));
    }

    private synchronized final boolean isInited() {
        return this.installState != null;
    }

    private synchronized final void setInstallState(InstallationState installState) {
        if (installState == null)
            throw new NullPointerException();
        this.installState = installState;
    }

    public synchronized final ModuleManager getManager() {
        if (!this.isInited())
            throw new IllegalStateException("Not set");
        return this.mngr;
    }

    @Override
    public synchronized final InstallationState getInstallState() {
        if (!this.isInited())
            throw new IllegalStateException("Not set");
        return this.installState;
    }

    public synchronized final NoChoicePredicate getNoChoiceSolution() {
        if (!this.isInited())
            throw new IllegalStateException("Not initialized");
        return this.noChoice;
    }

    @Override
    public synchronized final Set<ModuleReference> getUserReferencesToInstall() {
        if (!this.isInited())
            throw new IllegalStateException("Not initialized");
        return this.refs;
    }

    /**
     * The references to install to have all of the {@link #getGraph() graph} installed. E.g. checks
     * already installed modules and user rights.
     * 
     * @return the references to install, <code>null</code> if error.
     */
    @Override
    public synchronized final Set<ModuleReference> getReferencesToInstall() {
        if (!this.toAddComputed) {
            this.toAdd = computeReferencesToInstall();
            this.toAddComputed = true;
        }
        return this.toAdd;
    }

    private final Set<ModuleReference> computeReferencesToInstall() {
        final Set<ModuleReference> localAndRemote = this.getInstallState().getLocalAndRemote();
        final Set<ModuleFactory> factories = this.getGraph().getFactories();
        final Set<ModuleReference> toInstall = new HashSet<ModuleReference>(factories.size());
        for (final ModuleFactory f : factories) {
            if (!localAndRemote.contains(f.getReference()))
                toInstall.add(f.getReference());
        }

        final NoChoicePredicate s = this.getNoChoiceSolution();
        if (s == NoChoicePredicate.NO_CHANGE && toInstall.size() > 0)
            return null;
        else if (s == NoChoicePredicate.ONLY_INSTALL_ARGUMENTS && !this.getUserReferencesToInstall().containsAll(toInstall))
            return null;

        for (final ModuleReference toInst : toInstall) {
            if (!getManager().canCurrentUserInstall(ModuleAction.INSTALL, toInst, this.getInstallState()))
                return null;
        }

        return toInstall;
    }

    /**
     * The references to remove to have all of the {@link #getGraph() graph} installed. E.g. checks
     * conflicts and user rights.
     * 
     * @return the references to remove in uninstall order, <code>null</code> if error.
     */
    @Override
    public synchronized final Set<ModuleReference> getReferencesToRemove() {
        if (!this.toRemoveComputed) {
            this.toRemove = computeReferencesToRemove();
            this.toRemoveComputed = true;
        }
        return this.toRemove;
    }

    @Override
    public boolean forceRemove() {
        return false;
    }

    private final Set<ModuleReference> computeReferencesToRemove() {
        final Set<ModuleFactory> factories = this.getGraph().getFactories();
        final Collection<ModuleFactory> installedFactories = this.getInstallState().getAllInstalledFactories();
        // missing some factories for installed modules
        if (installedFactories == null)
            return null;
        final Set<ModuleReference> conflicts = new HashSet<ModuleReference>();
        for (final ModuleFactory f : installedFactories) {
            if (f.conflictsWith(factories))
                conflicts.add(f.getReference());
        }
        final Set<ModuleReference> toUninstall;
        try {
            toUninstall = getManager().getAllOrderedDependentModulesRecursively(conflicts);
        } catch (Exception e) {
            throw new IllegalStateException("couldn't find needing modules for " + conflicts, e);
        }

        if (this.getNoChoiceSolution() != null && toUninstall.size() > 0)
            return null;

        for (final ModuleReference toInst : toUninstall) {
            if (!getManager().canCurrentUserInstall(ModuleAction.UNINSTALL, toInst, getInstallState()))
                return null;
        }
        return toUninstall;
    }
}
