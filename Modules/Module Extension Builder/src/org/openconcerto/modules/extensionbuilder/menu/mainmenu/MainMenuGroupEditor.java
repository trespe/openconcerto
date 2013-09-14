package org.openconcerto.modules.extensionbuilder.menu.mainmenu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ActivableMutableTreeNode;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.tree.ReorderableJTree;

public class MainMenuGroupEditor extends AbstractSplittedPanel {

    private MenuItemTreeModel newModel;
    private JTree tree;

    public MainMenuGroupEditor(Extension extension) {
        super(extension);
        fillModel();
    }

    public void fillModel() {
        newModel.fillFromDescriptor(extension);
        tree.setModel(newModel);
        expand();

    }

    private void expand() {
        tree.expandRow(0);

        final List<MenuDescriptor> m = new ArrayList<MenuDescriptor>();
        m.addAll(extension.getCreateMenuList());
        m.addAll(extension.getRemoveMenuList());
        for (MenuDescriptor menuDescriptor : m) {
            final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node = e.nextElement();
                final Object userObject = node.getUserObject();
                if (userObject != null) {
                    final String nodeLabel = ((Item) userObject).getId();
                    if (nodeLabel != null && nodeLabel.equals(menuDescriptor.getId())) {
                        final TreePath path = new TreePath(((DefaultMutableTreeNode) node.getParent()).getPath());
                        tree.expandPath(path);
                    }
                }
            }
        }
    }

    @Override
    public JComponent createLeftComponent() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(2, 2, 2, 0);

        panel.add(new JLabel("Menus"), c);
        newModel = new MenuItemTreeModel();
        tree = new ReorderableJTree();
        tree.setModel(newModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        final DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                final ActivableMutableTreeNode tr = (ActivableMutableTreeNode) value;
                if (tr.getUserObject() instanceof Item) {
                    final String id = ((Item) tr.getUserObject()).getId();
                    value = id;
                }
                final JLabel r = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (tr.getUserObject() instanceof Item) {
                    final String id = ((Item) tr.getUserObject()).getId();
                    if (extension.getCreateMenuItemFromId(id) != null) {
                        r.setForeground(new Color(50, 80, 150));
                    }
                }
                if (!tr.isActive()) {
                    r.setForeground(Color.LIGHT_GRAY);
                }
                return r;
            }
        };

        treeRenderer.setLeafIcon(null);
        tree.setCellRenderer(treeRenderer);
        final JScrollPane comp2 = new JScrollPane(tree);
        comp2.setMinimumSize(new Dimension(250, 150));
        comp2.setPreferredSize(new Dimension(250, 150));
        c.weighty = 1;
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(comp2, c);

        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.insets = new Insets(2, 2, 2, 0);
        final JButton addGroupButton = new JButton("Ajouter un menu");
        panel.add(addGroupButton, c);
        c.gridy++;
        final JButton addItemButton = new JButton("Ajouter une action");
        panel.add(addItemButton, c);
        c.gridy++;
        final JButton removeButton = new JButton("Supprimer ");
        removeButton.setEnabled(false);
        panel.add(removeButton, c);

        c.gridy++;
        final JCheckBox hideCheckbox = new JCheckBox("Afficher les champs masqués");
        hideCheckbox.setSelected(true);
        panel.add(hideCheckbox, c);

        // Listeners
        addGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addNewGroup();
            }

        });
        addItemButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addNewItem();
            }

        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                    if (node.getUserObject() != null) {
                        String idToDelete = ((Item) node.getUserObject()).getId();
                        extension.removeCreateMenuForId(idToDelete);
                        extension.setChanged();
                        fillModel();
                    }

                }
            }
        });
        hideCheckbox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                newModel.setShowAll(hideCheckbox.isSelected());
                newModel.fillFromDescriptor(extension);
            }

        });
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                    if (node.getUserObject() != null) {
                        String selectedId = ((Item) node.getUserObject()).getId();
                        removeButton.setEnabled(extension.getCreateMenuItemFromId(selectedId) != null);
                    }
                } else {
                    removeButton.setEnabled(false);
                }

            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    newModel.toggleActive(tree.getSelectionPath());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                final TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath == null) {
                    setRightPanel(new JPanel());
                } else {
                    Item i = (Item) ((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getUserObject();
                    setRightPanel(new MenuItemEditor(newModel, i, extension));
                }
            }
        });
        tree.getModel().addTreeModelListener(new TreeModelListener() {

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                // Postpone expand because default behaviour is collapsing tree
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        expand();
                    }
                });
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
            }

            @Override
            public void treeNodesChanged(TreeModelEvent e) {
            }
        });
        return panel;
    }

    protected void addNewGroup() {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) (tree.getModel().getRoot())).getFirstChild();
        DefaultMutableTreeNode node = root;
        if (node.getChildCount() > 0) {
            node = (DefaultMutableTreeNode) node.getFirstChild();
        }
        if (tree.getSelectionPath() != null) {
            node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        }
        if (node != root) {
            final String newGroupId = "group" + node.getParent().getChildCount() + 1;
            DefaultMutableTreeNode newNode = new ActivableMutableTreeNode(new Group(newGroupId));
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            parent.insert(newNode, parent.getIndex(node));

            final MenuDescriptor desc = new MenuDescriptor(newGroupId);
            desc.setType(MenuDescriptor.GROUP);
            desc.setInsertInMenu(((Item) parent.getUserObject()).getId());
            extension.addCreateMenu(desc);
            extension.setChanged();

            newModel.reload();
            tree.setSelectionPath(new TreePath(newModel.getPathToRoot(newNode)));
        }
        extension.setChanged();
    }

    protected void addNewItem() {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) (tree.getModel().getRoot())).getFirstChild();
        DefaultMutableTreeNode node = root;
        if (node.getChildCount() > 0) {
            node = (DefaultMutableTreeNode) node.getFirstChild();
        }
        if (tree.getSelectionPath() != null) {
            node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        }
        if (node != root) {
            final String newActionId = "action" + node.getParent().getChildCount() + 1;
            DefaultMutableTreeNode newNode = new ActivableMutableTreeNode(new Item(newActionId));
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            parent.insert(newNode, parent.getIndex(node));
            final MenuDescriptor desc = new MenuDescriptor(newActionId);
            desc.setType(MenuDescriptor.CREATE);
            desc.setInsertInMenu(((Item) parent.getUserObject()).getId());
            extension.addCreateMenu(desc);
            extension.setChanged();
            newModel.reload();
            tree.setSelectionPath(new TreePath(newModel.getPathToRoot(newNode)));
        }

    }

    // public Group getFilteredGroup() {
    // // Parcours du Tree
    // Group filteredGroup = new Group(n.getId());
    //
    // walk(newModel, filteredGroup, newModel.getRoot());
    // filteredGroup = (Group) filteredGroup.getItem(0);
    // filteredGroup.dumpTwoColumn();
    // return filteredGroup;
    // }

    protected void walk(MenuItemTreeModel model, Group gr, Object o) {
        int cc = model.getChildCount(o);
        for (int i = 0; i < cc; i++) {
            ActivableMutableTreeNode child = (ActivableMutableTreeNode) model.getChild(o, i);
            if (child.isActive()) {
                final Item userObject = (Item) child.getUserObject();
                if (userObject instanceof Group) {
                    final Group item = new Group(userObject.getId());
                    item.setLocalHint(new LayoutHints(userObject.getLocalHint()));
                    gr.add(item);
                    walk(model, item, child);
                } else {
                    final Item item = new Item(userObject.getId());
                    item.setLocalHint(new LayoutHints(userObject.getLocalHint()));
                    gr.add(item);
                }

            }
        }
    }

}
