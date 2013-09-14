package org.openconcerto.modules.extensionbuilder.component;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.openconcerto.modules.extensionbuilder.table.ForbiddenFieldName;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;

public class GroupTreeModel extends DefaultTreeModel {

    private boolean showAll = true;
    private GroupDescritor componentDescriptor;

    public GroupTreeModel() {
        super(null, false);

    }

    public void setShowAll(boolean b) {
        if (b != showAll) {
            this.showAll = b;
            System.err.println("GroupTreeModel.setShowAll()" + b);
            reload();
        }
    }

    public void fillFromGroup(GroupDescritor n, Group tableGroup) {
        if (n == null) {
            throw new IllegalArgumentException("null GroupDescriptor");
        }
        this.componentDescriptor = n;
        final ActivableMutableTreeNode root = new ActivableMutableTreeNode(null);
        root.setActive(true);
        if (tableGroup == null) {
            this.setRoot(root);
            return;
        }
        System.err.println("Desc:");
        System.err.println(n.getGroup().printTree());
        System.err.println("Table:");
        System.err.println(tableGroup.printTree());

        addToTreeNode(root, n.getGroup(), n, 0);
        // Add from tableGroup
        // Group filteredTableGroup = new Group(tableGroup.getId());
        // filteredTableGroup = Group.copy(tableGroup, filteredTableGroup);
        for (int i = 0; i < tableGroup.getSize(); i++) {
            Item item = tableGroup.getItem(i);
            final String id = item.getId();
            if (!n.getGroup().contains(id) && ForbiddenFieldName.isAllowed(id) && !id.equals("ID")) {
                addToTreeNode((DefaultMutableTreeNode) root.getFirstChild(), item, n, 0);
            }
        }

        // /////////
        this.setRoot(root);
    }

    void addToTreeNode(DefaultMutableTreeNode node, Item item, GroupDescritor n, int depth) {
        System.err.println("GroupTreeModel.addToTreeNode():" + node + " item:" + item + " Desc:" + n + " Depth:" + depth);

        if (depth > 50) {
            return;
        }
        depth++;
        final ActivableMutableTreeNode newChild = new ActivableMutableTreeNode(item);

        newChild.setActive(n.containsGroupId(item.getId()));
        if (showAll || newChild.isActive()) {
            node.add(newChild);
        }
        if (item instanceof Group) {
            final Group gr = (Group) item;
            final int childCount = gr.getSize();
            for (int i = 0; i < childCount; i++) {
                final Item it = gr.getItem(i);
                addToTreeNode(newChild, it, n, depth);
            }
            newChild.setAllowsChildren(true);
        } else {
            newChild.setAllowsChildren(false);
        }
    }

    public void toggleActive(TreePath selectionPath) {
        if (selectionPath == null) {
            return;
        }
        final ActivableMutableTreeNode n = (ActivableMutableTreeNode) selectionPath.getLastPathComponent();

        final Item item = (Item) n.getUserObject();
        if (item instanceof Group) {
            // A Group is always active
            return;
        }
        n.setActive(!n.isActive());
        if (n.isActive()) {
            this.componentDescriptor.updateGroupFrom(this);
        } else {
            this.componentDescriptor.removeGroup(item);
        }
        reload(n);
        componentDescriptor.fireGroupChanged();
    }

    @Override
    protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children) {
        // To update preview while reordering
        super.fireTreeNodesInserted(source, path, childIndices, children);
        componentDescriptor.fireGroupChanged();
    }

    @Override
    public boolean isLeaf(Object node) {
        final ActivableMutableTreeNode n = (ActivableMutableTreeNode) node;
        if (n.getUserObject() == null)
            return super.isLeaf(node);
        return !(n.getUserObject() instanceof Group);
    }
}
