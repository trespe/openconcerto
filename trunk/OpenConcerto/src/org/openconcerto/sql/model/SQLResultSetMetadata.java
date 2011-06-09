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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.postgresql.PGResultSetMetaData;

/**
 * A ResultSetMetaData that wraps onto another one, translating getTableName() for postgresql.
 * 
 * @author Sylvain
 */
public class SQLResultSetMetadata implements ResultSetMetaData {
    private final ResultSetMetaData delegate;

    public SQLResultSetMetadata(ResultSetMetaData delegate) {
        this.delegate = delegate;
    }

    private ResultSetMetaData getDelegate() {
        return this.delegate;
    }

    public String getCatalogName(int column) throws SQLException {
        return this.getDelegate().getCatalogName(column);
    }

    public String getColumnClassName(int column) throws SQLException {
        return this.getDelegate().getColumnClassName(column);
    }

    public int getColumnCount() throws SQLException {
        return this.getDelegate().getColumnCount();
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        return this.getDelegate().getColumnDisplaySize(column);
    }

    public String getColumnLabel(int column) throws SQLException {
        return this.getDelegate().getColumnLabel(column);
    }

    public String getColumnName(int column) throws SQLException {
        if (this.getDelegate() instanceof PGResultSetMetaData)
            return ((PGResultSetMetaData) this.getDelegate()).getBaseColumnName(column);
        else
            return this.getDelegate().getColumnName(column);
    }

    public int getColumnType(int column) throws SQLException {
        return this.getDelegate().getColumnType(column);
    }

    public String getColumnTypeName(int column) throws SQLException {
        return this.getDelegate().getColumnTypeName(column);
    }

    public int getPrecision(int column) throws SQLException {
        return this.getDelegate().getPrecision(column);
    }

    public int getScale(int column) throws SQLException {
        return this.getDelegate().getScale(column);
    }

    public String getSchemaName(int column) throws SQLException {
        return this.getDelegate().getSchemaName(column);
    }

    public String getTableName(int column) throws SQLException {
        if (this.getDelegate() instanceof PGResultSetMetaData)
            return ((PGResultSetMetaData) this.getDelegate()).getBaseTableName(column);
        else
            return this.getDelegate().getTableName(column);
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        return this.getDelegate().isAutoIncrement(column);
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        return this.getDelegate().isCaseSensitive(column);
    }

    public boolean isCurrency(int column) throws SQLException {
        return this.getDelegate().isCurrency(column);
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        return this.getDelegate().isDefinitelyWritable(column);
    }

    public int isNullable(int column) throws SQLException {
        return this.getDelegate().isNullable(column);
    }

    public boolean isReadOnly(int column) throws SQLException {
        return this.getDelegate().isReadOnly(column);
    }

    public boolean isSearchable(int column) throws SQLException {
        return this.getDelegate().isSearchable(column);
    }

    public boolean isSigned(int column) throws SQLException {
        return this.getDelegate().isSigned(column);
    }

    public boolean isWritable(int column) throws SQLException {
        return this.getDelegate().isWritable(column);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.getDelegate().isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.getDelegate().unwrap(iface);
    }
}
