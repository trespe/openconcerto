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
 
 package org.openconcerto.utils.cc;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.Tuple2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * A class to specify which objects to include or exclude.
 * 
 * @author Sylvain
 * @see #isIncluded(Object)
 */
@Immutable
public class IncludeExclude<T> {

    static private final IncludeExclude<Object> EMPTY = new IncludeExclude<Object>(Collections.<Object> emptySet(), Collections.<Object> emptySet(), true);
    static private final IncludeExclude<Object> FULL = new IncludeExclude<Object>(null, Collections.<Object> emptySet(), true);

    @SuppressWarnings("unchecked")
    public static <T> IncludeExclude<T> getEmpty() {
        return (IncludeExclude<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static <T> IncludeExclude<T> getFull() {
        return (IncludeExclude<T>) FULL;
    }

    public static <T> IncludeExclude<T> getNormalized(Collection<? extends T> includes) {
        return getNormalized(includes, Collections.<T> emptySet());
    }

    public static <T> IncludeExclude<T> getNormalized(Collection<? extends T> includes, Collection<? extends T> excludes) {
        return new IncludeExclude<T>(includes, excludes).normal;
    }

    private final Set<T> includes;
    private final Set<T> excludes;
    private final IncludeExclude<T> normal;

    public IncludeExclude(Collection<? extends T> includes) {
        this(includes, Collections.<T> emptySet());
    }

    /**
     * Create a new instance.
     * 
     * @param includes which objects to include, <code>null</code> meaning all.
     * @param excludes which objects to exclude, <code>null</code> meaning all.
     */
    public IncludeExclude(Collection<? extends T> includes, Collection<? extends T> excludes) {
        this(includes, excludes, false);
    }

    private IncludeExclude(Collection<? extends T> includes, Collection<? extends T> excludes, final boolean isNormal) {
        super();
        this.includes = includes == null ? null : Collections.unmodifiableSet(new HashSet<T>(includes));
        this.excludes = excludes == null ? null : Collections.unmodifiableSet(new HashSet<T>(excludes));
        this.normal = isNormal ? this : this.normalize();
        assert this.normal.excludes.isEmpty() || this.normal.includes == null;
    }

    private final IncludeExclude<T> normalize() {
        if (this.excludes == null)
            return getEmpty();
        if (this.excludes.isEmpty() && this.includes == null)
            return getFull();
        if (this.excludes.isEmpty() || this.includes == null)
            return this;
        return new IncludeExclude<T>(CollectionUtils.substract(this.includes, this.excludes), Collections.<T> emptySet(), true);
    }

    public final IncludeExclude<T> getNormal() {
        return this.normal;
    }

    /**
     * Whether the passed object is included or excluded by this.
     * 
     * @param s an object to test.
     * @return <code>true</code> if is in include and not in exclude.
     */
    public final boolean isIncluded(final T s) {
        if (this.isAllIncluded())
            return true;
        else if (this.isNoneIncluded())
            return false;
        else
            return (this.normal.includes == null || this.normal.includes.contains(s)) && !this.normal.excludes.contains(s);
    }

    /**
     * Whether this includes all objects.
     * 
     * @return <code>true</code> if {@link #isIncluded(Object)} always return <code>true</code>
     */
    public final boolean isAllIncluded() {
        return this.normal == FULL;
    }

    /**
     * Whether this includes no objects.
     * 
     * @return <code>true</code> if {@link #isIncluded(Object)} always return <code>false</code>
     */
    public final boolean isNoneIncluded() {
        return this.normal == EMPTY;
    }

    /**
     * The one and only object included by this.
     * 
     * @return <code>true</code> if {@link #isIncluded(Object)} returns <code>true</code> for one
     *         and only one object.
     */
    public final Tuple2<Boolean, T> getSole() {
        if (this.normal.includes == null) {
            return Tuple2.create(false, null);
        } else {
            assert this.normal.excludes.isEmpty();
            final boolean res = this.normal.includes.size() == 1;
            return Tuple2.create(res, res ? this.normal.includes.iterator().next() : null);
        }
    }

    /**
     * Return the one and only object included by this, or the passed object.
     * 
     * @param ifNoSole the object to return if this doesn't include exactly one object.
     * @return {@link #getSole()} or <code>ifNoSole</code>.
     */
    public final T getSole(final T ifNoSole) {
        final Tuple2<Boolean, T> sole = this.getSole();
        if (sole.get0())
            return sole.get1();
        else
            return ifNoSole;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((excludes == null) ? 0 : excludes.hashCode());
        result = prime * result + ((includes == null) ? 0 : includes.hashCode());
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
        final IncludeExclude<?> other = (IncludeExclude<?>) obj;
        return CompareUtils.equals(this.includes, other.includes) && CompareUtils.equals(this.excludes, other.excludes);
    }

}
