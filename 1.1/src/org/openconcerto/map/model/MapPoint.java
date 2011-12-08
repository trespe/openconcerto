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
 
 package org.openconcerto.map.model;

public class MapPoint {

    private final long x;
    private final long y;

    public MapPoint(final long x, final long y) {
        this.x = x;
        this.y = y;
    }

    public final long getX() {
        return this.x;
    }

    public final long getY() {
        return this.y;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MapPoint) {
            final MapPoint p = (MapPoint) obj;
            return (p.x == this.x && p.y == this.y);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return (int) this.getX() * (int) this.getY();
    }
}
