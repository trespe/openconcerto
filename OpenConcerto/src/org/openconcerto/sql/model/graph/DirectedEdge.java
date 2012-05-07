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
 
 package org.openconcerto.sql.model.graph;

import net.jcip.annotations.ThreadSafe;

import org.jgrapht.Graph;

@ThreadSafe
public class DirectedEdge<V> {

    private final V src;
    private final V dst;

    public DirectedEdge(V src, V dst) {
        this.src = src;
        this.dst = dst;
    }

    public final V getSource() {
        return this.src;
    }

    public final V getTarget() {
        return this.dst;
    }

    public final V oppositeVertex(final V end) {
        if (end == this.getSource())
            return this.getTarget();
        else if (end == this.getTarget())
            return this.getSource();
        else
            throw new IllegalArgumentException(end + " is not an end of " + this);
    }

    @Override
    public String toString() {
        return "<" + this.getSource() + " -> " + this.getTarget() + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DirectedEdge)) {
            return false;
        }
        final DirectedEdge<?> o = (DirectedEdge<?>) other;
        return this.getSource().equals(o.getSource()) && this.getTarget().equals(o.getTarget());
    }

    @Override
    public int hashCode() {
        return this.getSource().hashCode() + this.getTarget().hashCode();
    }

    public static final <V, E extends DirectedEdge<V>> void addEdge(Graph<V, E> g, E e) {
        g.addEdge(e.getSource(), e.getTarget(), e);
    }
}
