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
 
 package org.openconcerto.sql.view;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.navigator.SQLBrowser;
import org.openconcerto.sql.view.list.IListe;

import java.awt.GridLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TabbedListeModifyPanel extends JPanel {
    private final SQLElement element;
    private final IListe liste;
    private IListPanel listPanel;
    protected JTabbedPane tabbedPane = new JTabbedPane();
    protected EditPanel editorModifyComp, editorAddComp;
    private int selectedId = -1;
    private JPanel emptyPanelModify = new JPanel();
    private JPanel emptyPanelAdd = new JPanel();
    private SQLBrowser browser;

    public TabbedListeModifyPanel(SQLElement elem, SQLBrowser browser) {
        this(elem, null, browser);
    }

    public TabbedListeModifyPanel(SQLElement elem, IListe list, SQLBrowser browser) {
        this.element = elem;
        this.liste = list;
        this.browser = browser;
        tabbedPane.setTransferHandler(new TransferHandler() {

            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                System.out.println(".canImport()");
                return true;
            }

            protected Transferable createTransferable(JComponent c) {
                System.out.println(".createTransferable()");
                Transferable tr = new Transferable() {

                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                        System.out.println(".getTransferData()");
                        return new JLabel("salut");
                    }

                    public DataFlavor[] getTransferDataFlavors() {
                        System.out.println(".getTransferDataFlavors()");
                        DataFlavor[] array = new DataFlavor[0];
                        return array;
                    }

                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        System.out.println(".isDataFlavorSupported()");
                        return true;
                    }

                };

                return tr;

            }

            public void exportAsDrag(JComponent comp, InputEvent e, int action) {
                System.out.println(".exportAsDrag()");
                super.exportAsDrag(comp, e, action);
            }

            protected void exportDone(JComponent source, Transferable data, int action) {
                System.out.println(".exportDone()");
                super.exportDone(source, data, action);
            }

            public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
                System.out.println(".exportToClipboard()");
                super.exportToClipboard(comp, clip, action);
            }

            public int getSourceActions(JComponent c) {
                System.out.println(".getSourceActions()");
                return COPY;
            }

            public Icon getVisualRepresentation(Transferable t) {
                System.out.println(".getVisualRepresentation()");
                return super.getVisualRepresentation(t);
            }

            public boolean importData(JComponent comp, Transferable t) {
                System.out.println(".importData()");
                return super.importData(comp, t);
            }
        });

        MouseListener listener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                System.out.println(".mousePressed()");
                JComponent c = (JComponent) e.getSource();
                TransferHandler handler = c.getTransferHandler();
                handler.exportAsDrag(c, e, TransferHandler.COPY);
            }
        };
        tabbedPane.addMouseListener(listener);
    }

    public final IListe getListe() {
        return this.liste;
    }

    public SQLComponent getModifComp() {
        return this.editorModifyComp.getSQLComponent();
    }

    /**
     * @param elem
     */
    private void initEditor() {
        if (editorModifyComp == null) {
            /*
             * editormodifComp = element.createComponent(); try { String parentField =
             * element.getParentForeignField(); if (parentField != null) { ((BaseSQLComponent)
             * editormodifComp).doNotShow(element.getTable().getField(parentField)); } } catch
             * (Exception e) {
             * 
             * e.printStackTrace(); } editormodifComp.uiInit();
             */
            List hiddenFields = getHiddenFields();

            editorModifyComp = new EditPanel(element, EditPanel.MODIFICATION, hiddenFields);

            editorModifyComp.setModifyLabel("Valider les modifications [F12]");
            editorModifyComp.disableCancel();
            editorModifyComp.disableDelete();

            editorModifyComp.addEditPanelListener(new EditPanelListener() {

                public void cancelled() {
                    // TODO Auto-generated method stub

                }

                public void deleted() {
                    // TODO Auto-generated method stub

                }

                public void inserted(int id) {
                    // TODO Auto-generated method stub

                }

                public void modified() {
                    hideEditTab();

                }

            });

        }
    }

    /**
     * @return
     */
    private List getHiddenFields() {
        final String parentField = element.getParentForeignField();

        List hiddenFields = new Vector();
        if (parentField != null)
            hiddenFields.add(element.getTable().getField(parentField));
        return hiddenFields;
    }

    /*
     * public SQLComponent getEditorComponent() {
     * 
     * return editormodifComp; }
     */

    public void initTab() {
        this.setOpaque(false);
        this.setLayout(new GridLayout(1, 1));
        System.err.println("this" + this);
        final TabbedListeModifyPanel me = this;
        listPanel = new TabbedIListPanel(element, liste, this);
        tabbedPane.addTab("Liste des " + element.getPluralName() + " [F8]", listPanel);

        // tabbedPane.addTab("Edition (F2)", editormodifComp);

        this.add(tabbedPane);

        this.tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                System.err.println("TabbedListeModify.stateChanged()");
                if (tabbedPane.getSelectedComponent() == emptyPanelModify) {
                    final int indexOfComponent = tabbedPane.indexOfComponent(emptyPanelModify);
                    tabbedPane.removeTabAt(indexOfComponent);
                    initEditor();
                    String desc = getEditorTitle();
                    tabbedPane.insertTab(desc, null, editorModifyComp, null, indexOfComponent);

                    tabbedPane.setSelectedComponent(editorModifyComp);
                    editorModifyComp.selectionId(selectedId, -1);// select(selectedId);

                }
                if (tabbedPane.getSelectedComponent() == editorModifyComp) {
                    editorModifyComp.selectionId(selectedId, -1);
                }
            }
        });
    }

    /*
     * public void updateEditor(boolean visible, String description) { final int indexOfComponent =
     * tabbedPane.indexOfComponent(editormodifComp); if (!visible) { if (indexOfComponent != -1) {
     * tabbedPane.remove(editormodifComp); } } else { final String desc = "Edition:" + description + "
     * [F9]"; if (indexOfComponent == -1) { tabbedPane.insertTab(desc, null, editormodifComp, null,
     * 1); } else { tabbedPane.setTitleAt(indexOfComponent, desc); } } }
     */

    public void activateList() {
        tabbedPane.setSelectedComponent(listPanel);
        SwingUtilities.invokeLater(new Runnable() {
            // Sinon n'active pas
            public void run() {
                listPanel.grabFocus();
            }
        });
    }

    public void showEditTab() {
        int index = tabbedPane.indexOfComponent(editorModifyComp);
        if (index != -1) {

            tabbedPane.setSelectedComponent(editorModifyComp);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Sinon n'active pas
                    editorModifyComp.grabFocus();
                    // tabbedPane.getSelectedComponent()==.transferFocus();
                }
            });
        } else {
            index = tabbedPane.indexOfComponent(emptyPanelModify);
            if (index != -1) {
                tabbedPane.setSelectedComponent(emptyPanelModify);
            }
        }
    }

    public void showAddTab(List sqlRows) {
        int index = tabbedPane.indexOfComponent(editorAddComp);
        if (index != -1) {
            // existe
            tabbedPane.setSelectedComponent(editorAddComp);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Sinon n'active pas
                    editorAddComp.grabFocus();
                    // tabbedPane.getSelectedComponent()==.transferFocus();
                }
            });
        } else {
            List hiddenFields = getHiddenFields();
            editorAddComp = new EditPanel(element, EditPanel.CREATION, hiddenFields);
            editorAddComp.setValues(sqlRows);

            int insertIndex = tabbedPane.indexOfComponent(editorModifyComp);

            if (insertIndex < 0)
                insertIndex = 1;
            else
                insertIndex++;
            tabbedPane.insertTab("Ajouter " + element.getSingularName() + " [F10]", null, editorAddComp, "", insertIndex);
            tabbedPane.setSelectedComponent(editorAddComp);
            editorAddComp.addEditPanelListener(new EditPanelListener() {

                public void cancelled() {
                    hideAddTab();

                }

                public void deleted() {
                    // TODO Auto-generated method stub

                }

                public void inserted(int id) {
                    hideAddTab();

                }

                public void modified() {

                }

            });
        }
    }

    /**
     * Valide les modifications apportées dans le panneau d'edition
     */
    public void validateEdition() {
        editorModifyComp.modifier();
    }

    public void setSelectedId(int id) {
        this.selectedId = id;
        if (selectedId > 0) {
            createEditTab();

        } else {
            hideEditTab();
        }
    }

    private void hideEditTab() {
        if (editorModifyComp == null)
            tabbedPane.remove(emptyPanelModify);
        else
            tabbedPane.remove(editorModifyComp);
    }

    private void hideAddTab() {
        if (editorAddComp != null) {
            tabbedPane.remove(editorAddComp);
        }
    }

    private void createEditTab() {
        String desc = getEditorTitle();
        if (editorModifyComp == null) {
            tabbedPane.insertTab(desc, null, emptyPanelModify, null, 1);
        } else {
            tabbedPane.insertTab(desc, null, editorModifyComp, null, 1);
        }
    }

    /**
     * @param row
     * @return
     */
    private String getEditorTitle() {
        final SQLRow row = element.getTable().getRow(selectedId);
        return "Edition:" + element.getDescription(row) + " [F9]";
    }

    public SQLBrowser getBrowser() {
        // TODO Auto-generated method stub
        return browser;
    }

}
