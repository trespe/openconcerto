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
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.utils.NetUtils;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SQLSyntaxH2 extends SQLSyntax {

    SQLSyntaxH2() {
        super(SQLSystem.H2);
        this.typeNames.putAll(Boolean.class, "boolean", "bool", "bit");
        this.typeNames.putAll(Integer.class, "integer", "int", "int4", "mediumint");
        this.typeNames.putAll(Byte.class, "tinyint");
        this.typeNames.putAll(Short.class, "smallint", "int2");
        this.typeNames.putAll(Long.class, "bigint", "int8");
        this.typeNames.putAll(BigDecimal.class, "decimal", "numeric", "number");
        this.typeNames.putAll(Float.class, "real");
        this.typeNames.putAll(Double.class, "double precision", "float", "float4", "float8");
        this.typeNames.putAll(Timestamp.class, "timestamp", "smalldatetime", "datetime");
        this.typeNames.putAll(java.util.Date.class, "date");
        this.typeNames.putAll(Blob.class, "blob", "tinyblob", "mediumblob", "longblob", "image",
        // byte[]
                "bytea", "raw", "varbinary", "longvarbinary", "binary");
        this.typeNames.putAll(Clob.class, "clob", "text", "tinytext", "mediumtext", "longtext");
        this.typeNames.putAll(String.class, "varchar", "longvarchar", "char", "character", "CHARACTER VARYING");
    }

    @Override
    public String getIDType() {
        return " int";
    }

    @Override
    public boolean isAuto(SQLField f) {
        if (f.getDefaultValue() == null)
            return false;

        final String def = ((String) f.getDefaultValue()).toUpperCase();
        // we used to use IDENTITY which translate to long
        return (f.getType().getJavaType() == Integer.class || f.getType().getJavaType() == Long.class) && def.contains("NEXT VALUE") && def.contains("SYSTEM_SEQUENCE");
    }

    @Override
    public String getAuto() {
        // IDENTITY means long
        return " SERIAL";
    }

    @Override
    public String disableFKChecks(DBRoot b) {
        return "SET REFERENTIAL_INTEGRITY FALSE ;";
    }

    @Override
    public String enableFKChecks(DBRoot b) {
        return "SET REFERENTIAL_INTEGRITY TRUE ;";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> normalizeIndexInfo(final Map m) {
        // NON_UNIQUE is a boolean, COLUMN_NAME has a non-quoted name
        return m;
    }

    @Override
    public String getDropIndex(String name, SQLName tableName) {
        return "DROP INDEX IF EXISTS " + SQLBase.quoteIdentifier(name) + ";";
    }

    protected String setNullable(SQLField f, boolean b) {
        return "ALTER COLUMN " + f.getQuotedName() + " SET " + (b ? "" : "NOT") + " NULL";
    }

    @Override
    public List<String> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final List<String> res = new ArrayList<String>();
        if (toAlter.contains(Properties.TYPE)) {
            // MAYBE implement AlterTableAlterColumn.CHANGE_ONLY_TYPE
            final String newDef = toAlter.contains(Properties.DEFAULT) ? defaultVal : getDefault(f, type);
            final boolean newNullable = toAlter.contains(Properties.NULLABLE) ? nullable : getNullable(f);
            final SQLName seqName = f.getOwnedSequence();
            // sequence is used for the default so if default change, remove it (same behaviour than
            // H2)
            final String seqSQL = seqName == null || toAlter.contains(Properties.DEFAULT) ? "" : " SEQUENCE " + seqName.quote();
            res.add("ALTER COLUMN " + f.getQuotedName() + " " + getFieldDecl(type, newDef, newNullable) + seqSQL);
        } else {
            if (toAlter.contains(Properties.DEFAULT))
                res.add(this.setDefault(f, defaultVal));
        }
        // Contrary to the documentation "alter column type" doesn't change the nullable
        // e.g. ALTER COLUMN "VARCHAR" varchar(150) DEFAULT 'testAllProps' NULL
        if (toAlter.contains(Properties.NULLABLE))
            res.add(this.setNullable(f, nullable));
        return res;
    }

    @Override
    public String getDropRoot(String name) {
        return "DROP SCHEMA IF EXISTS " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    public String getCreateRoot(String name) {
        return "CREATE SCHEMA " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    public String transfDefaultJDBC2SQL(SQLField f) {
        String res = (String) f.getDefaultValue();
        if (res != null && f.getType().getJavaType() == String.class && res.trim().toUpperCase().startsWith("STRINGDECODE")) {
            // MAYBE create an attribute with a mem h2 db, instead of using db of f
            res = (String) f.getTable().getBase().getDataSource().executeScalar("CALL " + res);
            // this will be given to other db system, so don't use base specific quoting
            res = SQLBase.quoteStringStd(res);
        }
        return res;
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return Tuple2.create(true, " ");
    }

    @Override
    public void _loadData(final File f, final SQLTable t) {
        checkServerLocalhost(t);
        final String quotedPath = t.getBase().quoteString(f.getAbsolutePath());
        t.getDBSystemRoot().getDataSource().execute("insert into " + t.getSQLName().quote() + " select * from CSVREAD(" + quotedPath + ", NULL, 'UTF8', ',', '\"', '\\', '\\N') ;");
    }

    @Override
    protected void _storeData(final SQLTable t, final File f) {
        checkServerLocalhost(t);
        final String quotedPath = t.getBase().quoteString(f.getAbsolutePath());
        final String quotedSel = t.getBase().quoteString(SQLSyntaxPG.selectAll(t).asString());
        t.getBase().getDataSource().execute("CALL CSVWRITE(" + quotedPath + ", " + quotedSel + ", 'UTF8', ',', '\"', '\\', '\\N', '\n');");
    }

    @Override
    protected boolean isServerLocalhost(SQLServer s) {
        return s.getName().startsWith("mem") || s.getName().startsWith("file") || NetUtils.isSelfAddr(getAddr(s));
    }

    private String getAddr(SQLServer s) {
        if (s.getName().startsWith("tcp") || s.getName().startsWith("ssl")) {
            final int startIndex = "tcp://".length();
            final int endIndex = s.getName().indexOf('/', startIndex);
            return s.getName().substring(startIndex, endIndex < 0 ? s.getName().length() : endIndex);
        } else
            return null;
    }

    @Override
    public String getCreateSynonym(SQLTable t, SQLName newName) {
        return null;
    }

    @Override
    public boolean supportMultiAlterClause() {
        return false;
    }

    @Override
    public String getFormatTimestamp(String sqlTS, boolean basic) {
        return "FORMATDATETIME(" + sqlTS + ", " + SQLBase.quoteStringStd(basic ? TS_BASIC_JAVA_FORMAT : TS_EXTENDED_JAVA_FORMAT) + ")";
    }

    // (SELECT "C1" as "num", "C2" as "name" FROM VALUES(1, 'Hello'), (2, 'World')) AS V;
    @Override
    public String getConstantTable(List<List<String>> rows, String alias, List<String> columnsAlias) {
        // TODO submit a bug report to ask for V("num", "name") notation
        final StringBuilder sb = new StringBuilder();
        sb.append("( SELECT ");
        final int colCount = columnsAlias.size();
        for (int i = 0; i < colCount; i++) {
            sb.append(SQLBase.quoteIdentifier("C" + (i + 1)));
            sb.append(" as ");
            sb.append(SQLBase.quoteIdentifier(columnsAlias.get(i)));
            sb.append(", ");
        }
        // remove last ", "
        sb.setLength(sb.length() - 2);
        sb.append(" FROM ");
        sb.append(this.getValues(rows, colCount));
        sb.append(" ) AS ");
        sb.append(SQLBase.quoteIdentifier(alias));
        return sb.toString();
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        // src can be null since H2 supports alias to Java static functions
        // perhaps join on FUNCTION_COLUMNS to find out parameters' types
        final String src = "coalesce(\"SOURCE\", \"JAVA_CLASS\" || '.' || \"JAVA_METHOD\" ||' parameter(s): ' || \"COLUMN_COUNT\")";
        return "SELECT ALIAS_SCHEMA as \"schema\", ALIAS_NAME as \"name\", " + src + " as \"src\" FROM \"INFORMATION_SCHEMA\".FUNCTION_ALIASES where ALIAS_CATALOG=" + b.quoteString(b.getMDName())
                + " and ALIAS_SCHEMA in (" + quoteStrings(b, schemas) + ")";
    }

    @Override
    public String getTriggerQuery(SQLBase b, TablesMap tables) {
        return "SELECT \"TRIGGER_NAME\", \"TABLE_SCHEMA\", \"TABLE_NAME\", \"JAVA_CLASS\" as \"ACTION\", \"SQL\" from INFORMATION_SCHEMA.TRIGGERS " + getTablesMapJoin(b, tables) + " where "
                + getInfoSchemaWhere(b);
    }

    private String getTablesMapJoin(final SQLBase b, final TablesMap tables) {
        return getTablesMapJoin(b, tables, SQLBase.quoteIdentifier("TABLE_SCHEMA"), SQLBase.quoteIdentifier("TABLE_NAME"));
    }

    private final String getInfoSchemaWhere(SQLBase b) {
        return "\"TABLE_CATALOG\" = " + b.quoteString(b.getMDName());
    }

    @Override
    public String getColumnsQuery(SQLBase b, TablesMap tables) {
        return "SELECT \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , \"CHARACTER_SET_NAME\", \"COLLATION_NAME\", \"SEQUENCE_NAME\" from INFORMATION_SCHEMA.\"COLUMNS\" " + getTablesMapJoin(b, tables) + " where " + getInfoSchemaWhere(b);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConstraints(SQLBase b, TablesMap tables) throws SQLException {
        final String sel = "SELECT \"TABLE_SCHEMA\", \"TABLE_NAME\", \"CONSTRAINT_NAME\", \n"
        //
                + "case \"CONSTRAINT_TYPE\"  when 'REFERENTIAL' then 'FOREIGN KEY' else \"CONSTRAINT_TYPE\" end as \"CONSTRAINT_TYPE\", \"COLUMN_LIST\"\n"
                //
                + "FROM INFORMATION_SCHEMA.CONSTRAINTS " + getTablesMapJoin(b, tables)
                // where
                + " where " + getInfoSchemaWhere(b);
        // don't cache since we don't listen on system tables
        final List<Map<String, Object>> res = (List<Map<String, Object>>) b.getDBSystemRoot().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
        for (final Map<String, Object> m : res) {
            // FIXME change h2 to use ValueArray in MetaTable to handle names with ','
            // new ArrayList otherwise can't be encoded to XML
            m.put("COLUMN_NAMES", new ArrayList<String>(SQLRow.toList((String) m.remove("COLUMN_LIST"))));
        }
        return res;
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return "DROP TRIGGER " + new SQLName(t.getTable().getSchema().getName(), t.getName()).quote();
    }
}
