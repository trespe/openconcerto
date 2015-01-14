package org.jopenchart.sample.devguide;

import java.awt.Dimension;

import javax.swing.JFrame;

import org.jopenchart.ChartPanel;
import org.jopenchart.gauge.AngularGauge;

public class GaugeSample {
    public static void main(String[] args) {
        AngularGauge c = new AngularGauge();
        c.setMinValue(0);
        c.setMaxValue(100);
        c.setValue(30);
        c.setDimension(new Dimension(800, 400));
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }
}
