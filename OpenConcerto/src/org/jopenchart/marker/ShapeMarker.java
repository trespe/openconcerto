package org.jopenchart.marker;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jopenchart.Chart;


public abstract class ShapeMarker {
    private Color color;
    private int dataSetIndex;
    private double data;
    private float size;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getDataSetIndex() {
        return dataSetIndex;
    }

    public void setDataSetIndex(int dataSetIndex) {
        this.dataSetIndex = dataSetIndex;
    }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public abstract void draw(Chart c, Graphics2D g);

    protected int getY() {
        // TODO Auto-generated method stub
        return 0;
    }

    protected int getX() {
        // TODO Auto-generated method stub
        return 0;
    }

}
