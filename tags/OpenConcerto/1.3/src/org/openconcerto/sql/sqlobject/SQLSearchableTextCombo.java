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
import org.openconcerto.ui.component.IComboCacheListModel;
import org.openconcerto.ui.component.combo.ISearchableTextCombo;

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

    @Override
    public void init(SQLRowItemView v) {
        if (this.getCache() == null)
            this.initCache(new ISQLListModel(v.getField()).load());
    }

    /**
     * Load <code>cache</code> and only afterwards call
     * {@link #initCache(org.openconcerto.utils.model.IListModel)}.
     * 
     * @param cache the cache to set.
     */
    public void initCacheLater(final ISQLListModel cache) {
        cache.initCacheLater(this);
    }

    public static class ISQLListModel extends IComboCacheListModel {

        public ISQLListModel(final SQLField f) {
            this(new ITextComboCacheSQL(f));
        }

        public ISQLListModel(final ITextComboCacheSQL c) {
            super(c);
        }
    }
}
