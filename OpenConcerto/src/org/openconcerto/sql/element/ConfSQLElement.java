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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.i18n.Phrase;

import java.util.Collections;
import java.util.List;

/**
 * @author Sylvain CUAZ
 */
public class ConfSQLElement extends SQLElement {

    @Deprecated
    public ConfSQLElement(String tableName, String singular, String plural) {
        this(Configuration.getInstance(), tableName, singular, plural);
    }

    @Deprecated
    public ConfSQLElement(Configuration conf, String tableName, String singular, String plural) {
        this(conf.getRoot().findTable(tableName), singular, plural);
    }

    @Deprecated
    public ConfSQLElement(SQLTable table, String singular, String plural) {
        super(singular, plural, table);
    }

    public ConfSQLElement(String tableName) {
        this(tableName, null);
    }

    public ConfSQLElement(Configuration conf, String tableName) {
        this(conf, tableName, null);
    }

    public ConfSQLElement(SQLTable table) {
        this(table, null);
    }

    public ConfSQLElement(String tableName, final Phrase name) {
        this(Configuration.getInstance(), tableName, name);
    }

    public ConfSQLElement(Configuration conf, String tableName, final Phrase name) {
        this(conf.getRoot().findTable(tableName), name);
    }

    public ConfSQLElement(final SQLTable primaryTable, final Phrase name) {
        super(primaryTable, name);
    }

    @Override
    protected List<String> getComboFields() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getListFields() {
        return Collections.emptyList();
    }

    @Override
    public SQLComponent createComponent() {
        return null;
    }
}
