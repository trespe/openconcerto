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
 
 package org.openconcerto.erp.core.humanresources.payroll.component;

import org.openconcerto.erp.core.humanresources.payroll.element.VariablePayeSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.ui.JMultiLineToolTip;

import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;


// FIXME SQLTableListener rowModified --> changement de categorie, ...
public class VariableTree extends JTree {

    private static DefaultMutableTreeNode rootVar = new DefaultMutableTreeNode("Variables");
    private static DefaultMutableTreeNode nodeVariablesPaye = null;

    // map des noeuds des categories des variables
    private static Map<String, DefaultMutableTreeNode> mapCategorie;

    private static final DefaultTreeModel model = new DefaultTreeModel(rootVar, false);

    public VariableTree() {

        super();

        this.setModel(model);

        // Arbre des variables
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (leaf) {
                    Object obj = value;
                    if (obj != null) {
                        if (obj instanceof FormuleTreeNode) {

                            final FormuleTreeNode n = (FormuleTreeNode) obj;
                            String strTmp = n.getTextInfosValue();
                            if (strTmp != null && strTmp.length() != 0) { //                                
                                setToolTipText(strTmp);
                            } else {
                                setToolTipText(null);
                            }
                        }
                    }
                } else {
                    setToolTipText(null);
                }

                return this;
            }
        };
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        this.setCellRenderer(renderer);

        this.expandRow(0);

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    private static void loadNode() {
        Map m = VariablePayeSQLElement.getMapTree();

        rootVar.removeAllChildren();
        mapCategorie = new HashMap<String, DefaultMutableTreeNode>();

        Object[] key = m.keySet().toArray();

        for (int i = 0; i < key.length; i++) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(key[i]);

            int place = 0;
            for (; place < rootVar.getChildCount(); place++) {

                if (node.toString().compareToIgnoreCase(rootVar.getChildAt(place).toString()) < 0) {
                    break;
                }
            }

            rootVar.insert(node, place);

            List l = (List) m.get(key[i]);
            for (int j = 0; j < l.size(); j++) {

                // Variables propres aux salarié
                if (l.get(j) instanceof SQLField) {

                    VariableTreeNode nodeVar = new VariableTreeNode((SQLField) l.get(j));
                    int n = 0;
                    for (; n < node.getChildCount(); n++) {

                        if (nodeVar.toString().compareToIgnoreCase(node.getChildAt(n).toString()) < 0) {
                            break;
                        }
                    }

                    node.insert(nodeVar, n);
                } else {

                    // Variable Paye
                    if (l.get(j) instanceof SQLRow) {
                        if (nodeVariablesPaye == null) {
                            nodeVariablesPaye = node;
                        }

                        String cat = ((SQLRow) l.get(j)).getString("CATEGORIE").trim();

                        VariableRowTreeNode nodeVar = new VariableRowTreeNode((SQLRow) l.get(j));

                        // Variables sans catégorie
                        if (cat.length() == 0) {

                            int n = 0;
                            for (; n < node.getChildCount(); n++) {

                                if (nodeVar.toString().compareToIgnoreCase(node.getChildAt(n).toString()) < 0) {
                                    break;
                                }
                            }
                            node.insert(nodeVar, n);

                        } else {

                            // Variable avec categorie deja existante
                            if (mapCategorie.get(cat.toLowerCase()) != null) {

                                DefaultMutableTreeNode nodeCat = mapCategorie.get(cat.toLowerCase());
                                int n = 0;
                                for (; n < nodeCat.getChildCount(); n++) {
                                    if (nodeVar.toString().compareToIgnoreCase(nodeCat.getChildAt(n).toString()) <= 0) {
                                        break;
                                    }
                                }

                                nodeCat.insert(nodeVar, n);
                            } else {
                                DefaultMutableTreeNode nodeCat = new DefaultMutableTreeNode(cat.toLowerCase());

                                int n = 0;
                                for (; n < node.getChildCount(); n++) {

                                    if (nodeCat.toString().compareToIgnoreCase(node.getChildAt(n).toString()) <= 0) {
                                        break;
                                    }
                                }
                                node.insert(nodeCat, n);
                                mapCategorie.put(cat.toLowerCase(), nodeCat);
                                nodeCat.add(nodeVar);
                            }
                        }
                    } else {
                        // Liste des fonctions mathématique
                        if (l.get(j) instanceof HashMap) {

                            Map mapFonction = (HashMap) l.get(j);
                            Object[] keys = mapFonction.keySet().toArray();
                            for (int k = 0; k < keys.length; k++) {
                                FonctionTreeNode nodeFunction = new FonctionTreeNode(keys[k].toString(), mapFonction.get(keys[k]).toString());
                                int n = 0;
                                for (; n < node.getChildCount(); n++) {

                                    if (nodeFunction.toString().compareToIgnoreCase(node.getChildAt(n).toString()) <= 0) {
                                        break;
                                    }
                                }
                                node.insert(nodeFunction, n);
                            }
                        }
                    }
                }
            }
        }

        // FIXME mis à jour de l'arbre
        model.reload();

        System.err.println("TREE CHANGED");
    }

    public JToolTip createToolTip() {

        JMultiLineToolTip t = new JMultiLineToolTip();
        t.setFixedWidth(300);
        return t;
    }

    private static void modifyNode(SQLRow row, MutableTreeNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {
            Object o = nodeParent.getChildAt(i);
            if (o instanceof VariableRowTreeNode) {
                VariableRowTreeNode nodeTmp = (VariableRowTreeNode) o;
                if (nodeTmp.getID() == row.getID()) {
                    nodeTmp.setRow(row);
                    model.nodeChanged(nodeTmp);
                    System.err.println("Find & refresh");
                    return;
                }
            }
        }
    }

    private static void addNode(SQLRow row, MutableTreeNode nodeParent) {
        VariableRowTreeNode nodeVar = new VariableRowTreeNode(row);
        int n = 0;
        for (; n < nodeParent.getChildCount(); n++) {
            if (nodeVar.toString().compareToIgnoreCase(nodeParent.getChildAt(n).toString()) <= 0) {
                break;
            }
        }

        model.insertNodeInto(nodeVar, nodeParent, n);
    }

    private static void removeNode(SQLRow row, MutableTreeNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {
            Object o = nodeParent.getChildAt(i);
            if (o instanceof VariableRowTreeNode) {
                VariableRowTreeNode nodeTmp = (VariableRowTreeNode) o;
                if (nodeTmp.getID() == row.getID()) {
                    model.removeNodeFromParent(nodeTmp);
                    System.err.println("Find & removed");
                    return;
                }
            }
        }

    }

    private static void setSQLTableListener() {
        Configuration.getInstance().getBase().getTable("VARIABLE_PAYE").addTableListener(new SQLTableListener() {

            public void rowModified(SQLTable table, int id) {
                System.err.println("refresh line");

                modifyNode(table.getRow(id), nodeVariablesPaye);

                for (Iterator i = mapCategorie.keySet().iterator(); i.hasNext();) {
                    Object o = mapCategorie.get(i.next());

                    if (o instanceof DefaultMutableTreeNode) {
                        modifyNode(table.getRow(id), (DefaultMutableTreeNode) o);
                    }
                }

            }

            public void rowAdded(SQLTable table, int id) {
                System.err.println("Variable tree node row added ");

                // Thread.dumpStack();
                if (nodeVariablesPaye != null) {
                    // Variable Paye

                    SQLRow rowTmp = table.getRow(id);
                    String cat = rowTmp.getString("CATEGORIE").trim();

                    // Variables sans catégorie
                    if (cat.length() == 0) {
                        addNode(table.getRow(id), nodeVariablesPaye);
                    } else {

                        // Variable avec categorie deja existante
                        if (mapCategorie.get(cat.toLowerCase()) != null) {

                            DefaultMutableTreeNode nodeCat = mapCategorie.get(cat.toLowerCase());
                            addNode(table.getRow(id), nodeCat);
                        } else {
                            DefaultMutableTreeNode nodeCat = new DefaultMutableTreeNode(cat.toLowerCase());
                            addNode(table.getRow(id), nodeCat);
                            mapCategorie.put(cat.toLowerCase(), nodeCat);

                        }
                    }
                }
            }

            public void rowDeleted(SQLTable table, int id) {

                System.err.println("remove line");
                removeNode(table.getRow(id), nodeVariablesPaye);

                for (Iterator i = mapCategorie.keySet().iterator(); i.hasNext();) {
                    Object o = mapCategorie.get(i.next());

                    if (o instanceof DefaultMutableTreeNode) {
                        removeNode(table.getRow(id), (DefaultMutableTreeNode) o);
                    }
                }
            }
        });
    }

    static {
        loadNode();
        setSQLTableListener();
    }
}
