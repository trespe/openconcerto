package org.openconcerto.modules.extensionbuilder.translation.menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.openconcerto.modules.extensionbuilder.AbstractSplittedPanel;
import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.component.ActivableMutableTreeNode;
import org.openconcerto.modules.extensionbuilder.menu.mainmenu.MenuItemTreeModel;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;

public class MenuTranslationPanel extends AbstractSplittedPanel {

    private MenuItemTreeModel newModel;
    private JTree tree;

    public MenuTranslationPanel(Extension extension) {
        super(extension);
        fillModel();
    }

    public void fillModel() {
        newModel.fillFromDescriptor(extension);
        tree.setModel(newModel);
        tree.expandRow(0);
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
        tree = new JTree() {
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

        // init

        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                final TreePath selectionPath = tree.getSelectionPath();
                if (selectionPath == null) {
                    setRightPanel(new JPanel());
                } else {
                    Item i = (Item) ((DefaultMutableTreeNode) selectionPath.getLastPathComponent()).getUserObject();
                    setRightPanel(new MenuTranslationItemEditor(i, extension));
                }
            }
        });
        return panel;
    }

}