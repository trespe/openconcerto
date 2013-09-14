package org.openconcerto.modules.extensionbuilder.table;

public class ElementDescriptor {
    private String id;
    private String tableName;

    public ElementDescriptor(String id, String tableName) {
        this.id = id;
        this.tableName = tableName;
    }

    public String getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "ElementDescriptor id: " + id + " table: " + tableName;
    }
}
