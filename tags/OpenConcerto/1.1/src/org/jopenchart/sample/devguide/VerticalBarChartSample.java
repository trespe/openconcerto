package org.jopenchart.sample.devguide;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.ChartPanel;
import org.jopenchart.barchart.VerticalBarChart;
import org.jopenchart.sample.model.DataModel1DDynamic;

public class VerticalBarChartSample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        VerticalBarChart c = new VerticalBarChart();
        c.setColor(new Color(0, 155, 100));
        // c.setBackgroundRenderer(new SolidAreaRenderer(Color.pink));
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0"));
        axis.addLabel(new AxisLabel("500 €"));
        axis.addLabel(new AxisLabel("1000 €"));
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
        c.setBarWidth(40);
        c.setSpaceBetweenBars(20);
        c.setDimension(new Dimension(800, 400));

        c.setDataModel(new DataModel1DDynamic(c));
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);

    }
}
