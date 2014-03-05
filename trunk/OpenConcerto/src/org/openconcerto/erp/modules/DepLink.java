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

import org.openconcerto.sql.model.graph.DirectedEdge;
import net.jcip.annotations.Immutable;

@Immutable
final class DepLink extends DirectedEdge<ModuleFactory> {

    private final Object dep;

    DepLink(final ModuleFactory src, final Object dep, final ModuleFactory dst) {
        super(src, dst);
        if (dep == null)
            throw new NullPointerException("Null label");
        this.dep = dep;
    }

    public final Object getDepID() {
        return this.dep;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.dep.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DepLink other = (DepLink) obj;
        return this.dep.equals(other.dep);
    }
}
