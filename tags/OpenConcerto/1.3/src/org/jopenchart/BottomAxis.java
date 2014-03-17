package org.jopenchart;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BottomAxis extends Axis {

    boolean leftAlign;

    public BottomAxis(Axis a, boolean leftAlign) {
        super("x");
        this.addLabels(a.getLabels());
        this.leftAlign = leftAlign;
    }

    public BottomAxis(boolean leftAlign) {
        super("x");
        this.leftAlign = leftAlign;
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
            int alignment = (this.leftAlign ? getX() : 0);
            g.drawLine(alignment + (int) x, getY(), alignment + (int) x, getY() + getMarkerLenght());
            int labelH = g.getFontMetrics().getAscent();
            double labelW = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getWidth() / 2 - 1;
            // Label
            g.setColor(label.getColor());
            g.drawString(label.getLabel(), alignment + (int) (x - labelW), labelH + getY() + getMarkerLenght() + getMarkerSpacing());

        }
        g.setColor(this.getColor());
        g.drawLine(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY());

    }
}
