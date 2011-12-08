package org.jopenchart;

import java.util.ArrayList;
import java.util.List;

public class DataModelMultiple extends DataModel implements DataModelListener {
    private final List<DataModel1D> models = new ArrayList<DataModel1D>();

    public DataModelMultiple() {

    }

    public DataModelMultiple(List<List<Number>> multipleData) {
        for (List<Number> list : multipleData) {
            this.addModel(new DataModel1D(list));
        }
    }

    public DataModel1D getModel(int index) {
        return this.models.get(index);
    }

    public void addModel(DataModel1D model) {
        this.models.add(model);
        model.addDataModelListener(this);
    }

    public int getSize() {
        return this.models.size();
    }

    public Number getMaxValue() {
        Number max = null;

        for (DataModel1D model : models) {
            Number b = model.getMaxValue();
            if (max == null) {
                max = b;
            } else if (b != null && b.doubleValue() > max.doubleValue()) {
                max = b;
            }
        }
        return max;
    }

    public Number getMinValue() {
        Number min = null;
        for (DataModel1D model : models) {
            Number b = model.getMinValue();
            if (min == null) {
                min = b;
            } else if (b != null && b.doubleValue() < min.doubleValue()) {
                min = b;
            }
        }
        return min;
    }

    @Override
    public void dataChanged() {
        fireDataModelChanged();
    }

    public void removeAll() {
        this.models.clear();
    }

    @Override
    public synchronized int getState() {
        for (DataModel1D model : models) {
            if (model.getState() == LOADING) {
                return LOADING;
            }
        }
        return LOADED;
    }
}
