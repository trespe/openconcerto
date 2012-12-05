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

import org.openconcerto.utils.CompareUtils;
import net.jcip.annotations.Immutable;

@Immutable
public class ModuleVersion implements Comparable<ModuleVersion> {

    public static final int MAX = 10000;
    private static final long MERGED_MAX = MAX * (long) MAX;
    private final int major, minor;
    private final long merged;

    public ModuleVersion(int major, int minor) {
        super();
        if (major >= MAX)
            throw new IllegalArgumentException("Major too big " + major);
        if (minor >= MAX)
            throw new IllegalArgumentException("Minor too big " + minor);
        this.major = major;
        this.minor = minor;
        this.merged = major * MAX + minor;
    }

    public ModuleVersion(long merged) {
        if (merged >= MERGED_MAX)
            throw new IllegalArgumentException("Merged too big " + merged);
        this.major = (int) (merged / MAX);
        // since MAX is an int, merged % MAX is also an int
        this.minor = (int) (merged % MAX);
        this.merged = merged;
    }

    public final int getMajor() {
        return this.major;
    }

    public final int getMinor() {
        return this.minor;
    }

    public final long getMerged() {
        return this.merged;
    }

    @Override
    public int compareTo(ModuleVersion o) {
        return CompareUtils.compareLong(this.merged, o.merged);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.merged ^ (this.merged >>> 32));
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
        return this.merged == ((ModuleVersion) obj).merged;
    }

    @Override
    public String toString() {
        return this.getMajor() + "." + this.getMinor();
    }
}
