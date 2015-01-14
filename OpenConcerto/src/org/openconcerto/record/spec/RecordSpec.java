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
 
 package org.openconcerto.record.spec;

import org.openconcerto.record.Constraint;
import org.openconcerto.record.Constraints;
import org.openconcerto.record.Record;
import org.openconcerto.record.spec.RecordItemSpec.Problem;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.Immutable;

@Immutable
public final class RecordSpec {

    private final String name;
    private final Map<String, RecordItemSpec> items;
    private final Constraint constraint;

    public RecordSpec(final String name, final Collection<RecordItemSpec> items, final Constraint c) {
        super();
        if (name == null)
            throw new NullPointerException("Null value");
        this.name = name;
        final Map<String, RecordItemSpec> map = new LinkedHashMap<String, RecordItemSpec>(items.size());
        for (final RecordItemSpec itemSpec : items) {
            if (map.put(itemSpec.getName(), itemSpec) != null)
                throw new IllegalArgumentException("Duplicate name : " + itemSpec.getName());
        }
        this.items = Collections.unmodifiableMap(map);
        this.constraint = c == null ? Constraints.none() : c;
    }

    public final String getName() {
        return this.name;
    }

    public final Map<String, RecordItemSpec> getItems() {
        return this.items;
    }

    public final Constraint getValidConstraint() {
        return this.constraint;
    }

    public final Map<String, Set<Problem>> check(final Record record) {
        return this.check(record, false);
    }

    public final Map<String, Set<Problem>> check(final Record record, final boolean allowPartial) {
        if (!record.getSpec().equals(this.getName()))
            throw new IllegalArgumentException("Name mismatch '" + record.getSpec() + "' != '" + this.getName() + "'");
        final Map<String, Set<Problem>> res = new HashMap<String, Set<Problem>>();
        for (final Entry<String, RecordItemSpec> e : this.getItems().entrySet()) {
            final String itemName = e.getKey();
            final RecordItemSpec itemSpec = e.getValue();
            if (!allowPartial || record.getItems().containsKey(itemName)) {
                final Set<Problem> pbs = itemSpec.check(record.getItems().get(itemName));
                if (!pbs.isEmpty())
                    res.put(itemName, pbs);
            }
        }
        if (!(allowPartial || this.getValidConstraint().check(record)) || !this.getItems().keySet().containsAll(record.getItems().keySet()))
            res.put(null, EnumSet.of(Problem.VALIDITY));
        return res;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " '" + this.getName() + "' ; constraint : " + this.getValidConstraint() + " ; items : \n" + this.getItems().values();
    }
}
