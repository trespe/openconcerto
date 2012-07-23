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
 
 package org.openconcerto.sql.model;

/**
 * A table with an alias, e.g. OBSERVATION obs.
 * 
 * @author Sylvain CUAZ
 */
public class AliasedTable implements SQLItem, TableRef {

    private final SQLTable t;
    private final String alias;

    public AliasedTable(SQLTable t) {
        this(t, null);
    }

    /**
     * Creates a new instance.
     * 
     * @param t a table, eg /OBSERVATION/.
     * @param alias the alias, can be <code>null</code>, eg "obs".
     */
    public AliasedTable(SQLTable t, String alias) {
        if (t == null)
            throw new NullPointerException("f is null");
        this.t = t;
        this.alias = alias == null ? t.getName() : alias;
    }

    @Override
    public final SQLTable getTable() {
        return this.t;
    }

    @Override
    public final String getAlias() {
        return this.alias;
    }

    /**
     * Get an aliased field.
     * 
     * @param fieldName the name of a field of {@link #getTable()}, e.g. "DESIGNATION".
     * @return the aliased field, e.g. "obs"."DESIGNATION".
     */
    @Override
    public final AliasedField getField(String fieldName) {
        return new AliasedField(this, fieldName);
    }

    @Override
    public AliasedField getKey() {
        return getField(this.getTable().getKey().getName());
    }

    @Override
    public String getSQL() {
        // always use fullname, otherwise must check the datasource's
        // default schema
        final String tableName = this.getTable().getSQLName().quote();
        final String qAlias = SQLBase.quoteIdentifier(getAlias());
        return tableName + (qAlias.equals(tableName) ? "" : " " + qAlias);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " <" + this.getSQL() + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AliasedTable) {
            final AliasedTable o = (AliasedTable) obj;
            return this.getAlias().equals(o.getAlias()) && this.getTable().equals(o.getTable());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.getAlias().hashCode() + this.getTable().hashCode();
    }
}
