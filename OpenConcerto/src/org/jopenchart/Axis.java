package org.jopenchart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class Axis {
    private String label;

    private boolean labelVisible;

    private List<AxisLabel> labels = new ArrayList<AxisLabel>(2);

    private int width;

    private int height;

    private int x;

    private int y;

    private int markerLenght = 2;

    private Color color = new Color(176, 176, 176);

    protected Chart chart;

    public Axis(String label) {
        this.label = label;

    }

    public String getLabel() {
        return label;
    }

    public void setLabelVisible(boolean b) {
        this.labelVisible = b;
    }

    public boolean isLabelVisible() {
        return this.labelVisible;
    }

    public void addLabel(AxisLabel label) {
        labels.add(label);
    }

    public void addLabels(List<AxisLabel> labels) {
        this.labels.addAll(labels);

    }

    public int getWidth() {

        return width;
    }

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxLabelWidth(Graphics2D g) {
        int max = 0;
        for (AxisLabel label : this.labels) {

            int w = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getWidth();
            if (w > max)
                max = w;
        }

        return max;
    }

    public int getMaxLabelHeight(Graphics2D g) {
        int max = 0;
        for (AxisLabel label : this.labels) {

            int h = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getHeight();
            if (h > max)
                max = h;
        }

        return max;
    }

    public void render(Graphics2D g) {

    }

    public List<AxisLabel> getLabels() {
        return this.labels;
    }

    public final void setWidth(int width) {
        this.width = width;
    }

    public final void setHeight(int height) {
        this.height = height;
    }

    public final void setX(int x) {
        this.x = x;
    }

    public final void setY(int y) {
        this.y = y;
    }

    public int getMarkerLenght() {
        if (labels.isEmpty())
            return 0;
        return markerLenght;
    }

    public final Color getColor() {
        return color;
    }

    public final void setColor(Color color) {
        this.color = color;
    }

    public int getMarkerSpacing() {
        if (labels.isEmpty())
            return 0;
        return 3;
    }

    public void setChart(Chart chart) {
        this.chart = chart;

    }

    public void removeAllLabels() {
        this.labels.clear();

    }

}
