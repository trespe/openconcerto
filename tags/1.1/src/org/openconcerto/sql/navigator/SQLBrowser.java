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
 
 /*
 * Créé le 21 mai 2005
 */
package org.openconcerto.sql.navigator;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class SQLBrowser extends JPanel {

    private final List<SQLBrowserColumn> columns = new Vector<SQLBrowserColumn>();
    private SQLBrowserColumn activeCol;
    private final JPanel minimizedPanel = new JPanel();
    private final JPanel mainPanel = new JPanel();
    private final Set<SQLBrowserListener> browserListeners = new HashSet<SQLBrowserListener>();
    private int maxVisibleColumn = 5;
    private final ITransformer<SQLElement, Collection<SQLField>> childrenTransf;
    private int selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION;

    private final List<Action> actions = new ArrayList<Action>();

    public SQLBrowser(final RowsSQLBrowserColumn root) {
        this(root, null);
    }

    /**
     * Create a new browser. childrenTransf allow one to make a custom hierarchy.
     * 
     * @param root the first column.
     * @param childrenTransf a transformer that is passed a SQLElement and should return the
     *        children fields or <code>null</code> to take the default ones, can be
     *        <code>null</code>.
     */
    public SQLBrowser(final RowsSQLBrowserColumn root, final ITransformer<SQLElement, Collection<SQLField>> childrenTransf) {
        this.childrenTransf = childrenTransf;
        this.activeCol = null;

        this.setBackground(new Color(255, 251, 242));
        this.setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.ipadx = 1;
        this.add(this.minimizedPanel, c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.mainPanel, c);
        // Layout
        this.minimizedPanel.setLayout(new GridLayout(1, 1));
        this.mainPanel.setBackground(new Color(140, 141, 121));
        final GridLayout grid = new GridLayout(1, 1);
        grid.setHgap(1);
        this.mainPanel.setLayout(grid);
        this.minimizedPanel.setVisible(false);

        this.actions.add(createColumnAction(0, KeyEvent.VK_F1));
        this.actions.add(createColumnAction(1, KeyEvent.VK_F2));
        this.actions.add(createColumnAction(2, KeyEvent.VK_F3));
        this.actions.add(createColumnAction(3, KeyEvent.VK_F4));
        this.addChangeAction(0, KeyEvent.VK_F1, KeyEvent.VK_F2);
        this.addChangeAction(1, KeyEvent.VK_F3, KeyEvent.VK_F4);
        this.addChangeAction(2, KeyEvent.VK_F5, KeyEvent.VK_F6);
        this.addChangeAction(3, KeyEvent.VK_F7, KeyEvent.VK_F8);

        root.reload();
        this.addColumn(root, null);
    }

    public void setMaxVisibleColumn(final int i) {
        this.maxVisibleColumn = i;
    }

    public void addColumn(final SQLBrowserColumn col, final SQLBrowserColumn after) {
        if (after != null) {
            if (after.next() == col) {
                col.deselect();
                this.rmColumnAfter(col);
                return;
            }
            // remove ceux d'apres
            this.rmColumnAfter(after);
        }
        if (!this.columns.contains(col)) {
            col.setParentBrowser(this);
            // in ListSelectionModel, the modes are sorted from the most restrictive to the least
            col.setSelectionMode(Math.min(col.getSelectionMode(), this.selectionMode));
            this.columns.add(col);
            // don't reload: (except for the root) every column is created as a child of another
            // thus setParentIDs() has already been called

            addColToGUI(col);
            this.minimizeExtraColumns();
        }
    }

    private void addColToGUI(final SQLBrowserColumn col) {
        this.mainPanel.add(col);
        col.setShortcut("F" + this.mainPanel.getComponentCount());
    }

    /**
     * Remove all columns after <code>col</code>, ie it will then be the last column.
     * 
     * @param col the column after which to remove.
     */
    public void rmColumnAfter(final SQLBrowserColumn col) {
        if (col == null)
            throw new NullPointerException("from null");

        final int index = this.columns.indexOf(col);
        for (int i = this.columns.size() - 1; i > index; i--) {
            final SQLBrowserColumn e = this.columns.get(i);
            // first tell the column so that it can still use previous, etc.
            e.setParentBrowser(null);
            this.columns.remove(i);
            this.mainPanel.remove(e);
        }
        this.revalidate();
    }

    private void rebuild() {
        this.minimizedPanel.removeAll();
        this.mainPanel.removeAll();
        for (int i = 0; i < this.columns.size(); i++) {
            final SQLBrowserColumn col = this.columns.get(i);

            if (col.isMinimized()) {
                this.minimizedPanel.add(col);
                this.minimizedPanel.setVisible(true);
            } else {
                this.addColToGUI(col);
            }
        }
        this.revalidate();
    }

    // *** getColumns

    protected final SQLBrowserColumn<?, ?> getActiveColumn() {
        return this.activeCol;
    }

    public List<SQLBrowserColumn> getColumns() {
        return Collections.unmodifiableList(this.columns);
    }

    public List<SQLBrowserColumn> getVisibleColumns() {
        final int minimizedCount = this.minimizedPanel.getComponentCount();
        return Collections.unmodifiableList(this.columns.subList(minimizedCount, this.columns.size()));
    }

    public SQLBrowserColumn getLastColumn() {
        return this.columns.get(this.columns.size() - 1);
    }

    private RowsSQLBrowserColumn getFirstColumn() {
        return (RowsSQLBrowserColumn) this.getColumns().get(0);
    }

    // *** min/max

    /**
     * Assure that no more than <code>maxVisibleColumns</code> are visible.
     */
    private void minimizeExtraColumns() {
        final int extraColsCount = this.columns.size() - this.maxVisibleColumn;
        if (extraColsCount > 0) {
            for (int i = 0; i < extraColsCount; i++) {
                final SQLBrowserColumn col = this.columns.get(i);
                col.setMinimizedState(true);
            }
            this.rebuild();
        }
    }

    /**
     * Maximize all columns from <code>column</code>.
     * 
     * @param column the first column to be maximized.
     */
    public void maximizeFrom(final SQLBrowserColumn column) {
        this.minMax(column, true);
        column.setActive();
    }

    private void minMax(final SQLBrowserColumn column, boolean max) {
        final int index = this.columns.indexOf(column);
        final int start = max ? index : 0;
        final int stop = max ? this.columns.size() : index;
        for (int i = start; i < stop; i++) {
            final SQLBrowserColumn col = this.columns.get(i);
            col.setMinimizedState(!max);
        }
        this.rebuild();
    }

    /**
     * Minimize all columns up to <code>column</code>.
     * 
     * @param column the last column to be minimized.
     */
    public void minimizeUntil(final SQLBrowserColumn column) {
        this.minMax(column, false);
    }

    // *** listeners

    public void addSQLBrowserListener(final SQLBrowserListener l) {
        this.browserListeners.add(l);
    }

    public void removeSQLBrowserListener(final SQLBrowserListener l) {
        this.browserListeners.remove(l);
    }

    void fireSQLBrowserColumnSelected(final SQLBrowserColumn col) {
        for (final SQLBrowserListener element : this.browserListeners) {
            element.columnSelected(col);
        }
    }

    // *** active

    public void activate(final SQLBrowserColumn column) {
        if (column != null) {
            if (column.isMinimized())
                this.maximizeFrom(column);
            column.activate();
        }
    }

    public void activateVisibleColumn(int index) {
        final List<SQLBrowserColumn> cols = this.getVisibleColumns();
        if (index >= cols.size())
            index = cols.size() - 1;
        this.activate(cols.get(index));
    }

    void columnFocusChanged(final SQLBrowserColumn col, boolean gained) {
        if (this.activeCol == col && !gained) {
            this.activeCol = null;
        } else if (this.activeCol != col && gained)
            this.activeCol = col;
    }

    // *** reveal

    /**
     * Select the passed row.
     * 
     * @param r the row to select, <code>null</code> does nothing.
     */
    public void setSelectedRow(final SQLRow r) {
        if (r != null)
            this.selectPath(this.getPath(r));
    }

    // path

    private List<SQLRow> getPath(final SQLRow r) {
        if (r == null)
            return Collections.emptyList();

        final RowsSQLBrowserColumn first = this.getFirstColumn();
        final List<SQLRow> path = new ArrayList<SQLRow>();
        SQLRow currentRow = r;
        while (currentRow != null && !first.getElement().equals(getElement(currentRow.getTable()))) {
            path.add(0, currentRow);
            currentRow = getElement(currentRow.getTable()).getParent(currentRow);
        }
        if (currentRow == null)
            throw new IllegalArgumentException(r + " is not a children of " + first);
        path.add(0, currentRow);
        return path;
    }

    private void selectPath(final List<SQLRow> path) {
        this.getFirstColumn().setSelectedRow(null);
        for (final SQLRow r : path) {
            this.getLastColumn().setSelectedRow(r);
        }
    }

    static SQLElement getElement(final SQLTable t) {
        return Configuration.getInstance().getDirectory().getElement(t);
    }

    /*
     * Fonction de debug
     */
    public void dump(final PrintStream p) {
        for (int i = 0; i < this.columns.size(); i++) {
            final SQLBrowserColumn col = this.columns.get(i);
            p.println(i + " :::" + col);
        }
    }

    /**
     * Returns this list of tables from the root to previous column of <code>stopCol</code>.
     * 
     * @param stopCol this method will stop before stopCol, eg BATIMENT column.
     * @return a List of SQLTable, eg [/ETABLISSEMENT/, /SITE/].
     */
    public List<SQLTable> getTablesBefore(final SQLBrowserColumn stopCol) {
        final List<SQLTable> result = new ArrayList<SQLTable>();
        for (int i = 0; i < this.columns.size(); i++) {
            final SQLBrowserColumn col = this.columns.get(i);
            if (col == stopCol) {
                break;
            } else if (col instanceof RowsSQLBrowserColumn) {
                result.add(((RowsSQLBrowserColumn) col).getTable());
            }
        }
        return result;
    }

    /*
     * Renvoie une liste de SQLRow un genre de List<SQLRow> comme dirait Sylvain
     */
    public List<SQLRow> getFirstSelectedRows() {
        final List<SQLRow> result = new ArrayList<SQLRow>();
        for (int i = 0; i < this.columns.size(); i++) {
            final SQLBrowserColumn col = this.columns.get(i);
            if (col instanceof RowsSQLBrowserColumn) {
                final SQLRow firstSelectedRow = ((RowsSQLBrowserColumn) col).getFirstSelectedRow();
                if (firstSelectedRow != null)
                    result.add(firstSelectedRow);
            }
        }
        return result;
    }

    /**
     * The selected rows of the last column with a non empty selection.
     * 
     * @return the last selected rows.
     */
    public List<SQLRow> getSelectedRows() {
        final List<SQLBrowserColumn> reverse = new ArrayList<SQLBrowserColumn>(this.columns);
        Collections.reverse(reverse);
        for (final SQLBrowserColumn<?, ?> col : reverse) {
            if (!col.getSelectedRows().isEmpty())
                return col.getSelectedRows();
        }
        return Collections.emptyList();
    }

    /**
     * The selected rows of the last column which restricts the browser selection. Eg if the last
     * column has "ALL" selected, return the selection of the previous column.
     * 
     * @return the last meaningful selection.
     */
    public List<SQLRow> getLastMeaningfullRows() {
        final SQLBrowserColumn<?, ?> col = getLastMeaningfullCol();
        return col == null ? Collections.<SQLRow> emptyList() : col.getUserSelectedRows();
    }

    /**
     * The selected rows of all columns with meaningful selection, ie not ALL (except if searched)
     * and not empty.
     * 
     * @return the selected rows of all columns.
     */
    public List<Set<SQLRow>> getMeaningfullRows() {
        final SQLBrowserColumn<?, ?> col = getLastMeaningfullCol();
        if (col == null)
            return Collections.<Set<SQLRow>> emptyList();
        else {
            final List<Set<SQLRow>> res = new ArrayList<Set<SQLRow>>();
            res.add(new HashSet<SQLRow>(col.getUserSelectedRows()));
            SQLBrowserColumn<?, ?> c = col.previousRowsColumn();
            while (c != null) {
                res.add(c.getModel().getHighlighted());
                c = c.previousRowsColumn();
            }
            return res;
        }
    }

    private final SQLBrowserColumn getLastMeaningfullCol() {
        final List<SQLBrowserColumn> reverse = new ArrayList<SQLBrowserColumn>(this.columns);
        Collections.reverse(reverse);
        for (final SQLBrowserColumn<?, ?> col : reverse) {
            final List<SQLRow> selectedRows = col.getUserSelectedRows();
            // meaningfull == not ALL (except if searched) && not empty
            if (!selectedRows.isEmpty() && (!col.isAllSelected() || col.isSearched()))
                return col;
        }
        return null;
    }

    public final ITransformer<SQLElement, Collection<SQLField>> getChildrenTransformer() {
        return this.childrenTransf;
    }

    /**
     * Set the selection mode of the lists.
     * 
     * @param selectionMode ListSelectionModel.SINGLE_SELECTION SINGLE_INTERVAL_SELECTION
     *        ListSelectionModel.SINGLE_INTERVAL_SELECTION MULTIPLE_INTERVAL_SELECTION
     *        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
     */
    public void setSelectionMode(final int selectionMode) {
        this.selectionMode = selectionMode;
        for (int i = 0; i < this.columns.size(); i++) {
            final SQLBrowserColumn col = this.columns.get(i);
            col.setSelectionMode(this.selectionMode);
        }
    }

    // *** actions

    public final List<Action> getActions() {
        return this.actions;
    }

    private final void addChangeAction(final int colIndex, final int keyUp, final int keyDown) {
        this.actions.add(createChangeFilterAction(colIndex, KeyStroke.getKeyStroke(keyUp, InputEvent.CTRL_DOWN_MASK), true));
        this.actions.add(createChangeFilterAction(colIndex, KeyStroke.getKeyStroke(keyDown, InputEvent.CTRL_DOWN_MASK), false));
    }

    private Action createColumnAction(final int colIndex, final int key) {
        return createColumnAction(colIndex, KeyStroke.getKeyStroke(key, 0));
    }

    private Action createColumnAction(final int colIndex, final KeyStroke key) {
        return new AbstractAction() {
            {
                putValue(Action.NAME, "select col " + colIndex + " of " + SQLBrowser.this);
                putValue(Action.ACTION_COMMAND_KEY, "Column" + colIndex + "[" + key + "]");
                putValue(Action.ACCELERATOR_KEY, key);
            }

            public void actionPerformed(final ActionEvent e) {
                // deiconify
                ((Frame) SwingUtilities.getAncestorOfClass(Frame.class, SQLBrowser.this)).setExtendedState(Frame.NORMAL);
                // bring to front
                SwingUtilities.getWindowAncestor(SQLBrowser.this).toFront();
                SQLBrowser.this.activateVisibleColumn(colIndex);
            }
        };
    }

    private Action createChangeFilterAction(final int colIndex, final KeyStroke key, final boolean up) {
        return new AbstractAction() {
            {
                putValue(Action.NAME, "change selection of col " + colIndex + " of " + SQLBrowser.this);
                putValue(Action.ACTION_COMMAND_KEY, "Change Column" + colIndex + "[" + key + "]");
                putValue(Action.ACCELERATOR_KEY, key);
            }

            public void actionPerformed(final ActionEvent e) {
                final List<SQLBrowserColumn> cols = SQLBrowser.this.getVisibleColumns();
                // test if the column exists
                if (colIndex < cols.size())
                    cols.get(colIndex).select(!up);
            }
        };
    }

}
