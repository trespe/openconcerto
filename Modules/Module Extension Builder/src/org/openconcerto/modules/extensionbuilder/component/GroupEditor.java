package org.openconcerto.modules.extensionbuilder.component;

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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.tree.ReorderableJTree;

public class GroupEditor extends AbstractSplittedPanel {

    private ComponentDescritor n;
    private ItemTreeModel newModel;
    private JTree tree;
    private Group tableGroup;

    public GroupEditor(ComponentDescritor n, Extension extension) {
        super(extension);
        this.n = n;
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

        panel.add(new JLabelBold("Champs et groupes"), c);
        newModel = new ItemTreeModel();
        tree = new ReorderableJTree() {
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject == null) {
                    return "null";
                }
                if (userObject instanceof Group) {
                    Group d = (Group) userObject;
                    return " " + d.getId();
                }
                return userObject.toString();
            }
        };
        tree.setModel(newModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        final DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                final JLabel r = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                final ActivableMutableTreeNode tr = (ActivableMutableTreeNode) value;
                if (!tr.isActive()) {
                    r.setForeground(Color.LIGHT_GRAY);
                }
                if (tr.getUserObject() instanceof Item) {
                    r.setText(((Item) tr.getUserObject()).getId());
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
        final JButton addGroupButton = new JButton("Ajouter un groupe");
        panel.add(addGroupButton, c);
        c.gridy++;
        final JButton showHideButton = new JButton("Afficher / Masquer");
        showHideButton.setEnabled(false);
        panel.add(showHideButton, c);

        c.gridy++;
        final JCheckBox hideCheckbox = new JCheckBox("Afficher les champs masqués");
        hideCheckbox.setSelected(true);

        panel.add(hideCheckbox, c);

        // init

        addGroupButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addNewGroup();

            }

        });

        showHideButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newModel.toggleActive(tree.getSelectionPath());
            }
        });

        hideCheckbox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                newModel.setShowAll(hideCheckbox.isSelected());
                setMainTable(n.getTable());
                setRightPanel(new JPanel());
            }
        });

        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final Object selectedValue = tree.getSelectionPath();
                showHideButton.setEnabled((selectedValue != null));
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
                    setRightPanel(new ItemEditor(i, n));
                }
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
            DefaultMutableTreeNode newNode = new ActivableMutableTreeNode(new Group("group" + node.getParent().getChildCount() + 1));
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            parent.insert(newNode, parent.getIndex(node));
            newModel.reload();
            tree.setSelectionPath(new TreePath(newModel.getPathToRoot(newNode)));
        }

    }

    public void setMainTable(String table) {
        n.setTable(table);

        initGroupFromTable(extension.getAllKnownFieldName(table));
        newModel.fillFromGroup(n, this.tableGroup);

        tree.expandRow(0);
    }

    public void initGroupFromTable(List<String> fields) {
        System.out.println("GroupEditor.initGroupFromTable()");
        System.out.println("GroupEditor.initGroupFromTable Component group");

        this.tableGroup = new Group(n.getId());
        for (String field : fields) {
            Item i = n.getItemFromId(field);
            Item newItem = new Item(field);

            if (i != null) {
                System.out.println("GroupEditor.initGroupFromTable() searching found: " + i + ":" + i.getLocalHint());
                newItem.setLocalHint(new LayoutHints(i.getLocalHint()));
            }
            this.tableGroup.add(newItem);
        }
        System.out.println("GroupEditor.initGroupFromTable Table group");

    }

    public Group getFilteredGroup() {
        // Parcours du Tree
        Group filteredGroup = new Group(n.getId());
        if (n.getTable() == null) {
            throw new IllegalStateException("Not table defined for " + n);
        }
        walk(newModel, filteredGroup, newModel.getRoot());
        filteredGroup = (Group) filteredGroup.getItem(0);

        return filteredGroup;
    }

    protected void walk(ItemTreeModel model, Group gr, Object o) {
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
                    final SQLTable table = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable(n.getTable());
                    if (table.contains(userObject.getId())) {
                        SQLField field = table.getField(userObject.getId());
                        if (!field.isPrimaryKey() && !field.getName().endsWith("ORDRE") && !field.getName().endsWith("ARCHIVE")) {
                            gr.add(item);
                        }
                    }
                }

            }
        }
    }

}
