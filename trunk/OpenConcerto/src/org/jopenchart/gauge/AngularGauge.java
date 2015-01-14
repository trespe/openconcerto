package org.jopenchart.gauge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import org.jopenchart.Chart;
import org.jopenchart.DataModel1D;

public class AngularGauge extends Chart {

    private int minValue;
    private int maxValue;
    private Color backgroundValueColor = new Color(220, 220, 220);

    public AngularGauge() {
        model = new DataModel1D();
    }

    public void setMinValue(int min) {
        this.minValue = min;
    }

    public void setMaxValue(int max) {
        this.maxValue = max;
    }

    public void setValue(int value) {
        ((DataModel1D) model).setValueAt(0, value);
    }

    public void setBackgroundValueColor(Color c) {
        this.backgroundValueColor = c;
    }

    @Override
    public void setDimension(Dimension dimension) {
        super.setDimension(dimension);
        this.setChartRectangle(new Rectangle(0, 0, dimension.width, dimension.height));
    }

    @Override
    public void renderPlot(Graphics2D g) {
        super.renderPlot(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        System.out.println(getChartRectangle());
        int middleX = getChartRectangle().x + getChartRectangle().width / 2;
        int middleY = getChartRectangle().y + getChartRectangle().height / 2;
        int size = Math.min(getChartRectangle().width, getChartRectangle().height) - 2;
        if (backgroundValueColor != null) {
            g.setColor(this.backgroundValueColor);
            g.fillArc(middleX - size / 2, middleY - size / 2, size, size, -180, -180);
        }
        g.setColor(getColor(0));
        final double d = 180 / ((maxValue - minValue) / ((DataModel1D) model).getValueAt(0).doubleValue());
        int b = -(int) Math.round(d);
        if (b > 0) {
            b = 0;
        } else if (b < -180) {
            b = -180;
        }

        g.fillArc(middleX - size / 2, middleY - size / 2, size, size, -180, b);
        // Middle white circle
        g.setColor(Color.WHITE);
        int size2 = (int) (size / 1.5f);
        g.fillArc(middleX - size2 / 2, middleY - size2 / 2, size2, size2, 0, 360);
        // Value
        g.setColor(getColor(0));
        String s = getTextValue();
        // final float fontSize = size2 * 0.5f;
        // g.setFont(g.getFont().deriveFont(fontSize));
        int fontSize = g.getFont().getSize();
        Rectangle2D r = g.getFontMetrics().getStringBounds(s, g);
        g.drawString(s, (int) (middleX - r.getWidth() / 2), (int) (middleY + fontSize / 3));
    }

    public String getTextValue() {
        String s = String.valueOf(((DataModel1D) model).getValueAt(0));
        return s;
    }

}
