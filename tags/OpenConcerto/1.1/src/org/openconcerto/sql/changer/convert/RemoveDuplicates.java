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
 
 package org.openconcerto.sql.changer.convert;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Remove duplicate rows of a table and a constraint so that it can't happen again. If the table has
 * no primary key then the constraint will be a primary key otherwise it will be a unique
 * constraint.
 * 
 * @author Sylvain CUAZ
 */
public class RemoveDuplicates extends Changer<SQLTable> {

    private final List<String> fields;

    public RemoveDuplicates(DBSystemRoot b) {
        super(b);
        this.fields = new ArrayList<String>();
    }

    @Override
    protected Class<? extends DBStructureItem> getMaxLevel() {
        return SQLTable.class;
    }

    public final List<String> getFields() {
        return this.fields;
    }

    public final void setFields(List<String> fields) {
        this.fields.clear();
        this.fields.addAll(fields);
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.setFields(SQLRow.toList(System.getProperty("org.openconcerto.sql.pks", "")));
    }

    protected void changeImpl(final SQLTable t) throws SQLException {
        this.getStream().print(t.getName() + "... ");
        if (this.getFields().size() == 0)
            throw new IllegalStateException("no fields defined");
        this.getStream().print(getFields() + " ");

        // test if duplicates can happen
        if (t.getPKsNames().equals(this.getFields()) || t.getConstraint(ConstraintType.UNIQUE, getFields()) != null)
            this.getStream().println("already");
        else {
            this.getStream().println(SQLUtils.executeAtomic(getDS(), new ConnectionHandlerNoSetup<String, SQLException>() {
                @Override
                public String handle(SQLDataSource ds) throws SQLException {
                    // cannot just select * group by into a tmp table, delete from t, and insert
                    // since when "grouping by" each selected column must be an aggregate

                    // need a primary key to differentiate duplicates
                    final String addedPK;
                    if (t.getKey() == null) {
                        addedPK = SQLSyntax.ID_NAME;
                        ds.execute(new AlterTable(t).addColumn(addedPK, getSyntax().getPrimaryIDDefinition()).asString());
                        t.fetchFields();
                    } else
                        addedPK = null;
                    assert t.getKey() != null;

                    // find out one ID per unique row
                    final SQLSelect sel = new SQLSelect(t.getBase());
                    sel.addSelect(t.getKey(), "min");
                    for (final String f : getFields())
                        sel.addGroupBy(t.getField(f));
                    ds.execute("CREATE TEMPORARY TABLE " + SQLBase.quoteIdentifier("ID_TO_KEEP") + " as " + sel.asString());

                    // delete the rest
                    ds.execute(t.getBase().quote("DELETE FROM %f where %n not in (SELECT * from " + SQLBase.quoteIdentifier("ID_TO_KEEP") + ")", t, t.getKey()));

                    // add constraint
                    if (addedPK != null) {
                        ds.execute(new AlterTable(t).dropColumn(addedPK).asString());
                        ds.execute(new AlterTable(t).addClause("ADD PRIMARY KEY(" + SQLSyntax.quoteIdentifiers(getFields()) + ")", ClauseType.ADD_CONSTRAINT).asString());
                        return "added primary key";
                    } else {
                        ds.execute(new AlterTable(t).addUniqueConstraint("uniq", getFields()).asString());
                        return "added unique constraint";
                    }
                }
            }));
        }
    }
}
