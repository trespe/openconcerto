package org.jopenchart.barchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

import org.jopenchart.DataModel1D;

public class VerticalStackBarChart extends VerticalBarChart {
    private Stroke gridStroke;

    private Color gridColor = Color.LIGHT_GRAY;

    public VerticalStackBarChart() {

        gridStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { 5, 3 }, 0);

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
        double maxXValue = higherRange.doubleValue();

        double minXValue = lowerRange.doubleValue();
        double rangeXValue = maxXValue - minXValue;

        double ratioy = (double) graphHeight / rangeXValue;
        long x = graphPosX + this.getSpaceBetweenBars();

        for (int i = 0; i < nbBar; i++) {
            long y = graphPosY + graphHeight;

            for (int j = 0; j < this.getModelNumber(); j++) {

                final DataModel1D model1 = this.getModel(j);
                final Number valueAt = model1.getValueAt(i);
                if (valueAt != null) {
                    double h = valueAt.doubleValue() * ratioy;

                    g.setColor(this.getColor(j));
                    y -= Math.ceil(h) - 1;

                    g.fillRect((int) x, (int) y, this.getBarWidth(), (int) h);
                }

            }

            x += this.getSpaceBetweenBars() + this.getBarWidth();
        }
        g.setColor(Color.pink);
        g.drawRect(this.getChartRectangle().x, this.getChartRectangle().y, this.getChartRectangle().width, this.getChartRectangle().height);
    }

    public void setMultipleData(List<List<Number>> multipleData) {
        for (List<Number> list : multipleData) {
            this.addModel(new DataModel1D(list.toArray(new Number[0])));
        }

    }

}
