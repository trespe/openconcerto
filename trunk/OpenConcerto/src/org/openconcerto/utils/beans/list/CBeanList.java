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
 
 package org.openconcerto.utils.beans.list;

import org.openconcerto.utils.beans.Bean;
import org.openconcerto.utils.model.IMutableListModel;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

public class CBeanList extends JPanel implements ListSelectionListener {

    // items to display
    private CBeanTableModel model;
    private JTable table;
    // will be forwarded selections
    private final ListSelectionListener parent;

    private boolean canDelete;

    public CBeanList(Class abeanClass, ListModel listModel) {
        this(null, listModel, abeanClass);
    }

    public CBeanList(ListSelectionListener parent, ListModel listModel, Class abeanClass) {
        this(parent, listModel, abeanClass, null, null);
    }

    public CBeanList(ListSelectionListener parent, ListModel listModel, Class abeanClass, PropertyDescriptor[] props, Map renderers) {
        this.parent = parent;
        this.canDelete = false;

        this.setUp(abeanClass, listModel, props, renderers);
    }

    public void setDeleteEnabled(boolean b) {
        if (!this.isMutable())
            throw new IllegalStateException(this + " not mutable.");
        this.canDelete = b;
    }

    public boolean isDeleteEnabled() {
        return this.canDelete;
    }

    private void setUp(Class clazz, ListModel listModel, PropertyDescriptor[] props, Map renderers) {
        if (!Bean.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("class is not a CBean");

        this.model = new CBeanTableModel(clazz, listModel, props);
        this.table = new JTable(this.model);
        if (renderers != null) {
            final Iterator iter = renderers.keySet().iterator();
            while (iter.hasNext()) {
                final Class columnClass = (Class) iter.next();
                this.table.setDefaultRenderer(columnClass, (TableCellRenderer) renderers.get(columnClass));
            }
        }
        this.table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                // VK_* must be listened on keyPress
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedRows();
                }
            }
        });
        this.table.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    try {
                        Robot robot = new java.awt.Robot();
                        if (table.getSelectedRowCount() != 0) {
                            robot.keyPress(KeyEvent.VK_SHIFT);
                            robot.mousePress(InputEvent.BUTTON1_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_MASK);
                            robot.keyRelease(KeyEvent.VK_SHIFT);
                        } else {
                            robot.mousePress(InputEvent.BUTTON1_MASK);
                            robot.mouseRelease(InputEvent.BUTTON1_MASK);
                        }
                    } catch (AWTException ae) {
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e.getComponent(), e.getX(), e.getY());
                }

            }

        });

        if (this.parent != null)
            this.table.getSelectionModel().addListSelectionListener(this);

        this.setLayout(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(this.table);

        Dimension d = scrollPane.getPreferredSize();
        scrollPane.setMinimumSize(new Dimension(d.width, 100));
        this.add(scrollPane, new GridBagConstraints(0, 0, 1, 4, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.table.getParent().setBackground(Color.WHITE);
    }

    // La sélection a changé.
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            this.parent.valueChanged(e);
        }
    }

    public void setSelection(Bean bean) {
        int index = this.model.getBeans().indexOf(bean);
        // sélectionne le nouveau
        this.table.getSelectionModel().setSelectionInterval(index, index);
    }

    public List getSelectedBeans() {
        return this.model.getBeans(this.table.getSelectedRows());
    }

    public Bean getSelectedBean() {
        int row = this.table.getSelectedRow();
        return (Bean) (row == -1 ? null : this.model.getBeans().get(row));
    }

    public void setTransferHandler(TransferHandler th) {
        this.table.setDragEnabled(th != null);
        this.table.setTransferHandler(th);
    }

    public void deselect() {
        this.table.removeRowSelectionInterval(0, this.table.getRowCount() - 1);
    }

    /**
     * Delete selected rows of the table. Has no effect if delete is not enabled.
     */
    public void deleteSelectedRows() {
        if (this.isDeleteEnabled()) {
            // on efface pas getSelectedRows[i] car
            // la liste est modifie a chaque effacement,
            // TODO: faire que le listModel ai un removeElements(indexes[]);
            int[] sel = this.table.getSelectedRows();
            for (int i = 0; i < sel.length; i++) {
                int index = this.table.getSelectedRows()[0];
                this.getMutableListModel().removeElementAt(index);
            }
        }
    }

    private void showPopup(Component comp, int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        popup.setFont(new Font("Tahoma", Font.PLAIN, 11));
        JMenuItem item = new JMenuItem("Enlever de la liste");
        popup.add(item);
        popup.show(comp, x, y);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSelectedRows();
            };
        });
        item.setEnabled(this.canDelete);
    }

    /**
     * Returns the list model
     * 
     * @return the list model.
     */
    public ListModel getListModel() {
        return this.model.getList();
    }

    /**
     * Returns the list model
     * 
     * @return the list model.
     * @throws ClassCastException if the ListModel is not mutable.
     */
    public IMutableListModel getMutableListModel() {
        return (IMutableListModel) this.getListModel();
    }

    /**
     * Can we enable delete.
     * 
     * @return <code>true</code> if the list model is mutable.
     */
    public boolean isMutable() {
        return this.getListModel() instanceof IMutableListModel;
    }
}
