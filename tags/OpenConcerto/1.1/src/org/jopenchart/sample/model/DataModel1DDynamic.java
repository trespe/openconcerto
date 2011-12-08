package org.jopenchart.sample.model;

import java.util.Random;

import org.jopenchart.DataModel1D;
import org.jopenchart.barchart.VerticalBarChart;

public class DataModel1DDynamic extends DataModel1D {
    public DataModel1DDynamic(final VerticalBarChart c) {
        Thread t = new Thread(new Runnable() {

            public void run() {
                Random r = new Random();
                for (int i = 0; i < getSize(); i++) {

                    final int value = 40 + r.nextInt(60);
                    if (value > c.getModel(0).getMaxValue().longValue()) {
                        c.getLeftAxis().getLabels().get(1).setLabel("" + value / 2);
                        c.getLeftAxis().getLabels().get(2).setLabel("" + value);
                    }
                    setValueAt(i, value/* (i%4+1)*10 */);
                    try {
                        Thread.sleep(500, 0);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    fireDataModelChanged();
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public int getSize() {
        return 20;
    }

}
