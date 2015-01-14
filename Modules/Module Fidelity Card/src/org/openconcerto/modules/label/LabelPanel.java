package org.openconcerto.modules.label;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.List;

import javax.swing.JPanel;

import org.openconcerto.sql.model.SQLRowAccessor;

public class LabelPanel extends JPanel implements Printable {

    private List<? extends SQLRowAccessor> list;
    private int lines;
    private int columns;
    private Boolean[][] mask;
    private final Color VERY_LIGHT_GRAY = new Color(230, 230, 230);
    private LabelRenderer renderer;
    private boolean ignoreMargins = false;

    public LabelPanel(List<? extends SQLRowAccessor> list, int l, int c, LabelRenderer labelRenderer) {
        this.list = list;
        this.renderer = labelRenderer;
        setSizeLayoutSize(l, c);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int w = getWidth() / columns;
                int h = getHeight() / lines;
                int j = e.getX() / w;
                int i = e.getY() / h;
                if (i >= 0 && i < lines && j >= 0 && j < columns) {
                    mask[i][j] = !mask[i][j];
                }
                repaint();
            }
        });
    }

    private void setSizeLayoutSize(int l, int c) {
        this.lines = l;
        this.columns = c;
        this.mask = new Boolean[l][c];
        for (int i = 0; i < l; i++) {
            for (int j = 0; j < c; j++) {
                this.mask[i][j] = Boolean.FALSE;
            }
        }
    }

    public void setIgnoreMargins(boolean ignoreMargins) {
        this.ignoreMargins = ignoreMargins;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        int w = this.getWidth() / this.columns;
        int h = this.getHeight() / this.lines;

        Graphics2D g2 = (Graphics2D) g;
        float dash1[] = { 10.0f };
        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
        BasicStroke n = new BasicStroke(1.0f);
        int rowIndex = 0;

        for (int i = 0; i < this.lines; i++) {
            for (int j = 0; j < this.columns; j++) {
                boolean mask = this.mask[i][j];
                if (mask) {
                    // Disabled
                    g.setColor(VERY_LIGHT_GRAY);
                    g.fillRect(j * w, i * h, w, h);
                } else {
                    // Dashed borders
                    g.setColor(Color.WHITE);
                    g2.setStroke(n);
                    g.drawRect(j * w, i * h, w, h);
                    g.setColor(Color.GRAY);
                    g2.setStroke(dashed);
                    g.drawRect(j * w, i * h, w, h);
                    // Label
                    if (rowIndex < list.size()) {
                        SQLRowAccessor row = list.get(rowIndex);
                        renderer.paintLabel(g, row, j * w, i * h, w, h);
                    }
                    rowIndex++;
                }

            }
        }

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 600);
    }

    public void setLineCount(int l) {
        setSizeLayoutSize(l, this.columns);
        repaint();
    }

    public void setColumnCount(int c) {
        setSizeLayoutSize(this.lines, c);
        repaint();
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        int nbLabelPerPage = 0;
        for (int i = 0; i < this.lines; i++) {
            for (int j = 0; j < this.columns; j++) {
                boolean mask = this.mask[i][j];
                if (!mask) {
                    nbLabelPerPage++;
                }
            }
        }
        if (nbLabelPerPage < 1) {
            return NO_SUCH_PAGE;
        }
        int labelCount = this.list.size();
        if (nbLabelPerPage * pageIndex > labelCount) {
            return NO_SUCH_PAGE;
        }

        int imageableWidth = (int) pageFormat.getImageableWidth();
        int imageableHeight = (int) pageFormat.getImageableHeight();
        int imageableX = (int) pageFormat.getImageableX();
        int imageableY = (int) pageFormat.getImageableY();
        if (ignoreMargins) {
            imageableWidth = (int) pageFormat.getWidth();
            imageableHeight = (int) pageFormat.getHeight();
            imageableX = 0;
            imageableY = 0;
        }
        int w = imageableWidth / this.columns;
        int h = imageableHeight / this.lines;

        // print labels
        int rowIndex = pageIndex * nbLabelPerPage;
        for (int i = 0; i < this.lines; i++) {
            for (int j = 0; j < this.columns; j++) {
                boolean mask = this.mask[i][j];
                if (!mask && rowIndex < list.size()) {
                    SQLRowAccessor row = list.get(rowIndex);
                    renderer.paintLabel(g, row, j * w + imageableX, i * h + imageableY, w, h);
                }
                rowIndex++;
            }

        }
        return PAGE_EXISTS;
    }
}
