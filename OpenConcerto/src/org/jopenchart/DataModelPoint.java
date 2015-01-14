package org.jopenchart;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataModelPoint extends DataModel {

    private List<Point> points = new ArrayList<Point>();

    public DataModelPoint() {

    }

    public DataModelPoint(int n) {

        Random r = new Random(n);
        for (int i = 0; i < n; i++) {
            points.add(new Point(r.nextInt(80) + 20, r.nextInt(100)));
        }

    }

    public int getSize() {
        return points.size();
    }

    public Point getPoint(int i) {
        return points.get(i);
    }

    // FIXME : compute
    public Number getMinX() {
        return 0;
    }

    public Number getMinY() {
        return 0;
    }

    public Number getMaxX() {
        return 100;
    }

    public Number getMaxY() {
        return 100;
    }
}
