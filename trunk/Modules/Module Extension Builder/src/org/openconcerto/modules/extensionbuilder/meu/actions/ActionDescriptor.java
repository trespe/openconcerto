package org.openconcerto.modules.extensionbuilder.meu.actions;

public class ActionDescriptor {

    private String id;
    private String location; // header, popup, both
    private String table;
    private String componentId;
    public static final String LOCATION_HEADER = "header";
    public static final String LOCATION_POPUP = "popup";
    public static final String LOCATION_HEADER_POPUP = "header,popup";

    public ActionDescriptor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

}
