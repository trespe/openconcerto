package org.jopenchart.barchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import org.jopenchart.Axis;
import org.jopenchart.BottomAxis;
import org.jopenchart.DataModel1D;
import org.jopenchart.LeftAxis;

public class VerticalBarChart extends BarChart {
    public VerticalBarChart() {
        left = new LeftAxis();
        bottom = new BottomAxis();
    }

    public void setLeftAxis(Axis axis) {
        this.left = new LeftAxis(axis);
        this.left.setChart(this);

    }

    public void setBottomAxis(Axis axis) {
        this.bottom = new BottomAxis(axis);
        this.bottom.setChart(this);
    }

    public void setModel(DataModel1D model) {
        this.getDataModel().removeAll();
        this.addModel(model);

    }

    public void renderPlot(Graphics2D g) {
        int nbBar = getBarNumber();
        int graphPosX = this.getChartRectangle().x;
        int graphPosY = this.getChartRectangle().y;
        int graphHeight = this.getChartRectangle().height;

        Number higherRange = this.getHigherRange();
        Number lowerRange = this.getLowerRange();
        if (higherRange == null) {
            higherRange = this.getDataModel().getMaxValue();
        }
        if (lowerRange == null) {
            lowerRange = this.getDataModel().getMinValue();
        }
        final DataModel1D model1 = this.getModel(0);
        double maxYValue = Math.max(0, model1.getMaxValue().doubleValue());
        double minYValue = Math.min(0, model1.getMinValue().doubleValue());

        double rangeYValue = maxYValue - minYValue;
        double ratioy = ((double) graphHeight) / rangeYValue;
        long x = graphPosX + this.getSpaceBetweenBars();
        // long posY = Math.round(model1.getMinValue().doubleValue() * ratioy);
        for (int i = 0; i < nbBar; i++) {
            long y = graphPosY + graphHeight;

            final Number valueAt = model1.getValueAt(i);
            if (valueAt != null) {
                double h = valueAt.doubleValue() * ratioy;
                g.setColor(this.getColor(0));
                y -= Math.ceil(h) - 1;

                g.fillRect((int) x, (int) y, this.getBarWidth(), (int) h);
            }

            x += this.getSpaceBetweenBars() + this.getBarWidth();
        }
        g.setColor(Color.pink);

    }

    @Override
    public void renderAxis(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke());
        left.render(g);
        bottom.render(g);
    }

    public void setDataModel(DataModel1D model) {
        this.setModel(model);

    }

    public double getXFromValue(Number value) {
        return this.getChartRectangle().x - this.getBarWidth() / 2 + (this.getSpaceBetweenBars() + this.getBarWidth()) * value.longValue();

    }

    public LeftAxis getLeftAxis() {
        return (LeftAxis) this.left;

    }
}
