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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Construct an ALTER TABLE statement.
 * 
 * @author Sylvain
 */
public final class AlterTable extends ChangeTable<AlterTable> {

    private final SQLTable t;

    public AlterTable(SQLTable t) {
        super(t.getServer().getSQLSystem().getSyntax(), t.getName());
        this.t = t;
    }

    // for CREATE TABLE
    AlterTable(SQLSyntax s, String tableName) {
        super(s, tableName);
        this.t = null;
    }

    public final AlterTable addColumn(String name, String definition) {
        // column keyword is not accepted by H2
        return this.addClause("ADD " + SQLBase.quoteIdentifier(name) + " " + definition, ClauseType.ADD_COL);
    }

    public final AlterTable dropColumn(String name) {
        return this.addClause("DROP COLUMN " + SQLBase.quoteIdentifier(name), ClauseType.DROP_COL);
    }

    public final AlterTable alterColumn(String fname, SQLField from) {
        return this.alterColumn(fname, from, EnumSet.allOf(Properties.class));
    }

    /**
     * Transform <code>fname</code> into <code>from</code>.
     * 
     * @param fname the field to change.
     * @param from the field to copy.
     * @param toTake which properties of <code>from</code> to copy.
     * @return this.
     */
    public final AlterTable alterColumn(String fname, SQLField from, Set<Properties> toTake) {
        for (final String s : this.getSyntax().getAlterField(this.t.getField(fname), from, toTake))
            this.addClause(s, ClauseType.ALTER_COL);
        return thisAsT();
    }

    /**
     * Alter column properties. Any property value can be <code>null</code> if not used. This method
     * has many parameters since in many systems, properties cannot be changed individually.
     * 
     * @param fname the field to change.
     * @param toAlter which properties to change.
     * @param type the new type.
     * @param defaultVal the new default value.
     * @param nullable whether this field can hold NULL.
     * @return this.
     * @see #alterColumn(String, SQLField, Set)
     */
    public final AlterTable alterColumn(String fname, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        for (final String s : this.getSyntax().getAlterField(this.t.getField(fname), toAlter, type, defaultVal, nullable))
            this.addClause(s, ClauseType.ALTER_COL);
        return thisAsT();
    }

    public final AlterTable alterColumnNullable(String f, boolean b) {
        return this.alterColumn(f, Collections.singleton(Properties.NULLABLE), null, null, b);
    }

    /**
     * Drop a foreign constraint.
     * 
     * @param name the name of the constraint to drop.
     * @return this.
     * @see Link#getName()
     */
    public final AlterTable dropForeignConstraint(String name) {
        return this.addClause(getSyntax().getDropFK() + SQLBase.quoteIdentifier(name), ClauseType.DROP_CONSTRAINT);
    }

    public final AlterTable dropConstraint(String name) {
        return this.addClause(getSyntax().getDropConstraint() + SQLBase.quoteIdentifier(name), ClauseType.DROP_CONSTRAINT);
    }

    public final AlterTable dropIndex(final String name) {
        return this.addOutsideClause(new OutsideClause() {
            @Override
            public ClauseType getType() {
                return ClauseType.DROP_INDEX;
            }

            @Override
            public String asString(SQLName tableName) {
                return getSyntax().getDropIndex(name, tableName);
            }
        });
    }

    public final String asString() {
        return this.asString(this.t.getDBRoot().getName());
    }

    @Override
    protected String asString(String rootName, ConcatStep step) {
        return this.asString(rootName, step.getTypes());
    }

    @Override
    public String asString(String rootName) {
        return this.asString(rootName, EnumSet.allOf(ClauseType.class));
    }

    private final String asString(final String rootName, final Set<ClauseType> types) {
        final SQLName tableName = new SQLName(rootName, this.getName());
        final List<String> genClauses = new ArrayList<String>(this.getClauses(tableName, types));
        this.modifyClauses(genClauses);
        if (types.contains(ClauseType.ADD_CONSTRAINT))
            genClauses.addAll(this.getForeignConstraints(rootName));

        final StringBuffer res = new StringBuffer(512);
        final String alterTable = "ALTER TABLE " + tableName.quote();
        // sometimes there's only OutsideClauses
        if (this.getSyntax().supportMultiAlterClause() && genClauses.size() > 0) {
            res.append(alterTable + " \n");
            res.append(CollectionUtils.join(genClauses, ",\n"));
            res.append(";");
        } else {
            res.append(CollectionUtils.join(genClauses, "\n", new ITransformer<String, String>() {
                @Override
                public String transformChecked(String input) {
                    return alterTable + " " + input + ";";
                }
            }));
        }

        this.outClausesAsString(res, tableName, types);

        return res.toString();
    }

    protected void modifyClauses(final List<String> genClauses) {
    }

}
