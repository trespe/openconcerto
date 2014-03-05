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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

/***************************************************************************************************
 * Arbre contenant l'ensemble des rubriques de paye
 **************************************************************************************************/
public class RubriquePayeTree extends JTree {
    private final static SQLTable tableBrut = Configuration.getInstance().getBase().getTable("RUBRIQUE_BRUT");
    private final static SQLTable tableCotis = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");
    private final static SQLTable tableCaisseCotis = Configuration.getInstance().getBase().getTable("CAISSE_COTISATION");
    private final static SQLTable tableNet = Configuration.getInstance().getBase().getTable("RUBRIQUE_NET");
    private final static SQLTable tableComm = Configuration.getInstance().getBase().getTable("RUBRIQUE_COMM");
    private static DefaultMutableTreeNode nodeBrut;
    private static DefaultMutableTreeNode nodeCotisation;
    private static DefaultMutableTreeNode nodeNet;
    private static DefaultMutableTreeNode nodeComm;

    private static final DefaultMutableTreeNode rootVar = new DefaultMutableTreeNode("Rubriques");
    private static final DefaultTreeModel model = new DefaultTreeModel(rootVar, false);

    private static final Map mapNodeCotisation = new HashMap();

    public RubriquePayeTree() {
        super();
        this.setModel(model);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        this.setCellRenderer(renderer);
        DefaultMutableTreeNode currentNode = ((DefaultMutableTreeNode) this.getModel().getRoot()).getNextNode();
        do {
            if (currentNode.getLevel() == 1)
                this.expandPath(new TreePath(currentNode.getPath()));
            currentNode = currentNode.getNextNode();
        } while (currentNode != null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 400);
    }

    private static void loadAllRubrique() {

        // Rubrique Brut
        loadNodeBrut();
        rootVar.add(nodeBrut);

        // Rubrique Cotisation
        loadNodeCot();
        rootVar.add(nodeCotisation);

        // Rubrique Net
        loadNodeNet();
        rootVar.add(nodeNet);

        // Rubrique Comm
        loadNodeComm();
        rootVar.add(nodeComm);
    }

    private static void loadNodeComm() {
        // Rubrique Comm
        nodeComm = new DefaultMutableTreeNode("Commentaires");

        // on liste l'ensemble des rubriques de comm
        SQLSelect selAllCodeCommName = new SQLSelect(Configuration.getInstance().getBase());
        selAllCodeCommName.addSelect(tableComm.getField("ID"));
        selAllCodeCommName.addRawOrder("\"RUBRIQUE_COMM\".\"CODE\"");
        String reqAllCodeCommName = selAllCodeCommName.asString();
        System.err.println("REquest load node brut :: " + reqAllCodeCommName);
        Object[] objCodeCommID = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllCodeCommName, new ArrayListHandler())).toArray();

        for (int i = 0; i < objCodeCommID.length; i++) {

            SQLRow rowTmp = tableComm.getRow(Integer.parseInt(((Object[]) objCodeCommID[i])[0].toString()));
            VariableRowTreeNode nodeVar = new VariableRowTreeNode(rowTmp);
            addNode(nodeVar, nodeComm);
        }
    }

    private static void loadNodeNet() {

        nodeNet = new DefaultMutableTreeNode("Net");

        // on liste l'ensemble des rubriques de net
        SQLSelect selAllCodeNetName = new SQLSelect(Configuration.getInstance().getBase());
        selAllCodeNetName.addSelect(tableNet.getField("ID"));
        selAllCodeNetName.addRawOrder("\"RUBRIQUE_NET\".\"CODE\"");
        String reqAllCodeNetName = selAllCodeNetName.asString();
        System.err.println("REquest load node brut :: " + reqAllCodeNetName);
        Object[] objCodeNetID = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllCodeNetName, new ArrayListHandler())).toArray();

        for (int i = 0; i < objCodeNetID.length; i++) {
            SQLRow rowTmp = tableNet.getRow(Integer.parseInt(((Object[]) objCodeNetID[i])[0].toString()));
            VariableRowTreeNode nodeVar = new VariableRowTreeNode(rowTmp);
            addNode(nodeVar, nodeNet);
        }
    }

    private static void loadNodeCot() {
        nodeCotisation = new DefaultMutableTreeNode("Cotisation");

        // on liste l'ensemble des rubriques de cotisation
        SQLSelect selAllCodeCotisName = new SQLSelect(Configuration.getInstance().getBase());
        selAllCodeCotisName.addSelect(tableCotis.getField("ID"));
        selAllCodeCotisName.addRawOrder("\"RUBRIQUE_COTISATION\".\"CODE\"");
        String reqAllCodeCotisName = selAllCodeCotisName.asString();
        System.err.println("REquest load node brut :: " + reqAllCodeCotisName);
        Object[] objCodeCotisID = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllCodeCotisName, new ArrayListHandler())).toArray();

        for (int i = 0; i < objCodeCotisID.length; i++) {
            SQLRow rowTmp = tableCotis.getRow(Integer.parseInt(((Object[]) objCodeCotisID[i])[0].toString()));

            if (mapNodeCotisation.get(rowTmp.getObject("ID_CAISSE_COTISATION")) == null) {
                SQLRow rowCaisseCotis = tableCaisseCotis.getRow(rowTmp.getInt("ID_CAISSE_COTISATION"));
                DefaultMutableTreeNode nodeCot = new DefaultMutableTreeNode(rowCaisseCotis.getString("NOM"));
                mapNodeCotisation.put(rowTmp.getObject("ID_CAISSE_COTISATION"), nodeCot);
                nodeCotisation.add(nodeCot);
            }

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) mapNodeCotisation.get(rowTmp.getObject("ID_CAISSE_COTISATION"));
            VariableRowTreeNode nodeVar = new VariableRowTreeNode(rowTmp);
            addNode(nodeVar, node);
        }

    }

    private static void loadNodeBrut() {

        nodeBrut = new DefaultMutableTreeNode("Brut");

        // on liste l'ensemble des rubriques de brut
        SQLSelect selAllCodeBrutName = new SQLSelect(Configuration.getInstance().getBase());
        selAllCodeBrutName.addSelect(tableBrut.getField("ID"));
        selAllCodeBrutName.addRawOrder("\"RUBRIQUE_BRUT\".\"CODE\"");
        String reqAllCodeBrutName = selAllCodeBrutName.asString();
        System.err.println("REquest load node brut :: " + reqAllCodeBrutName);
        Object[] objCodeBrutID = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllCodeBrutName, new ArrayListHandler())).toArray();

        for (int i = 0; i < objCodeBrutID.length; i++) {
            SQLRow rowTmp = tableBrut.getRow(Integer.parseInt(((Object[]) objCodeBrutID[i])[0].toString()));
            VariableRowTreeNode nodeVar = new VariableRowTreeNode(rowTmp);
            addNode(nodeVar, nodeBrut);
        }

    }

    private static void addNode(VariableRowTreeNode nodeToAdd, MutableTreeNode nodeParent) {
        int n = 0;
        for (; n < nodeParent.getChildCount(); n++) {
            if (nodeToAdd.toString().compareToIgnoreCase(nodeParent.getChildAt(n).toString()) < 0) {
                break;
            }
        }
        model.insertNodeInto(nodeToAdd, nodeParent, n);
    }

    private static void modifyNode(SQLRow row, MutableTreeNode nodeParent) {
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

    private static void removeNode(SQLRow row, MutableTreeNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {
            Object o = nodeParent.getChildAt(i);
            if (o instanceof VariableRowTreeNode) {
                VariableRowTreeNode v = (VariableRowTreeNode) o;
                if (v.getID() == row.getID()) {
                    model.removeNodeFromParent(v);
                }
            }
        }
    }

    private static int test = 0;

    private static void setSQLTableListener() {

        System.err.println("Ajout listener " + test++);
        SQLTableListener listener = new SQLTableListener() {
            public void rowModified(SQLTable table, int id) {

                System.err.println("row modified --> " + table.getName() + ", " + id);

                if (table.getName().equalsIgnoreCase(tableBrut.getName())) {
                    modifyNode(table.getRow(id), nodeBrut);
                } else {
                    if (table.getName().equalsIgnoreCase(tableCotis.getName())) {
                        // TODO fonction specifique au cotisation structure differente
                        modifyNode(table.getRow(id), nodeCotisation);
                    } else {
                        if (table.getName().equalsIgnoreCase(tableNet.getName())) {
                            modifyNode(table.getRow(id), nodeNet);
                        } else {
                            if (table.getName().equalsIgnoreCase(tableComm.getName())) {
                                modifyNode(table.getRow(id), nodeComm);
                            }
                        }
                    }
                }
            }

            public void rowAdded(SQLTable table, int id) {

                System.err.println("row added --> " + table.getName() + ", " + id);
                // Thread.dumpStack();

                VariableRowTreeNode nodeVar = new VariableRowTreeNode(table.getRow(id));
                if (table.getName().equalsIgnoreCase(tableBrut.getName())) {

                    addNode(nodeVar, nodeBrut);
                } else {
                    if (table.getName().equalsIgnoreCase(tableCotis.getName())) {

                        SQLRow rowTmp = table.getRow(id);
                        if (mapNodeCotisation.get(rowTmp.getObject("ID_CAISSE_COTISATION")) == null) {
                            SQLRow rowCaisseCotis = tableCaisseCotis.getRow(rowTmp.getInt("ID_CAISSE_COTISATION"));
                            DefaultMutableTreeNode nodeCot = new DefaultMutableTreeNode(rowCaisseCotis.getString("NOM"));
                            mapNodeCotisation.put(rowTmp.getObject("ID_CAISSE_COTISATION"), nodeCot);
                            nodeCotisation.add(nodeCot);
                        }

                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) mapNodeCotisation.get(rowTmp.getObject("ID_CAISSE_COTISATION"));

                        addNode(nodeVar, node);
                    } else {
                        if (table.getName().equalsIgnoreCase(tableNet.getName())) {

                            addNode(nodeVar, nodeNet);
                        } else {
                            if (table.getName().equalsIgnoreCase(tableComm.getName())) {

                                addNode(nodeVar, nodeComm);
                            }
                        }
                    }
                }
            }

            public void rowDeleted(SQLTable table, int id) {

                System.err.println("row deleted --> " + table.getName() + ", " + id);

                if (table.getName().equalsIgnoreCase(tableBrut.getName())) {
                    for (int i = 0; i < nodeBrut.getChildCount(); i++) {
                        removeNode(table.getRow(id), nodeBrut);
                    }
                } else {
                    if (table.getName().equalsIgnoreCase(tableCotis.getName())) {
                        removeNode(table.getRow(id), nodeCotisation);
                    } else {
                        if (table.getName().equalsIgnoreCase(tableNet.getName())) {
                            removeNode(table.getRow(id), nodeNet);
                        } else {
                            if (table.getName().equalsIgnoreCase(tableComm.getName())) {
                                removeNode(table.getRow(id), nodeComm);
                            }
                        }
                    }
                }
            }
        };

        tableBrut.addTableListener(listener);
        tableComm.addTableListener(listener);
        tableCotis.addTableListener(listener);
        tableNet.addTableListener(listener);
    }

    static {
        loadAllRubrique();
        setSQLTableListener();
    }
}
