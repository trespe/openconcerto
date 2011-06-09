package org.jopenchart.sample.devguide;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;

import org.jopenchart.ChartPanel;
import org.jopenchart.Label;
import org.jopenchart.piechart.PieChart;

public class PieChartSample {
    /**
     * @param args
     */
    public static void main(String[] args) {
        chart1();
        chart2();
    }

    private static void chart1() {
        PieChart c = new PieChart();
        c.addLabel(new Label("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        c.addLabel(new Label("BBBBBBBBBB"));
        c.addLabel(new Label("CCCCCCCCCCCCCCCCC"));
        c.setDimension(new Dimension(400, 200));

        ArrayList<Number> l = new ArrayList<Number>();
        l.add(50);
        l.add(20);
        l.add(10);
        c.setData(l);
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }

    private static void chart2() {
        PieChart c = new PieChart();
        c.addLabel(new Label("AAAAAA"));
        c.addLabel(new Label("BBBB"));
        c.addLabel(new Label("CCC"));
        c.setDimension(new Dimension(400, 200));

        ArrayList<Number> l = new ArrayList<Number>();
        l.add(50);
        l.add(20);
        l.add(10);
        c.setData(l);
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setLocation(0, 200);
        f.setVisible(true);
    }
}
