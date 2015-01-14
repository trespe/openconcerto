package org.openconcerto.modules.extensionbuilder.list;

import java.util.ArrayList;
import java.util.List;

public class ListDescriptor {

    private final List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
    private String id;

    private String mainTable;

    public ListDescriptor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMainTable(String mainTable) {
        this.mainTable = mainTable;
    }

    public String getMainTable() {
        return mainTable;
    }

    public List<ColumnDescriptor> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public void add(ColumnDescriptor f) {
        this.columns.add(f);

    }

    public int getColumnCount() {
        return this.columns.size();
    }

    public void removeAllColumns() {
        this.columns.clear();
    }
}
