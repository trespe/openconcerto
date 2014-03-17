package org.jopenchart.barchart;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.jopenchart.Chart;
import org.jopenchart.DataModel1D;
import org.jopenchart.DataModelMultiple;

public class BarChart extends Chart {

    Integer spaceBetweenBars;

    Integer spaceBetweenGroups;

    int barWidth;

    private Number lowerRange;

    private Number higherRange;

    BarChart() {
        this.model = new DataModelMultiple();
        this.barWidth = 10;
    }

    public void addModel(DataModel1D model) {
        model.setChart(this);
        this.getDataModel().addModel(model);
    }

    public int getModelNumber() {
        return this.getDataModel().getSize();
    }

    public DataModel1D getModel(int index) {
        return this.getDataModel().getModel(index);
    }

    public void setBarWidth(Integer barWidth) {
        this.barWidth = barWidth;
    }

    public int getBarWidth() {
        return barWidth;
    }

    public Integer getSpaceBetweenBars() {
        if (spaceBetweenBars == null) {
            return getSpaceBetweenGroups() / 2;
        }
        return spaceBetweenBars;
    }

    public void setSpaceBetweenBars(Integer space) {
        spaceBetweenBars = space;
    }

    public Integer getSpaceBetweenGroups() {
        if (spaceBetweenGroups == null) {
            return 8;
        }
        return spaceBetweenGroups;
    }

    public void setSpaceBetweenGroups(Integer space) {
        spaceBetweenGroups = space;
    }

    public int getBarNumber() {
        int r = 0;
        for (int i = 0; i < this.getDataModel().getSize(); i++) {
            DataModel1D m = this.getDataModel().getModel(i);
            final int s = m.getSize();
            if (s > r) {
                r = s;
            }
        }
        return r;
    }

    public Number getLowerRange() {
        return this.lowerRange;
    }

    public Number getHigherRange() {
        return this.higherRange;
    }

    public DataModelMultiple getDataModel() {
        return (DataModelMultiple) this.model;
    }

    public void setLowerRange(Number i) {
        this.lowerRange = i;

    }

    public void setHigherRange(Number i) {
        this.higherRange = i;

    }

    public void prepareRendering(Graphics2D g) {
        final Dimension dimension = this.getDimension();
        if (dimension == null) {
            throw new IllegalStateException("Chart dimensions not defined");
        }
        int leftWidth = 1 + left.getMaxLabelWidth(g) + left.getMarkerLenght() + left.getMarkerSpacing();
        int rightWidth = bottom.getMaxLabelWidth(g) / 2;
        int topHeight = left.getMaxLabelHeight(g) / 2;
        int bottomHeight = 1 + bottom.getMaxLabelHeight(g) + bottom.getMarkerLenght() + bottom.getMarkerSpacing() + left.getMaxLabelHeight(g) / 2;

        int graphWidth = dimension.width - leftWidth - rightWidth;
        int graphHeight = dimension.height - topHeight - bottomHeight;

        left.setX(0);
        left.setY(topHeight);
        left.setWidth(leftWidth);
        left.setHeight(graphHeight);

        bottom.setX(leftWidth - 1);
        bottom.setY(topHeight + graphHeight);
        bottom.setWidth(graphWidth);
        bottom.setHeight(bottomHeight);

        this.setChartRectangle(new Rectangle(leftWidth, topHeight, graphWidth, graphHeight));

    }

}
