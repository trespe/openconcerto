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
import org.openconcerto.utils.Tuple2;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * Stores in-memory which modules are installed (avoiding file system and database access).
 * 
 * @author Sylvain
 */
@Immutable
final class InstallationState {

    static private <T> Set<T> copySet(final Collection<T> s) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(s));
    }

    static public final InstallationState NOTHING = new InstallationState();

    private final Set<ModuleReference> installedLocally;
    private final Set<ModuleReference> installedRemotely;
    private final Set<ModuleReference> localAndRemote;
    private final Set<ModuleReference> localOrRemote;
    private final boolean missingFactories;
    private final Map<String, ModuleFactory> installedFactories;

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

        final Tuple2<Boolean, Map<String, ModuleFactory>> computed = this.computeInstalledFactories(pool);
        this.missingFactories = computed.get0();
        this.installedFactories = computed.get1();
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

    // true if some installed module lacks a factory
    public final boolean isMissingFactories() {
        return this.missingFactories;
    }

    // available factories for installed modules
    public final Map<String, ModuleFactory> getInstalledFactories() {
        return this.installedFactories;
    }

    // factories for all installed (local or remote) modules
    // null if some installed module lacks a factory
    public final Collection<ModuleFactory> getAllInstalledFactories() {
        if (this.isMissingFactories())
            return null;
        else
            return this.getInstalledFactories().values();
    }

    private final Tuple2<Boolean, Map<String, ModuleFactory>> computeInstalledFactories(final FactoriesByID pool) {
        boolean missing = false;
        final Set<ModuleReference> localOrRemote = this.getLocalOrRemote();
        final Map<String, ModuleFactory> res = new HashMap<String, ModuleFactory>(localOrRemote.size());
        for (final ModuleReference ref : localOrRemote) {
            if (ref.getVersion() == null)
                throw new IllegalStateException("Installed module missing version : " + ref);
            final List<ModuleFactory> factories = pool.getFactories(ref);
            if (factories.size() == 0) {
                missing = true;
            } else {
                assert factories.size() == 1 : "Despite a non-null version, more than one match";
                res.put(ref.getID(), CollectionUtils.getSole(factories));
            }
        }
        return Tuple2.create(missing, Collections.unmodifiableMap(res));
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
