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

import java.util.ArrayList;
import java.util.List;

public class MapPointSelection {
    private final List<MapPoint> points = new ArrayList<MapPoint>();
    long maxX = 0;
    long maxY = 0;
    long minX = Long.MAX_VALUE;
    long minY = Long.MAX_VALUE;

    public MapPointSelection() {
        this(null);
    }

    public MapPointSelection(String dbString) {
        if (dbString != null && dbString.length() > 0) {
            final String[] pts = dbString.split(";");
            for (int i = 0; i < pts.length; i++) {
                String string = pts[i];
                int index = string.indexOf(',');
                String sx = string.substring(0, index);
                String sy = string.substring(index + 1);
                final MapPoint p = new MapPoint(Long.parseLong(sx), Long.parseLong(sy));
                this.add(p);
            }
        }
    }

    public void mutateTo(MapPointSelection o) {
        this.clear();
        this.points.addAll(o.points);
        this.maxX = o.maxX;
        this.maxY = o.maxY;
        this.minX = o.minX;
        this.minY = o.minY;
    }

    public void add(MapPoint p) {
        if (!this.points.contains(p)) {
            long x = p.getX();
            long y = p.getY();
            if (x < this.minX)
                this.minX = x;
            if (x > this.maxX)
                this.maxX = x;

            if (y < this.minY)
                this.minY = y;
            if (y > this.maxY)
                this.maxY = y;

            this.points.add(p);
        }
    }

    public String toDBString() {
        String r = "";
        for (int i = 0; i < this.points.size(); i++) {
            MapPoint element = this.points.get(i);
            r += element.getX() + "," + element.getY();
            if (i != this.points.size() - 1) {
                r += ";";
            }
        }
        return r;
    }

    public void clear() {
        this.maxX = 0;
        this.maxY = 0;
        this.minX = Long.MAX_VALUE;
        this.minY = Long.MAX_VALUE;

        this.points.clear();
    }

    public int size() {
        return this.points.size();
    }

    public MapPoint get(int i) {
        return this.points.get(i);
    }

    public long getMaxX() {
        return this.maxX;
    }

    public long getMaxY() {
        return this.maxY;
    }

    public long getMinX() {
        return this.minX;
    }

    public long getMinY() {
        return this.minY;
    }
}
