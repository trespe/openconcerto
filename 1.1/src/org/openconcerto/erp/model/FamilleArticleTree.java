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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.core.humanresources.payroll.component.VariableRowTreeNode;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.view.EditFrame;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class FamilleArticleTree extends JTree implements MouseListener {
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Toutes");
    private final DefaultTreeModel model = new DefaultTreeModel(rootNode, false);

    private final SQLElement familleElt = Configuration.getInstance().getDirectory().getElement("FAMILLE_ARTICLE");

    private Map<Integer, DefaultMutableTreeNode> mapNode = new HashMap<Integer, DefaultMutableTreeNode>();

    public FamilleArticleTree() {

        super();

        this.setModel(model);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        this.setCellRenderer(renderer);
        loadTree();
        this.addMouseListener(this);
        this.setTableListener();
        this.setDragEnabled(true);
    }

    /**
     * Chargement de toutes les familles
     */
    private void loadTree() {
        SQLSelect sel = new SQLSelect(familleElt.getTable().getBase());

        sel.addSelect(familleElt.getTable().getField("ID"));
        sel.addSelect(familleElt.getTable().getField("ID_FAMILLE_ARTICLE_PERE"));

        List l = (List) familleElt.getTable().getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                Object[] tmp = (Object[]) l.get(i);
                addFamille(((Number) tmp[0]).intValue(), ((Number) tmp[1]).intValue());
            }
        }
    }

    /**
     * Ajoute une famille dans l'arbre
     * 
     * @param id
     * @param idPere
     */
    private void addFamille(int id, int idPere) {
        DefaultMutableTreeNode nodePere = this.mapNode.get(Integer.valueOf(idPere));
        SQLRow row = familleElt.getTable().getRow(id);
        FamilleTreeNode newNode = new FamilleTreeNode(row);
        this.mapNode.put(Integer.valueOf(id), newNode);

        if (id > 1) {
            if (nodePere != null && idPere > 1) {
                addNode(newNode, nodePere);
            } else {
                addNode(newNode, rootNode);
            }
        }
    }

    /**
     * Ajoute un noeud dans l'arbre dans l'ordre alphabétique
     * 
     * @param nodeToAdd
     * @param nodeParent
     */
    private void addNode(FamilleTreeNode nodeToAdd, MutableTreeNode nodeParent) {
        int n = 0;
        for (; n < nodeParent.getChildCount(); n++) {
            if (nodeToAdd.toString().compareToIgnoreCase(nodeParent.getChildAt(n).toString()) < 0) {
                break;
            }
        }
        model.insertNodeInto(nodeToAdd, nodeParent, n);
    }

    /**
     * 
     * @param row
     * @param nodeParent
     */
    private void modifyNode(SQLRow row, MutableTreeNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {
            Object o = nodeParent.getChildAt(i);
            if (o instanceof VariableRowTreeNode) {
                VariableRowTreeNode v = (VariableRowTreeNode) o;
                if (v.getID() == row.getID()) {
                    v.setRow(row);
                    model.nodeChanged(v);
                }
            }
        }
    }

    private void setTableListener() {
        SQLTableListener listener = new SQLTableListener() {
            public void rowModified(SQLTable table, int id) {

                System.err.println("row modified --> " + table.getName() + ", " + id);
                MutableTreeNode node = mapNode.get(Integer.valueOf(id));
                if (node != null && node instanceof FamilleTreeNode) {
                    modifyNode(table.getRow(id), node);
                }
            }

            public void rowAdded(SQLTable table, int id) {

                System.err.println("row added --> " + table.getName() + ", " + id);
                // Thread.dumpStack();

                SQLRow row = table.getRow(id);
                int idPere = row.getInt("ID_FAMILLE_ARTICLE_PERE");

                addFamille(id, idPere);
            }

            public void rowDeleted(SQLTable table, int id) {

                System.err.println("row deleted --> " + table.getName() + ", " + id);
                MutableTreeNode node = mapNode.get(Integer.valueOf(id));
                for (int i = 0; i < node.getChildCount(); i++) {
                    removeNode(table.getRow(id), node);
                }
            }
        };

        familleElt.getTable().addTableListener(listener);
    }

    private void removeNode(SQLRow row, MutableTreeNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {
            Object o = nodeParent.getChildAt(i);
            if (o instanceof FamilleTreeNode) {
                FamilleTreeNode v = (FamilleTreeNode) o;
                if (v.getId() == row.getID()) {
                    model.removeNodeFromParent(v);
                }
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {

        Object o = this.getSelectionPath().getLastPathComponent();

        int id = 1;

        if (e.getButton() == MouseEvent.BUTTON3) {
            if (o instanceof FamilleTreeNode) {

                final FamilleTreeNode nodeSelect = (FamilleTreeNode) o;
                id = nodeSelect.getId();
            }

            final int idSelect = id;

            // Ajouter, supprimer, modifier une famille

            JPopupMenu menu = new JPopupMenu();
            menu.add(new AbstractAction("Ajouter une famille") {
                public void actionPerformed(ActionEvent e) {
                    EditFrame frameAddFamille = new EditFrame(familleElt, EditFrame.CREATION);
                    SQLRowValues rowVals = new SQLRowValues(familleElt.getTable());
                    rowVals.put("ID_FAMILLE_ARTICLE_PERE", idSelect);
                    frameAddFamille.getSQLComponent().select(rowVals);
                    frameAddFamille.setVisible(true);

                }
            });

            if (idSelect > 1) {
                menu.add(new AbstractAction("Modifier") {
                    public void actionPerformed(ActionEvent e) {
                        EditFrame frameModFamille = new EditFrame(familleElt, EditFrame.MODIFICATION);
                        frameModFamille.selectionId(idSelect, 1);
                        frameModFamille.setVisible(true);

                    }
                });

                menu.add(new AbstractAction("Supprimer") {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            familleElt.archive(idSelect);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
            menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
        }

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
