package org.jopenchart.barchart;

import org.jopenchart.DataModel1D;

public class GraphIndex {
    private DataModel1D model;
    private int indexOnModel;
    private int minX;
    private int maxX;

    public GraphIndex(DataModel1D model, int indexOnModel, int minX, int maxX) {
        this.model = model;
        this.indexOnModel = indexOnModel;
        this.minX = minX;
        this.maxX = maxX;
    }

    public boolean containsX(int x) {
        return x >= minX && x < maxX;
    }

    public DataModel1D getModel() {
        return model;
    }

    public int getIndexOnModel() {
        return indexOnModel;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }
}
