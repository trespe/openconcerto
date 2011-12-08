package org.jopenchart.sample.devguide;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.ChartPanel;
import org.jopenchart.DataModel1D;
import org.jopenchart.barchart.VerticalStackBarChart;

public class VerticalStackBarChartSample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        VerticalStackBarChart c = new VerticalStackBarChart();
        c.setColor(new Color(0, 155, 100));
        // c.setBackgroundRenderer(new SolidAreaRenderer(Color.pink));
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0%"));
        axis.addLabel(new AxisLabel("100%"));
        axis.addLabel(new AxisLabel("1000%"));
        c.setLeftAxis(axis);
        c.setDimension(new Dimension(400, 200));

        c.addModel(new DataModel1D(new Float[] { 5f, 10f, 50f, 30f, 200f, 20f, 30f }));
        c.addModel(new DataModel1D(new Float[] { 20f, 90f, 50f, 70f, 0f, 180f, 70f }));

        // c.setLowerRange(0);
        // c.setHigherRange(100);

        // c.setDataModel(new DataModel1DDynamic());
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);

    }

}
