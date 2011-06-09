package org.jopenchart;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

public class ChartPanel extends JPanel implements DataModelListener, MouseMotionListener {
    private Chart chart;

    public ChartPanel(Chart c) {
        this.chart = c;

        c.getDataModel().addDataModelListener(this);
        this.setMinimumSize(c.getDimension());
        this.setPreferredSize(c.getDimension());
        this.addMouseMotionListener(this);

    }

    @Override
    protected void paintComponent(Graphics g) {
        System.err.println("ChartPanel.paintComponent()");
        super.paintComponent(g);
        chart.render((Graphics2D) g);
    }

    public void dataChanged() {
        System.err.println("ChartPanel.dataChanged()");
        invalidate();
        repaint();

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Number n = chart.highLightAt(e.getX(), e.getY());
        this.setToolTipText(getToolTipTextFrom(n));
    }

    public String getToolTipTextFrom(Number n) {
        if (n == null) {
            return null;
        }
        return String.valueOf(n);
    }
}
