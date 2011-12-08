package org.jopenchart.sample.model;

import java.util.Random;

import org.jopenchart.DataModel1D;

public class DataModel1DFuzzy extends DataModel1D {
    public DataModel1DFuzzy() {
        Thread t = new Thread(new Runnable() {

            public void run() {
                Random r = new Random();
                for (int i = 0; i < getSize(); i++) {

                    setValueAt(i, 40 + r.nextInt(60)/* (i%4+1)*10 */);

                    fireDataModelChanged();
                }
                while (true) {
                    for (int i = 0; i < 1; i++) {

                        setValueAt(i, 40 + r.nextInt(60)/* (i%4+1)*10 */);
                        try {
                            Thread.sleep(50, 0);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        fireDataModelChanged();
                    }
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public int getSize() {
        return 6;
    }
}
