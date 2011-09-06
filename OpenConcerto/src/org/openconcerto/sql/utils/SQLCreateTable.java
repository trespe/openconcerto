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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.SQLKey;

import java.util.Collections;
import java.util.List;

/**
 * Construct an CREATE TABLE statement with an ID, a field archive and order.
 * 
 * @author Sylvain
 */
public class SQLCreateTable extends SQLCreateTableBase<SQLCreateTable> {

    private final DBRoot b;
    private boolean plain;

    public SQLCreateTable(final DBRoot b, final String name) {
        super(b.getServer().getSQLSystem().getSyntax(), name);
        this.b = b;
        this.reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.plain = false;
    }

    public final DBRoot getRoot() {
        return this.b;
    }

    /**
     * Whether ID, ARCHIVE and ORDER are added automatically.
     * 
     * @param b <code>true</code> if no clauses should automagically be added.
     */
    public final void setPlain(boolean b) {
        this.plain = b;
    }

    public SQLCreateTable addForeignColumn(String foreignTable) {
        return this.addForeignColumn(foreignTable, "");
    }

    public SQLCreateTable addForeignColumn(String foreignTableN, String suffix) {
        final String fk = SQLKey.PREFIX + foreignTableN + (suffix.length() == 0 ? "" : "_" + suffix);
        final SQLTable foreignTable = this.b.getTable(foreignTableN);
        if (foreignTable == null)
            throw new IllegalArgumentException("Unknown table in " + this.b + " : " + foreignTableN);
        return this.addForeignColumn(fk, foreignTable, true);
    }

    @Override
    public List<String> getPrimaryKey() {
        return this.plain ? super.getPrimaryKey() : Collections.singletonList(SQLSyntax.ID_NAME);
    }

    protected void checkPK() {
        if (!this.plain)
            throw new IllegalStateException("can only set primary key in plain mode, otherwise it is automatically added");
    }

    @Override
    protected void modifyClauses(List<String> genClauses) {
        if (!this.plain) {
            genClauses.add(0, SQLBase.quoteIdentifier(SQLSyntax.ID_NAME) + this.getSyntax().getPrimaryIDDefinition());
            genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ARCHIVE_NAME) + this.getSyntax().getArchiveDefinition());
            // MS treat all NULL equals contrary to the standard
            if (getSyntax().getSystem() == SQLSystem.MSSQL) {
                genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + this.getSyntax().getOrderType() + " DEFAULT " + this.getSyntax().getOrderDefault());
            } else {
                genClauses.add(SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + this.getSyntax().getOrderDefinition());
            }
        }
    }

    @Override
    protected void modifyOutClauses(List<DeferredClause> clauses) {
        super.modifyOutClauses(clauses);
        if (!this.plain && getSyntax().getSystem() == SQLSystem.MSSQL) {
            clauses.add(new OutsideClause() {
                @Override
                public ClauseType getType() {
                    return ClauseType.ADD_INDEX;
                }

                @Override
                public String asString(SQLName tableName) {
                    return "create unique index idx on " + tableName.quote() + "(" + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") where " + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME)
                            + " is not null";
                }
            });
        }
    }

    public final String asString() {
        return super.asString(this.b.getName());
    }

}
