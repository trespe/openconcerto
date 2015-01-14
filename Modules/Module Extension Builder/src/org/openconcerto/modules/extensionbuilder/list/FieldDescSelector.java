package org.openconcerto.modules.extensionbuilder.list;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.table.FieldDescriptor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.list.ReorderableJList;

public class FieldDescSelector extends JPanel {
    private FieldTreeModel treeModel;
    private DefaultListModel listModel;
    private final JTree tree;
    private JList list;
    private ListDescriptor listDescriptor;

    FieldDescSelector(ListDescriptor desc, Extension extension) {
        this.listDescriptor = desc;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(new JLabelBold("Champs disponibles"), c);
        c.gridx += 2;
        this.add(new JLabelBold("Colonnes affichés"), c);

        // Col 0
        c.gridx = 0;
        c.gridy++;
        treeModel = new FieldTreeModel(extension);
        this.tree = new JTree(treeModel) {
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject == null)
                    return "null";
                FieldDescriptor d = (FieldDescriptor) userObject;

                if (d.getForeignTable() != null) {
                    return " " + d.getName() + " référence vers la table " + d.getForeignTable();
                }
                return " " + d.getName();
            }
        };
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        final DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer();

        treeRenderer.setLeafIcon(null);
        treeRenderer.setOpenIcon(new ImageIcon(this.getClass().getResource("ref.png")));
        treeRenderer.setClosedIcon(new ImageIcon(this.getClass().getResource("ref.png")));
        tree.setCellRenderer(treeRenderer);
        c.gridheight = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(tree), c);
        // Col 1
        c.gridx = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTH;
        final JButton buttonAdd = new JButton(">");
        this.add(buttonAdd, c);
        c.gridy++;
        final JButton buttonRemove = new JButton("<");
        this.add(buttonRemove, c);
        // Col 2
        c.gridx = 2;
        c.gridy--;
        c.gridheight = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        listModel = new DefaultListModel() {
            @Override
            public void addElement(Object obj) {
                if (!(obj instanceof ColumnDescriptor)) {
                    throw new IllegalArgumentException(obj + " is not a ColumnDescriptor");
                }
                super.addElement(obj);
            }

        };
        list = new ReorderableJList();
        list.setModel(listModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ColumnDescriptor f = (ColumnDescriptor) value;
                String label = f.getFieldsPaths();
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        this.add(new JScrollPane(list), c);
        // Listeners
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    addTreeSelectionToList();
                }
            }

        });
        buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addTreeSelectionToList();

            }
        });
        buttonRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedInList();

            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedInList();
                }
                e.consume();
            }
        });

    }

    public void setMainTable(String table) {
        this.listDescriptor.setMainTable(table);
        treeModel.fillFromTable(table);
        listModel.removeAllElements();
        for (ColumnDescriptor d : this.listDescriptor.getColumns()) {
            this.listModel.addElement(d);
        }
    }

    private void deleteSelectedInList() {
        Object[] vals = list.getSelectedValues();
        if (vals == null)
            return;
        for (int i = 0; i < vals.length; i++) {
            Object object = vals[i];
            this.listModel.removeElement(object);
        }
        list.clearSelection();
    }

    private void addTreeSelectionToList() {
        final TreePath[] paths = tree.getSelectionPaths();
        if (paths == null)
            return;
        for (int i = 0; i < paths.length; i++) {
            TreePath treePath = paths[i];
            Object[] obj = treePath.getPath();
            FieldDescriptor d = null;
            FieldDescriptor root = null;
            for (int j = 0; j < obj.length; j++) {
                final Object object = obj[j];
                if (object != null) {
                    final FieldDescriptor f = (FieldDescriptor) (((DefaultMutableTreeNode) object).getUserObject());
                    if (f != null) {
                        final FieldDescriptor fieldDescriptor = new FieldDescriptor(f);
                        if (root == null) {
                            root = fieldDescriptor;

                        }
                        if (d != null) {
                            d.setLink(fieldDescriptor);
                        }
                        d = fieldDescriptor;
                    }
                }
            }

            // On ne met pas les foreigns keys
            if (d != null && root != null) {
                boolean add = true;
                final String extendedLabel = root.getPath();
                final int size = listModel.getSize();
                // Check if already in the list
                for (int j = 0; j < size; j++) {
                    if (((ColumnDescriptor) listModel.getElementAt(j)).getFieldsPaths().contains(extendedLabel)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    final ColumnDescriptor colDesc = new ColumnDescriptor(root.getPath());
                    colDesc.setFieldsPaths(root.getPath());
                    listModel.addElement(colDesc);
                    listDescriptor.add(colDesc);
                }
            }
        }
        tree.clearSelection();
    }

}
