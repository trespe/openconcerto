package org.openconcerto.modules.extensionbuilder.table;

public class FieldDescriptor {
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_TIME = "time";
    public static final String TYPE_DATETIME = "dateAndTime";
    public static final String TYPE_REF = "ref";
    private final String table;

    private String name;
    private String type;
    private String defaultValue;
    private String length;
    private FieldDescriptor link;
    private String foreignTable;

    public FieldDescriptor(String table, String name, String type, String defaultValue, String length, String foreingTable) {
        this.table = table;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.length = length;
        this.foreignTable = foreingTable;
    }

    public FieldDescriptor(FieldDescriptor f) {
        this.name = f.name;
        this.type = f.type;
        this.defaultValue = f.defaultValue;
        this.length = f.length;
        this.table = f.table;
        this.foreignTable = f.foreignTable;
    }

    @Override
    public String toString() {
        String string = this.table + " " + this.name + " type:" + this.type + " default:" + this.defaultValue + " l:" + this.length + " t:" + this.foreignTable;
        if (link != null) {
            string += " => " + link.toString();
        }
        return string;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getTable() {
        return table;
    }

    public void setLink(FieldDescriptor fieldDescriptor) {
        this.link = fieldDescriptor;
    }

    public FieldDescriptor getLink() {
        return link;
    }

    public void setForeignTable(String foreignTable) {
        this.foreignTable = foreignTable;
    }

    public String getForeignTable() {
        return foreignTable;
    }

    public String getExtendedLabel() {
        FieldDescriptor f = this;
        String label = f.getName();
        while (f.getLink() != null) {
            label += " / " + f.getLink().getName();
            f = f.getLink();
        }
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FieldDescriptor) {
            FieldDescriptor f = (FieldDescriptor) obj;
            if (getTable().equals(f.getTable()) && getName().equals(f.getTable())) {
                return getLink() != null && getLink().equals(f.getLink());
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.table.hashCode();
    }

    public String getPath() {
        FieldDescriptor f = this;
        String label = f.getName();
        while (f.getLink() != null) {
            label += "." + f.getLink().getName();
            f = f.getLink();
        }
        return label;
    }
}
