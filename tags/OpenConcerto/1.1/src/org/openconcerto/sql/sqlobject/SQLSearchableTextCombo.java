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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.SQLTextCombo.ITextComboCacheSQL;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;
import org.openconcerto.utils.change.CollectionChangeEvent;
import org.openconcerto.utils.change.IListDataEvent;
import org.openconcerto.utils.model.DefaultIMutableListModel;

import java.util.Collection;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * An ISearchableTextCombo with the cache from COMPLETION.
 * 
 * @author Sylvain CUAZ
 */
public class SQLSearchableTextCombo extends ISearchableTextCombo implements RowItemViewComponent {

    public SQLSearchableTextCombo() {
        this(ComboLockedMode.UNLOCKED);
    }

    public SQLSearchableTextCombo(boolean locked) {
        super(locked);
    }

    public SQLSearchableTextCombo(ComboLockedMode mode) {
        super(mode);
    }

    public SQLSearchableTextCombo(ComboLockedMode mode, int rows, int columns) {
        super(mode, rows, columns);
    }

    public SQLSearchableTextCombo(ComboLockedMode mode, boolean textArea) {
        super(mode, textArea);
    }

    public SQLSearchableTextCombo(ComboLockedMode mode, int rows, int columns, boolean textArea) {
        super(mode, rows, columns, textArea);
    }

    public void init(SQLRowItemView v) {
        // after uiInit since our superclass add listeners to our UI
        this.initCacheLater(new ISQLListModel(v.getField()));
    }

    /**
     * Load <code>cache</code> and only afterwards call
     * {@link #initCache(org.openconcerto.utils.model.IListModel)}.
     * 
     * @param cache the cache to set.
     */
    public void initCacheLater(final ISQLListModel cache) {
        cache.load(new Runnable() {
            @Override
            public void run() {
                initCache(cache);
            }
        });
    }

    public static class ISQLListModel extends DefaultIMutableListModel<String> {

        private final ITextComboCacheSQL cache;
        private final ListDataListener l;

        public ISQLListModel(final SQLField f) {
            this(new ITextComboCacheSQL(f));
        }

        public ISQLListModel(final ITextComboCacheSQL c) {
            this.cache = c;
            this.l = new ListDataListener() {

                @SuppressWarnings("unchecked")
                public void contentsChanged(ListDataEvent e) {
                    // selection change, see DefaultIMutableListModel#setSelectedItem()
                    if (e.getIndex0() < 0)
                        return;

                    final CollectionChangeEvent evt = ((IListDataEvent) e).getCollectionChangeEvent();
                    this.remove(evt);
                    this.add(evt.getItemsAdded());
                }

                public void intervalAdded(ListDataEvent e) {
                    this.add(getList().subList(e.getIndex0(), e.getIndex1() + 1));
                }

                public void intervalRemoved(ListDataEvent e) {
                    this.remove(((IListDataEvent) e).getCollectionChangeEvent());
                }

                private void add(Collection<String> toAdd) {
                    for (final String s : toAdd) {
                        ISQLListModel.this.cache.addToCache(s);
                    }
                }

                @SuppressWarnings("unchecked")
                private void remove(CollectionChangeEvent evt) {
                    for (final String s : (Collection<String>) evt.getItemsRemoved())
                        ISQLListModel.this.cache.deleteFromCache(s);
                }
            };
        }

        private void load(final Runnable r) {
            if (this.cache.isValid()) {
                new SwingWorker<List<String>, Object>() {

                    @Override
                    protected List<String> doInBackground() throws Exception {
                        return ISQLListModel.this.cache.loadCache();
                    }

                    @Override
                    protected void done() {
                        // don't remove and add from the cache, items just came from it
                        removeListDataListener(ISQLListModel.this.l);
                        removeAllElements();
                        try {
                            addAll(get());
                        } catch (Exception e1) {
                            // tant pis, pas de cache
                            e1.printStackTrace();
                        }
                        addListDataListener(ISQLListModel.this.l);
                        if (r != null)
                            r.run();
                    }

                }.execute();
            }
        }
    }
}
