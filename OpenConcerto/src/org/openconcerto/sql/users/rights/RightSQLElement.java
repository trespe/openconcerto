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

import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.i18n.I18nUtils;

import java.util.Arrays;
import java.util.List;

public class RightSQLElement extends ConfSQLElement {

    public static final String TABLE_NAME = "RIGHT";

    static public SQLCreateTable getCreateTable(final DBRoot root) {
        final SQLCreateTable res = new SQLCreateTable(root, TABLE_NAME);
        res.addVarCharColumn("CODE", 128);
        res.addVarCharColumn("NOM", 256);
        res.addVarCharColumn("DESCRIPTION", 500);
        return res;
    }

    public RightSQLElement() {
        super(TABLE_NAME);
        this.setL18nPackageName(I18nUtils.getPackageName(TM.class));
    }

    @Override
    public boolean isShared() {
        return true;
    }

    protected List<String> getListFields() {
        return Arrays.asList("CODE", "NOM", "DESCRIPTION");
    }

    protected List<String> getComboFields() {
        return Arrays.asList("NOM");
    }

    @Override
    public CollectionMap<String, String> getShowAs() {
        return CollectionMap.singleton(null, "NOM");
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this, 2, 1) {
            public void addViews() {
                this.addView("CODE");
                this.addView("NOM");
                this.addView(new ITextArea(), "DESCRIPTION", "2");
            }
        };
    }
}
