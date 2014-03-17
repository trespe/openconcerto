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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Index factories by version and ID. Factories are sorted most recent version first.
 * 
 * @author Sylvain
 */
@ThreadSafe
final class FactoriesByID {

    static private final SortedMap<ModuleVersion, ModuleFactory> createSortedMap() {
        return new TreeMap<ModuleVersion, ModuleFactory>(Collections.reverseOrder());
    }

    static private final SortedMap<ModuleVersion, ModuleFactory> EMPTY_MAP = Collections.unmodifiableSortedMap(createSortedMap());

    // immutable
    @GuardedBy("this")
    private Map<String, SortedMap<ModuleVersion, ModuleFactory>> map;

    public FactoriesByID() {
        this.map = Collections.emptyMap();
    }

    public FactoriesByID(FactoriesByID f) {
        this.map = f.getMap();
    }

    private synchronized final void setMap(final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy) {
        for (final Entry<String, SortedMap<ModuleVersion, ModuleFactory>> e : copy.entrySet()) {
            e.setValue(Collections.unmodifiableSortedMap(e.getValue()));
        }
        this.map = Collections.unmodifiableMap(copy);
    }

    public synchronized final Map<String, SortedMap<ModuleVersion, ModuleFactory>> getMap() {
        return this.map;
    }

    private final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy() {
        return this.copy(Collections.<String> emptySet());
    }

    private final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy(final Set<String> idExcludes) {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> m = this.getMap();
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> res = new HashMap<String, SortedMap<ModuleVersion, ModuleFactory>>(m.size());
        for (final Entry<String, SortedMap<ModuleVersion, ModuleFactory>> e : m.entrySet()) {
            final String id = e.getKey();
            if (!idExcludes.contains(id)) {
                res.put(id, new TreeMap<ModuleVersion, ModuleFactory>(e.getValue()));
            }
        }
        return res;
    }

    // ** add

    synchronized final ModuleFactory add(final ModuleFactory f) {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy = this.copy();
        final ModuleFactory res = add(f, copy);
        setMap(copy);
        return res;
    }

    static private final ModuleFactory add(final ModuleFactory f, final Map<String, SortedMap<ModuleVersion, ModuleFactory>> map) {
        SortedMap<ModuleVersion, ModuleFactory> m = map.get(f.getID());
        if (m == null) {
            // most recent version first
            m = createSortedMap();
            map.put(f.getID(), m);
        }
        return m.put(f.getVersion(), f);
    }

    synchronized final void addAll(Set<ModuleFactory> toAdd) {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy = this.copy();
        for (final ModuleFactory f : toAdd) {
            add(f, copy);
        }
        setMap(copy);
    }

    // ** get

    final boolean contains(final ModuleFactory factory) {
        return this.getVersions(factory.getID()).get(factory.getVersion()).equals(factory);
    }

    final SortedMap<ModuleVersion, ModuleFactory> getVersions(final String moduleID) {
        final SortedMap<ModuleVersion, ModuleFactory> res = this.getMap().get(moduleID);
        return res == null ? EMPTY_MAP : res;
    }

    final ModuleFactory getFactory(final String id) {
        return this.getFactory(new ModuleReference(id, null));
    }

    /**
     * Return the factory matching the passed reference.
     * 
     * @param ref a reference, possibly without a version.
     * @return the factory if there's one and only one matching, <code>null</code> otherwise.
     */
    final ModuleFactory getFactory(final ModuleReference ref) {
        return CollectionUtils.getSole(this.getFactories(ref));
    }

    /**
     * Get all factories matching the passed reference.
     * 
     * @param ref a reference, possibly without a version.
     * @return the matching factories, never <code>null</code>.
     */
    final List<ModuleFactory> getFactories(final ModuleReference ref) {
        return this.getFactories(ref, false);
    }

    final List<ModuleFactory> getFactories(final ModuleReference ref, final boolean mostRecentIfNoVersion) {
        final SortedMap<ModuleVersion, ModuleFactory> map = this.getVersions(ref.getID());
        if (map.isEmpty())
            return Collections.emptyList();

        ModuleVersion version = ref.getVersion();
        if (version == null && mostRecentIfNoVersion)
            version = map.firstKey();
        if (version == null) {
            return new ArrayList<ModuleFactory>(map.values());
        } else {
            final ModuleFactory val = map.get(version);
            if (val == null)
                return Collections.emptyList();
            else
                return Collections.singletonList(val);
        }
    }

    final Collection<ModuleFactory> getFactories() {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> map = this.getMap();
        final List<ModuleFactory> res = new ArrayList<ModuleFactory>(map.size() * 3);
        for (final SortedMap<ModuleVersion, ModuleFactory> e : map.values()) {
            res.addAll(e.values());
        }
        return res;
    }

    /**
     * Get all factories in this instance that conflict with the passed ones.
     * 
     * @param factories some factories.
     * @return the factories of this instance that conflicts.
     */
    final Set<ModuleFactory> getConflicts(final Collection<ModuleFactory> factories) {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> m = this.getMap();
        final Set<ModuleFactory> res = new HashSet<ModuleFactory>();
        for (final SortedMap<ModuleVersion, ModuleFactory> id : m.values()) {
            for (final ModuleFactory mf : id.values()) {
                if (mf.conflictsWith(factories))
                    res.add(mf);
            }
        }
        return res;
    }

    // ** remove

    synchronized final SortedMap<ModuleVersion, ModuleFactory> remove(final String id) {
        final SortedMap<ModuleVersion, ModuleFactory> res = this.getVersions(id);
        this.setMap(this.copy(Collections.singleton(id)));
        return res;
    }

    synchronized final void removeAll(Set<ModuleFactory> toRemove) {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> copy = this.copy();
        for (final ModuleFactory f : toRemove) {
            final SortedMap<ModuleVersion, ModuleFactory> m = copy.get(f.getID());
            if (m != null) {
                m.remove(f.getVersion());
            }
        }
        this.setMap(copy);
    }

    // ** Object

    @Override
    public String toString() {
        final Map<String, SortedMap<ModuleVersion, ModuleFactory>> m = this.getMap();
        final int size = m.size();
        final StringBuilder sb = new StringBuilder(size * 32);
        sb.append(this.getClass().getSimpleName());
        if (size == 0) {
            sb.append(" empty");
        } else {
            sb.append(" {");
            for (final Entry<String, SortedMap<ModuleVersion, ModuleFactory>> e : m.entrySet()) {
                sb.append(e.getKey());
                sb.append(" : ");
                sb.append(e.getValue().keySet());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.map.hashCode();
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
        final FactoriesByID other = (FactoriesByID) obj;
        return this.map.equals(other.map);
    }
}
