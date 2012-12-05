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
 
 package org.openconcerto.sql.view.column;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.PopupMouseListener;
import org.openconcerto.ui.SwingThreadUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.SwingWorker2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public class ColumnPanel extends JPanel {

    private ColumnPanelFetcher fetcher;

    private List<Column> columns;

    private int colunmWidth;

    private List<ColumnView> columnViews;

    private final ColumnRowRenderer cRenderer;
    private final ColumnFooterRenderer fRenderer;

    private boolean loading;

    private ColumnHeaderRenderer headerRenderer;

    public ColumnPanel(int colunmWidth, ColumnRowRenderer cRenderer, ColumnFooterRenderer fRenderer) {
        this.colunmWidth = colunmWidth;
        this.cRenderer = cRenderer;
        this.fRenderer = fRenderer;
        JPopupMenu menu = new JPopupMenu();
        menu.add(new AbstractAction("Mettre à jour") {

            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }

        });
        this.addMouseListener(new PopupMouseListener(menu));

    }

    public void setHeaderRenderer(ColumnHeaderRenderer r) {
        this.headerRenderer = r;
    }

    public synchronized void setFetch(ColumnPanelFetcher fetcher) {
        this.fetcher = fetcher;
        SwingThreadUtils.invoke(new Runnable() {

            @Override
            public void run() {
                loadColumnHeader();

            }
        });
        // Update view is database change
        final Set<SQLTable> tables = this.fetcher.getFecthTables();
        for (SQLTable sqlTable : tables) {
            sqlTable.addTableModifiedListener(new SQLTableModifiedListener() {
                @Override
                public void tableModified(SQLTableEvent evt) {
                    SwingThreadUtils.invoke(new Runnable() {
                        @Override
                        public void run() {
                            reload();
                        }
                    });
                }
            });
        }
    }

    public void reload() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalAccessError("not in EventDispatchThread");
        }
        if (!loading) {

            this.removeAll();
            loadColumnHeader();
            revalidate();
        }
    }

    /**
     * Step 1: create UI from column names
     * */
    private synchronized void loadColumnHeader() {
        // Must be called from EventDispatchThread
        loading = true;
        SwingWorker2<Object, Object> worker = new SwingWorker2<Object, Object>() {

            @Override
            protected Object doInBackground() throws Exception {
                fetcher.clear();
                int cCount = fetcher.getColumnCount();
                final List<String> names = fetcher.getColumnName();
                columns = new ArrayList<Column>(cCount);
                for (int i = 0; i < cCount; i++) {
                    columns.add(new Column(names.get(i)));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Always call get() for error checking
                    get();
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur de chargement des colonnes", e);
                }
                layoutColumns();
                loadColumnsContent();
            }
        };
        worker.execute();
    }

    protected void layoutColumns() {
        final int size = this.columns.size();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        if (this.headerRenderer != null) {
            c.gridwidth = size * 2 - 1;
            c.weightx = 1;
            c.weighty = 0;
            c.gridx = 0;
            this.add(this.headerRenderer, c);
            c.gridy++;
            c.gridwidth = 1;
        }

        c.insets = new Insets(0, 3, 0, 2);
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        columnViews = new ArrayList<ColumnView>(size);
        for (int i = 0; i < size; i++) {
            c.weightx = 1;
            Column column = this.columns.get(i);
            final ColumnView cView = new ColumnView(column, colunmWidth, cRenderer, fRenderer);
            cView.setOpaque(false);
            this.columnViews.add(cView);
            this.add(cView, c);
            c.gridx++;
            c.weightx = 0;
            if (i < size - 1) {
                this.add(new JSeparator(JSeparator.VERTICAL), c);
            }
        }
        revalidate();

    }

    /**
     * Step 2: fill ColumnView
     * */
    private void loadColumnsContent() {
        SwingWorker2<Object, Integer> worker = new SwingWorker2<Object, Integer>() {

            @Override
            protected Integer doInBackground() throws Exception {
                int cCount = columns.size();
                List<SQLRowAccessor> allRows = new ArrayList<SQLRowAccessor>();
                for (int i = 0; i < cCount; i++) {
                    final List<? extends SQLRowAccessor> rows = fetcher.getRowsForColumn(i);
                    columns.get(i).setRows(rows);
                    allRows.addAll(rows);
                }
                if (headerRenderer != null) {
                    headerRenderer.setContent(allRows);
                }
                return cCount;

            }

            @Override
            protected void done() {
                try {
                    // Always call get() for error checking
                    get();
                    for (ColumnView col : columnViews) {
                        col.updateContent();
                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur de mise à jour des colonnes", e);
                }
                revalidate();
                loading = false;
            }
        };
        worker.execute();
    }
}
