/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ui.preferences;

import org.openconcerto.ui.clipboard.ClipboardItems;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class PrefTree extends JPanel implements DocumentListener {
    private final JComboBox searchBox = new JComboBox();
    private final JTree tree;
    private final DefaultMutableTreeNode root;
    private String text = "";
    JButton button;

    public PrefTree(DefaultMutableTreeNode root) {
        super();
        this.root = root;
        this.setLayout(new GridBagLayout());
        this.setOpaque(true);
        this.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        final JTextComponent textComponent = (JTextComponent) this.searchBox.getEditor().getEditorComponent();
        textComponent.getDocument().addDocumentListener(this);
        ClipboardItems.addJPopupMenu(textComponent);
        this.searchBox.setOpaque(false);
        this.searchBox.setEditable(true);
        this.searchBox.setToolTipText("Recherche de mot clé dans les préférences");
        this.add(this.searchBox, c);

        c.gridx++;
        c.weightx = 0;
        this.button = new JButton(new ImageIcon(PrefTree.class.getResource("clear.png")));
        this.button.setDisabledIcon(new ImageIcon(PrefTree.class.getResource("blank.png")));
        this.button.setPreferredSize(new Dimension(20, 20));
        this.button.setMinimumSize(new Dimension(20, 20));
        this.button.setEnabled(false);
        this.button.setBorderPainted(false);
        this.button.setFocusPainted(false);
        this.button.setContentAreaFilled(false);
        this.button.setOpaque(false);
        this.button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JTextComponent) PrefTree.this.searchBox.getEditor().getEditorComponent()).setText("");
            }
        });
        this.add(this.button, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1;

        c.fill = GridBagConstraints.BOTH;

        this.tree = new JTree(this.root);

        // Set the renderer
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            Font font = null;
            Font boldFont = null;

            public Component getTreeCellRendererComponent(JTree aTree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasTheFocus) {
                Component co = super.getTreeCellRendererComponent(aTree, value, sel, expanded, leaf, row, hasTheFocus);
                if (this.font == null) {
                    this.font = co.getFont();
                    this.boldFont = this.font.deriveFont(Font.BOLD);
                }
                co.setFont(this.font);
                if (value instanceof PrefTreeNode) {
                    PrefTreeNode e = (PrefTreeNode) value;
                    if (!e.isMatching()) {
                        co.setForeground(Color.LIGHT_GRAY);
                    }
                    if (e.isBold()) {
                        co.setFont(this.boldFont);
                    }
                }
                return co;
            }
        };
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        this.tree.setCellRenderer(renderer);

        this.tree.expandRow(1);
        JScrollPane pane1 = new JScrollPane(this.tree);
        pane1.setBorder(null);
        add(pane1, c);

    } // ---- update of search text

    public void changedUpdate(DocumentEvent e) {
        updateTree();
    }

    public void insertUpdate(DocumentEvent e) {
        updateTree();
    }

    public void removeUpdate(DocumentEvent e) {
        updateTree();
    }

    // ----

    /**
     * Update the tree from the search text
     */
    private void updateTree() {
        this.text = ((JTextComponent) this.searchBox.getEditor().getEditorComponent()).getText().trim();

        if (this.text.length() > 0) {
            DefaultMutableTreeNode newroot = (DefaultMutableTreeNode) this.root.clone();
            filterChildren(this.root, newroot);

            this.tree.setModel(new DefaultTreeModel(newroot));
            for (int i = 0; i < this.tree.getRowCount(); i++) {
                this.tree.expandRow(i);
            }
            this.button.setEnabled(true);
        } else {
            if (this.tree.getModel().getRoot() != this.root)
                this.tree.setModel(new DefaultTreeModel(this.root));
            this.button.setEnabled(false);
        }
    }

    private boolean filterChildren(DefaultMutableTreeNode oldroot, DefaultMutableTreeNode newroot) {
        boolean matched = false;
        for (int i = 0; i < oldroot.getChildCount(); i++) {
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) oldroot.getChildAt(i);
            if (oldNode instanceof PrefTreeNode) {
                PrefTreeNode newNode = (PrefTreeNode) oldNode.clone();
                if (((PrefTreeNode) oldNode).match(new String[] { this.text })) {
                    matched = true;
                    newNode.setMatch(true);
                    newroot.add(newNode);
                } else {
                    newNode.setMatch(false);
                }

                boolean r = filterChildren(oldNode, newNode);
                if (r) {
                    matched = true;
                    newroot.add(newNode);
                }
            }

        }
        return matched;
    }

    /**
     * Manage listeners on node selection
     */
    public void addTreeSelectionListener(TreeSelectionListener l) {
        this.tree.addTreeSelectionListener(l);
    }

    public void removeTreeSelectionListener(TreeSelectionListener l) {
        this.tree.removeTreeSelectionListener(l);
    }

    /**
     * Select a specified Node
     * 
     * @return false is the node is not found or not visible
     */
    public boolean select(final TreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Trying to select a null node");
        }
        final TreePath p = new TreePath(((DefaultTreeModel) this.tree.getModel()).getPathToRoot(node));
        this.tree.expandPath(p);
        this.tree.scrollPathToVisible(p);

        int index = this.tree.getRowForPath(p);
        if (index >= 0) {
            this.tree.setSelectionRow(index);
            return true;
        }
        return false;
    }
}
