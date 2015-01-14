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
import org.openconcerto.record.RecordRef;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

@Immutable
public final class RecordItemSpec {

    static public enum Problem {
        TYPE, VALIDITY, EMPTINESS
    }

    private final String name;
    private final Type type;
    private final int maxListSize;
    private final String referencedSpec;
    private final Object defaultValue;
    private final boolean required;
    private final Constraint isValid, isEmpty;
    private final Map<String, Object> validProps;
    private final boolean userMustCheck, userMustModify;

    public RecordItemSpec(final String name, final Type type) {
        this(name, type, null);
    }

    public RecordItemSpec(final String name, final Type type, final Constraint isValid) {
        this(name, type, 0, null, null, false, null, null, null, false, false);
    }

    RecordItemSpec(String name, Type type, final int maxListSize, final String referencedSpec, Object defaultValue, boolean required, Constraint isValid, Map<String, Object> validProps,
            Constraint isEmpty, boolean userMustCheck, boolean userMustModify) {
        super();
        if (name == null)
            throw new NullPointerException("Null value");
        this.name = name;
        if (type == null)
            throw new NullPointerException("Null type");
        this.type = type;
        if (maxListSize < 0)
            throw new IllegalArgumentException("Negative max size : " + maxListSize);
        this.maxListSize = maxListSize;
        if ((referencedSpec != null) != (this.getType().isRecordType()))
            throw new IllegalArgumentException("Invalid referencedSpec for " + this.getType() + " : " + referencedSpec);
        this.referencedSpec = referencedSpec;
        this.defaultValue = defaultValue;
        this.required = required;
        this.isValid = isValid == null ? Constraints.none() : isValid;
        this.validProps = validProps == null ? Collections.<String, Object> emptyMap() : Collections.unmodifiableMap(new HashMap<String, Object>(validProps));
        this.isEmpty = isEmpty == null ? Constraints.getDefaultEmpty() : isEmpty;
        this.userMustCheck = userMustCheck;
        this.userMustModify = userMustModify;
    }

    public final String getName() {
        return this.name;
    }

    public final Type getType() {
        return this.type;
    }

    /**
     * The maximum count of elements.
     * 
     * @return the maximum, never negative, <code>0</code> meaning not a list.
     */
    public final int getMaxListSize() {
        return this.maxListSize;
    }

    public final String getReferencedSpec() {
        return this.referencedSpec;
    }

    public final Object getDefaultValue() {
        return this.defaultValue;
    }

    public final Constraint getValidConstraint() {
        return this.isValid;
    }

    public final Map<String, Object> getValidProperties() {
        return this.validProps;
    }

    public final Constraint getEmptyConstraint() {
        return this.isEmpty;
    }

    public final boolean isRequired() {
        return this.required;
    }

    public final Set<Problem> check(final Object obj) {
        if (obj != null) {
            if (!this.getType().check(obj))
                return EnumSet.of(Problem.TYPE);
            if (this.getReferencedSpec() != null) {
                final Collection<?> c = obj instanceof Collection ? (Collection<?>) obj : Collections.singleton(obj);
                for (final Object o : c) {
                    final String foreignSpec;
                    if (o instanceof RecordRef)
                        foreignSpec = ((RecordRef) o).getSpec().getName();
                    else
                        foreignSpec = ((Record) o).getSpec();
                    if (!foreignSpec.equals(this.getReferencedSpec()))
                        return EnumSet.of(Problem.TYPE);
                }
            }
            if (this.getMaxListSize() > 0) {
                if (!(obj instanceof Collection)) {
                    return EnumSet.of(Problem.VALIDITY);
                } else if (((Collection<?>) obj).size() > this.getMaxListSize()) {
                    return EnumSet.of(Problem.VALIDITY);
                }
            }
        }
        if (!this.getValidConstraint().check(obj))
            return EnumSet.of(Problem.VALIDITY);
        if (this.isRequired() && this.getEmptyConstraint().check(obj))
            return EnumSet.of(Problem.EMPTINESS);
        return EnumSet.noneOf(Problem.class);
    }

    public final boolean isUserMustCheck() {
        return this.userMustCheck;
    }

    public final boolean isUserMustModify() {
        return this.userMustModify;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " '" + this.getName() + "' ; type : " + this.getType() + " ; constraint : " + this.getValidConstraint();
    }
}
