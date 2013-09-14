package org.openconcerto.modules.extensionbuilder.menu.mainmenu;

public class MenuDescriptor {

    public static final String CREATE = "create";
    public static final String LIST = "list";
    public static final String GROUP = "group";
    private String id;
    private String listId;
    private String componentId;
    private String type;
    private String insertInMenu;

    public MenuDescriptor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    /**
     * 
     * @return "list" or "create"
     * */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInsertInMenu() {
        return insertInMenu;
    }

    public void setInsertInMenu(String insertInMenu) {
        this.insertInMenu = insertInMenu;
    }

}
