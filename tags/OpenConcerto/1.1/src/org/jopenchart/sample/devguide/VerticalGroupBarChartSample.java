package org.jopenchart.sample.devguide;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.ChartPanel;
import org.jopenchart.DataModel1D;
import org.jopenchart.barchart.VerticalGroupBarChart;

public class VerticalGroupBarChartSample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        VerticalGroupBarChart c = new VerticalGroupBarChart();
        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.decode("#4A79A5"));
        colors.add(Color.decode("#639ACE"));
        colors.add(Color.decode("#94BAE7"));
        c.setColors(colors);
        // c.setBackgroundRenderer(new SolidAreaRenderer(Color.pink));
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0"));
        axis.addLabel(new AxisLabel("100"));
        axis.addLabel(new AxisLabel("200"));
        c.setLeftAxis(axis);

        Axis axisX = new Axis("x");
        axisX.addLabel(new AxisLabel("Janvier", 1));
        axisX.addLabel(new AxisLabel("Février", 2));
        axisX.addLabel(new AxisLabel("Mars", 3));
        axisX.addLabel(new AxisLabel("Avril", 4));
        axisX.addLabel(new AxisLabel("Mai", 5));
        axisX.addLabel(new AxisLabel("Juin", 6));
        axisX.addLabel(new AxisLabel("Juillet", 7));
        axisX.addLabel(new AxisLabel("Août", 8));
        axisX.addLabel(new AxisLabel("Septembre", 9));
        axisX.addLabel(new AxisLabel("Octobre", 10));
        axisX.addLabel(new AxisLabel("Novembre", 11));
        axisX.addLabel(new AxisLabel("Décembre", 12));
        c.setBottomAxis(axisX);

        c.setDimension(new Dimension(800, 200));
        c.setBarWidth(14);
        c.addModel(new DataModel1D(new Float[] { 5f, 10f, 50f, 30f, 200f, 20f, 30f, 120f, 180f, 70f, 10f, 120f }));
        c.addModel(new DataModel1D(new Float[] { 20f, 90f, 55f, 70f, 120f, 180f, 70f, 90f, 55f, 70f, 20f, 150f }));
        c.addModel(new DataModel1D(new Float[] { 28f, 9f, 60f, 50f, 100f, 150f, 40f, 60f, 50f, 100f, 30f, 180f }));
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("VerticalGroupBarChart");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);

    }

}
