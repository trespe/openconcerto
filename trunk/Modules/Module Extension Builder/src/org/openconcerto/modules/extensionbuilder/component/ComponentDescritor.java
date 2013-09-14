package org.openconcerto.modules.extensionbuilder.component;


public class ComponentDescritor extends GroupDescritor {

    private String table;

    public ComponentDescritor(String id) {
        super(id);

    }

    public void setTable(String table) {
        this.table = table;

    }

    public String getTable() {
        return table;
    }


}
