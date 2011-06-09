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
        return f.getType().getJavaType() == Long.class && def.contains("NEXT VALUE") && def.contains("SYSTEM_SEQUENCE");
    }

    @Override
    public String getAuto() {
        return " IDENTITY";
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
        return SQLSelect.quote("ALTER COLUMN %n SET " + (b ? "" : "NOT") + " NULL", f);
    }

    @Override
    public List<String> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final List<String> res = new ArrayList<String>();
        if (toAlter.contains(Properties.TYPE)) {
            // MAYBE implement AlterTableAlterColumn.CHANGE_ONLY_TYPE
            final String newDef = toAlter.contains(Properties.DEFAULT) ? defaultVal : getDefault(f, type);
            final boolean newNullable = toAlter.contains(Properties.NULLABLE) ? nullable : getNullable(f);
            res.add(SQLSelect.quote("ALTER COLUMN %n " + type + getDefaultClause(newDef) + getNullableClause(newNullable), f));
        } else {
            if (toAlter.contains(Properties.NULLABLE))
                res.add(this.setNullable(f, nullable));
            if (toAlter.contains(Properties.DEFAULT))
                res.add(this.setDefault(f, defaultVal));
        }
        return res;
    }

    @Override
    public String getDropRoot(String name) {
        return SQLSelect.quote("DROP SCHEMA IF EXISTS %i ;", name);
    }

    @Override
    public String getCreateRoot(String name) {
        return SQLSelect.quote("CREATE SCHEMA %i ;", name);
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
        t.getDBSystemRoot().getDataSource().execute(SQLSelect.quote("insert into %f select * from CSVREAD(%s, NULL, 'UTF8', ',', '\"', '\\', '\\N') ;", t, f.getAbsolutePath()));
    }

    @Override
    protected void _storeData(final SQLTable t, final File f) {
        checkServerLocalhost(t);
        final SQLSelect sel = SQLSyntaxPG.selectAll(t);
        t.getBase().getDataSource().execute(SQLSelect.quote("CALL CSVWRITE(%s, %s, 'UTF8', ',', '\"', '\\', '\\N', '\n');", f.getAbsolutePath(), sel.asString()));
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
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        // TODO use === or at least fix H2 :
        // TRUE or null => TRUE
        // FALSE or null => null
        final String nullSafe = x + " = " + y + " or ( " + x + " is null and " + y + " is null)";
        if (eq)
            return nullSafe;
        else
            return x + " <> " + y + " or (" + x + " is null and " + y + " is not null) " + " or (" + x + " is not null and " + y + " is null) ";
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        // src is null since H2 only supports alias to Java static functions
        // "SELECT ALIAS_SCHEMA as \"schema\", ALIAS_NAME as \"name\", null as \"src\" FROM \"INFORMATION_SCHEMA\".FUNCTION_ALIASES where ALIAS_CATALOG='"
        // + this.getBase().getMDName() + "' and ALIAS_SCHEMA in (" +
        // toString(proceduresBySchema.keySet()) + ")";

        // H2 functions are per db not per schema, so this doesn't fit our structure
        return null;
    }

    @Override
    public String getTriggerQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        return "SELECT \"TRIGGER_NAME\", \"TABLE_SCHEMA\", \"TABLE_NAME\", \"JAVA_CLASS\" as \"ACTION\", \"SQL\" from INFORMATION_SCHEMA.TRIGGERS where " + getInfoSchemaWhere(b, schemas, tables);
    }

    private final String getInfoSchemaWhere(SQLBase b, Set<String> schemas, Set<String> tables) {
        final String tableWhere = tables == null ? "" : " and \"TABLE_NAME\" in (" + quoteStrings(b, tables) + ")";
        return "\"TABLE_CATALOG\" = '" + b.getMDName() + "' and \"TABLE_SCHEMA\" in (" + quoteStrings(b, schemas) + ") " + tableWhere;
    }

    @Override
    public String getColumnsQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        return "SELECT \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , \"CHARACTER_SET_NAME\", \"COLLATION_NAME\" from INFORMATION_SCHEMA.\"COLUMNS\" where " + getInfoSchemaWhere(b, schemas, tables);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConstraints(SQLBase b, Set<String> schemas, Set<String> tables) throws SQLException {
        final String sel = "SELECT \"TABLE_SCHEMA\", \"TABLE_NAME\", \"CONSTRAINT_NAME\", \n"
        //
                + "case \"CONSTRAINT_TYPE\"  when 'REFERENTIAL' then 'FOREIGN KEY' else \"CONSTRAINT_TYPE\" end as \"CONSTRAINT_TYPE\", \"COLUMN_LIST\"\n"
                //
                + "FROM INFORMATION_SCHEMA.CONSTRAINTS"
                // where
                + " where \"CONSTRAINT_TYPE\" not in ('REFERENTIAL', 'PRIMARY KEY') and\n" + getInfoSchemaWhere(b, schemas, tables);
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
        return SQLBase.quoteStd("DROP TRIGGER %i", new SQLName(t.getTable().getSchema().getName(), t.getName()));
    }
}
