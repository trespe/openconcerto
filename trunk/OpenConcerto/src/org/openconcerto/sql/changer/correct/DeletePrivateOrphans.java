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
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Delete unreferenced private rows.
 * 
 * @author Sylvain
 */
public class DeletePrivateOrphans extends Changer<SQLTable> {

    public DeletePrivateOrphans(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        getStream().print(t);
        if (Configuration.getInstance() == null || Configuration.getInstance().getDirectory() == null)
            throw new IllegalStateException("no directory");
        final SQLElement elem = Configuration.getInstance().getDirectory().getElement(t);
        if (elem == null) {
            getStream().println(" : no element");
            return;
        }

        final Set<SQLField> privateParentReferentFields = elem.getPrivateParentReferentFields();
        if (privateParentReferentFields.size() == 0) {
            getStream().println(" : not a private table");
            return;
        }
        if (elem.getParentForeignField() != null)
            throw new IllegalStateException("Private with a parent : " + elem.getParentForeignField());
        final Set<SQLField> referentKeys = t.getDBSystemRoot().getGraph().getReferentKeys(t);
        if (!referentKeys.equals(privateParentReferentFields))
            throw new IllegalStateException("Table is not only private : " + referentKeys);

        getStream().println("... ");

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                final SQLCreateTable createTable = new SQLCreateTable(t.getDBRoot(), "TO_DELETE_IDS");
                // don't use SQLTable since refreshing takes a lot of time
                // also we can thus use a temporary table
                createTable.setTemporary(true);
                createTable.setPlain(true);
                final String pkName = "ID";
                createTable.addColumn(pkName, t.getKey());
                createTable.setPrimaryKey(t.getKey().getName());
                getDS().execute(createTable.asString());
                // temporary table shouldn't be prefixed
                final SQLName toDeleteIDsName = new SQLName(createTable.getName());

                final SQLSelect selAllIDs = new SQLSelect(true).addSelect(t.getKey());
                // don't delete undefined
                selAllIDs.setExcludeUndefined(true);
                getDS().execute("INSERT INTO " + toDeleteIDsName.quote() + " " + selAllIDs.asString());
                final long total = getCount(toDeleteIDsName);

                if (total == 0) {
                    getStream().println("nothing to delete");
                } else {
                    // delete all used IDs
                    for (final SQLField pp : privateParentReferentFields) {
                        getDS().execute(t.getBase().quote("DELETE from %i where %i in ( " + new SQLSelect(true).addSelect(pp).asString() + ")", toDeleteIDsName, pkName));
                    }
                    // delete unused rows
                    getStream().println("deleting " + getCount(toDeleteIDsName) + " / " + total);
                    getDS().execute(t.getBase().quote("DELETE from %f where %n in ( select %i from %i )", t, t.getKey(), pkName, toDeleteIDsName));
                }
                getDS().execute("DROP TABLE " + toDeleteIDsName.quote());

                // if we just deleted LIAISON we need to delete OBSERVATION
                if (total > 0) {
                    final Set<SQLTable> privateTables = new HashSet<SQLTable>();
                    for (final SQLRowValues privateVals : elem.getPrivateGraph().getGraph().getItems()) {
                        privateTables.add(privateVals.getTable());
                    }
                    // don't loop endlessly
                    privateTables.remove(t);
                    for (final SQLTable privateTable : privateTables) {
                        DeletePrivateOrphans.this.changeImpl(privateTable);
                    }
                }

                return null;
            }
        });

        getStream().println(t + " done");
    }

    private final long getCount(final SQLName toDeleteIDsName) {
        // since we don't use SQLTable.fire() don't use the cache
        return ((Number) getDS().execute("SELECT count(*) from " + toDeleteIDsName.quote(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false))).longValue();
    }
}
