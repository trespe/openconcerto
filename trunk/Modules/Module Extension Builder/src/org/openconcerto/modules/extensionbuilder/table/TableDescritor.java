package org.openconcerto.modules.extensionbuilder.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Log;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ComponentDescritor;
import org.openconcerto.modules.extensionbuilder.menu.mainmenu.MenuDescriptor;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLTable;

public class TableDescritor {
    private String name;
    private List<FieldDescriptor> fields = new ArrayList<FieldDescriptor>();

    public TableDescritor(String string) {
        this.name = string;

    }

    public void createElement(final Extension ext) {
        // Create elements
        ComptaPropsConfiguration conf = ComptaPropsConfiguration.getInstanceCompta();
        DBRoot root = conf.getRootSociete();
        if (conf.getDirectory().getElement(name) == null) {
            final SQLTable table = root.getTable(name);
            final SQLElement e = new SQLElement("ext." + name, "ext." + name, table) {

                @Override
                protected List<String> getListFields() {
                    return new ArrayList<String>(0);
                }

                @Override
                protected SQLComponent createComponent() {

                    for (final ComponentDescritor cDescriptor : ext.getCreateComponentList()) {
                        if (cDescriptor.getTable().equals(table.getTable().getName())) {

                            final GroupSQLComponent gComponent = new GroupSQLComponent(this, cDescriptor.getGroup());
                            return gComponent;

                        }
                    }
                    JOptionPane.showMessageDialog(new JFrame(), "Unable to create default creation component for table " + name);
                    return null;
                }
            };
            conf.getDirectory().addSQLElement(e);
            Log.get().info("Autocreate element for table: " + table.getName());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FieldDescriptor> getFields() {
        return fields;
    }

    public void add(FieldDescriptor f) {
        fields.add(f);

    }

    public void remove(FieldDescriptor field) {
        fields.remove(field);

    }

    @Override
    public String toString() {
        return name;
    }

    public void sortFields() {
        Collections.sort(this.fields, new Comparator<FieldDescriptor>() {

            @Override
            public int compare(FieldDescriptor o1, FieldDescriptor o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

    }

}
