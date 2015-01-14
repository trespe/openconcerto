package org.jopenchart.table;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jopenchart.AreaRenderer;
import org.jopenchart.Chart;
import org.jopenchart.DataModel2D;

public class SimpleTable extends Chart {
    private Color labelColorBackground = new Color(220, 220, 220);
    private Color labelColorText = Color.BLACK;
    private Color valueColorBackground = Color.WHITE;
    private Color valueColorText = Color.BLACK;
    private Color gridColor = Color.LIGHT_GRAY;
    private DataModel2D data;
    private Color[][] bgColors;
    private Color[][] fgColors;

    public SimpleTable(DataModel2D data) {
        this.data = data;
        bgColors = new Color[data.getRowCount()][data.getColumnCount()];
        fgColors = new Color[data.getRowCount()][data.getColumnCount()];
    }

    @Override
    public void renderBackground(Graphics2D g) {
        int colWidth = (int) Math.round(this.getDimension().getWidth() / (data.getColumnCount() + 1));
        int rowHeight = (int) Math.round(this.getDimension().getHeight() / (data.getRowCount() + 1));

        // BG
        if (labelColorBackground != null) {
            g.setColor(labelColorBackground);
            for (int i = 0; i < data.getColumnCount(); i++) {
                g.fillRect((i + 1) * colWidth, 0, colWidth, rowHeight);
            }

            for (int i = 0; i < data.getRowCount(); i++) {
                g.fillRect(0, (i + 1) * rowHeight, colWidth, rowHeight);
            }
        }
        for (int i = 0; i < data.getRowCount(); i++) {
            for (int j = 0; j < data.getColumnCount(); j++) {
                Color c = bgColors[i][j];
                if (c == null) {
                    c = valueColorBackground;
                }
                if (c != null) {
                    g.setColor(c);
                    g.fillRect((j + 1) * colWidth, (i + 1) * rowHeight, colWidth, rowHeight);
                }
            }
        }
        // Lines
        if (gridColor != null) {
            g.setColor(gridColor);
            for (int i = 0; i <= data.getColumnCount() + 1; i++) {
                int x1 = i * colWidth;
                g.drawLine(x1, 0, x1, (1 + data.getRowCount()) * rowHeight);

            }

            for (int i = 0; i <= data.getRowCount() + 1; i++) {
                int y = i * rowHeight;
                g.drawLine(0, y, (1 + data.getColumnCount()) * colWidth, y);
            }
        }
    }

    @Override
    public void renderPlot(Graphics2D g) {
        int colWidth = (int) Math.round(this.getDimension().getWidth() / (data.getColumnCount() + 1));
        int rowHeight = (int) Math.round(this.getDimension().getHeight() / (data.getRowCount() + 1));

        // Column
        for (int j = 0; j < data.getColumnCount(); j++) {
            Color c = labelColorText;
            if (c != null) {
                int x = (j + 1) * colWidth;
                int y = 0;
                String str = data.getColumnLabel(j);
                if (str != null) {
                    g.setColor(c);
                    Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
                    x += (colWidth - r.getWidth()) / 2;
                    g.drawString(str, x, y + (rowHeight + g.getFont().getSize()) / 2 - 2);
                }

            }
        }
        // Row labels
        for (int i = 0; i < data.getRowCount(); i++) {
            Color c = labelColorText;
            if (c != null) {
                int x = 0;
                int y = (i + 1) * rowHeight;
                String str = data.getRowLabel(i);
                if (str != null) {
                    g.setColor(c);
                    Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
                    x += (colWidth - r.getWidth()) / 2;
                    g.drawString(str, x, y + (rowHeight + g.getFont().getSize()) / 2 - 2);
                }
            }
        }

        // Value
        for (int i = 0; i < data.getRowCount(); i++) {
            for (int j = 0; j < data.getColumnCount(); j++) {
                Color c = fgColors[i][j];
                if (c == null) {
                    c = valueColorText;
                }
                if (c != null) {
                    int x = (j + 1) * colWidth;
                    int y = (i + 1) * rowHeight;
                    String str = data.getValue(i, j);
                    if (str != null) {
                        g.setColor(c);
                        Rectangle2D r = g.getFontMetrics().getStringBounds(str, g);
                        x += colWidth - r.getWidth() - 2;
                        g.drawString(str, x, y + (rowHeight + g.getFont().getSize()) / 2 - 2);
                    }

                }
            }
        }
    }

    public void setBackgoundColor(Color c, int row, int col) {
        bgColors[row][col] = c;
    }

    public void setForegoundColor(Color c, int row, int col) {
        fgColors[row][col] = c;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
    }

    public void setLabelColorBackground(Color labelColorBackground) {
        this.labelColorBackground = labelColorBackground;
    }

    public void setLabelColorText(Color labelColorText) {
        this.labelColorText = labelColorText;
    }

    public void setValueColorBackground(Color valueColorBackground) {
        this.valueColorBackground = valueColorBackground;
    }

    public void setValueColorText(Color valueColorText) {
        this.valueColorText = valueColorText;
    }
}
