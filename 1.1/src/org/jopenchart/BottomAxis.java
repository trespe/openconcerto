package org.jopenchart;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BottomAxis extends Axis {

    public BottomAxis(Axis a) {
        super("x");
        this.addLabels(a.getLabels());
    }

    public BottomAxis() {
        super("x");
    }

    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final int size = this.getLabels().size();
        double dx = ((double) this.getWidth()) / ((double) size - 1);
        double x;
        for (int i = 0; i < size; i++) {
            AxisLabel label = this.getLabels().get(i);
            g.setColor(this.getColor());
            if (label.getValue() != null) {
                x = this.chart.getXFromValue(label.getValue());
            } else {
                x = i * dx;
            }
            g.drawLine((int) x, getY(), (int) x, getY() + getMarkerLenght());
            int labelH = g.getFontMetrics().getAscent();
            double labelW = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getWidth() / 2 - 1;
            // Label
            g.setColor(label.getColor());
            g.drawString(label.getLabel(), (int) (x - labelW), labelH + getY() + getMarkerLenght() + getMarkerSpacing());

        }
        g.setColor(this.getColor());
        g.drawLine(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY());

    }
}
