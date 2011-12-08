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
 
 /*
 * Created on 5 nov. 2004
 */
package org.openconcerto.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;

public final class ContainerUtils {

    private ContainerUtils() {
    }

    /**
     * The nearest component from a point in a container. Useful since
     * {@link Container#getComponentAt(Point)} returns the component at the exact point, and thus
     * sometimes itself.
     * 
     * @param cont a container.
     * @param p a point within <code>cont</code>.
     * @return the closest child from <code>p</code>.
     */
    public static final Component getNearestChild(final Container cont, final Point p) {
        Component res = null;
        double distance = Integer.MAX_VALUE;
        final Rectangle rv = new Rectangle();
        for (final Component comp : cont.getComponents()) {
            final double d = getDistance(p, comp.getBounds(rv));
            if (d < distance) {
                distance = d;
                res = comp;
            }
        }

        return res;
    }

    public static double getDistance(Point p, Rectangle bounds) {
        if (bounds.contains(p))
            return 0;
        else {
            final int x = getNearest(p.x, bounds.x, bounds.x + bounds.width);
            final int y = getNearest(p.y, bounds.y, bounds.y + bounds.height);
            final int diffX = Math.abs(p.x - x);
            final int diffY = Math.abs(p.y - y);
            return Math.sqrt(diffX * diffX + diffY * diffY);
        }

    }

    private static int getNearest(int p, int start, int end) {
        if (p < start)
            return start;
        else if (p > end)
            return end;
        else
            return p;
    }

    /**
     * Returns the distance between point <code>p</code> in <code>cont</code> and <code>comp</code>
     * a descendant of <code>cont</code>.
     * 
     * @param cont a containter.
     * @param p a point within <code>cont</code>.
     * @param comp a descendant of <code>cont</code>.
     * @param bounds to avoid allocating a new <code>Rectangle</code> object on the heap, can be
     *        <code>null</code>.
     * @return the distance between the point and the component.
     */
    public static double getDistance(Container cont, Point p, final Component comp, Rectangle bounds) {
        return getDistance(p, getBoundsInAncestor(cont, comp, bounds));
    }

    // much faster than SwingUtilities.convertPointTo/FomScreen()
    public static Rectangle getBoundsInAncestor(Container ancestor, final Component comp, Rectangle bounds) {
        bounds = comp.getBounds(bounds);
        Container parent = comp.getParent();
        while (parent != null && parent != ancestor) {
            bounds.translate(parent.getX(), parent.getY());
            parent = parent.getParent();
        }
        return bounds;
    }
}
