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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo.ISQLListModel;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.sqlobject.SQLTextCombo.ITextComboCacheSQL;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.preferences.JavaPrefPreferencePanel;
import org.openconcerto.ui.preferences.PrefView;
import org.openconcerto.utils.PrefType;

import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public abstract class ModulePreferencePanel extends JavaPrefPreferencePanel {

    static private DBRoot getRoot() {
        return ModuleManager.getInstance().getRoot();
    }

    static String getAppPrefPath() {
        return Configuration.getInstance().getAppID() + '/';
    }

    public static class SQLPrefView<T> extends PrefView<T> {

        public SQLPrefView(PrefType<T> type, String name, String prefKey) {
            super(type, name, prefKey);
        }

        public SQLPrefView(PrefType<T> type, int length, String name, String prefKey) {
            super(type, length, name, prefKey);
        }

        @Override
        protected JComponent createComponent() {
            final JComponent comp;
            if (Boolean.class.isAssignableFrom(this.getViewClass())) {
                // ATTN hack to view the focus (should try to paint around the button)
                comp = new JCheckBox(" ");
            } else if (Date.class.isAssignableFrom(this.getViewClass())) {
                comp = new JDate();
            } else if (String.class.isAssignableFrom(this.getViewClass()) && this.getLength() >= 512) {
                comp = new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, true);
            } else {
                comp = new SQLTextCombo(false);
            }
            return comp;
        }

        @Override
        public void init(final JavaPrefPreferencePanel prefPanel) {
            final JComponent comp = this.getVW().getComp();
            if (comp instanceof SQLTextCombo) {
                ((SQLTextCombo) comp).initCache(createCache(prefPanel));
            } else if (comp instanceof SQLSearchableTextCombo) {
                ((SQLSearchableTextCombo) comp).initCacheLater(new ISQLListModel(createCache(prefPanel)));
            }
        }

        private ITextComboCacheSQL createCache(final JavaPrefPreferencePanel prefPanel) {
            return new ITextComboCacheSQL(getRoot(), prefPanel.getPrefPath() + '/' + this.getPrefKey());
        }
    }

    public ModulePreferencePanel(final String title) {
        super(title, null);

    }

    public final void init(final ModuleFactory module, final boolean local) {
        this.setPrefs(module.getPreferences(local, getRoot()));
    }
}
