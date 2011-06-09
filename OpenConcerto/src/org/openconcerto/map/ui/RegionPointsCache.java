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
 
 package org.openconcerto.map.ui;

import org.openconcerto.map.model.MapPoint;
import org.openconcerto.map.model.Region;
import org.openconcerto.utils.ArrayListOfInt;

import java.util.List;


public class RegionPointsCache {

    private Region region;
    public int[] absX;
    public int[] absY;
    final ArrayListOfInt pointsX;
    final ArrayListOfInt pointsY;

    RegionPointsCache(final Region r, final double zoom) {
        this.region = r;
        final int size = r.getPoints().size();
        pointsX = new ArrayListOfInt(size);
        pointsY = new ArrayListOfInt(size);
        final List<MapPoint> points2 = r.getPoints();
        int regionPoints = points2.size();
        int lastX = -1;
        int lastY = -1;
        int lastlastX = -1;
        int lastlastY = -1;
        for (int j = 0; j < regionPoints; j++) {

            final MapPoint p = points2.get(j);

            int currentX = (int) (p.getX() / zoom);
            int currentY = (int) (p.getY() / zoom);

            if (currentX == lastX && currentY == lastY) {
                continue;
            }

            if (lastlastX == currentX && lastlastY == currentY) {
                removeLast();
                lastX = currentX;
                lastY = currentY;
                continue;
            }
            lastlastX = lastX;
            lastlastY = lastY;
            lastX = currentX;
            lastY = currentY;
            add(currentX, currentY);

        }

        absX = pointsX.toArray();
        absY = pointsY.toArray();
    }

    private final void add(int currentX, int currentY) {
        pointsX.add(currentX);
        pointsY.add(currentY);
    }

    /**
     * 
     */
    private void removeLast() {
        final int size = pointsX.size();
        if (size > 0) {
            pointsX.remove(size - 1);
            pointsY.remove(size - 1);
        }
    }

    public long getMaxX() {
        return region.getMaxX();
    }

    public long getMaxY() {
        return region.getMaxY();
    }

    public long getMinX() {
        return region.getMinX();
    }

    public long getMinY() {
        return region.getMinY();
    }

    public int size() {
        return pointsX.size();
    }

    public Region getRegion() {
        return region;
    }

}
