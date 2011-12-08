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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.ui.EnhancedTable;
import org.openconcerto.ui.state.JTableStateManager;
import org.openconcerto.ui.table.JCheckBoxTableCellRender;
import org.openconcerto.utils.TableSorter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class RowValuesSelector extends EnhancedTable {

    private final JTableStateManager stateManager;
    private final RowValuesTableModel model;
    private boolean editorAndRendererDone;
    private final SQLTableElement tableElement;
    private final List<ActionListener> listeners = new Vector<ActionListener>();
    private final HashMap<SQLRowValues, Boolean> map = new HashMap<SQLRowValues, Boolean>();

    public RowValuesSelector(RowValuesTableModel model, File f) {
        this.stateManager = new JTableStateManager(this, f, true);
        this.model = model;
        for (int i = 0; i < model.getColumnCount(); i++) {
            model.setEditable(false, i);
        }

        tableElement = new SQLTableElement(Boolean.class, "") {
            final JCheckBoxTableCellRender checkBoxTableCellRender = new JCheckBoxTableCellRender();

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return checkBoxTableCellRender;
            }

            @Override
            public TableCellEditor getTableCellEditor(JTable table) {
                return checkBoxTableCellRender;
            }

            @Override
            public Object getValueFrom(SQLRowValues row) {
                Boolean o = map.get(row);
                if (o == null) {
                    o = Boolean.TRUE;
                    map.put(row, o);
                }
                // System.out.println(o);
                return o;
            }

            @Override
            public void setValueFrom(SQLRowValues row, Object value) {
                map.put(row, (Boolean) value);
                super.setValueFrom(row, value);
                fireActionEvent();
            }

            @Override
            public boolean isCellEditable(SQLRowValues vals) {

                return true;
            }
        };
        model.addColumn(tableElement);

        TableSorter sorter = new TableSorter(model);

        // Force the header to resize and repaint itself
        this.setModel(sorter);

        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent event) {
                updateRenderer();
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
            }
        });

        this.getTableHeader().setReorderingAllowed(false);

        this.stateManager.loadState();
        this.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
                repaint();
            }

            public void componentShown(ComponentEvent e) {

            }
        });
        ToolTipManager.sharedInstance().unregisterComponent(this);
        ToolTipManager.sharedInstance().unregisterComponent(this.getTableHeader());
        updateRenderer();
    }

    /**
     * @param list
     */
    private synchronized final void updateRenderer() {
        if (!editorAndRendererDone) {
            editorAndRendererDone = true;
            List list = this.model.getList();
            for (int i = 0; i < list.size(); i++) {

                TableColumn aColumn = getColumnModel().getColumn(i);
                SQLTableElement sqlTableElement = (SQLTableElement) list.get(i);

                TableCellRenderer renderer = sqlTableElement.getTableCellRenderer();
                aColumn.setCellRenderer(renderer);

            }

            TableColumn col = getColumnModel().getColumn(getColumnModel().getColumnCount() - 1);
            col.setMaxWidth(40);

        }
    }

    public void loadState(String filename) {
        this.stateManager.loadState(new File(filename));

    }

    // Pour remplir tout l'espace
    public boolean getScrollableTracksViewportHeight() {
        return getPreferredSize().height < getParent().getHeight();
    }

    protected void resizeAndRepaint() {
        // Ne pas virer, car on l'appelle hors du package
        super.resizeAndRepaint();
    }

    public synchronized List<SQLRowValues> getSelectedRowValues() {
        List<SQLRowValues> l = new Vector<SQLRowValues>(model.getRowCount());
        for (int i = 0; i < model.getRowCount(); i++) {
            final SQLRowValues rowValues = model.getRowValuesAt(i);
            if (tableElement.getValueFrom(rowValues).equals(Boolean.TRUE)) {
                l.add(rowValues);
            }
        }
        return l;
    }

    public synchronized void addActionListener(ActionListener l) {
        if (!this.listeners.contains(l)) {
            this.listeners.add(l);
        } else {
            throw new IllegalArgumentException("The listener is already an ActionListner of " + this);
        }
    }

    public synchronized void removeActionListener(ActionListener l) {
        if (this.listeners.contains(l)) {
            this.listeners.remove(l);
        } else {
            throw new IllegalArgumentException("The listener not an ActionListner of " + this);
        }
    }

    private synchronized void fireActionEvent() {
        for (ActionListener listener : this.listeners) {
            listener.actionPerformed(new ActionEvent(this, this.listeners.size(), "SELECTED_CHANGED"));
        }
    }

    @Override
    public String toString() {
        return "RowValuesSelector with " + this.listeners.size() + " ActionListeners on model: " + this.model;
    }

    public void selectAll() {
        setAll(true);
    }

    public void unselectAll() {
        setAll(false);
    }

    public void select(SQLRowValues row, boolean b) {
        map.put(row, Boolean.valueOf(b));
        model.fireTableDataChanged();
        fireActionEvent();
    }

    public void unselect(SQLRowValues row, boolean b) {
        map.put(row, Boolean.valueOf(b));
        model.fireTableDataChanged();
        fireActionEvent();
    }

    /**
     * 
     */
    private void setAll(boolean b) {
        for (int i = 0; i < model.getRowCount(); i++) {
            final SQLRowValues rowValues = model.getRowValuesAt(i);
            map.put(rowValues, Boolean.valueOf(b));
        }
        model.fireTableDataChanged();
        fireActionEvent();
    }

}
