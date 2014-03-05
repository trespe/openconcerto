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

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores in-memory which modules are installed (avoiding file system and database access).
 * 
 * @author Sylvain
 */
final class InstallationState {

    static private <T> Set<T> copySet(final Collection<T> s) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(s));
    }

    static public final InstallationState NOTHING = new InstallationState();

    private final Set<ModuleReference> installedLocally;
    private final Set<ModuleReference> installedRemotely;
    private final Set<ModuleReference> localAndRemote;
    private final Set<ModuleReference> localOrRemote;
    private final Set<ModuleFactory> installedFactories;

    private InstallationState() {
        this(Collections.<ModuleReference> emptySet(), Collections.<ModuleReference> emptySet(), null);
    }

    public InstallationState(final ModuleManager mngr) throws SQLException, IOException {
        this(copySet(mngr.getModulesInstalledLocally()), copySet(mngr.getModulesInstalledRemotely()), mngr.copyFactories());
    }

    private InstallationState(final Set<ModuleReference> installedLocally, final Set<ModuleReference> installedRemotely, final FactoriesByID pool) {
        this.installedLocally = installedLocally;
        this.installedRemotely = installedRemotely;

        Set<ModuleReference> tmp = new HashSet<ModuleReference>(this.installedLocally);
        tmp.retainAll(this.installedRemotely);
        this.localAndRemote = Collections.unmodifiableSet(tmp);

        tmp = new HashSet<ModuleReference>(this.installedLocally);
        tmp.addAll(this.installedRemotely);
        this.localOrRemote = Collections.unmodifiableSet(tmp);

        this.installedFactories = this.computeInstalledFactories(pool);
    }

    public Set<ModuleReference> getLocal() {
        return this.installedLocally;
    }

    public Set<ModuleReference> getRemote() {
        return this.installedRemotely;
    }

    public final Set<ModuleReference> getLocalAndRemote() {
        return this.localAndRemote;
    }

    public final Set<ModuleReference> getLocalOrRemote() {
        return this.localOrRemote;
    }

    // factories for all installed (local or remote) modules
    // null if some installed module lacks a factory
    public final Set<ModuleFactory> getInstalledFactories() {
        return this.installedFactories;
    }

    private final Set<ModuleFactory> computeInstalledFactories(final FactoriesByID pool) {
        final Set<ModuleReference> localOrRemote = this.getLocalOrRemote();
        final Set<ModuleFactory> res = new HashSet<ModuleFactory>(localOrRemote.size());
        for (final ModuleReference ref : localOrRemote) {
            if (ref.getVersion() == null)
                throw new IllegalStateException("Installed module missing version : " + ref);
            final ModuleFactory factory = CollectionUtils.getSole(pool.getFactories(ref));
            if (factory == null)
                return null;
            res.add(factory);
        }
        return Collections.unmodifiableSet(res);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.installedLocally.hashCode();
        result = prime * result + this.installedRemotely.hashCode();
        result = prime * result + ((this.installedFactories == null) ? 0 : this.installedFactories.hashCode());
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
        final InstallationState other = (InstallationState) obj;
        return this.installedLocally.equals(other.installedLocally) && this.installedRemotely.equals(other.installedRemotely) && CompareUtils.equals(this.installedFactories, other.installedFactories);
    }
}
