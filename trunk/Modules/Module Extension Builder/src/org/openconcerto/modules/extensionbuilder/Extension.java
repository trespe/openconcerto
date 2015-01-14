package org.openconcerto.modules.extensionbuilder;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Log;
import org.openconcerto.erp.config.MenuAndActions;
import org.openconcerto.erp.config.MenuManager;
import org.openconcerto.erp.modules.MenuContext;
import org.openconcerto.modules.extensionbuilder.component.ComponentDescritor;
import org.openconcerto.modules.extensionbuilder.list.ColumnDescriptor;
import org.openconcerto.modules.extensionbuilder.list.ListDescriptor;
import org.openconcerto.modules.extensionbuilder.menu.mainmenu.MenuDescriptor;
import org.openconcerto.modules.extensionbuilder.meu.actions.ActionDescriptor;
import org.openconcerto.modules.extensionbuilder.table.AllTableListModel;
import org.openconcerto.modules.extensionbuilder.table.ElementDescriptor;
import org.openconcerto.modules.extensionbuilder.table.FieldDescriptor;
import org.openconcerto.modules.extensionbuilder.table.TableDescritor;
import org.openconcerto.modules.extensionbuilder.translation.Translation;
import org.openconcerto.modules.extensionbuilder.translation.action.ActionTranslation;
import org.openconcerto.modules.extensionbuilder.translation.field.FieldTranslation;
import org.openconcerto.modules.extensionbuilder.translation.field.TableTranslation;
import org.openconcerto.modules.extensionbuilder.translation.menu.MenuTranslation;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.group.modifier.AddGroupModifier;
import org.openconcerto.ui.group.modifier.AddItemModifier;
import org.openconcerto.ui.group.modifier.MoveToGroupModifier;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.i18n.TranslationManager;

public class Extension {

    // Descriptors of the extension
    private List<ElementDescriptor> elementList = new ArrayList<ElementDescriptor>();
    private List<TableDescritor> createTableList = new ArrayList<TableDescritor>();
    private List<TableDescritor> modifyTableList = new ArrayList<TableDescritor>();
    private List<ListDescriptor> createListList = new ArrayList<ListDescriptor>();
    private List<TableTranslation> tableTranslations = new ArrayList<TableTranslation>();
    private List<FieldTranslation> fieldTranslations = new ArrayList<FieldTranslation>();
    private List<MenuTranslation> menuTranslations = new ArrayList<MenuTranslation>();
    private List<ActionTranslation> actionTranslations = new ArrayList<ActionTranslation>();
    private List<ComponentDescritor> createComponentList = new ArrayList<ComponentDescritor>();
    private List<ComponentDescritor> modifyComponentList = new ArrayList<ComponentDescritor>();
    private List<MenuDescriptor> createMenuList = new ArrayList<MenuDescriptor>();
    private List<MenuDescriptor> removeMenuList = new ArrayList<MenuDescriptor>();
    private List<ActionDescriptor> createActionList = new ArrayList<ActionDescriptor>();

    // Listeners
    private List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    private String name;
    private boolean notSaved;
    private boolean autoStart;
    private boolean isStarted;

    public Extension(String name) {
        this.name = name;
        this.notSaved = true;
    }

    public void clearAll() {
        elementList.clear();
        createTableList.clear();
        modifyTableList.clear();
        createListList.clear();
        tableTranslations.clear();
        fieldTranslations.clear();
        menuTranslations.clear();
        actionTranslations.clear();
        createComponentList.clear();
        modifyComponentList.clear();
        createMenuList.clear();
        removeMenuList.clear();
        createActionList.clear();
        listeners.clear();

        notSaved = true;
        autoStart = false;
        isStarted = false;

    }

    boolean isStarted() {
        return this.isStarted;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void start(DBRoot root, boolean inModuleStart) throws SQLException {

        // Ensure that database is configured
        boolean databaseOk = setupDatabase(root);
        if (!databaseOk) {
            Log.get().severe("Extension " + this.getName() + " not started due to database error");
            return;
        }
        // Register translations
        registerTranslations();
        // Create menus
        if (!inModuleStart) {
            final MenuAndActions copy = MenuManager.getInstance().copyMenuAndActions();
            registerMenuActions(copy);
            MenuManager.getInstance().setMenuAndActions(copy);
        }
        Log.get().info("Extension " + this.getName() + " started");
        this.isStarted = true;
        this.autoStart = true;
        fireChanged();
    }

    private void registerTranslations() {
        final String locale = Locale.getDefault().toString();
        for (MenuTranslation mTranslation : this.menuTranslations) {
            if (locale.equals(mTranslation.getLocale())) {
                TranslationManager.getInstance().setTranslationForMenu(mTranslation.getId(), mTranslation.getLabel());
            }
        }
        for (ActionTranslation mTranslation : this.actionTranslations) {
            if (locale.equals(mTranslation.getLocale())) {
                TranslationManager.getInstance().setTranslationForAction(mTranslation.getId(), mTranslation.getLabel());
            }
        }
        for (FieldTranslation mTranslation : this.fieldTranslations) {
            if (locale.equals(mTranslation.getLocale())) {
                TranslationManager.getInstance().setTranslationForItem(mTranslation.getTableName() + "." + mTranslation.getFieldName(), mTranslation.getLabel());
            }
        }
        for (TableTranslation mTranslation : this.tableTranslations) {
            if (locale.equals(mTranslation.getLocale())) {
                // FIXME voir avec Sylvain
            }
        }
    }

    private void registerMenuActions(MenuAndActions menuAndActions) {
        // register actions
        for (final MenuDescriptor element : getCreateMenuList()) {
            if (element.getType().equals(MenuDescriptor.CREATE)) {
                Log.get().info("Registering action for menu creation id:'" + element.getId() + "'");

                menuAndActions.putAction(new CreateFrameAbstractAction() {

                    @Override
                    public JFrame createFrame() {

                        JFrame editFrame = new JFrame();
                        String componentId = element.getComponentId();
                        if (componentId == null) {
                            throw new IllegalStateException("No ComponentId for menu " + element.getId());
                        }
                        ComponentDescritor n = getCreateComponentFromId(componentId);
                        if (n == null) {
                            throw new IllegalStateException("No ComponentDescritor for " + componentId);
                        }
                        final SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(n.getTable());
                        if (t == null) {
                            throw new IllegalStateException("No table  " + n.getTable());
                        }
                        final SQLElement element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(t);

                        final GroupSQLComponent gComponent = new GroupSQLComponent(element, n.getGroup());
                        editFrame.setTitle(EditFrame.getCreateMessage(element));
                        editFrame.setContentPane(new EditPanel(gComponent, EditMode.CREATION));
                        editFrame.pack();

                        return editFrame;

                    }
                }, element.getId(), true);

            } else if (element.getType().equals(MenuDescriptor.LIST)) {
                menuAndActions.putAction(new CreateFrameAbstractAction() {

                    @Override
                    public JFrame createFrame() {
                        final String componentId = element.getListId();
                        final ListDescriptor listDesc = getCreateListFromId(componentId);
                        if (listDesc == null) {
                            throw new IllegalStateException("No ListDescriptor  " + componentId);
                        }
                        final SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(listDesc.getMainTable());
                        if (t == null) {
                            throw new IllegalStateException("No table  " + listDesc.getMainTable());
                        }
                        final SQLElement element = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElement(t);
                        final ListSQLRequest req = new ListSQLRequest(element.getTable(), Collections.EMPTY_LIST);
                        final SQLTableModelSourceOnline source = new SQLTableModelSourceOnline(req);
                        final List<SQLTableModelColumn> cols = new ArrayList<SQLTableModelColumn>();
                        for (ColumnDescriptor cDesc : listDesc.getColumns()) {
                            final String fieldspath = cDesc.getFieldsPaths();
                            final String[] paths = fieldspath.split(",");
                            final Set<FieldPath> fps = new LinkedHashSet<FieldPath>();
                            for (int i = 0; i < paths.length; i++) {
                                // LOCAL, id_batiment.id_site.nom
                                final SQLName name = SQLName.parse(paths[i].trim());

                                final PathBuilder p = new PathBuilder(element.getTable());
                                final int stop = name.getItemCount() - 1;
                                for (int j = 0; j < stop; j++) {
                                    String it = name.getItem(j);
                                    p.addForeignField(it);
                                }
                                final FieldPath fp = new FieldPath(p.build(), name.getName());
                                fps.add(fp);

                            }
                            cols.add(new BaseSQLTableModelColumn(cDesc.getId(), String.class) {

                                @Override
                                protected Object show_(SQLRowAccessor r) {
                                    final List<String> l = new ArrayList<String>();
                                    for (final FieldPath fp : fps) {
                                        final String string = fp.getString((SQLRowValues) r);
                                        if (string != null)
                                            l.add(string);
                                    }
                                    return CollectionUtils.join(l, " ");
                                }

                                @Override
                                public Set<FieldPath> getPaths() {
                                    return fps;
                                }
                            });

                        }

                        source.getColumns().addAll(cols);

                        final IListe list = new IListe(source);
                        final IListFrame editFrame = new IListFrame(new ListeAddPanel(element, list));
                        editFrame.pack();
                        return editFrame;

                    }
                }, element.getId(), true);
            } else if (element.getType().equals(MenuDescriptor.LIST)) {
                // No action to register
            } else {
                Log.get().warning("unknown type " + element.getType());
            }
        }

        // System.err.println("****" + MenuManager.getInstance().getActionForId("test1"));
        //
        // final MenuAndActions copy = MenuManager.getInstance().copyMenuAndActions();
        // // create group
        // final Group group = copy.getGroup();
        initMenuGroup(menuAndActions.getGroup());
        // MenuManager.getInstance().setMenuAndActions(copy);
        // System.err.println("*******" + MenuManager.getInstance().getActionForId("test1"));
    }

    public void initMenuGroup(final Group group) {
        for (MenuDescriptor element : getCreateMenuList()) {
            if (element.getType().equals(MenuDescriptor.GROUP)) {
                AddGroupModifier add = new AddGroupModifier(element.getId());
                if (group.getDescFromID(element.getId()) == null) {
                    // only add if not exists
                    add.applyOn(group);
                }
                String menuId = element.getInsertInMenu();
                Group dest = (Group) group.getDescFromID(menuId);
                if (dest != null) {
                    MoveToGroupModifier mode = new MoveToGroupModifier(element.getId(), dest);
                    mode.applyOn(group);
                } else {
                    Log.get().severe("No group " + menuId + " found to move item " + element.getId());
                }
            }
        }
        // create items
        for (MenuDescriptor element : getCreateMenuList()) {
            if (!element.getType().equals(MenuDescriptor.GROUP)) {
                AddItemModifier add = new AddItemModifier(element.getId());
                if (group.getDescFromID(element.getId()) == null) {
                    // only add if not exists
                    add.applyOn(group);
                }

                String menuId = element.getInsertInMenu();
                Group dest = (Group) group.getDescFromID(menuId);
                if (dest != null) {
                    MoveToGroupModifier mode = new MoveToGroupModifier(element.getId(), dest);
                    mode.applyOn(group);
                } else {
                    Log.get().severe("No group " + menuId + " found to move group " + element.getId());
                }
            }
        }
        for (MenuDescriptor element : getRemoveMenuList()) {
            String eId = element.getId();
            Item item = group.getDescFromID(eId);
            if (item != null) {
                item.setLocalHint(item.getLocalHint().getBuilder().setVisible(false).build());
            } else {
                Log.get().severe("No Item " + eId + " found in group " + group.getId());
            }
        }
        System.err.println("Extension.initMenuGroup()" + group.printTree());

    }

    public ComponentDescritor getCreateComponentFromId(String id) {
        for (ComponentDescritor menuDescriptor : this.createComponentList) {
            if (menuDescriptor.getId().equals(id)) {
                return menuDescriptor;
            }
        }
        return null;
    }

    private boolean setupDatabase(DBRoot root) throws SQLException {
        List<ChangeTable<?>> changesToApply = new ArrayList<ChangeTable<?>>();
        List<SQLCreateTable> createToApply = new ArrayList<SQLCreateTable>();
        // Create fields and tables if needed
        final List<TableDescritor> t = new ArrayList<TableDescritor>();
        t.addAll(this.createTableList);
        t.addAll(this.modifyTableList);
        Set<String> tableNames = new HashSet<String>();
        for (TableDescritor tDesc : t) {
            String tableName = tDesc.getName();
            tableNames.add(tableName);
            final SQLTable table = root.getTable(tableName);
            final ChangeTable<?> createTable;
            if (table == null) {
                createTable = new SQLCreateTable(root, tableName);
                createToApply.add((SQLCreateTable) createTable);
            } else {
                createTable = new AlterTable(table);

            }
            // fields creation
            boolean mustAdd = false;
            for (FieldDescriptor fDesc : tDesc.getFields()) {
                final SQLField f = (table == null) ? null : table.getFieldRaw(fDesc.getName());
                if (f == null) {
                    final String type = fDesc.getType();
                    if (type.equals(FieldDescriptor.TYPE_STRING)) {
                        int l = 256;
                        try {
                            l = Integer.parseInt(fDesc.getLength());
                        } catch (Exception e) {
                            Log.get().log(Level.WARNING, "Extension: unable to parse length: " + fDesc.getLength(), e);
                        }
                        createTable.addVarCharColumn(fDesc.getName(), l);
                    } else if (type.equals(FieldDescriptor.TYPE_INTEGER)) {
                        int defaultVal = 0;
                        try {
                            defaultVal = Integer.parseInt(fDesc.getDefaultValue());
                        } catch (Exception e) {
                            Log.get().log(Level.WARNING, "Extension: unable to parse default integer value : " + fDesc.getDefaultValue(), e);
                        }
                        createTable.addIntegerColumn(fDesc.getName(), defaultVal);
                    } else if (type.equals(FieldDescriptor.TYPE_DECIMAL)) {
                        BigDecimal defaultVal = BigDecimal.ZERO;
                        try {
                            defaultVal = new BigDecimal(fDesc.getDefaultValue());
                        } catch (Exception e) {
                            Log.get().log(Level.WARNING, "Extension: unable to parse default bigdecimal value : " + fDesc.getDefaultValue(), e);
                        }
                        createTable.addNumberColumn(fDesc.getName(), BigDecimal.class, defaultVal, false);
                    } else if (type.equals(FieldDescriptor.TYPE_BOOLEAN)) {
                        String defaultValue = "false";
                        if (fDesc.getDefaultValue() != null && fDesc.getDefaultValue().equals("true")) {
                            defaultValue = "true";
                        }
                        createTable.addColumn(fDesc.getName(), "boolean", defaultValue, false);
                    } else if (type.equals(FieldDescriptor.TYPE_DATE)) {
                        createTable.addColumn(fDesc.getName(), "date");
                    } else if (type.equals(FieldDescriptor.TYPE_TIME)) {
                        createTable.addColumn(fDesc.getName(), "time");
                    } else if (type.equals(FieldDescriptor.TYPE_DATETIME)) {
                        createTable.addDateAndTimeColumn(fDesc.getName());
                    } else if (type.equals(FieldDescriptor.TYPE_REF)) {
                        // created later
                        mustAdd = false;
                    }
                    mustAdd = true;
                } else {
                    // Le champs existe, on ne fait rien
                    // checker les types
                }
            }
            if (mustAdd && !(createTable instanceof SQLCreateTable)) {
                changesToApply.add(createTable);
            }
        }
        // Let's do it
        // FIXME : if changesToApply create field that createToApply : bing
        if (!createToApply.isEmpty()) {
            root.createTables(createToApply);
        }
        if (!changesToApply.isEmpty()) {
            for (String change : ChangeTable.cat(changesToApply)) {
                root.getDBSystemRoot().getDataSource().execute(change);
            }

        }
        // Refetch if needed
        if (!changesToApply.isEmpty() || !createToApply.isEmpty()) {
            root.getSchema().updateVersion();
            root.refetch(tableNames);
            Log.get().info("Fetching table changes (" + changesToApply.size() + " fields and " + createToApply.size() + " tables)");
        }
        // Compute foreign keys to create
        changesToApply.clear();
        for (TableDescritor tDesc : t) {
            final SQLTable table = root.getTable(tDesc.getName());
            for (FieldDescriptor fDesc : tDesc.getFields()) {
                final SQLField f = (table == null) ? null : table.getFieldRaw(fDesc.getName());
                if (f == null && fDesc.getType().equals(FieldDescriptor.TYPE_REF)) {
                    final String fTableName = fDesc.getForeignTable();
                    final SQLTable fTable = root.getTable(fTableName);
                    if (fTable != null) {
                        final AlterTable mTable = new AlterTable(table);
                        mTable.addForeignColumn(fDesc.getName(), fTable);
                        changesToApply.add(mTable);
                    } else {
                        JOptionPane.showMessageDialog(new JFrame(), "L'extension ne peut pas s'installer car la table " + fTableName + " n'existe pas.");
                        return false;
                    }
                }
            }
        }
        // Create foreign keys
        if (!changesToApply.isEmpty()) {
            for (String change : ChangeTable.cat(changesToApply)) {
                root.getDBSystemRoot().getDataSource().execute(change);
            }
            root.getSchema().updateVersion();
            root.refetch(tableNames);
            Log.get().info("Fetching " + changesToApply.size() + " foreign fields creation");
        }
        // Create elements for created tables
        for (TableDescritor tDesc : t) {

            tDesc.createElement(this);
        }
        return true;
    }

    public void stop() {
        this.isStarted = false;
        this.autoStart = false;
        Log.get().info("Extension " + this.getName() + " stopped");
        // TODO : remove menu changes
        fireChanged();
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public void importFromXML(String xml) {

        System.out.println("Extension.importFromXML():" + xml);
        if (xml.length() > 0) {
            final SAXBuilder sxb = new SAXBuilder();
            try {
                final Document doc = sxb.build(new StringReader(xml));
                final Element root = doc.getRootElement();
                if (root.getAttributeValue("autostart", "false").equals("true")) {
                    this.autoStart = true;
                }
                // elements parsing
                final List<Element> elements = root.getChildren("element");
                for (Element eElement : elements) {
                    final String id = eElement.getAttributeValue("id");
                    final String tableName = eElement.getAttributeValue("tableName");
                    ElementDescriptor eDesc = new ElementDescriptor(id, tableName);
                    // TODO : fkeyaction
                    this.elementList.add(eDesc);
                }
                // tables parsing
                final List<Element> tables = root.getChildren("table");
                for (Element eTable : tables) {
                    final String type = eTable.getAttributeValue("type");
                    final String name = eTable.getAttributeValue("name");
                    final TableDescritor tDesc = new TableDescritor(name);
                    final List<Element> fields = eTable.getChildren("field");
                    for (Element field : fields) {
                        FieldDescriptor f = createFieldDescriptorFrom(name, field);
                        tDesc.add(f);
                    }
                    if (tDesc.getFields().size() > 0) {
                        if (type.equals("create")) {
                            this.createTableList.add(tDesc);
                        } else if (type.equals("modify")) {
                            this.modifyTableList.add(tDesc);
                        } else {
                            throw new IllegalStateException("Unknown table type: " + type);
                        }
                    }

                }
                // translations
                final List<Element> translations = root.getChildren("translation");
                for (Element eTranslation : translations) {
                    final String lang = eTranslation.getAttributeValue("locale");
                    if (lang == null) {
                        throw new IllegalArgumentException("no locale found in translation element");
                    }
                    final List<Element> tTables = eTranslation.getChildren("element");
                    for (Element element : tTables) {
                        final String tableName = element.getAttributeValue("refid");
                        final TableTranslation t = new TableTranslation(lang, tableName);
                        t.setSingular(element.getAttributeValue("singular"));
                        t.setPlural(element.getAttributeValue("plural"));
                        this.tableTranslations.add(t);
                        final List<Element> tFields = element.getChildren("item");
                        for (Element elementF : tFields) {
                            final FieldTranslation tF = new FieldTranslation(lang, tableName, elementF.getAttributeValue("id"));
                            tF.setLabel(elementF.getAttributeValue("label"));
                            tF.setDocumentation(elementF.getAttributeValue("doc"));
                            this.fieldTranslations.add(tF);
                        }
                    }

                    final List<Element> tMenu = eTranslation.getChildren("menu");
                    for (Element element : tMenu) {
                        final MenuTranslation t = new MenuTranslation(lang, element.getAttributeValue("refid"));
                        t.setLabel(element.getAttributeValue("label"));
                        this.menuTranslations.add(t);
                    }
                    final List<Element> tActions = eTranslation.getChildren("action");
                    for (Element element : tActions) {
                        final ActionTranslation t = new ActionTranslation(lang, element.getAttributeValue("refid"));
                        t.setLabel(element.getAttributeValue("label"));
                        this.actionTranslations.add(t);
                    }
                }

                // list parsing
                final List<Element> lists = root.getChildren("list");
                for (Element eList : lists) {
                    final String type = eList.getAttributeValue("type");
                    final String id = eList.getAttributeValue("id");
                    final String refid = eList.getAttributeValue("refid");

                    final ListDescriptor listDesc = new ListDescriptor(id);
                    String mainTable = this.getTableNameForElementId(refid);
                    if (mainTable == null) {
                        // fallback to table name
                        mainTable = refid;
                    }
                    listDesc.setMainTable(mainTable);
                    final List<Element> columns = eList.getChildren("column");
                    for (Element field : columns) {
                        ColumnDescriptor f = createColumnDescriptorFrom(mainTable, field);
                        listDesc.add(f);
                    }
                    if (listDesc.getColumnCount() > 0) {
                        if (type.equals("create")) {
                            this.createListList.add(listDesc);
                        } else {
                            throw new IllegalStateException("Unknown table type: " + type);
                        }
                    }

                } // component parsing
                final List<Element> components = root.getChildren("component");
                for (Element eList : components) {
                    final String type = eList.getAttributeValue("type");
                    if (type.equals("create")) {
                        final String id = eList.getAttributeValue("id");
                        final String table = eList.getAttributeValue("table");
                        final ComponentDescritor tDesc = new ComponentDescritor(id);
                        tDesc.setTable(table);
                        walkGroup(eList, tDesc.getGroup());
                        System.out.println("SimpleXMLAddon.importFromXML() " + tDesc);
                        this.createComponentList.add(tDesc);
                    }
                }
                // Menu
                final List<Element> menus = root.getChildren("menu");
                for (Element eList : menus) {
                    final String type = eList.getAttributeValue("type");
                    if (type.equals("create")) {
                        final List<Element> actions = eList.getChildren("action");
                        for (Element action : actions) {
                            final String id = action.getAttributeValue("id");
                            final String insertInMenu = action.getAttributeValue("insertInMenu");
                            final String actionType = action.getAttributeValue("type");
                            if (actionType.equals(MenuDescriptor.LIST)) {
                                // a list frame
                                final String listId = action.getAttributeValue("listId");
                                final MenuDescriptor mDesc = new MenuDescriptor(id);
                                mDesc.setInsertInMenu(insertInMenu);
                                mDesc.setType(actionType);
                                mDesc.setListId(listId);
                                this.createMenuList.add(mDesc);
                            } else if (actionType.equals(MenuDescriptor.CREATE)) {
                                // a create frame
                                final String componentId = action.getAttributeValue("componentId");
                                final MenuDescriptor mDesc = new MenuDescriptor(id);
                                mDesc.setInsertInMenu(insertInMenu);
                                mDesc.setType(actionType);
                                mDesc.setComponentId(componentId);
                                this.createMenuList.add(mDesc);
                            } else if (actionType.equals(MenuDescriptor.GROUP)) {
                                // a create frame
                                final MenuDescriptor mDesc = new MenuDescriptor(id);
                                mDesc.setInsertInMenu(insertInMenu);
                                mDesc.setType(actionType);
                                this.createMenuList.add(mDesc);
                            } else {
                                throw new IllegalStateException("Unknown action type " + actionType + " for action " + id);
                            }

                        }
                    } else if (type.equals("remove")) {
                        final List<Element> actions = eList.getChildren("action");
                        for (Element action : actions) {
                            final String id = action.getAttributeValue("id");
                            this.removeMenuList.add(new MenuDescriptor(id));
                        }
                    }
                }

                // Actions
                final List<Element> actions = root.getChildren("action");
                for (Element actionElement : actions) {
                    final String type = actionElement.getAttributeValue("type");
                    if (type.equals("create")) {

                        final String id = actionElement.getAttributeValue("id");
                        final String location = actionElement.getAttributeValue("location");
                        final String table = actionElement.getAttributeValue("table");
                        final String componentId = actionElement.getAttributeValue("componentId");
                        ActionDescriptor action = new ActionDescriptor(id);
                        action.setLocation(location);
                        action.setTable(table);
                        action.setComponentId(componentId);
                        this.createActionList.add(action);

                    } else {
                        // TODO: remove
                        throw new IllegalStateException("Unknown action type " + type);
                    }

                }

            } catch (Exception e) {
                System.err.println("SimpleXMLAddon.importFromXML(): parsing error :" + e.getMessage());
                e.printStackTrace();
            }
        }
        notSaved = false;

        fireChanged();
    }

    String toXML() {
        final Element rootElement = new Element("extension");
        rootElement.setAttribute("id", this.name);
        rootElement.setAttribute("autostart", String.valueOf(this.isStarted));
        rootElement.setAttribute("format", "1.0");
        final Document document = new Document(rootElement);

        // Element
        for (ElementDescriptor eDescriptor : this.elementList) {
            final Element eElement = new Element("element");
            eElement.setAttribute("tableName", eDescriptor.getTableName());
            eElement.setAttribute("id", eDescriptor.getId());
            // TODO: fkey action : <fkeyaction action="set_empty|cascade|restrict"
            // fields="id_language"/>
            rootElement.addContent(eElement);
        }

        // Table create
        for (TableDescritor tDescriptor : this.createTableList) {
            final Element eTable = new Element("table");
            eTable.setAttribute("type", "create");
            eTable.setAttribute("name", tDescriptor.getName());
            for (FieldDescriptor fDescriptor : tDescriptor.getFields()) {
                final Element eField = new Element("field");
                eField.setAttribute("name", fDescriptor.getName());
                eField.setAttribute("type", fDescriptor.getType());
                if (fDescriptor.getLength() != null) {
                    eField.setAttribute("length", fDescriptor.getLength());
                }
                if (fDescriptor.getDefaultValue() != null) {
                    eField.setAttribute("default", fDescriptor.getDefaultValue());
                }
                if (fDescriptor.getForeignTable() != null) {
                    eField.setAttribute("ftable", fDescriptor.getForeignTable());
                }
                eTable.addContent(eField);
            }
            rootElement.addContent(eTable);
        }

        // Table modify
        for (TableDescritor tDescriptor : this.modifyTableList) {
            final Element eTable = new Element("table");
            eTable.setAttribute("type", "modify");
            eTable.setAttribute("name", tDescriptor.getName());
            for (FieldDescriptor fDescriptor : tDescriptor.getFields()) {
                final Element eField = new Element("field");
                eField.setAttribute("name", fDescriptor.getName());
                eField.setAttribute("type", fDescriptor.getType());
                if (fDescriptor.getLength() != null) {
                    eField.setAttribute("length", fDescriptor.getLength());
                }
                if (fDescriptor.getDefaultValue() != null) {
                    eField.setAttribute("default", fDescriptor.getDefaultValue());
                }
                if (fDescriptor.getForeignTable() != null) {
                    eField.setAttribute("ftable", fDescriptor.getForeignTable());
                }
                eTable.addContent(eField);
            }
            rootElement.addContent(eTable);
        }
        // Translations
        final HashSet<String> locales = new HashSet<String>();
        for (Translation tr : tableTranslations) {
            locales.add(tr.getLocale());
        }
        for (Translation tr : fieldTranslations) {
            locales.add(tr.getLocale());
        }
        for (Translation tr : menuTranslations) {
            locales.add(tr.getLocale());
        }
        for (Translation tr : actionTranslations) {
            locales.add(tr.getLocale());
        }
        final List<String> lLocales = new ArrayList<String>(locales);
        Collections.sort(lLocales);
        for (String locale : lLocales) {
            final Element eTranslation = new Element("translation");
            eTranslation.setAttribute("locale", locale);
            rootElement.addContent(eTranslation);
            // Tables
            for (TableTranslation tTranslation : tableTranslations) {
                if (tTranslation.getLocale().equals(locale)) {
                    final Element eTable = new Element("element");
                    eTable.setAttribute("refid", tTranslation.getTableName());
                    final String singular = tTranslation.getSingular();
                    if (singular != null && !singular.isEmpty()) {
                        eTable.setAttribute("singular", singular);
                    }
                    final String plural = tTranslation.getPlural();
                    if (plural != null && !plural.isEmpty()) {
                        eTable.setAttribute("plural", plural);
                    }
                    for (FieldTranslation fTranslation : fieldTranslations) {
                        // Fields
                        if (fTranslation.getLocale().equals(locale) && fTranslation.getTableName().equals(tTranslation.getTableName())) {
                            final Element eField = new Element("item");
                            eField.setAttribute("id", fTranslation.getFieldName());
                            eField.setAttribute("label", fTranslation.getLabel());
                            if (fTranslation.getDocumentation() != null) {
                                eField.setAttribute("doc", fTranslation.getDocumentation());
                            }
                            eTable.addContent(eField);
                        }
                    }
                    eTranslation.addContent(eTable);

                }
            }
            // Menus
            for (MenuTranslation tMenu : menuTranslations) {
                if (tMenu.getLocale().equals(locale)) {
                    final Element eMenu = new Element("menu");
                    eMenu.setAttribute("refid", tMenu.getId());
                    eMenu.setAttribute("label", tMenu.getLabel());
                    eTranslation.addContent(eMenu);
                }
            }

            // Actions
            for (ActionTranslation tAction : actionTranslations) {
                if (tAction.getLocale().equals(locale)) {
                    final Element eMenu = new Element("action");
                    eMenu.setAttribute("refid", tAction.getId());
                    eMenu.setAttribute("label", tAction.getLabel());
                    eTranslation.addContent(eMenu);
                }
            }
        }

        // Actions
        for (ActionDescriptor action : this.createActionList) {
            final Element eAction = new Element("action");
            eAction.setAttribute("type", "create");
            eAction.setAttribute("id", action.getId());
            eAction.setAttribute("location", action.getLocation());
            eAction.setAttribute("table", action.getTable());
            eAction.setAttribute("componentId", action.getComponentId());
            rootElement.addContent(eAction);
        }
        // Menu create
        if (!this.createMenuList.isEmpty()) {
            final Element eMenu = new Element("menu");
            eMenu.setAttribute("type", "create");
            for (MenuDescriptor menu : this.createMenuList) {
                final Element eActionMenu = new Element("action");
                eActionMenu.setAttribute("id", menu.getId());
                eActionMenu.setAttribute("insertInMenu", menu.getInsertInMenu());
                final String type = menu.getType();
                eActionMenu.setAttribute("type", type);
                if (!type.equals(MenuDescriptor.CREATE) && !type.equals(MenuDescriptor.LIST) && !type.equals(MenuDescriptor.GROUP)) {
                    throw new IllegalStateException("Menu type " + type + " not supported");
                }

                if (type.endsWith("list") && menu.getListId() != null) {
                    eActionMenu.setAttribute("listId", menu.getListId());
                } else if (type.endsWith("create") && menu.getComponentId() != null) {
                    eActionMenu.setAttribute("componentId", menu.getComponentId());
                }
                eMenu.addContent(eActionMenu);
            }
            rootElement.addContent(eMenu);

        }
        // Menu remove
        if (!this.removeMenuList.isEmpty()) {
            final Element eMenu = new Element("menu");
            eMenu.setAttribute("type", "remove");
            for (MenuDescriptor menu : this.removeMenuList) {
                final Element eActionMenu = new Element("action");
                eActionMenu.setAttribute("id", menu.getId());
                eMenu.addContent(eActionMenu);
            }
            rootElement.addContent(eMenu);
        }
        // List create
        for (ListDescriptor listDescriptor : this.createListList) {
            final Element eList = new Element("list");
            eList.setAttribute("type", "create");
            eList.setAttribute("id", listDescriptor.getId());
            String refFromTable = getRefFromTable(listDescriptor.getMainTable());
            if (refFromTable == null) {
                refFromTable = listDescriptor.getMainTable();
            }
            if (refFromTable != null) {
                eList.setAttribute("refid", refFromTable);
                for (ColumnDescriptor fieldDescriptor : listDescriptor.getColumns()) {
                    final Element eField = new Element("column");
                    eField.setAttribute("id", fieldDescriptor.getId());
                    eField.setAttribute("fields", fieldDescriptor.getFieldsPaths());
                    eField.setAttribute("style", fieldDescriptor.getStyle());
                    eList.addContent(eField);
                }

                rootElement.addContent(eList);
            }

        }
        // Component create
        for (ComponentDescritor componentDescriptor : this.createComponentList) {
            final Element eComponent = new Element("component");
            eComponent.setAttribute("type", "create");
            eComponent.setAttribute("id", componentDescriptor.getId());
            eComponent.setAttribute("table", componentDescriptor.getTable());
            appendGroup(eComponent, componentDescriptor.getGroup());

            rootElement.addContent(eComponent);
        }
        // Component modify
        for (ComponentDescritor componentDescriptor : this.modifyComponentList) {
            System.out.println(componentDescriptor);
            throw new IllegalAccessError("Not yet implemented");
        }
        // Output
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        final BufferedOutputStream oStream = new BufferedOutputStream(bOut);
        try {
            out.output(document, oStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            return bOut.toString("utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void appendGroup(Element eComponent, Group group) {
        int size = group.getSize();
        for (int i = 0; i < size; i++) {
            Item it = group.getItem(i);
            final Element gr;
            if (it instanceof Group) {
                gr = new Element("group");
                appendGroup(gr, (Group) it);
            } else {
                gr = new Element("item");
            }
            gr.setAttribute("id", it.getId());
            if (it.getLocalHint() != null) {
                final LayoutHints hints = it.getLocalHint();
                final String type;
                if (hints.largeWidth()) {
                    type = "verylarge";
                } else if (hints.fillWidth()) {
                    type = "large";
                } else {
                    type = "normal";
                }
                gr.setAttribute("type", type);
                if (hints.isSeparated()) {
                    gr.setAttribute("isSeparated", "true");
                }
                if (hints.showLabel()) {
                    gr.setAttribute("showLabel", "true");
                }
            }

            eComponent.addContent(gr);

        }

    }

    private void walkGroup(Element e, Group group) {
        @SuppressWarnings("unchecked")
        final List<Element> elements = e.getChildren();
        for (Element element : elements) {
            String id = element.getAttributeValue("id", "unknown");
            String type = element.getAttributeValue("type", "default");
            String showLabel = element.getAttributeValue("showLabel", "true");
            String isSeparated = element.getAttributeValue("isSeparated", "false");
            if (element.getName().equals("item")) {
                final Item it = new Item(id);
                if (type.equals("large")) {
                    it.setLocalHint(new LayoutHints(false, false, showLabel.equals("true"), isSeparated.equals("true"), true, false));
                } else if (type.equals("verylarge")) {
                    it.setLocalHint(new LayoutHints(true, false, showLabel.equals("true"), isSeparated.equals("true"), true, false));
                } else {
                    it.setLocalHint(new LayoutHints(false, false, showLabel.equals("true"), isSeparated.equals("true"), false, false));
                }
                System.out.println("Extension.walkGroup()" + it + " " + it.getLocalHint() + " from " + type);
                group.add(it);

            } else if (element.getName().equals("group")) {
                final Group g = new Group(id);
                group.add(g);
                walkGroup(element, g);
            } else {
                throw new IllegalStateException("Unknown element: " + element.getName());
            }
        }
    }

    private FieldDescriptor createFieldDescriptorFrom(String table, Element field) {
        FieldDescriptor f = new FieldDescriptor(table, field.getAttributeValue("name"), field.getAttributeValue("type"), field.getAttributeValue("default"), field.getAttributeValue("length"),
                field.getAttributeValue("ftable"));
        Element child = field.getChild("field");
        if (child != null) {
            f.setLink(createFieldDescriptorFrom(field.getAttributeValue("ftable"), child));
        }
        return f;
    }

    private ColumnDescriptor createColumnDescriptorFrom(String table, Element field) {
        final ColumnDescriptor f = new ColumnDescriptor(field.getAttributeValue("id"));
        f.setFieldsPaths(field.getAttributeValue("fields"));
        f.setStyle(field.getAttributeValue("style"));
        return f;
    }

    private void fireChanged() {
        for (ChangeListener listener : listeners) {
            listener.stateChanged(new ChangeEvent(this));
        }

    }

    public List<TableDescritor> getCreateTableList() {
        return createTableList;
    }

    public void addCreateTable(TableDescritor value) {
        this.createTableList.add(value);
        setChanged();
    }

    public void removeCreateTable(TableDescritor value) {
        this.createTableList.remove(value);
        setChanged();
    }

    public List<TableDescritor> getModifyTableList() {
        return modifyTableList;
    }

    public List<ListDescriptor> getCreateListList() {
        return createListList;
    }

    public ListDescriptor getCreateListFromId(String id) {
        for (ListDescriptor listDescriptor : this.createListList) {
            if (listDescriptor.getId().equals(id)) {
                return listDescriptor;
            }
        }
        return null;
    }

    public void addCreateList(ListDescriptor item) {
        this.createListList.add(item);
        setChanged();

    }

    public void removeCreateList(ListDescriptor item) {
        this.createListList.remove(item);
        setChanged();
    }

    public void addChangeListener(ChangeListener listener) {
        if (!this.listeners.contains(listener))
            this.listeners.add(listener);

    }

    public TableDescritor getOrCreateTableDescritor(String tableName) {

        for (TableDescritor td : this.modifyTableList) {
            if (td.getName().equalsIgnoreCase(tableName)) {
                return td;
            }
        }
        // create table descritor for the table
        final TableDescritor td = new TableDescritor(tableName);
        this.modifyTableList.add(td);
        setChanged();
        return td;
    }

    public SQLTable getSQLTable(TableDescritor tableDesc) {
        try {
            SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(tableDesc.getName());
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isNotSaved() {
        return this.notSaved;
    }

    public void setChanged() {
        this.notSaved = true;
        this.fireChanged();
        String xml = this.toXML();
        System.out.println(xml);
    }

    // FIXME: filter textfield pour eviter les pb d'insert/update
    // TODO: eviter les doublons lors du renommage

    public void save() {
        String xml = this.toXML();
        System.out.println(xml);
        // delete old version
        deleteFromDB();
        // insert new version
        final SQLTable extensionTable = getExtensionTable();
        SQLRowValues v = new SQLRowValues(extensionTable);
        v.put("IDENTIFIER", this.getName());
        v.put("XML", this.toXML());
        try {
            v.insert();
            this.notSaved = false;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(new JFrame(), "Error while saving extension");
        }

        this.fireChanged();
    }

    private SQLTable getExtensionTable() {
        return ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(ExtensionBuilderModule.TABLE_NAME);
    }

    private void deleteFromDB() {
        final SQLTable extensionTable = getExtensionTable();
        String query = "DELETE FROM " + extensionTable.getSQL() + " WHERE \"IDENTIFIER\" = " + SQLBase.quoteStringStd(this.getName());
        extensionTable.getDBSystemRoot().getDataSource().execute(query);
    }

    public List<String> getAllKnownTableNames() {
        final List<String> l = new ArrayList<String>();
        final Set<String> s = new HashSet<String>();
        for (SQLTable t : AllTableListModel.getAllDatabaseTables()) {
            s.add(t.getName());
        }
        for (TableDescritor td : this.getCreateTableList()) {
            s.add(td.getName());
        }
        s.remove("FWK_MODULE_METADATA");
        s.remove("FWK_SCHEMA_METADATA");
        s.remove("FWK_UNDEFINED_IDS");
        s.remove(ExtensionBuilderModule.TABLE_NAME);
        l.addAll(s);
        Collections.sort(l, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        return l;
    }

    public TableDescritor getTableListDescriptor(String tableName) {
        for (TableDescritor td : this.createTableList) {
            if (td.getName().equalsIgnoreCase(tableName)) {
                return td;
            }
        }
        return null;
    }

    public List<String> getTranslatedFieldOfTable(String tableName) {
        final List<String> l = new ArrayList<String>();
        for (FieldTranslation tr : this.fieldTranslations) {
            if (tr.getTableName().equals(tableName)) {
                l.add(tr.getFieldName());
            }
        }
        return l;
    }

    public TableTranslation getTableTranslation(String lang, String tableName) {
        for (TableTranslation tr : this.tableTranslations) {
            if (tr.getLocale().equals(lang) && tr.getTableName().equals(tableName)) {
                return tr;
            }
        }
        return null;
    }

    public String getFieldTranslation(String lang, String tableName, String fName) {
        for (FieldTranslation tr : this.fieldTranslations) {
            if (tr.getLocale().equals(lang) && tr.getTableName().equals(tableName) && tr.getFieldName().equals(fName)) {
                return tr.getLabel();
            }
        }
        return null;
    }

    public List<ComponentDescritor> getCreateComponentList() {
        return this.createComponentList;
    }

    public void addCreateComponent(ComponentDescritor desc) {
        this.createComponentList.add(desc);
    }

    public void removeCreateComponent(ComponentDescritor desc) {
        this.createComponentList.remove(desc);
        setChanged();
    }

    public List<MenuDescriptor> getCreateMenuList() {
        return createMenuList;
    }

    public void addCreateMenu(MenuDescriptor desc) {
        this.createMenuList.add(desc);
    }

    public MenuDescriptor getCreateMenuItemFromId(String id) {
        for (MenuDescriptor menuDescriptor : this.createMenuList) {
            if (menuDescriptor.getId().equals(id)) {
                return menuDescriptor;
            }
        }
        return null;
    }

    public List<MenuDescriptor> getRemoveMenuList() {
        return removeMenuList;
    }

    public void addRemoveMenu(MenuDescriptor desc) {
        this.removeRemoveMenuForId(desc.getId());
        this.removeMenuList.add(desc);
    }

    public List<String> getAllKnownFieldName(String tableName) {
        final Set<String> l = new HashSet<String>();
        // fields created in the extension

        final List<TableDescritor> desc = getCreateTableList();
        for (TableDescritor tableDescritor : desc) {
            if (tableDescritor.getName().equals(tableName)) {
                final List<FieldDescriptor> fDescs = tableDescritor.getFields();
                for (FieldDescriptor fieldDescriptor : fDescs) {
                    l.add(fieldDescriptor.getName());
                }
            }
        }
        // + champs dans la base
        final Set<SQLTable> tables = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTables();
        for (SQLTable sqlTable : tables) {
            final String tName = sqlTable.getName();
            if (tName.equals(tableName)) {
                Set<String> f = sqlTable.getFieldsName();
                for (String string : f) {
                    l.add(string);
                }

            }
        }

        return new ArrayList<String>(l);
    }

    public List<String> getAllKnownActionNames() {
        ArrayList<String> s = new ArrayList<String>();
        Collection<SQLElement> elements = ComptaPropsConfiguration.getInstanceCompta().getDirectory().getElements();
        for (SQLElement element : elements) {
            Collection<IListeAction> actions = element.getRowActions();
            for (IListeAction action : actions) {
                if (action instanceof RowAction) {
                    RowAction rAction = (RowAction) action;
                    final String id = rAction.getID();
                    if (id != null)
                        s.add(id);
                }
            }
        }
        Collections.sort(s);
        return s;
    }

    public List<String> getActionNames() {
        ArrayList<String> s = new ArrayList<String>();
        for (ActionDescriptor action : this.createActionList) {
            s.add(action.getId());
        }
        Collections.sort(s);
        return s;
    }

    public List<ActionDescriptor> getActionDescriptors() {
        Collections.sort(this.createActionList, new Comparator<ActionDescriptor>() {

            @Override
            public int compare(ActionDescriptor o1, ActionDescriptor o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return this.createActionList;
    }

    public boolean isEmpty() {
        return createTableList.isEmpty() && modifyTableList.isEmpty() && createListList.isEmpty() && tableTranslations.isEmpty() && fieldTranslations.isEmpty() && menuTranslations.isEmpty()
                && actionTranslations.isEmpty() && createComponentList.isEmpty() && modifyComponentList.isEmpty() && createMenuList.isEmpty() && removeMenuList.isEmpty() && createActionList.isEmpty();
    }

    public String getTableNameForElementId(String id) {
        for (ElementDescriptor element : this.elementList) {
            if (element.getId().equals(id)) {
                return element.getTableName();
            }
        }
        return null;
    }

    private String getRefFromTable(String table) {
        for (ElementDescriptor element : this.elementList) {
            if (element.getTableName().equals(table)) {
                return element.getId();
            }
        }
        return null;
    }

    public List<MenuTranslation> getMenuTranslations() {
        return menuTranslations;
    }

    public List<ActionTranslation> getActionTranslations() {
        return actionTranslations;
    }

    public List<FieldTranslation> getFieldTranslations() {
        return fieldTranslations;
    }

    public void removeChangeListener(ChangeListener listener) {
        this.listeners.remove(listener);

    }

    public void setName(String name) {
        if (!name.equals(this.name)) {
            deleteFromDB();
            this.name = name;
            save();
            setChanged();
        }
    }

    public void setupMenu(MenuContext ctxt) {
        final Group group = ctxt.getMenuAndActions().getGroup();
        initMenuGroup(group);
        registerMenuActions(ctxt.getMenuAndActions());
    }

    public void removeRemoveMenuForId(String id) {
        for (int i = removeMenuList.size() - 1; i >= 0; i--) {
            final MenuDescriptor m = removeMenuList.get(i);
            if (m.getId().equals(id)) {
                removeMenuList.remove(i);
            }
        }

    }

    public void removeCreateMenuForId(String id) {
        for (int i = createMenuList.size() - 1; i >= 0; i--) {
            final MenuDescriptor m = createMenuList.get(i);
            if (m.getId().equals(id)) {
                createMenuList.remove(i);
            }
        }

    }

    public MenuDescriptor getRemoveMenuItemFromId(String itemId) {
        for (MenuDescriptor m : removeMenuList) {
            if (m.getId().equals(itemId)) {
                return m;
            }
        }
        return null;
    }

    public void renameMenuItem(String previousId, String newId) {
        if (!previousId.equals(newId)) {
            final List<MenuDescriptor> descs = new ArrayList<MenuDescriptor>(createMenuList.size() + removeMenuList.size());
            descs.addAll(createMenuList);
            descs.addAll(removeMenuList);
            for (MenuDescriptor m : descs) {
                if (m.getId().equals(previousId)) {
                    m.setId(newId);
                }
            }

        }

    }

    public void moveMenuItem(String itemId, String parentId) {
        for (MenuDescriptor m : createMenuList) {
            if (m.getId().equals(itemId)) {
                m.setInsertInMenu(parentId);
            }
        }

    }

    public void setMenuTranslation(String id, String text, Locale locale) {
        MenuTranslation mTranslation = null;
        for (MenuTranslation mTr : this.menuTranslations) {
            if (mTr.getId().equals(id) && mTr.getLocale().equals(locale.toString())) {
                mTranslation = mTr;
                break;
            }

        }
        if (mTranslation == null) {
            mTranslation = new MenuTranslation(locale.toString(), id);
            this.menuTranslations.add(mTranslation);
        }
        mTranslation.setLabel(text);

    }
}
