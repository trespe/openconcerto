package org.jopenchart.piechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jopenchart.Chart;
import org.jopenchart.DataModel1D;
import org.jopenchart.Label;

public class PieChart extends Chart {
    protected final List<Label> labels = new ArrayList<Label>();

    private Color separatorColor = Color.WHITE;

    protected int innerWidth;

    private int innerHeight;

    protected Color innerColor;
    protected Map<Label, Color> colorMap = new HashMap<Label, Color>();

    protected Color[] colors;

    protected double[] spaces;

    public void setData(List<Number> data) {
        DataModel1D m = new DataModel1D();
        m.addAll(data);
        setDataModel(m);
    }

    public void setDataModel(DataModel1D m) {
        this.model = m;
    }

    @Override
    public void setDimension(Dimension dimension) {
        super.setDimension(dimension);
        this.setChartRectangle(new Rectangle(0, 0, dimension.width, dimension.height));
    }

    public void renderPlot(Graphics2D g) {
        final DataModel1D model1 = (DataModel1D) this.getDataModel();
        final int stop = model1.getSize();
        if (stop < 1) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int xCenter = getCenterX();
        int yCenter = getCenterY();

        double total = getTotalValue().doubleValue();

        computeColorsAndSpaces();

        int maxSpace = (int) spaces[0];

        int height2 = Math.min(xCenter, yCenter) - maxSpace - this.getMaxLabelHeight(g) / 2 - 2;
        int width2 = height2;
        int posX = (getCenterX() - width2);
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
                int x1 = width2 + posX + (int) Math.round((Math.cos(angleRadian)) * (width2 + current_space));
                int y1 = height2 + posY + (int) Math.round((-Math.sin(angleRadian)) * (height2 + current_space));
                int x2 = width2 + posX + (int) Math.round((Math.cos(angleRadian)) * (width2 + current_space + 3));
                int y2 = height2 + posY + (int) Math.round((-Math.sin(angleRadian)) * (height2 + current_space + 3));
                g.setColor(Color.GRAY);
                g.drawLine(x1, y1, x2, y2);
                int x3;
                int xLabel;
                int y4 = y2 + g.getFontMetrics().getAscent() / 2 - 2;
                if (x1 > xCenter) {
                    // Label a droite
                    x3 = x2 + 2 + (this.getChartRectangle().width - x1) / 2;
                    xLabel = x3 + 2;

                    if (x3 + getLabelWidth(g, label) > this.getChartRectangle().getWidth()) {
                        // Pas assez de place, besoin de decaler à gauche le label de droite
                        x3 = (int) this.getChartRectangle().getWidth() - getLabelWidth(g, label) - 4;
                        xLabel = x3 + 4;

                    }

                } else {
                    // Label à gauche
                    x3 = x2;
                    x2 = (xCenter - width2) / 2;
                    xLabel = x2 - this.getMaxLabelWidth(g) - 2;

                    if (xLabel < 0) {
                        xLabel = 0;
                        x2 = getLabelWidth(g, label);
                    }

                }

                // ligne horizontale partant de la gauche vers la droite
                if (x2 < x3) {

                    g.drawLine(x2, y2, x3, y2);
                }

                g.drawString(label, xLabel, y4);
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
                g.drawLine(xCenter, yCenter, x2, y2);
                startAngle -= angle;
            }
        }

    }

    protected void computeColorsAndSpaces() {
        double space = 0;
        DataModel1D model1 = (DataModel1D) this.getDataModel();
        int stop = model1.getSize();
        colors = new Color[stop];
        spaces = new double[stop];
        Color origine = this.getColor(0);

        double mR = origine.getRed();
        double mV = origine.getGreen();
        double mB = origine.getBlue();
        double dR = (255D - origine.getRed()) / stop;
        double dV = (255D - origine.getGreen()) / stop;
        double dB = (255D - origine.getBlue()) / stop;
        for (int i = 0; i < stop; i++) {
            colors[i] = new Color((int) mR, (int) mV, (int) mB);
            mR += dR;
            mV += dV;
            mB += dB;
            spaces[i] = space;
        }
        // Apply user colors
        for (int i = 0; i < stop; i++) {
            if (i < labels.size()) {
                final Label label = labels.get(i);
                final Color color = colorMap.get(label);
                if (color != null) {
                    colors[i] = color;
                }
            }

        }
    }

    protected int getLabelWidth(Graphics2D g, String label) {
        int w = (int) g.getFontMetrics().getStringBounds(label, g).getWidth();
        return w;
    }

    protected Number getTotalValue() {
        DataModel1D model1 = (DataModel1D) this.getDataModel();
        double total = 0D;
        for (int i = 0; i < model1.getSize(); i++) {
            Number n = model1.getValueAt(i);
            if (n != null)
                total += n.doubleValue();
        }
        return total;
    }

    protected int getCenterY() {
        return this.getChartRectangle().height / 2;
    }

    protected int getCenterX() {
        return this.getChartRectangle().width / 2;
    }

    private void renderInner(Graphics2D g, int width2, int height2, double[] spaces) {
        double startAngle = 0D;
        DataModel1D model1 = (DataModel1D) this.getDataModel();
        int stop = model1.getSize();
        g.setColor(this.innerColor);

        double ratio = 360 / this.getTotalValue().doubleValue();
        int posX = (getCenterX() - this.innerWidth / 2);
        int posY = (getCenterY() - this.innerHeight / 2);//

        for (int i = 0; i < stop; i++) {
            Number n = model1.getValueAt(i);
            if (n == null) {
                continue;
            }
            int angle = (int) Math.round(n.doubleValue() * ratio);
            double moveAngle = startAngle - angle / 2D;
            double angleRadian = ((moveAngle) * Math.PI * 2) / 360;

            double current_space = spaces[i];
            int x = posX + (int) ((Math.cos(angleRadian)) * current_space);
            int y = posY + (int) ((-Math.sin(angleRadian)) * current_space);

            g.fillArc(x, y, this.innerWidth, this.innerHeight, (int) Math.round(startAngle) + 50, -angle - 100);

            startAngle -= angle;

        }
    }

    public Color getInnerColor() {
        return innerColor;
    }

    public Color getSeparatorColor() {
        return separatorColor;
    }

    protected int getMaxLabelHeight(Graphics2D g) {
        int max = 0;
        for (Label label : this.labels) {
            int w = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getHeight();
            if (w > max)
                max = w;
        }
        return max;
    }

    public int getMaxLabelWidth(Graphics2D g) {
        int max = 0;
        for (Label label : this.labels) {
            int w = (int) g.getFontMetrics().getStringBounds(label.getLabel(), g).getWidth();
            if (w > max)
                max = w;
        }
        return max;
    }

    public void addLabel(Label label) {
        this.labels.add(label);
    }

    public void addLabel(Label label, Color red) {
        colorMap.put(label, red);
        this.labels.add(label);
    }

    public void setInnerDimension(int width, int height) {
        this.innerWidth = width;
        this.innerHeight = height;
    }

    public void setInnerColor(Color color) {
        this.innerColor = color;

    }

}
