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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableListener;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObject;
import org.openconcerto.utils.checks.EmptyObjectHelper;
import org.openconcerto.utils.checks.ValidListener;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.collections.Predicate;
import org.apache.commons.dbutils.handlers.ArrayListHandler;

// TODO Drag'n drop des nodes
public class ITreeSelection extends JTree implements MouseListener, EmptyObject, ValueWrapper<Integer>, RowItemViewComponent {

    private SQLElement element;

    private ITreeSelectionNode rootNode;
    private DefaultTreeModel model;

    // Map <Id, Node>
    private Map<Integer, ITreeSelectionNode> mapNode = new HashMap<Integer, ITreeSelectionNode>();

    protected static final int EMPTY_ID = SQLRow.MIN_VALID_ID - 1;
    private EmptyObjectHelper helper;
    private final PropertyChangeSupport supp;

    public ITreeSelection() {

        this(null);
    }

    public ITreeSelection(SQLElement element) {
        super();
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        this.setCellRenderer(renderer);

        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.element = element;

        this.supp = new PropertyChangeSupport(this);

        // Value changed
        this.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                ITreeSelection.this.supp.firePropertyChange("value", null, getUncheckedValue());
            }
        });

    }

    private void initTree() {
        if (this.element == null) {
            this.rootNode = new ITreeSelectionNode(null);
        } else {
            SQLRow row = this.element.getTable().getRow(element.getTable().getUndefinedID());
            this.rootNode = new ITreeSelectionNode(row);
        }
        this.model = new DefaultTreeModel(this.rootNode);
        this.setModel(this.model);
        loadTree();
        this.expandRow(0);
        setTableListener();
        this.addMouseListener(this);
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

        TreePath path = this.getSelectionPath();
        if (path != null) {
            Object o = path.getLastPathComponent();

            int id = 1;

            if (e.getButton() == MouseEvent.BUTTON3) {

                final int idSelect = getSelectedID();

                // Ajouter, supprimer, modifier un élément dans l'arbre

                JPopupMenu menu = new JPopupMenu();
                menu.add(new AbstractAction("Ajouter un élément") {
                    public void actionPerformed(ActionEvent e) {
                        addElement(idSelect);
                    }
                });

                if (idSelect > 1) {
                    menu.add(new AbstractAction("Modifier") {
                        public void actionPerformed(ActionEvent e) {
                            modifyElement(idSelect);
                        }
                    });

                    menu.add(new AbstractAction("Supprimer") {
                        public void actionPerformed(ActionEvent e) {
                            removeElement(idSelect);
                        }
                    });
                }
                menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            }
        }
    }

    public int getSelectedID() {
        TreePath path = this.getSelectionPath();
        int id = 1;
        if (path != null) {
            Object o = path.getLastPathComponent();

            if (o instanceof ITreeSelectionNode) {

                final ITreeSelectionNode nodeSelect = (ITreeSelectionNode) o;
                id = nodeSelect.getId();
            }
        }
        return id;
    }

    /**
     * Ajouter une feuille
     */
    public void addElement(int idRoot) {
        EditFrame frameAdd = new EditFrame(element, EditFrame.CREATION);
        SQLRowValues rowVals = new SQLRowValues(element.getTable());
        if (idRoot > 1) {
            rowVals.put("ID_" + element.getTable().getName() + "_PERE", idRoot);
            frameAdd.getSQLComponent().select(rowVals);
        }
        FrameUtil.showPacked(frameAdd);

    }

    /**
     * Supprimer la feuille
     */
    public void removeElement(int id) {
        if (id > 1) {
            try {
                element.archive(id);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Modifier la feuille
     */
    public void modifyElement(int id) {
        if (id > 1) {
            EditFrame frameModFamille = new EditFrame(element, EditFrame.MODIFICATION);
            frameModFamille.selectionId(id, 1);
            FrameUtil.showPacked(frameModFamille);
        }
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public JComponent getComp() {
        return this;
    }

    public void resetValue() {
        this.setValue(1);
    }

    public void addEmptyListener(EmptyListener l) {
        this.helper.addListener(l);
    }

    public void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public Integer getUncheckedValue() {

        int id = 1;
        TreePath path = this.getSelectionPath();

        if (path != null) {
            Object o = path.getLastPathComponent();

            if (o instanceof ITreeSelectionNode) {
                final ITreeSelectionNode nodeSelect = (ITreeSelectionNode) o;
                id = nodeSelect.getId();
            }
        }
        return id;
    }

    public Integer getValue() throws IllegalStateException {
        return ((Number) this.helper.getValue()).intValue();
    }

    public boolean isEmpty() {
        return this.helper.isEmpty();
    }

    public void addValidListener(ValidListener l) {
        // TODO Auto-generated method stub
    }

    public boolean isValidated() {

        return true;
    }

    @Override
    public void init(SQLRowItemView v) {
        // final SQLElement element;
        if (this.element == null) {
            final SQLTable foreignTable = v.getField().getTable().getDBSystemRoot().getGraph().getForeignTable(v.getField());
            this.element = Configuration.getInstance().getDirectory().getElement(foreignTable);
        }
        // else
        // element = this.element;
        this.helper = new EmptyObjectHelper(this, new Predicate() {
            public boolean evaluate(Object object) {
                final Integer val = ((Number) object).intValue();
                return val.intValue() <= EMPTY_ID;
            }
        });
        initTree();
    }

    public void setValue(Integer val) {
        if (val == null)
            val = EMPTY_ID;
        // TODO Auto-generated method stub
        ITreeSelectionNode v = this.mapNode.get(val);
        if (v.getId() == val) {
            this.setExpandsSelectedPaths(true);
            this.setSelectionPath(new TreePath(v.getPath()));
        }

        // System.err.println("Set value Tree " + val);
        // if (this.rootNode != null) {
        // for (int i = 0; i < this.rootNode.getChildCount(); i++) {
        // Object o = this.rootNode.getChildAt(i);
        // if (o instanceof FamilleTreeNode) {
        // FamilleTreeNode v = (FamilleTreeNode) o;
        // if (v.getId() == val) {
        // System.err.println("Select TreeNode row Id " + val);
        // this.setExpandsSelectedPaths(true);
        // this.setSelectionPath(new TreePath(v.getPath()));
        // }
        // }
        // }
        // }
    }

    private void loadTree() {

        SQLTable table = this.element.getTable();
        SQLSelect sel = new SQLSelect(table.getBase());

        sel.addSelect(table.getKey());
        sel.addSelect(table.getField("ID_" + table.getName() + "_PERE"));

        List l = (List) table.getBase().getDataSource().execute(sel.asString(), new ArrayListHandler());

        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                Object[] tmp = (Object[]) l.get(i);
                addNewNode(((Number) tmp[0]).intValue(), ((Number) tmp[1]).intValue());
            }
        }
    }

    /**
     * Ajoute une famille dans l'arbre
     * 
     * @param id
     * @param idPere
     */
    private void addNewNode(int id, int idPere) {
        SQLTable table = this.element.getTable();
        ITreeSelectionNode nodePere = this.mapNode.get(Integer.valueOf(idPere));
        SQLRow row = table.getRow(id);
        ITreeSelectionNode newNode = new ITreeSelectionNode(row);
        this.mapNode.put(Integer.valueOf(id), newNode);

        if (id > 1) {
            if (nodePere != null && idPere > 1) {
                addNode(newNode, nodePere);
            } else {
                if (idPere == 1) {
                    addNode(newNode, rootNode);
                }
            }
        }
    }

    /**
     * Ajoute un noeud dans l'arbre dans l'ordre alphabétique
     * 
     * @param nodeToAdd
     * @param nodeParent
     */
    private void addNode(ITreeSelectionNode nodeToAdd, ITreeSelectionNode nodeParent) {
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
    private void modifyNode(SQLRow row, ITreeSelectionNode node) {
        // for (int i = 0; i < node.getChildCount(); i++) {
        //
        // ITreeSelectionNode v = (ITreeSelectionNode) node.getChildAt(i);
        // if (v.getId() == row.getID()) {
        // v.setRow(row);
        // model.nodeChanged(v);
        // }
        //
        // }
        if (row.isArchived()) {
            // node.removeFromParent();
            model.removeNodeFromParent(node);
        } else {
            node.setRow(row);
            model.nodeChanged(node);
        }
    }

    /**
     * Suppression d'un noeud
     * 
     * @param row
     * @param nodeParent
     */
    private void removeNode(SQLRow row, ITreeSelectionNode nodeParent) {
        for (int i = 0; i < nodeParent.getChildCount(); i++) {

            ITreeSelectionNode v = (ITreeSelectionNode) nodeParent.getChildAt(i);
            if (v.getId() == row.getID()) {
                model.removeNodeFromParent(v);
            }

        }
    }

    /**
     * Table listener permettant de rafraichir l'arbre
     * 
     */
    private void setTableListener() {
        SQLTableListener listener = new SQLTableListener() {
            public void rowModified(SQLTable table, int id) {
                ITreeSelectionNode node = mapNode.get(Integer.valueOf(id));
                if (node != null) {
                    modifyNode(table.getRow(id), node);
                }
            }

            public void rowAdded(SQLTable table, int id) {
                SQLRow row = table.getRow(id);
                int idPere = row.getInt("ID_" + element.getTable().getName() + "_PERE");

                addNewNode(id, idPere);
            }

            public void rowDeleted(SQLTable table, int id) {
                ITreeSelectionNode node = mapNode.get(Integer.valueOf(id));
                for (int i = 0; i < node.getChildCount(); i++) {
                    removeNode(table.getRow(id), node);
                }
            }
        };

        this.element.getTable().addTableListener(listener);
    }

    public String getValidationText() {
        return "Aucune valeur sélectionnée dans l'arbre";
    }
}
