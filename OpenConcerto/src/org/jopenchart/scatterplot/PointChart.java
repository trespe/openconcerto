package org.jopenchart.scatterplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

import org.jopenchart.ArrayOfInt;
import org.jopenchart.Axis;
import org.jopenchart.BottomAxis;
import org.jopenchart.Chart;
import org.jopenchart.DataModel1D;
import org.jopenchart.DataModel2D;
import org.jopenchart.DataModelMultiple;
import org.jopenchart.DataModelPoint;
import org.jopenchart.LeftAxis;
import org.jopenchart.marker.ShapeMarker;

public class PointChart extends Chart {

    private Number lowerRange;

    private Number higherRange;

    private Color fillColor;

    private Double gridXStep;

    private Double gridYStep;

    private Stroke gridStroke;

    private Color gridColor = Color.LIGHT_GRAY;

    private List<Stroke> lineStrokes = new ArrayList<Stroke>();

    private List<ShapeMarker> markers = new ArrayList<ShapeMarker>();

    private Stroke defaultStroke = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public static final int TYPE_POINT = 0;
    public static final int TYPE_CIRCLE = 1;
    public static final int TYPE_SQUARE = 2;
    public static final int TYPE_PLAIN = 3;
    private int pointSize = 10;
    private int type = TYPE_POINT;

    public PointChart(DataModelPoint d) {

        left = new LeftAxis();
        bottom = new BottomAxis(true);
        gridStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { 5, 3 }, 0);
        setDataModel(d);
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setPointSize(int pointSize) {
        this.pointSize = pointSize;
    }

    public Stroke getStroke(int index) {
        if (index >= lineStrokes.size())
            return defaultStroke;
        return lineStrokes.get(index);
    }

    public void setDefaultLineStroke(Stroke s) {
        this.defaultStroke = s;
    }

    public DataModelPoint getDataModel() {
        return (DataModelPoint) this.model;
    }

    public void setDataModel(DataModelPoint m) {
        this.model = m;
    }

    public void setLowerRange(Number n) {
        this.lowerRange = n;
    }

    public void setHigherRange(Number n) {
        this.higherRange = n;
    }

    public void setFillColor(Color c) {
        this.fillColor = c;
    }

    public void setLeftAxis(Axis axis) {
        this.left = new LeftAxis(axis);
        this.left.setChart(this);
    }

    public void setBottomAxis(Axis axis) {
        this.bottom = new BottomAxis(axis, true);
        this.bottom.setChart(this);
    }

    @Override
    public void prepareRendering(Graphics2D g) {

        int leftWidth = 1 + left.getMaxLabelWidth(g) + left.getMarkerLenght() + left.getMarkerSpacing();
        int rightWidth = bottom.getMaxLabelWidth(g) / 2;
        int topHeight = left.getMaxLabelHeight(g) / 2;
        int bottomHeight = 1 + bottom.getMaxLabelHeight(g) + bottom.getMarkerLenght() + bottom.getMarkerSpacing();

        int graphWidth = this.getDimension().width - leftWidth - rightWidth;
        int graphHeight = this.getDimension().height - topHeight - bottomHeight;

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

    @Override
    public double getXFromValue(Number value) {

        return value.floatValue()*9.7f;
    }

    public void renderPlot(Graphics2D g) {
        renderGrid(g);

        DataModelPoint model1 = this.getDataModel();
        // Chart

        double maxYValue = model1.getMaxY().doubleValue();
        double minYValue = model1.getMinY().doubleValue();
        double rangeYValue = maxYValue - minYValue;

        int length = model1.getSize();

        int graphPosX = this.getChartRectangle().x;
        int graphPosY = this.getChartRectangle().y;
        int graphWidth = this.getChartRectangle().width;
        int graphHeight = this.getChartRectangle().height;

        double ratioX = (double) graphWidth / rangeYValue;

        double ratioY = (double) graphHeight / rangeYValue;

        for (int i = 0; i < length; i++) {
            g.setColor(getColor(i));
            g.setStroke(getStroke(i));
            int x = model1.getPoint(i).x + graphPosX;
            int y = model1.getPoint(i).y;
            if (type == TYPE_POINT) {
                g.drawLine(x - pointSize / 2, y, x + pointSize / 2, y);
                g.drawLine(x, y - pointSize / 2, x, y + pointSize / 2);
            } else if (type == TYPE_CIRCLE) {
                g.drawOval(x - pointSize / 2, y - pointSize / 2, pointSize, pointSize);
            } else if (type == TYPE_PLAIN) {
                g.fillOval(x - pointSize / 2, y - pointSize / 2, pointSize, pointSize);
            }

        }

        // Markers
        renderMarkers(g);

    }

    private void renderMarkers(Graphics2D g) {
        for (ShapeMarker marker : this.markers) {
            marker.draw(this, g);
        }

    }

    private void renderGrid(Graphics2D g) {
        int graphPosX = this.getChartRectangle().x - 1;
        int graphPosY = this.getChartRectangle().y + this.getChartRectangle().height;
        g.setColor(gridColor);
        // Vertical
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setStroke(gridStroke);

        if (this.gridXStep != null) {
            final double gridXStep2 = (this.gridXStep.doubleValue() * this.getChartRectangle().width) / 100D;

            for (double x = graphPosX + this.getChartRectangle().width; x > graphPosX; x -= gridXStep2) {
                g.drawLine((int) x, graphPosY, (int) x, this.getChartRectangle().y);
            }
        }
        // Horizontal
        if (this.gridYStep != null) {
            final double gridYStep2 = (this.gridYStep.doubleValue() * this.getChartRectangle().height) / 100D;
            for (double y = this.getChartRectangle().y; y < graphPosY; y += gridYStep2) {
                g.drawLine(graphPosX, (int) y, graphPosX + this.getChartRectangle().width, (int) y);
            }
        }
    }

    @Override
    public void renderAxis(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke());
        left.render(g);
        bottom.render(g);
    }

    /**
     * 
     * @param dx 0 - 100
     */
    public void setGridXStep(Double dx) {
        this.gridXStep = dx;

    }

    /**
     * 
     * @param dx 0 - 100
     */
    public void setGridYStep(Double dy) {
        this.gridYStep = dy;

    }

    public void setGridStroke(Stroke stroke) {
        this.gridStroke = stroke;

    }

    public void setGridColor(Color color) {
        this.gridColor = color;

    }

    public void setGridSegment(float lineLength, float blankLenght) {
        this.gridStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { lineLength, blankLenght }, 0);

    }

    public void setStrokes(List<Stroke> strokes) {
        this.lineStrokes.clear();
        this.lineStrokes.addAll(strokes);

    }

    public void addMarkers(List<ShapeMarker> markers) {
        this.markers.addAll(markers);

    }

}
