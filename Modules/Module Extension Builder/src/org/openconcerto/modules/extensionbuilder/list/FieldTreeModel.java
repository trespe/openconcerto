package org.openconcerto.modules.extensionbuilder.list;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.table.FieldDescriptor;
import org.openconcerto.modules.extensionbuilder.table.TableDescritor;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;

public class FieldTreeModel extends DefaultTreeModel {

    private Extension extension;
    private Set<FieldDescriptor> allFields = new HashSet<FieldDescriptor>();

    public FieldTreeModel(Extension extension) {
        super(null, false);
        this.extension = extension;
    }

    public void fillFromTable(String table) {
        System.out.println("FieldTreeModel.fillFromTable():" + table);
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        if (table == null) {
            this.setRoot(root);
            return;
        }
        addToTreeNode(root, table, 0);
        this.setRoot(root);
    }

    void addToTreeNode(DefaultMutableTreeNode node, String tableName, int depth) {
        if (depth > 4) {
            return;
        }
        depth++;
        // 1/ On regarde depuis le CreateTable
        TableDescritor desc = this.extension.getTableListDescriptor(tableName);
        if (desc == null) {
            // 2/ On regarde dans la base
            try {
                SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(tableName);
                if (t != null) {
                    desc = new TableDescritor(t.getName());
                    Set<SQLField> fields = t.getFields();
                    for (SQLField sqlField : fields) {
                        String name = sqlField.getName();
                        String table = null;
                        final SQLTable foreignTable = sqlField.getForeignTable();
                        if (foreignTable != null && foreignTable.getDBRoot().equals(t.getDBRoot())) {
                            table = foreignTable.getName();
                        }
                        if (!sqlField.isPrimaryKey() && !name.equals("ORDRE") && !name.equals("ARCHIVE") && !name.startsWith("ID_USER_COMMON")) {
                            FieldDescriptor d = new FieldDescriptor(tableName, name, "", "", "", table);
                            desc.add(d);
                        }
                    }
                    desc.sortFields();
                }
            } catch (Exception e) {
                desc = null;
            }
        }
        if (desc != null) {
            List<FieldDescriptor> fields = desc.getFields();
            for (FieldDescriptor fieldDescriptor : fields) {
                final DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldDescriptor);
                node.add(newChild);
                this.allFields.add(fieldDescriptor);
                if (fieldDescriptor.getForeignTable() != null) {
                    addToTreeNode(newChild, fieldDescriptor.getForeignTable(), depth);
                    newChild.setAllowsChildren(true);
                } else {
                    newChild.setAllowsChildren(false);
                }
            }
            this.setRoot(root);
        }
    }

    public boolean containsFieldDescritor(FieldDescriptor d) {
        return this.allFields.contains(d);
    }
}
