package org.jopenchart;

import java.util.ArrayList;
import java.util.List;

public class DataModel1D extends DataModel {
    private final List<Number> l = new ArrayList<Number>();

    public DataModel1D() {

    }

    public DataModel1D(Number[] data) {
        for (int i = 0; i < data.length; i++) {
            Number number = data[i];
            l.add(number);
        }
    }

    public DataModel1D(List<Number> list) {
        this.addAll(list);
    }

    public void addAll(List<Number> data) {
        l.addAll(data);
    }

    public int getSize() {
        return l.size();
    }

    public void setValueAt(int index, Number value) {

        ensureCapacity(index);

        l.set(index, value);
    }

    private void ensureCapacity(int index) {
        for (int i = l.size(); i <= index; i++) {
            l.add(null);
        }
    }

    public Number getValueAt(int index) {
        ensureCapacity(index);

        return l.get(index);
    }

    public Number getMaxValue() {
        Number max = 0;

        for (Number b : this.l) {
            if (max == null) {
                max = b;
            } else if (b != null && b.doubleValue() > max.doubleValue()) {
                max = b;
            }
        }
        return max;
    }

    public Number getMinValue() {
        Number min = 0;

        for (Number b : this.l) {
            if (min == null) {
                min = b;
            } else if (b != null && b.doubleValue() < min.doubleValue()) {
                min = b;
            }
        }
        return min;
    }

    public void clear() {
        for (int i = 0; i < this.getSize(); i++) {
            this.setValueAt(i, null);
        }
    }
}
