package org.jopenchart;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

public class LeftAxis extends Axis {

    public LeftAxis(Axis a) {
        super("y");
        this.addLabels(a.getLabels());
        this.setColor(a.getColor());
    }

    public LeftAxis() {
        super("y");
    }

    public void render(Graphics2D g) {
        List<AxisLabel> l = this.getLabels();
        Number max = 0;
        for (AxisLabel axisLabel : l) {
            Number value = axisLabel.getValue();
            if (value != null && value.doubleValue() > max.doubleValue()) {
                max = value;
            }
        }

        double ratioy = (double) this.getHeight() / max.doubleValue();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double dy = (double) this.getHeight() / (this.getLabels().size() - 1);
        double y = this.getHeight() + this.getY();
        for (AxisLabel label : this.getLabels()) {
            g.setColor(this.getColor());

            int minXValue = 0;
            int y1 = (int) y;
            if (max.intValue() > 0)
                y1 = getY() + getHeight() - (int) ((label.getValue().doubleValue() - minXValue) * ratioy);

            g.drawLine(this.getX() + this.getWidth() - getMarkerLenght() - 1, (int) y1, this.getX() + this.getWidth() - 1, (int) y1);
            int labelH = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getHeight() / 2 - 1;
            int labelW = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getWidth();

            g.setColor(label.getColor());
            g.drawString(label.getLabel(), this.getX() + this.getWidth() - this.getMarkerLenght() - labelW - 2, labelH + (int) y1);

            y -= dy;
        }
        g.setColor(this.getColor());
        g.drawLine(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth() - 1, this.getY() + this.getHeight());

    }
}
