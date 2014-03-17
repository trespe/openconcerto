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

import org.openconcerto.utils.cc.IPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * A single dependency, e.g. "apache >= 2 | jetty == 9".
 * 
 * @author Sylvain
 */
@Immutable
public final class Dependency {
    public static final List<Dependency> createList(final List<String> ids) {
        final List<Dependency> res = new ArrayList<Dependency>(ids.size());
        for (final String id : ids)
            res.add(createFromReference(new ModuleReference(id, null)));
        return res;
    }

    public static final Dependency createFromIDs(final List<String> ids) {
        final Map<String, IPredicate<ModuleFactory>> map = new LinkedHashMap<String, IPredicate<ModuleFactory>>(ids.size());
        for (final String id : ids) {
            map.put(id, createPred(new ModuleReference(id, null)));
        }
        return new Dependency(Collections.unmodifiableMap(map), true);
    }

    public static final Dependency createFromReferences(final List<ModuleReference> refs) {
        final Map<String, IPredicate<ModuleFactory>> map = new LinkedHashMap<String, IPredicate<ModuleFactory>>(refs.size());
        for (final ModuleReference ref : refs) {
            map.put(ref.getID(), createPred(ref));
        }
        return new Dependency(Collections.unmodifiableMap(map), true);
    }

    static private final IPredicate<ModuleFactory> createPred(final ModuleReference ref) {
        final ModuleVersion version = ref.getVersion();
        return version == null ? IPredicate.<ModuleFactory> truePredicate() : new IPredicate<ModuleFactory>() {
            @Override
            public boolean evaluateChecked(ModuleFactory input) {
                return version.equals(input.getVersion());
            }
        };
    }

    public static final Dependency createFromFactory(final ModuleFactory f) {
        return createFromReference(f.getReference());
    }

    public static final Dependency createFromReference(final ModuleReference ref) {
        return new Dependency(ref.getID(), createPred(ref));
    }

    private final Map<String, IPredicate<ModuleFactory>> predicates;

    public Dependency(final String moduleID, final IPredicate<ModuleFactory> pred) {
        this(Collections.singletonMap(moduleID, pred), true);
    }

    /**
     * Create a new instance.
     * 
     * @param map for each possible ID, whether a particular module is OK.
     */
    public Dependency(final Map<String, IPredicate<ModuleFactory>> map) {
        this(map, false);
    }

    private Dependency(final Map<String, IPredicate<ModuleFactory>> map, final boolean mapSafe) {
        this.predicates = mapSafe ? map : Collections.unmodifiableMap(new LinkedHashMap<String, IPredicate<ModuleFactory>>(map));
        if (this.predicates.size() == 0)
            throw new IllegalArgumentException("Empty");
    }

    public final Set<String> getRequiredIDs() {
        return this.predicates.keySet();
    }

    public final boolean isRequiredFactoryOK(ModuleFactory f) {
        final IPredicate<ModuleFactory> pred = this.predicates.get(f.getID());
        if (pred == null)
            return false;
        return pred.evaluateChecked(f);
    }
}
