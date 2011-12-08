package org.jopenchart;

import java.util.ArrayList;
import java.util.List;

public class DataModel {
    private List<DataModelListener> listeners = new ArrayList<DataModelListener>();
    private Chart chart;
    public static final int LOADING = 0;
    public static final int LOADED = 1;
    private int state = LOADED;

    public void addDataModelListener(DataModelListener l) {
        listeners.add(l);

    }

    public void fireDataModelChanged() {
        for (DataModelListener listener : listeners) {
            listener.dataChanged();
        }
    }

    public void setChart(Chart chart) {
        this.chart = chart;
    }

    public Chart getChart() {
        return chart;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
    }
}
