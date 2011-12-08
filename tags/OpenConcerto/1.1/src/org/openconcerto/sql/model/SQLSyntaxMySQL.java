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

import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.OutsideClause;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.dbcp.DelegatingConnection;

/**
 * MySQL can enable compression with the "useCompression" connection property. Compression status
 * can be checked with "show global status like 'Compression';".
 * 
 * @author Sylvain CUAZ
 */
class SQLSyntaxMySQL extends SQLSyntax {

    SQLSyntaxMySQL() {
        super(SQLSystem.MYSQL);
        this.typeNames.putAll(Boolean.class, "boolean", "bool", "bit");
        this.typeNames.putAll(Integer.class, "integer", "int");
        this.typeNames.putAll(Long.class, "bigint");
        this.typeNames.putAll(BigInteger.class, "bigint");
        this.typeNames.putAll(BigDecimal.class, "decimal", "numeric");
        this.typeNames.putAll(Float.class, "float");
        this.typeNames.putAll(Double.class, "double precision", "real");
        this.typeNames.putAll(Timestamp.class, "timestamp");
        this.typeNames.putAll(java.util.Date.class, "time");
        this.typeNames.putAll(Blob.class, "blob", "tinyblob", "mediumblob", "longblob", "varbinary", "binary");
        this.typeNames.putAll(Clob.class, "text", "tinytext", "mediumtext", "longtext", "varchar", "char");
        this.typeNames.putAll(String.class, "varchar", "char");
    }

    public String getIDType() {
        return " int";
    }

    @Override
    public boolean isAuto(SQLField f) {
        return "YES".equals(f.getMetadata("IS_AUTOINCREMENT"));
    }

    @Override
    public String getAuto() {
        return this.getIDType() + " AUTO_INCREMENT NOT NULL";
    }

    @Override
    public String getDateAndTimeType() {
        return "datetime";
    }

    @Override
    protected String getAutoDateType(SQLField f) {
        return "timestamp";
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return null;
    }

    @Override
    protected boolean supportsDefault(String typeName) {
        return !typeName.contains("text") && !typeName.contains("blob");
    }

    @Override
    public String transfDefaultJDBC2SQL(SQLField f) {
        final Class<?> javaType = f.getType().getJavaType();
        String res = (String) f.getDefaultValue();
        if (res == null)
            // either no default or NULL default
            // see http://dev.mysql.com/doc/refman/5.0/en/data-type-defaults.html
            // (works the same way for 5.1 and 6.0)
            if (Boolean.FALSE.equals(f.isNullable()))
                res = null;
            else {
                res = "NULL";
            }
        else if (javaType == String.class)
            // this will be given to other db system, so don't use base specific quoting
            res = SQLBase.quoteStringStd(res);
        // MySQL 5.0.24a puts empty strings when not specifying default
        else if (res.length() == 0)
            res = null;
        // quote neither functions nor CURRENT_TIMESTAMP
        else if (Date.class.isAssignableFrom(javaType) && !res.trim().endsWith("()") && !res.toLowerCase().contains("timestamp"))
            res = SQLBase.quoteStringStd(res);
        else if (javaType == Boolean.class)
            res = res.equals("0") ? "FALSE" : "TRUE";
        return res;
    }

    @Override
    public String getCreateTableSuffix() {
        return " ENGINE = InnoDB ";
    }

    @Override
    public String disableFKChecks(DBRoot b) {
        return "SET FOREIGN_KEY_CHECKS=0;";
    }

    @Override
    public String enableFKChecks(DBRoot b) {
        return "SET FOREIGN_KEY_CHECKS=1;";
    }

    @Override
    public String getDropFK() {
        return "DROP FOREIGN KEY ";
    }

    @Override
    public String getDropConstraint() {
        // in MySQL there's only 2 types of constraints : foreign keys and unique
        // fk are handled by getDropFK(), so this is just for unique
        // in MySQL UNIQUE constraint and index are one and the same thing
        return "DROP INDEX ";
    }

    @Override
    public Map<String, Object> normalizeIndexInfo(final Map m) {
        final Map<String, Object> res = copyIndexInfoMap(m);
        final Object nonUnique = res.get("NON_UNIQUE");
        // some newer versions of MySQL now return Boolean
        res.put("NON_UNIQUE", nonUnique instanceof Boolean ? nonUnique : Boolean.valueOf((String) nonUnique));
        res.put("COLUMN_NAME", res.get("COLUMN_NAME"));
        return res;
    }

    @Override
    public String getDropIndex(String name, SQLName tableName) {
        return "DROP INDEX " + SQLBase.quoteIdentifier(name) + " on " + tableName.quote() + ";";
    }

    @Override
    protected String getCreateIndex(String cols, SQLName tableName, Index i) {
        final String method = i.getMethod() != null ? " USING " + i.getMethod() : "";
        return super.getCreateIndex(cols, tableName, i) + method;
    }

    @Override
    public List<String> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final boolean newNullable = toAlter.contains(Properties.NULLABLE) ? nullable : getNullable(f);
        final String newType = toAlter.contains(Properties.TYPE) ? type : getType(f);
        final String newDef = toAlter.contains(Properties.DEFAULT) ? defaultVal : getDefault(f, newType);

        return Collections.singletonList(SQLSelect.quote("MODIFY COLUMN %n " + newType + getNullableClause(newNullable) + getDefaultClause(newDef), f));
    }

    @Override
    public String getDropRoot(String name) {
        return SQLSelect.quote("DROP DATABASE IF EXISTS %i ;", name);
    }

    @Override
    public String getCreateRoot(String name) {
        return SQLSelect.quote("CREATE DATABASE %i ;", name);
    }

    @Override
    protected void _storeData(final SQLTable t, final File file) {
        checkServerLocalhost(t);
        final CollectionMap<String, String> charsets = new CollectionMap<String, String>();
        for (final SQLField f : t.getFields()) {
            final Object charset = f.getInfoSchema().get("CHARACTER_SET_NAME");
            // non string field
            if (charset != null)
                charsets.put(charset, f.getName());
        }
        if (charsets.size() > 1)
            // MySQL dumps strings in binary, so fields must be consistent otherwise the
            // file is invalid
            throw new IllegalArgumentException(t + " has more than on character set : " + charsets);
        // if no string cols there should only be values within ASCII (eg dates, ints, etc)
        final String charset = charsets.size() == 0 ? "UTF8" : charsets.keySet().iterator().next();
        final String cols = CollectionUtils.join(t.getOrderedFields(), ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return SQLBase.quoteStringStd(input.getName());
            }
        });
        try {
            final File tmp = File.createTempFile(SQLSyntaxMySQL.class.getSimpleName() + "storeData", ".txt");
            // mysql cannot overwrite files
            tmp.delete();
            final SQLSelect sel = new SQLSelect(t.getBase(), true).addSelectStar(t);
            // store the data in the temp file
            t.getBase().getDataSource().execute(t.getBase().quote("SELECT " + cols + " UNION " + sel.asString() + " INTO OUTFILE %s " + getDATA_OPTIONS(t) + ";", tmp.getAbsolutePath()));
            // then read it to remove superfluous escape char and convert to utf8
            final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(tmp), charset));
            final Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            int count;
            final char[] buf = new char[1000 * 1024];
            int offset = 0;
            final char[] wbuf = new char[buf.length];
            boolean wasBackslash = false;
            while ((count = r.read(buf, offset, buf.length - offset)) != -1) {
                int wbufLength = 0;
                for (int i = 0; i < count; i++) {
                    final char c = buf[i];
                    // MySQL escapes the field delimiter (which other systems do as well)
                    // but also "LINES TERMINATED BY" which others don't understand
                    if (wasBackslash && c == '\n')
                        // overwrite the backslash
                        wbuf[wbufLength - 1] = c;
                    else
                        wbuf[wbufLength++] = c;
                    wasBackslash = c == '\\';
                }
                // the read buffer ends with a backslash
                if (wasBackslash) {
                    // restore state one char before
                    wbufLength--;
                    wasBackslash = wbuf[wbufLength - 1] == '\\';
                    buf[0] = '\\';
                    offset = 1;
                } else
                    offset = 0;
                w.write(wbuf, 0, wbufLength);
            }
            r.close();
            w.close();
            tmp.delete();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getDATA_OPTIONS(DBStructureItem<?> i) {
        return i.getAnc(SQLBase.class).quote("FIELDS TERMINATED BY ',' ENCLOSED BY '\"' ESCAPED BY %s LINES TERMINATED BY '\n' ", "\\");
    }

    @Override
    public void _loadData(final File f, final SQLTable t) {
        // we always store in utf8 regardless of the encoding of the columns
        final SQLDataSource ds = t.getDBSystemRoot().getDataSource();
        try {
            SQLUtils.executeAtomic(ds, new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    final String charsetClause;
                    final Connection conn = ((DelegatingConnection) ds.getConnection()).getInnermostDelegate();
                    if (((com.mysql.jdbc.Connection) conn).versionMeetsMinimum(5, 0, 38)) {
                        charsetClause = "CHARACTER SET utf8 ";
                    } else {
                        // variable name is in the first column
                        final String dbCharset = ds.executeA1("show variables like 'character_set_database'")[1].toString().trim().toLowerCase();
                        if (dbCharset.equals("utf8")) {
                            charsetClause = "";
                        } else {
                            throw new IllegalStateException("the database charset is not utf8 and this version doesn't support specifying another one : " + dbCharset);
                        }
                    }
                    ds.execute(t.getBase().quote("LOAD DATA LOCAL INFILE %s INTO TABLE %f " + charsetClause + getDATA_OPTIONS(t) + " IGNORE 1 LINES;", f.getAbsolutePath(), t));
                    return null;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't load " + f + " into " + t, e);
        }
    }

    @Override
    public SQLBase createBase(SQLServer server, String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        return new MySQLBase(server, name, login, pass, dsInit);
    }

    @Override
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        final String nullSafe = x + " <=> " + y;
        if (eq)
            return nullSafe;
        else
            return "NOT (" + nullSafe + ")";
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        // MySQL puts the db name in schema
        return "SELECT null as \"schema\", ROUTINE_NAME as \"name\", ROUTINE_DEFINITION as \"src\" FROM \"information_schema\".ROUTINES where ROUTINE_CATALOG is null and ROUTINE_SCHEMA = '"
                + b.getMDName() + "'";
    }

    @Override
    public String getTriggerQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        return "SELECT \"TRIGGER_NAME\", null as \"TABLE_SCHEMA\", EVENT_OBJECT_TABLE as \"TABLE_NAME\", ACTION_STATEMENT as \"ACTION\", null as \"SQL\" from INFORMATION_SCHEMA.TRIGGERS where "
                + getInfoSchemaWhere("\"EVENT_OBJECT_CATALOG\"", b, "EVENT_OBJECT_SCHEMA", schemas, "EVENT_OBJECT_TABLE", tables);
    }

    private final String getInfoSchemaWhere(final String catCol, SQLBase b, final String schemaCol, Set<String> schemas, final String tableCol, Set<String> tables) {
        final String tableWhere = tables == null ? "" : " and " + tableCol + " in (" + quoteStrings(b, tables) + ")";
        return catCol + " is null and " + schemaCol + " = '" + b.getMDName() + "' " + tableWhere;
    }

    @Override
    public String getColumnsQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        return "SELECT null as \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , \"CHARACTER_SET_NAME\", \"COLLATION_NAME\" from INFORMATION_SCHEMA.\"COLUMNS\" where "
                + getInfoSchemaWhere("\"TABLE_CATALOG\"", b, "TABLE_SCHEMA", schemas, "TABLE_NAME", tables);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConstraints(SQLBase b, Set<String> schemas, Set<String> tables) throws SQLException {
        final String sel = "SELECT null as \"TABLE_SCHEMA\", c.\"TABLE_NAME\", c.\"CONSTRAINT_NAME\", tc.\"CONSTRAINT_TYPE\", \"COLUMN_NAME\", c.\"ORDINAL_POSITION\"\n"
                // from
                + " FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE c\n"
                // "-- sub-select otherwise at least 15s\n" +
                + "JOIN (SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T where " + getInfoSchemaWhere("\"CONSTRAINT_CATALOG\"", b, "TABLE_SCHEMA", schemas, "TABLE_NAME", tables)
                + ") tc on tc.\"TABLE_SCHEMA\" = c.\"TABLE_SCHEMA\" and tc.\"TABLE_NAME\"=c.\"TABLE_NAME\" and tc.\"CONSTRAINT_NAME\"=c.\"CONSTRAINT_NAME\"\n"
                // where
                + " where \"CONSTRAINT_TYPE\" not in ('FOREIGN KEY', 'PRIMARY KEY') and\n" + getInfoSchemaWhere("c.\"TABLE_CATALOG\"", b, "c.TABLE_SCHEMA", schemas, "c.TABLE_NAME", tables)
                + "order by c.\"TABLE_SCHEMA\", c.\"TABLE_NAME\", c.\"CONSTRAINT_NAME\", c.\"ORDINAL_POSITION\"";
        // don't cache since we don't listen on system tables
        final List<Map<String, Object>> res = (List<Map<String, Object>>) b.getDBSystemRoot().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
        mergeColumnNames(res);
        return res;
    }

    static void mergeColumnNames(final List<Map<String, Object>> res) {
        final Iterator<Map<String, Object>> listIter = res.iterator();
        List<String> l = null;
        while (listIter.hasNext()) {
            final Map<String, Object> m = listIter.next();
            // don't leave the meaningless position (it will always be equal to 1)
            final int pos = ((Number) m.remove("ORDINAL_POSITION")).intValue();
            if (pos == 1) {
                l = new ArrayList<String>();
                m.put("COLUMN_NAMES", l);
            } else {
                listIter.remove();
            }
            l.add((String) m.remove("COLUMN_NAME"));
        }
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return SQLBase.quoteStd("DROP TRIGGER %i", new SQLName(t.getTable().getSchema().getName(), t.getName()));
    }

    @Override
    public String getUpdate(final SQLTable t, List<String> tables, Map<String, String> setPart) {
        final List<String> l = new ArrayList<String>(tables);
        l.add(0, t.getSQLName().quote());
        return CollectionUtils.join(l, ", ") + "\nSET " + CollectionUtils.join(setPart.entrySet(), ",\n", new ITransformer<Entry<String, String>, String>() {
            @Override
            public String transformChecked(Entry<String, String> input) {
                // MySQL needs to prefix the fields, since there's no designated table to update
                return t.getField(input.getKey()).getSQLName(t).quote() + " = " + input.getValue();
            }
        });
    }

    public OutsideClause getSetTableComment(final String comment) {
        return new OutsideClause() {
            @Override
            public ClauseType getType() {
                return ClauseType.OTHER;
            }

            @Override
            public String asString(SQLName tableName) {
                return "ALTER TABLE " + tableName.quote() + " COMMENT = " + SQLBase.quoteStringStd(comment) + ";";
            }
        };
    }
}
