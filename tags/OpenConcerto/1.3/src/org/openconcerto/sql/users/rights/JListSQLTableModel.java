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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

public class JListSQLTableModel extends DefaultListModel {

    private final List<IComboSelectionItem> list;
    private final SQLTable table;
    private final Comparator<IComboSelectionItem> comp;
    private final ComboSQLRequest request;
    String undefined;
    protected SwingWorker<?, ?> updating = null;
    private int idToSelect = -1;

    final SQLTableModifiedListener tableModifiedListener = new SQLTableModifiedListener() {
        private void rowModified(SQLTableEvent evt) {
            final int index = getIndexForId(evt.getId());
            final IComboSelectionItem newItem = JListSQLTableModel.this.request.getComboItem(evt.getId());
            if (newItem != null) {
                if (index >= 0)
                    JListSQLTableModel.this.list.remove(index);
                addNewItem(newItem);
            } else if (index >= 0) {
                JListSQLTableModel.this.list.remove(index);
                JListSQLTableModel.this.fireIntervalRemoved(JListSQLTableModel.this, index, index);
            }
        }

        private void addNewItem(final IComboSelectionItem newItem) {
            if (newItem != null) {
                JListSQLTableModel.this.list.add(newItem);
                Collections.sort(JListSQLTableModel.this.list, JListSQLTableModel.this.comp);
                JListSQLTableModel.this.fireContentsChanged(JListSQLTableModel.this, 0, JListSQLTableModel.this.getSize());
            }
        }

        @Override
        public void tableModified(SQLTableEvent evt) {
            if (evt.getId() < SQLRow.MIN_VALID_ID)
                fillTree();
            else if (evt.getMode() == Mode.ROW_ADDED) {
                addNewItem(JListSQLTableModel.this.request.getComboItem(evt.getId()));
            } else {
                // UPDATE or DELETE
                this.rowModified(evt);
            }
        }
    };

    public JListSQLTableModel(final ComboSQLRequest req) {
        this.table = req.getPrimaryTable();
        // this.undefined = undefined;
        this.request = req;
        this.list = new ArrayList<IComboSelectionItem>();

        this.comp = new Comparator<IComboSelectionItem>() {
            public int compare(IComboSelectionItem row1, IComboSelectionItem row2) {
                // weird items at the top
                if (isFirst(row1.getId()))
                    return -1;
                else if (isFirst(row2.getId()))
                    return 1;
                else
                    return row1.getLabel().compareToIgnoreCase(row2.getLabel());
            }

            private final boolean isFirst(final int id) {
                return id < SQLRow.MIN_VALID_ID || id == getTable().getUndefinedID();
            }
        };
        fillTree();

        // SQLTable Listener

        table.addTableModifiedListener(tableModifiedListener);
    }

    public void removeTableModifiedListener() {
        table.removeTableModifiedListener(this.tableModifiedListener);
    }

    public int getSize() {
        if (this.list == null) {
            return 0;
        } else {
            return this.list.size();
        }
    }

    public Object getElementAt(int index) {
        return this.list.get(index);
    }

    public void fillTree() {
        fillTree(null);
    }

    private static final List<String> cut(final String value) {
        final List<String> v = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
            String element = (String) tokenizer.nextElement();
            v.add(element);
        }
        return v;
    }

    public synchronized void fillTree(final String match) {
        // copied from SQLRequestComboBox (MAYBE factor)
        // déjà en train de se rafraichir
        if (this.isUpdating()) {
            this.updating.cancel(true);
        }
        final SwingWorker<List<IComboSelectionItem>, Object> worker = new SwingWorker<List<IComboSelectionItem>, Object>() {
            @Override
            protected List<IComboSelectionItem> doInBackground() throws Exception {
                final List<IComboSelectionItem> comboItems = JListSQLTableModel.this.request.getComboItems();

                final List<IComboSelectionItem> res;
                if (match == null) {
                    res = comboItems;
                } else {
                    res = new ArrayList<IComboSelectionItem>(comboItems.size() + 1);
                    List<String> matchValues = cut(match.toLowerCase());
                    for (IComboSelectionItem comboItem : comboItems) {
                        boolean test = true;
                        String s = comboItem.getLabel().toLowerCase();
                        for (String string : matchValues) {
                            if (!s.contains(string)) {
                                test = false;
                                break;
                            }
                        }
                        if (test) {
                            res.add(comboItem);
                        }
                    }
                }
                Collections.sort(res, JListSQLTableModel.this.comp);
                return res;
            }

            @Override
            protected void done() {
                try {
                    synchronized (JListSQLTableModel.this) {
                        // if cancel() is called after doInBackground() nothing happens
                        // but updating is set to a new instance
                        if (this.isCancelled() || JListSQLTableModel.this.updating != this)
                            // une autre maj arrive
                            return;

                        final List<IComboSelectionItem> items = this.get();
                        JListSQLTableModel.this.list.clear();
                        JListSQLTableModel.this.list.addAll(items);
                        JListSQLTableModel.this.setUpdating(null);
                        fireContentsChanged(JListSQLTableModel.this, 0, JListSQLTableModel.this.list.size());
                    }
                } catch (InterruptedException e) {
                    // ne devrait pas arriver puisque done() appelée après doInBackground()
                    e.printStackTrace();
                } catch (CancellationException e) {
                    // ne devrait pas arriver puisqu'on teste isCancelled()
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof RTInterruptedException))
                        // pas normal
                        e.printStackTrace();
                }
            }
        };
        this.setUpdating(worker);
        worker.execute();
    }

    public boolean isUpdating() {
        return this.updating != null;
    }

    private synchronized void setUpdating(SwingWorker<?, ?> w) {
        this.updating = w;
    }

    /**
     * Obtenir l'index pour un id donné
     * 
     * @param id
     * @return l'index de l'id, -1 le cas échéant
     */
    public int getIndexForId(int id) {

        if (isUpdating()) {
            try {
                this.updating.get();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (int i = 0; i < this.getSize(); i++) {
            final IComboSelectionItem rowAt = this.list.get(i);
            if (rowAt != null && id == rowAt.getId()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Retourne la row correspondante à l'index
     * 
     * @param index
     * @return null si l'index n'est pas correct
     */
    public SQLRowAccessor getRowAt(int index) {
        return (index < 0 || index >= getSize()) ? null : this.table.getRow(this.list.get(index).getId());
    }

    public final String getPrimaryKey() {
        return this.table.getKey().getName();
    }

    public final SQLTable getTable() {
        return this.table;
    }

    public final void setWhere(final Where w) {
        this.request.setWhere(w);
        this.fillTree();
    }

    public final void setItemCustomizer(final IClosure<IComboSelectionItem> c) {
        this.request.setItemCustomizer(c);
        this.fillTree();
    }
}
