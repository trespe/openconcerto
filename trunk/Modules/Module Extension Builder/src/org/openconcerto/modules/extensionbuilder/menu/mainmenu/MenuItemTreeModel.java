package org.openconcerto.modules.extensionbuilder.menu.mainmenu;

import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import org.openconcerto.erp.config.MenuManager;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ActivableMutableTreeNode;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;

public class MenuItemTreeModel extends DefaultTreeModel {

    private boolean showAll = true;
    private Extension extension;

    public MenuItemTreeModel() {
        super(null, false);

    }

    public void setShowAll(boolean b) {
        this.showAll = b;
        System.err.println("MenuItemTreeModel.setShowAll(): " + b);
    }

    public void fillFromDescriptor(Extension extension) {

        if (extension == null) {
            throw new IllegalArgumentException("null extension");
        }
        if (this.extension != null) {
            // this.extension.removeChangeListener(this);
        }
        this.extension = extension;
        // this.extension.addChangeListener(this);
        final Group currentMenuGroup = MenuManager.getInstance().getGroup();
        Group menuGroup = Group.copy(currentMenuGroup, new Group(currentMenuGroup.getId()));

        extension.initMenuGroup(menuGroup);

        final ActivableMutableTreeNode root = new ActivableMutableTreeNode(null);
        root.setActive(true);
        if (menuGroup == null) {
            return;
        }
        // FIXME manque des items...
        System.out.println(MenuManager.getInstance().getGroup().printTree());

        addToTreeNode(root, menuGroup, 0);
        setRoot(root);

    }

    void addToTreeNode(DefaultMutableTreeNode node, Item item, int depth) {
        if (depth > 50) {
            return;
        }
        depth++;
        final ActivableMutableTreeNode newChild = new ActivableMutableTreeNode(item);
        newChild.setActive(isActive(item.getId()));
        if (showAll || newChild.isActive()) {
            node.add(newChild);
        }
        if (item instanceof Group) {
            final Group gr = (Group) item;
            final int childCount = gr.getSize();
            for (int i = 0; i < childCount; i++) {
                final Item it = gr.getItem(i);
                addToTreeNode(newChild, it, depth);
            }
            newChild.setAllowsChildren(true);
        } else {
            newChild.setAllowsChildren(false);
        }
    }

    private boolean isActive(String id) {
        List<MenuDescriptor> l = extension.getRemoveMenuList();
        for (MenuDescriptor menuDescriptor : l) {
            if (menuDescriptor.getId().equals(id)) {
                return false;
            }
        }
        return true;
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
        final String id = item.getId();
        setActive(n.isActive(), id);
    }

    public void setActive(boolean active, String id) {
        if (active) {
            extension.removeRemoveMenuForId(id);
        } else {
            extension.addRemoveMenu(new MenuDescriptor(id));
        }
        extension.setChanged();

        DefaultMutableTreeNode n = getNode(id);
        if (n != null) {
            if (n instanceof ActivableMutableTreeNode) {
                ((ActivableMutableTreeNode) n).setActive(active);
            }

            reload(n);
        }

    }

    @Override
    public boolean isLeaf(Object node) {
        final ActivableMutableTreeNode n = (ActivableMutableTreeNode) node;
        if (n.getUserObject() == null)
            return super.isLeaf(node);
        return !(n.getUserObject() instanceof Group);
    }

    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        Item it = (Item) ((DefaultMutableTreeNode) newChild).getUserObject();
        Group g = (Group) ((DefaultMutableTreeNode) parent).getUserObject();
        extension.moveMenuItem(it.getId(), g.getId());
        super.insertNodeInto(newChild, parent, index);
        extension.setChanged();
    }

    @SuppressWarnings("rawtypes")
    public void renameMenuItem(String previousId, String newId) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getRoot();

        for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements();) {
            final DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();

            final String idFromNode = getIdFromNode(current);
            if (idFromNode != null && idFromNode.equals(previousId)) {
                setId(current, newId);

                reload(current);

            }
        }

        extension.renameMenuItem(previousId, newId);
        extension.setChanged();

    }

    private void setId(DefaultMutableTreeNode node, String newId) {
        Object o = node.getUserObject();
        if (o == null)
            return;
        ((Item) o).setId(newId);

    }

    private static String getIdFromNode(DefaultMutableTreeNode node) {
        Object o = node.getUserObject();
        if (o == null)
            return null;
        return ((Item) o).getId();
    }

    @SuppressWarnings("rawtypes")
    public DefaultMutableTreeNode getNode(String id) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getRoot();

        for (Enumeration e = root.breadthFirstEnumeration(); e.hasMoreElements();) {
            final DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();

            final String idFromNode = getIdFromNode(current);
            if (idFromNode != null && idFromNode.equals(id)) {
                return current;

            }
        }
        return null;
    }
}
