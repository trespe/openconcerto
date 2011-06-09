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
 
 /*
 * Field created on 4 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.utils.CompareUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

/**
 * The type of a SQL field. Allow one to convert a Java object to its sql serialization.
 * 
 * @see #toString(Object)
 * @see #check(Object)
 * @author Sylvain
 */
public abstract class SQLType {

    private static Class getClass(int type, final int size) {
        switch (type) {
        case Types.BIT:
            // As of MySQL 5.0.3, BIT is for storing bit-field values
            // As of Connector/J 3.1.9, transformedBitIsBoolean can be used
            // MAYBE remove Boolean after testing it works against 4.1 servers
            if (size == 1) {
                return Boolean.class;
            } else if (size <= Integer.SIZE) {
                return Integer.class;
            } else if (size <= Long.SIZE) {
                return Long.class;
            } else
                return BigInteger.class;
        case Types.BOOLEAN:
            return Boolean.class;
        case Types.DOUBLE:
            return Double.class;
        case Types.FLOAT:
        case Types.REAL:
            return Float.class;
        case Types.TIMESTAMP:
            return Timestamp.class;
        case Types.DATE:
            return java.util.Date.class;
        case Types.INTEGER:
        case Types.SMALLINT:
        case Types.TINYINT:
            return Integer.class;
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.BLOB:
            return Blob.class;
        case Types.CLOB:
            return Clob.class;
        case Types.BIGINT:
            // 8 bytes
            return Long.class;
        case Types.DECIMAL:
        case Types.NUMERIC:
            return BigDecimal.class;
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
            return String.class;
        default:
            // eg view columns are OTHER
            return Object.class;
        }
    }

    static private final Map<List<String>, SQLType> instances = new HashMap<List<String>, SQLType>();

    public static SQLType get(final int type, final int size) {
        return get(null, type, size, null, null);
    }

    /**
     * Get the corresponding type.
     * 
     * @param base the base of this type, can be <code>null</code> but {@link #toString(Object)}
     *        will have to use standard SQL which might not be valid for all bases (eg escapes).
     * @param type a value from java.sql.Types.
     * @param size the size as COLUMN_SIZE is defined in
     *        {@link java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * @param decDigits the number of fractional digits, can be <code>null</code>.
     * @param typeName data source dependent type name.
     * @return the corresponding instance.
     * @throws IllegalStateException if type is unknown.
     */
    public static SQLType get(final SQLBase base, final int type, final int size, Integer decDigits, final String typeName) {
        final List<String> typeID = Arrays.asList(base == null ? null : base.getURL(), type + "", size + "", String.valueOf(decDigits), typeName);
        SQLType res = instances.get(typeID);
        if (res == null) {
            final Class clazz = getClass(type, size);
            if (Boolean.class.isAssignableFrom(clazz))
                res = new BooleanType(type, size, typeName, clazz);
            else if (Number.class.isAssignableFrom(clazz))
                res = new NumberType(type, size, decDigits, typeName, clazz);
            else if (Timestamp.class.isAssignableFrom(clazz))
                res = new TimestampType(type, size, decDigits, typeName, clazz);
            // Date en dernier surclasse des autres
            else if (java.util.Date.class.isAssignableFrom(clazz))
                res = new DateType(type, size, decDigits, typeName, clazz);
            else if (String.class.isAssignableFrom(clazz))
                res = new StringType(type, size, typeName, clazz);
            else
                // BLOB & CLOB and the rest
                res = new UnknownType(type, size, typeName, clazz);
            res.setBase(base);
            instances.put(typeID, res);
        }
        return res;
    }

    public static SQLType get(final SQLBase base, Element typeElement) {
        int type = Integer.valueOf(typeElement.getAttributeValue("type")).intValue();
        int size = Integer.valueOf(typeElement.getAttributeValue("size")).intValue();
        String typeName = typeElement.getAttributeValue("typeName");
        final String decDigitsS = typeElement.getAttributeValue("decimalDigits");
        final Integer decDigits = decDigitsS == null ? null : Integer.valueOf(decDigitsS);
        return get(base, type, size, decDigits, typeName);
    }

    // *** instance

    // a value from java.sql.Types
    private final int type;
    // COLUMN_SIZE
    private final int size;
    // DECIMAL_DIGITS
    private final Integer decimalDigits;
    // TYPE_NAME
    private final String typeName;
    // the class this type accepts
    private final Class javaType;

    private SQLBase base;
    private SQLSyntax syntax;

    private String xml;

    private SQLType(int type, int size, Integer decDigits, String typeName, Class javaType) {
        this.type = type;
        this.size = size;
        this.decimalDigits = decDigits;
        this.typeName = typeName;
        this.javaType = javaType;
        this.xml = null;
    }

    /**
     * The SQL type.
     * 
     * @return a value from java.sql.Types.
     */
    public int getType() {
        return this.type;
    }

    private final boolean isNumeric() {
        return this.getType() == Types.DECIMAL || this.getType() == Types.NUMERIC;
    }

    public final int getSize() {
        return this.size;
    }

    public final Integer getDecimalDigits() {
        return this.decimalDigits;
    }

    public Class<?> getJavaType() {
        return this.javaType;
    }

    /**
     * Data source dependent type name.
     * 
     * @return the data source dependent type name.
     */
    public final String getTypeName() {
        return this.typeName;
    }

    private final void setBase(SQLBase base) {
        // set only once
        assert this.base == null;
        if (base != null) {
            this.base = base;
            this.syntax = this.base.getServer().getSQLSystem().getSyntax();
        }
    }

    public final SQLBase getBase() {
        return this.base;
    }

    public final SQLSyntax getSyntax() {
        return this.syntax;
    }

    @Override
    public boolean equals(Object obj) {
        return equals(obj, null);
    }

    public boolean equals(Object obj, SQLSystem otherSystem) {
        if (obj instanceof SQLType) {
            final SQLType o = (SQLType) obj;
            final boolean javaTypeOK = this.getJavaType().equals(o.getJavaType());
            if (!javaTypeOK) {
                return false;
            } else if (this.getJavaType() == Boolean.class) {
                // can take many forms (e.g. BIT(1) or BOOLEAN) but type and size are meaningless
                return true;
            }

            // for all intents and purposes NUMERIC == DECIMAL
            final boolean typeOK = this.isNumeric() ? o.isNumeric() : this.getType() == o.getType();
            if (!typeOK)
                return false;

            final boolean sizeOK;
            // date has no precision so size is meaningless
            // Floating-Point Types have no precision (apart from single or double precision, but
            // this is handled by typeOK)
            if (this.getType() == Types.DATE || this.getJavaType() == Float.class || this.getJavaType() == Double.class) {
                sizeOK = true;
            } else {
                if (otherSystem == null && o.getSyntax() != null)
                    otherSystem = o.getSyntax().getSystem();
                final SQLSystem thisSystem = this.getSyntax() == null ? null : this.getSyntax().getSystem();
                final boolean isTime = this.getType() == Types.TIME || this.getType() == Types.TIMESTAMP;
                final boolean decDigitsOK;
                // only TIME and NUMERIC use DECIMAL_DIGITS, others like integer use only size
                if (!this.isNumeric() && !isTime) {
                    decDigitsOK = true;
                } else if (this.isNumeric() ||
                // isTime() : if we don't know the system, play it safe and compare
                        thisSystem == null || otherSystem == null || thisSystem.isFractionalSecondsSupported() && otherSystem.isFractionalSecondsSupported()) {
                    decDigitsOK = CompareUtils.equals(this.getDecimalDigits(), o.getDecimalDigits());
                } else {
                    decDigitsOK = true;
                }
                // not all systems return the same size for TIME but only DECIMAL DIGITS matters
                sizeOK = decDigitsOK && (isTime || this.getSize() == o.getSize());
            }
            return sizeOK;
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.getType() + this.getSize() + this.getJavaType().hashCode();
    }

    public final String toString() {
        return "SQLType #" + this.getType() + "(" + this.getSize() + "," + this.getDecimalDigits() + "): " + this.getJavaType();
    }

    public final String toXML() {
        // this class is immutable and its instances shared so cache its XML
        if (this.xml == null) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("<type type=\"");
            sb.append(this.type);
            sb.append("\" size=\"");
            sb.append(this.size);
            if (this.decimalDigits != null) {
                sb.append("\" decimalDigits=\"");
                sb.append(this.decimalDigits);
            }
            sb.append("\" typeName=\"");
            sb.append(this.typeName);
            sb.append("\"/>");
            this.xml = sb.toString();
        }
        return this.xml;
    }

    /**
     * Serialize an object to its sql string.
     * 
     * @param o an instance of getJavaType(), eg "it's".
     * @return the sql representation, eg "'it''s'".
     * @throws IllegalArgumentException if o is not valid.
     * @see #isValid(Object)
     */
    public final String toString(Object o) {
        this.check(o);
        if (o == null)
            return "NULL";
        else
            return this.toStringRaw(o);
    }

    public final Object fromString(String s) {
        if (s == null)
            return null;
        else
            return this.fromNonNullString(s);
    }

    protected Object fromNonNullString(String s) {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Check if o is valid, do nothing if it is else throw an exception.
     * 
     * @param o the object to check.
     * @throws IllegalArgumentException if o is not valid.
     */
    public final void check(Object o) {
        if (!isValid(o))
            throw new IllegalArgumentException(o + " is not valid for " + this);
    }

    /**
     * Test whether o is valid for this type. Ie does o is an instance of getJavaType().
     * 
     * @param o the object to test.
     * @return <code>true</code> if o can be passed to {@link #toString(Object)}.
     * @see #check(Object)
     */
    public boolean isValid(Object o) {
        return o == null || this.getJavaType().isInstance(o);
    }

    abstract protected String toStringRaw(Object o);

    // ** static subclasses

    private static final class UnknownType extends SQLType {
        public UnknownType(int type, int size, String typeName, Class clazz) {
            super(type, size, null, typeName, clazz);
        }

        protected String toStringRaw(Object o) {
            throw new IllegalStateException("not implemented");
        }
    }

    private abstract static class ToStringType extends SQLType {
        public ToStringType(int type, int size, String typeName, Class clazz) {
            this(type, size, null, typeName, clazz);
        }

        public ToStringType(int type, int size, Integer decDigits, String typeName, Class clazz) {
            super(type, size, decDigits, typeName, clazz);
        }

        protected String toStringRaw(Object o) {
            return o.toString();
        }
    }

    private static class BooleanType extends ToStringType {
        public BooleanType(int type, int size, String typeName, Class clazz) {
            super(type, size, typeName, clazz);
        }

        @Override
        public Object fromNonNullString(String s) {
            return Boolean.valueOf(s);
        }

        @Override
        protected String toStringRaw(Object o) {
            if (this.getSyntax().getSystem() == SQLSystem.MSSQL) {
                // 'true'
                return this.getBase().quoteString(o.toString());
            } else
                return super.toStringRaw(o);
        }
    }

    private static class NumberType extends ToStringType {
        public NumberType(int type, int size, Integer decDigits, String typeName, Class clazz) {
            super(type, size, decDigits, typeName, clazz);
        }

        @Override
        public boolean isValid(Object o) {
            return super.isValid(o) || o instanceof Number;
        }

        @Override
        protected Object fromNonNullString(String s) {
            s = s.trim();
            if (s.length() == 0)
                return null;
            else if (this.getJavaType().equals(Integer.class))
                return Integer.valueOf(s);
            else
                return Double.valueOf(s);
        }
    }

    private static abstract class DateOrTimeType extends SQLType {
        public DateOrTimeType(int type, int size, Integer decDigits, String typeName, Class clazz) {
            super(type, size, decDigits, typeName, clazz);
        }

        @Override
        public final boolean isValid(Object o) {
            return super.isValid(o) || o instanceof java.util.Date || o instanceof java.util.Calendar || o instanceof Number;
        }

        protected long getTime(Object o) {
            if (o instanceof java.util.Date)
                return ((java.util.Date) o).getTime();
            else if (o instanceof java.util.Calendar)
                return ((java.util.Calendar) o).getTimeInMillis();
            else
                return ((Number) o).longValue();
        }
    }

    private static class DateType extends DateOrTimeType {
        public DateType(int type, int size, Integer decDigits, String typeName, Class clazz) {
            super(type, size, decDigits, typeName, clazz);
        }

        protected String toStringRaw(Object o) {
            return "'" + new Date(getTime(o)).toString() + "'";
        }
    }

    private static class TimestampType extends DateOrTimeType {
        public TimestampType(int type, int size, Integer decDigits, String typeName, Class clazz) {
            super(type, size, decDigits, typeName, clazz);
        }

        protected String toStringRaw(Object o) {
            final Timestamp ts;
            if (o instanceof Timestamp)
                ts = (Timestamp) o;
            else
                ts = new Timestamp(getTime(o));
            return "'" + ts.toString() + "'";
        }
    }

    private static class StringType extends SQLType {
        public StringType(int type, int size, String typeName, Class clazz) {
            super(type, size, null, typeName, clazz);
        }

        protected String toStringRaw(Object o) {
            return SQLBase.quoteString(this.getBase(), (String) o);
        }

        @Override
        protected Object fromNonNullString(String s) {
            return s;
        }
    }

}
