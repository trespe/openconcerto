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

import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Construct an CREATE TABLE statement.
 * 
 * @author Sylvain
 * @param <T> type of this
 */
public abstract class SQLCreateTableBase<T extends SQLCreateTableBase<T>> extends ChangeTable<T> {

    private String pk;
    private boolean tmp;

    public SQLCreateTableBase(final SQLSyntax syntax, final String name) {
        super(syntax, name);
        this.reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.pk = null;
        this.tmp = false;
    }

    public final T addColumn(String name, String definition) {
        return this.addClause(SQLBase.quoteIdentifier(name) + " " + definition, ClauseType.ADD_COL);
    }

    public final void setTemporary(boolean tmp) {
        this.tmp = tmp;
    }

    public final T setPrimaryKey(String... fields) {
        return this.setPrimaryKey(Arrays.asList(fields));
    }

    public final T setPrimaryKey(List<String> fields) {
        this.checkPK();
        if (fields.size() > 0) {
            this.pk = "PRIMARY KEY (" + CollectionUtils.join(fields, ",", new ITransformer<String, String>() {
                @Override
                public String transformChecked(String input) {
                    return SQLBase.quoteIdentifier(input);
                }
            }) + ")";
        } else
            this.pk = null;
        return thisAsT();
    }

    protected void checkPK() {
    }

    @Override
    public String asString(final String r) {
        return this.asString(r, true);
    }

    public final String asString(final String rootName, final boolean includeConstraint) {
        return this.asString(rootName, includeConstraint ? EnumSet.allOf(ClauseType.class) : EnumSet.complementOf(EnumSet.of(ClauseType.ADD_CONSTRAINT)));
    }

    @Override
    protected final String asString(String rootName, ConcatStep step) {
        switch (step) {
        case ALTER_TABLE:
            return this.asString(rootName, step.getTypes());
        case ADD_INDEX:
        case ADD_FOREIGN:
            return new AlterTable(getSyntax(), getName()).mutateTo(this).asString(rootName, step);
        default:
            return null;
        }
    }

    private String asString(final String rootName, final Set<ClauseType> types) {
        final StringBuffer res = new StringBuffer(512);
        final SQLName tableName;
        if (this.tmp) {
            // PG: temporary tables may not specify a schema name
            tableName = new SQLName(this.getName());
        } else {
            tableName = new SQLName(rootName, this.getName());
        }

        final List<String> genClauses = new ArrayList<String>(this.getClauses(tableName, types));
        this.modifyClauses(genClauses);
        if (this.pk != null && types.contains(ClauseType.ADD_COL))
            genClauses.add(this.pk);
        if (types.contains(ClauseType.ADD_CONSTRAINT)) {
            genClauses.addAll(this.getForeignConstraints(rootName));
        }
        if (genClauses.size() > 0) {
            if (this.tmp) {
                res.append("CREATE TEMPORARY TABLE ");
            } else {
                res.append("CREATE TABLE ");
            }
            res.append(tableName.quote() + " (\n");
            res.append(CollectionUtils.join(genClauses, ",\n"));
            res.append(") ");
            res.append(this.getSyntax().getCreateTableSuffix());
            res.append(";");
        }

        this.outClausesAsString(res, tableName, types);

        return res.toString();
    }

    protected void modifyClauses(final List<String> genClauses) {
    }

}
