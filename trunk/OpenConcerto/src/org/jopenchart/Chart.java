package org.jopenchart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class Chart {
    public static final Color COLOR_ORANGE = new Color(255, 157, 9);

    private String titleText;

    private Font titleFont;

    private Color titleColor;

    protected Axis top, bottom, left, right;

    // Colors
    protected List<Color> definedColors = new ArrayList<Color>(1);

    public static final AreaRenderer WHITE_BG_RENDERER = new SolidAreaRenderer(Color.WHITE);

    private AreaRenderer bgRenderer;

    private Dimension dimension;

    protected DataModel model;

    public Chart() {
        definedColors.add(new Color(255, 150, 0));
    }

    private Rectangle chartRectangle;

    public void setChartRectangle(Rectangle r) {
        if (chartRectangle == null || !r.equals(chartRectangle)) {
            chartRectangle = r;
        }
    }

    public Rectangle getChartRectangle() {
        return chartRectangle;
    }

    // Title

    void setTitle(String title) {
        this.titleText = title;
    }

    void setTitleFont(Font font) {
        this.titleFont = font;
    }

    void setTitleColor(Color color) {
        this.titleColor = color;
    }

    // Colors
    public void setColor(Color color) {
        definedColors.clear();
        definedColors.add(color);
    }

    public void addColor(Color color) {
        definedColors.add(color);
    }

    public void setColors(List<Color> colors) {
        definedColors.clear();
        definedColors.addAll(colors);
    }

    public Color getColor(int index) {
        if (index >= definedColors.size()) {
            return COLOR_ORANGE;
        }
        return definedColors.get(index);
    }

    public void setBackgroundRenderer(AreaRenderer renderer) {
        this.bgRenderer = renderer;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;

    }

    public Dimension getDimension() {
        return dimension;
    }

    public void render(Graphics2D g) {
        prepareRendering(g);
        renderBackground(g);

        renderPlot(g);
        renderAxis(g);
        renderLabels(g);
    }

    public void prepareRendering(Graphics2D g) {
    }

    public void renderBackground(Graphics2D g) {
        if (bgRenderer != null) {
            bgRenderer.render(g);
        }
        if (model.getState() == DataModel.LOADING) {
            g.setColor(Color.LIGHT_GRAY);
            final String str = "Chargement des données en cours...";
            Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
            final double x = this.getChartRectangle().x + (-r.getWidth() + this.getChartRectangle().width) / 2;
            int y = 32;
            g.drawString(str, (int) x, y);
        }
    }

    public void renderLabels(Graphics2D g) {

    }

    public void renderPlot(Graphics2D g) {

    }

    public void renderAxis(Graphics2D g) {

    }

    public DataModel getDataModel() {
        return model;
    }

    public double getXFromValue(Number value) {
        throw new IllegalStateException("Unsupported");

    }

    public Number highLightAt(int x, int y) {
        return null;
    }
}
