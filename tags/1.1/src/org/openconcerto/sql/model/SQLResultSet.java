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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;

/**
 * A resultSet that wraps onto another one, caching name to index translation, and using a
 * ResultSetFullnameHelper.
 * 
 * @author Sylvain
 */
public class SQLResultSet implements ResultSet {
    private final ResultSet delegate;
    private final ResultSetFullnameHelper helper;
    private final Map indexes;

    public SQLResultSet(ResultSet delegate) {
        this.delegate = delegate;
        this.helper = new ResultSetFullnameHelper(this);
        this.indexes = LazyMap.decorate(new HashMap(), new Transformer() {
            public Object transform(Object input) {
                final String colName = (String) input;
                try {
                    return new Integer(doFindColumn(colName));
                } catch (SQLException e) {
                    return e;
                }
            }
        });
    }

    private ResultSet getDelegate() {
        return this.delegate;
    }

    public boolean absolute(int row) throws SQLException {
        return getDelegate().absolute(row);
    }

    public void afterLast() throws SQLException {
        getDelegate().afterLast();
    }

    public void beforeFirst() throws SQLException {
        getDelegate().beforeFirst();
    }

    public void cancelRowUpdates() throws SQLException {
        getDelegate().cancelRowUpdates();
    }

    public void clearWarnings() throws SQLException {
        getDelegate().clearWarnings();
    }

    public void close() throws SQLException {
        getDelegate().close();
    }

    public void deleteRow() throws SQLException {
        getDelegate().deleteRow();
    }

    public int findColumn(String columnName) throws SQLException {
        final Object res = this.indexes.get(columnName);
        if (res instanceof SQLException)
            throw (SQLException) res;
        else {
            final int index = ((Number) res).intValue();
            if (index < 1)
                throw new SQLException(columnName + " not found");
            else
                return index;
        }
    }

    private int doFindColumn(String columnName) throws SQLException {
        try {
            return getDelegate().findColumn(columnName);
        } catch (SQLException e) {
            if (SQLField.isFullname(columnName))
                return this.helper.getIndex(columnName);
            else
                throw e;
        }
    }

    public boolean first() throws SQLException {
        return getDelegate().first();
    }

    public Array getArray(int i) throws SQLException {
        return getDelegate().getArray(i);
    }

    public Array getArray(String colName) throws SQLException {
        return getDelegate().getArray(colName);
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return getDelegate().getAsciiStream(columnIndex);
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
        return getDelegate().getAsciiStream(this.findColumn(columnName));
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getDelegate().getBigDecimal(columnIndex, scale);
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return getDelegate().getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        return getDelegate().getBigDecimal(columnName, scale);
    }

    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        return getDelegate().getBigDecimal(this.findColumn(columnName));
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return getDelegate().getBinaryStream(columnIndex);
    }

    public InputStream getBinaryStream(String columnName) throws SQLException {
        return getDelegate().getBinaryStream(this.findColumn(columnName));
    }

    public Blob getBlob(int i) throws SQLException {
        return getDelegate().getBlob(i);
    }

    public Blob getBlob(String colName) throws SQLException {
        return getDelegate().getBlob(colName);
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        return getDelegate().getBoolean(columnIndex);
    }

    public boolean getBoolean(String columnName) throws SQLException {
        return getDelegate().getBoolean(this.findColumn(columnName));
    }

    public byte getByte(int columnIndex) throws SQLException {
        return getDelegate().getByte(columnIndex);
    }

    public byte getByte(String columnName) throws SQLException {
        return getDelegate().getByte(this.findColumn(columnName));
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        return getDelegate().getBytes(columnIndex);
    }

    public byte[] getBytes(String columnName) throws SQLException {
        return getDelegate().getBytes(this.findColumn(columnName));
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return getDelegate().getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
        return getDelegate().getCharacterStream(this.findColumn(columnName));
    }

    public Clob getClob(int i) throws SQLException {
        return getDelegate().getClob(i);
    }

    public Clob getClob(String colName) throws SQLException {
        return getDelegate().getClob(colName);
    }

    public int getConcurrency() throws SQLException {
        return getDelegate().getConcurrency();
    }

    public String getCursorName() throws SQLException {
        return getDelegate().getCursorName();
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDelegate().getDate(columnIndex, cal);
    }

    public Date getDate(int columnIndex) throws SQLException {
        return getDelegate().getDate(columnIndex);
    }

    public Date getDate(String columnName, Calendar cal) throws SQLException {
        return getDelegate().getDate(columnName, cal);
    }

    public Date getDate(String columnName) throws SQLException {
        return getDelegate().getDate(this.findColumn(columnName));
    }

    public double getDouble(int columnIndex) throws SQLException {
        return getDelegate().getDouble(columnIndex);
    }

    public double getDouble(String columnName) throws SQLException {
        return getDelegate().getDouble(this.findColumn(columnName));
    }

    public int getFetchDirection() throws SQLException {
        return getDelegate().getFetchDirection();
    }

    public int getFetchSize() throws SQLException {
        return getDelegate().getFetchSize();
    }

    public float getFloat(int columnIndex) throws SQLException {
        return getDelegate().getFloat(columnIndex);
    }

    public float getFloat(String columnName) throws SQLException {
        return getDelegate().getFloat(this.findColumn(columnName));
    }

    public int getInt(int columnIndex) throws SQLException {
        return getDelegate().getInt(columnIndex);
    }

    public int getInt(String columnName) throws SQLException {
        return getDelegate().getInt(this.findColumn(columnName));
    }

    public long getLong(int columnIndex) throws SQLException {
        return getDelegate().getLong(columnIndex);
    }

    public long getLong(String columnName) throws SQLException {
        return getDelegate().getLong(this.findColumn(columnName));
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return new SQLResultSetMetadata(getDelegate().getMetaData());
    }

    public Object getObject(int arg0, Map arg1) throws SQLException {
        return getDelegate().getObject(arg0, arg1);
    }

    public Object getObject(int columnIndex) throws SQLException {
        return getDelegate().getObject(columnIndex);
    }

    public Object getObject(String arg0, Map arg1) throws SQLException {
        return getDelegate().getObject(arg0, arg1);
    }

    public Object getObject(String columnName) throws SQLException {
        return getDelegate().getObject(this.findColumn(columnName));
    }

    public Ref getRef(int i) throws SQLException {
        return getDelegate().getRef(i);
    }

    public Ref getRef(String colName) throws SQLException {
        return getDelegate().getRef(colName);
    }

    public int getRow() throws SQLException {
        return getDelegate().getRow();
    }

    public short getShort(int columnIndex) throws SQLException {
        return getDelegate().getShort(columnIndex);
    }

    public short getShort(String columnName) throws SQLException {
        return getDelegate().getShort(this.findColumn(columnName));
    }

    public Statement getStatement() throws SQLException {
        return getDelegate().getStatement();
    }

    public String getString(int columnIndex) throws SQLException {
        return getDelegate().getString(columnIndex);
    }

    public String getString(String columnName) throws SQLException {
        return getDelegate().getString(this.findColumn(columnName));
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getDelegate().getTime(columnIndex, cal);
    }

    public Time getTime(int columnIndex) throws SQLException {
        return getDelegate().getTime(columnIndex);
    }

    public Time getTime(String columnName, Calendar cal) throws SQLException {
        return getDelegate().getTime(columnName, cal);
    }

    public Time getTime(String columnName) throws SQLException {
        return getDelegate().getTime(this.findColumn(columnName));
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getDelegate().getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return getDelegate().getTimestamp(columnIndex);
    }

    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
        return getDelegate().getTimestamp(columnName, cal);
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {
        return getDelegate().getTimestamp(this.findColumn(columnName));
    }

    public int getType() throws SQLException {
        return getDelegate().getType();
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return getDelegate().getUnicodeStream(columnIndex);
    }

    public InputStream getUnicodeStream(String columnName) throws SQLException {
        return getDelegate().getUnicodeStream(this.findColumn(columnName));
    }

    public URL getURL(int columnIndex) throws SQLException {
        return getDelegate().getURL(columnIndex);
    }

    public URL getURL(String columnName) throws SQLException {
        return getDelegate().getURL(this.findColumn(columnName));
    }

    // **

    public SQLWarning getWarnings() throws SQLException {
        return getDelegate().getWarnings();
    }

    public void insertRow() throws SQLException {
        getDelegate().insertRow();
    }

    public boolean isAfterLast() throws SQLException {
        return getDelegate().isAfterLast();
    }

    public boolean isBeforeFirst() throws SQLException {
        return getDelegate().isBeforeFirst();
    }

    public boolean isFirst() throws SQLException {
        return getDelegate().isFirst();
    }

    public boolean isLast() throws SQLException {
        return getDelegate().isLast();
    }

    public boolean last() throws SQLException {
        return getDelegate().last();
    }

    public void moveToCurrentRow() throws SQLException {
        getDelegate().moveToCurrentRow();
    }

    public void moveToInsertRow() throws SQLException {
        getDelegate().moveToInsertRow();
    }

    public boolean next() throws SQLException {
        return getDelegate().next();
    }

    public boolean previous() throws SQLException {
        return getDelegate().previous();
    }

    public void refreshRow() throws SQLException {
        getDelegate().refreshRow();
    }

    public boolean relative(int rows) throws SQLException {
        return getDelegate().relative(rows);
    }

    public boolean rowDeleted() throws SQLException {
        return getDelegate().rowDeleted();
    }

    public boolean rowInserted() throws SQLException {
        return getDelegate().rowInserted();
    }

    public boolean rowUpdated() throws SQLException {
        return getDelegate().rowUpdated();
    }

    public void setFetchDirection(int direction) throws SQLException {
        getDelegate().setFetchDirection(direction);
    }

    public void setFetchSize(int rows) throws SQLException {
        getDelegate().setFetchSize(rows);
    }

    // update*

    public void updateArray(int columnIndex, Array x) throws SQLException {
        getDelegate().updateArray(columnIndex, x);
    }

    public void updateArray(String columnName, Array x) throws SQLException {
        getDelegate().updateArray(columnName, x);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        getDelegate().updateAsciiStream(columnIndex, x, length);
    }

    public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
        getDelegate().updateAsciiStream(columnName, x, length);
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        getDelegate().updateBigDecimal(columnIndex, x);
    }

    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
        getDelegate().updateBigDecimal(columnName, x);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        getDelegate().updateBinaryStream(columnIndex, x, length);
    }

    public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
        getDelegate().updateBinaryStream(columnName, x, length);
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        getDelegate().updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
        getDelegate().updateBlob(columnName, x);
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        getDelegate().updateBoolean(columnIndex, x);
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException {
        getDelegate().updateBoolean(columnName, x);
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        getDelegate().updateByte(columnIndex, x);
    }

    public void updateByte(String columnName, byte x) throws SQLException {
        getDelegate().updateByte(columnName, x);
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        getDelegate().updateBytes(columnIndex, x);
    }

    public void updateBytes(String columnName, byte[] x) throws SQLException {
        getDelegate().updateBytes(columnName, x);
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        getDelegate().updateCharacterStream(columnIndex, x, length);
    }

    public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
        getDelegate().updateCharacterStream(columnName, reader, length);
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        getDelegate().updateClob(columnIndex, x);
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
        getDelegate().updateClob(columnName, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        getDelegate().updateDate(columnIndex, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        getDelegate().updateDate(columnName, x);
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        getDelegate().updateDouble(columnIndex, x);
    }

    public void updateDouble(String columnName, double x) throws SQLException {
        getDelegate().updateDouble(columnName, x);
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        getDelegate().updateFloat(columnIndex, x);
    }

    public void updateFloat(String columnName, float x) throws SQLException {
        getDelegate().updateFloat(columnName, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        getDelegate().updateInt(columnIndex, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        getDelegate().updateInt(columnName, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        getDelegate().updateLong(columnIndex, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        getDelegate().updateLong(columnName, x);
    }

    public void updateNull(int columnIndex) throws SQLException {
        getDelegate().updateNull(columnIndex);
    }

    public void updateNull(String columnName) throws SQLException {
        getDelegate().updateNull(columnName);
    }

    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
        getDelegate().updateObject(columnIndex, x, scale);
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        getDelegate().updateObject(columnIndex, x);
    }

    public void updateObject(String columnName, Object x, int scale) throws SQLException {
        getDelegate().updateObject(columnName, x, scale);
    }

    public void updateObject(String columnName, Object x) throws SQLException {
        getDelegate().updateObject(columnName, x);
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        getDelegate().updateRef(columnIndex, x);
    }

    public void updateRef(String columnName, Ref x) throws SQLException {
        getDelegate().updateRef(columnName, x);
    }

    public void updateRow() throws SQLException {
        getDelegate().updateRow();
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        getDelegate().updateShort(columnIndex, x);
    }

    public void updateShort(String columnName, short x) throws SQLException {
        getDelegate().updateShort(columnName, x);
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        getDelegate().updateString(columnIndex, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        getDelegate().updateString(columnName, x);
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        getDelegate().updateTime(columnIndex, x);
    }

    public void updateTime(String columnName, Time x) throws SQLException {
        getDelegate().updateTime(columnName, x);
    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        getDelegate().updateTimestamp(columnIndex, x);
    }

    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
        getDelegate().updateTimestamp(columnName, x);
    }

    public boolean wasNull() throws SQLException {
        return getDelegate().wasNull();
    }

    public int getHoldability() throws SQLException {
        return getDelegate().getHoldability();
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return getDelegate().getNCharacterStream(columnIndex);
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return getDelegate().getNCharacterStream(columnLabel);
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        return getDelegate().getNClob(columnIndex);
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        return getDelegate().getNClob(columnLabel);
    }

    public String getNString(int columnIndex) throws SQLException {
        return getDelegate().getNString(columnIndex);
    }

    public String getNString(String columnLabel) throws SQLException {
        return getDelegate().getNString(columnLabel);
    }

    public RowId getRowId(int columnIndex) throws SQLException {
        return getDelegate().getRowId(columnIndex);
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        return getDelegate().getRowId(columnLabel);
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return getDelegate().getSQLXML(columnIndex);
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return getDelegate().getSQLXML(columnLabel);
    }

    public boolean isClosed() throws SQLException {
        return getDelegate().isClosed();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getDelegate().isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getDelegate().unwrap(iface);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        getDelegate().updateAsciiStream(columnIndex, x, length);
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        getDelegate().updateAsciiStream(columnIndex, x);
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        getDelegate().updateAsciiStream(columnLabel, x, length);
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        getDelegate().updateAsciiStream(columnLabel, x);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        getDelegate().updateBinaryStream(columnIndex, x, length);
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        getDelegate().updateBinaryStream(columnIndex, x);
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        getDelegate().updateBinaryStream(columnLabel, x, length);
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        getDelegate().updateBinaryStream(columnLabel, x);
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        getDelegate().updateBlob(columnIndex, inputStream, length);
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        getDelegate().updateBlob(columnIndex, inputStream);
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        getDelegate().updateBlob(columnLabel, inputStream, length);
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        getDelegate().updateBlob(columnLabel, inputStream);
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        getDelegate().updateCharacterStream(columnIndex, x, length);
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        getDelegate().updateCharacterStream(columnIndex, x);
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        getDelegate().updateCharacterStream(columnLabel, reader, length);
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        getDelegate().updateCharacterStream(columnLabel, reader);
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        getDelegate().updateClob(columnIndex, reader, length);
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        getDelegate().updateClob(columnIndex, reader);
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        getDelegate().updateClob(columnLabel, reader, length);
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        getDelegate().updateClob(columnLabel, reader);
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        getDelegate().updateNCharacterStream(columnIndex, x, length);
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        getDelegate().updateNCharacterStream(columnIndex, x);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        getDelegate().updateNCharacterStream(columnLabel, reader, length);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        getDelegate().updateNCharacterStream(columnLabel, reader);
    }

    public void updateNClob(int columnIndex, NClob clob) throws SQLException {
        getDelegate().updateNClob(columnIndex, clob);
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        getDelegate().updateNClob(columnIndex, reader, length);
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        getDelegate().updateNClob(columnIndex, reader);
    }

    public void updateNClob(String columnLabel, NClob clob) throws SQLException {
        getDelegate().updateNClob(columnLabel, clob);
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        getDelegate().updateNClob(columnLabel, reader, length);
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        getDelegate().updateNClob(columnLabel, reader);
    }

    public void updateNString(int columnIndex, String string) throws SQLException {
        getDelegate().updateNString(columnIndex, string);
    }

    public void updateNString(String columnLabel, String string) throws SQLException {
        getDelegate().updateNString(columnLabel, string);
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        getDelegate().updateRowId(columnIndex, x);
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        getDelegate().updateRowId(columnLabel, x);
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        getDelegate().updateSQLXML(columnIndex, xmlObject);
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        getDelegate().updateSQLXML(columnLabel, xmlObject);
    }
}
