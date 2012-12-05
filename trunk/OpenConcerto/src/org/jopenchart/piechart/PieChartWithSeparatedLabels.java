package org.jopenchart.piechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jopenchart.DataModel1D;

public class PieChartWithSeparatedLabels extends PieChart {

    public void renderPlot(Graphics2D g) {

        g.setColor(Color.RED);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int xCenter = getCenterX();
        int yCenter = getCenterY();

        DataModel1D model1 = (DataModel1D) this.getDataModel();
        double total = getTotalValue().doubleValue();

        double space = 0.0D;

        int stop = model1.getSize();
        computeColorsAndSpaces();

        int maxSpace = (int) spaces[0];

        int height2 = Math.min(xCenter, yCenter) - maxSpace - this.getMaxLabelHeight(g) / 2 - 2;
        int width2 = height2;
        int posX = (getCenterX() - width2);
        int squareSize = this.getMaxLabelHeight(g);
        if (posX < this.getMaxLabelWidth(g) + 4 * squareSize) {
            posX = this.getMaxLabelWidth(g) + 4 * squareSize;
        }
        int posY = (getCenterY() - width2);

        double ratio = 360 / total;
        double startAngle = 0D;
        for (int i = 0; i < stop; i++) {
            Number n = model1.getValueAt(i);
            if (n == null) {
                continue;
            }
            int angle = (int) Math.round(n.doubleValue() * ratio);
            double moveAngle = startAngle - angle / 2D;
            double angleRadian = ((moveAngle) * Math.PI * 2) / 360;

            double current_space = spaces[i];
            int x = posX + width2 / 2 + (int) ((Math.cos(angleRadian)) * current_space);
            int y = posY + height2 / 2 + (int) ((-Math.sin(angleRadian)) * current_space);

            g.setColor(colors[i]);
            g.setStroke(new BasicStroke(width2 - this.innerWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 12.0f, null, 10.0f));
            g.drawArc(x, y, width2, height2, (int) Math.round(startAngle), -angle);

            g.setStroke(new BasicStroke());
            if (i < labels.size()) {
                final String label = labels.get(i).getLabel();

                int xLabel = squareSize * 2;
                int yLabel = (int) (i * (squareSize * 1.5f) + squareSize / 2);
                g.fillRect(squareSize / 2, yLabel, squareSize, squareSize);

                g.setColor(Color.GRAY);

                g.drawString(label, xLabel, (int) (yLabel + squareSize * 0.8f));
            }
            startAngle -= angle;

        }

        if (this.getSeparatorColor() != null) {
            startAngle = 0D;
            g.setStroke(new BasicStroke(2f));
            g.setColor(this.getSeparatorColor());

            for (int i = 0; i < stop; i++) {
                Number n = model1.getValueAt(i);
                if (n == null) {
                    continue;
                }
                int angle = (int) Math.round(n.doubleValue() * ratio);

                double angleRadian = ((startAngle) * Math.PI * 2) / 360;
                double current_space = spaces[i];

                int x2 = posX + width2 + (int) ((Math.cos(angleRadian)) * (width2 + current_space + 2));
                int y2 = posY + height2 + (int) ((-Math.sin(angleRadian)) * (height2 + current_space + 2));
                g.drawLine(posX + width2, posY + height2, x2, y2);
                startAngle -= angle;
            }
        }

    }

}
