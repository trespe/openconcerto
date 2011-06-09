package org.jopenchart.barchart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import org.jopenchart.DataModel1D;

public class VerticalGroupBarChart extends VerticalBarChart {
    private List<GraphIndex> indexes = new ArrayList<GraphIndex>();
    private GraphIndex highlight;

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

        double maxYValue = Math.max(0, getDataModel().getMaxValue().doubleValue());
        double minYValue = Math.min(0, getDataModel().getMinValue().doubleValue());

        double rangeYValue = maxYValue - minYValue;

        double ratioy = ((double) graphHeight) / rangeYValue;
        long x = graphPosX + this.getSpaceBetweenGroups();
        // long posY = Math.round(model1.getMinValue().doubleValue() * ratioy);
        for (int i = 0; i < nbBar; i++) {

            for (int j = 0; j < this.getModelNumber(); j++) {
                long y = graphPosY + graphHeight;
                final DataModel1D model = this.getModel(j);
                final Number valueAt = model.getValueAt(i);
                if (valueAt != null) {
                    double h = valueAt.doubleValue() * ratioy;

                    g.setColor(this.getColor(j));
                    y -= Math.ceil(h) - 1;
                    g.fillRect((int) x, (int) y, this.getBarWidth(), (int) h);

                }
                x += this.getBarWidth() + this.getSpaceBetweenBars();
            }

            x += this.getSpaceBetweenGroups();
        }
        if (highlight != null) {

            final Number valueAt = highlight.getModel().getValueAt(highlight.getIndexOnModel());
            if (valueAt != null) {
                double h = valueAt.doubleValue() * ratioy;
                int y = graphPosY + graphHeight;
                y -= Math.ceil(h) - 1;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 6;
                final int left = (highlight.getMinX() + highlight.getMaxX() - s) / 2;
                final int top = y - s / 2;
                g.setColor(this.getColor(0));

                g.setColor(COLOR_ORANGE);
                g.fillRect((int) highlight.getMinX(), (int) y, this.getBarWidth(), (int) h);
                g.setColor(Color.WHITE);
                g.fillOval(left, top, s, s);
            }
        }

    }

    public double getXFromValue(Number value) {
        double gWidth = (this.getSpaceBetweenBars() + this.getBarWidth()) * this.getDataModel().getSize();
        return this.getChartRectangle().x - (gWidth + this.getSpaceBetweenGroups()) / 2 + (gWidth + getSpaceBetweenGroups()) * value.longValue();
    }

    @Override
    public void setChartRectangle(Rectangle r) {

        if (r.equals(this.getChartRectangle())) {
            return;
        }
        super.setChartRectangle(r);

        // remise à zero des index
        indexes.clear();
        // recalcul des index en suivant le même layout que dans le rendu
        int nbBar = getBarNumber();
        int graphPosX = this.getChartRectangle().x;
        long x = graphPosX + this.getSpaceBetweenGroups();
        for (int i = 0; i < nbBar; i++) {
            for (int j = 0; j < this.getModelNumber(); j++) {
                final DataModel1D model = this.getModel(j);
                GraphIndex index = new GraphIndex(model, i, (int) x, (int) (x + getBarWidth()));
                indexes.add(index);
                x += this.getBarWidth() + this.getSpaceBetweenBars();

            }
            x += this.getSpaceBetweenGroups();
        }

    }

    public Number highLightAt(int x, int y) {
        int stop = indexes.size();
        GraphIndex found = null;
        for (int i = 0; i < stop; i++) {
            GraphIndex index = this.indexes.get(i);
            if (index.containsX(x)) {
                found = index;
                break;
            }
        }
        if (this.highlight != found) {
            System.out.println(found);
            this.highlight = found;
            this.getDataModel().fireDataModelChanged();
        }
        if (this.highlight != null) {
            return this.highlight.getModel().getValueAt(this.highlight.getIndexOnModel());
        }
        return null;
    }

    public GraphIndex getHighlight() {
        return highlight;
    }
}
