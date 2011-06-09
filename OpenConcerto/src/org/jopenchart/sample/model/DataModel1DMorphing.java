package org.jopenchart.sample.model;

import java.util.ArrayList;
import java.util.List;

import org.jopenchart.DataModel1D;
import org.jopenchart.DataModelListener;

public class DataModel1DMorphing extends DataModel1D implements DataModelListener {
    private static final int STEP = 5;

    DataModel1D src;

    int count = 50;

    final List<Double> dy;

    private Number initialValue;

    public DataModel1DMorphing(final DataModel1D src, final Number initialValue) {
        this.src = src;
        this.initialValue = initialValue;
        this.src.addDataModelListener(this);
        final int size = src.getSize();
        dy = new ArrayList<Double>(size);
        for (int i = 0; i < size; i++) {
            dy.add(null);
        }
        computeDy();
        Thread t = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    for (int i = 0; i < count; i++) {
                        Number minValue = initialValue;
                        if (initialValue == null) {
                            minValue = src.getMinValue();
                        }
                        for (int j = 0; j < size; j++) {
                            Number srcValue = src.getValueAt(j);
                            if (srcValue != null) {

                                Number current = getValueAt(j);
                                if (current == null) {

                                    setValueAt(j, minValue);
                                } else if (dy.get(j) != null) {
                                    double currentDy = dy.get(j).doubleValue();
                                    double newValue = current.doubleValue() + currentDy;
                                    setValueAt(j, new Double(newValue));
                                    current = getValueAt(j);
                                    if (newValue > srcValue.doubleValue()) {
                                        setValueAt(j, srcValue);
                                        dy.set(j, null);
                                    }
                                }
                            }

                        }
                        try {
                            Thread.sleep(5, 0);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        fireDataModelChanged();
                    }
                    synchronized (DataModel1DMorphing.this.src) {
                        try {
                            System.out.println("wait");
                            src.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public int getSize() {

        return src.getSize();
    }

    public void dataChanged() {
        computeDy();

        count += STEP;
        System.out.println("notyfy");
        synchronized (src) {
            src.notifyAll();
        }

    }

    private void computeDy() {
        for (int j = 0; j < getSize(); j++) {
            Number v = src.getValueAt(j);
            if (v != null) {
                double currentDy = v.doubleValue() / STEP;
                dy.set(j, new Double(currentDy));
            }

        }
    }

    @Override
    public Number getMinValue() {
        if (this.initialValue == null) {
            return super.getMinValue();
        }
        return this.initialValue;
    }
}
