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
import org.openconcerto.sql.utils.ChangeTable.OutsideClause;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class SQLSyntaxMS extends SQLSyntax {

    SQLSyntaxMS() {
        super(SQLSystem.MSSQL);
        this.typeNames.putAll(Boolean.class, "bit");
        this.typeNames.putAll(Integer.class, "tinyint", "smallint", "unsigned smallint", "int", "unsigned int");
        this.typeNames.putAll(Long.class, "bigint");
        this.typeNames.putAll(BigDecimal.class, "unsigned bigint", "decimal", "numeric", "smallmoney", "money");
        this.typeNames.putAll(Float.class, "real");
        this.typeNames.putAll(Double.class, "float");
        this.typeNames.putAll(Timestamp.class, "smalldatetime", "datetime");
        this.typeNames.putAll(java.sql.Date.class, "date");
        this.typeNames.putAll(java.sql.Time.class, "time");
        this.typeNames.putAll(Blob.class, "image",
        // byte[]
                "varbinary", "binary");
        this.typeNames.putAll(Clob.class, "text", "ntext", "unitext");
        this.typeNames.putAll(String.class, "char", "varchar", "nchar", "nvarchar", "unichar", "univarchar");
    }

    @Override
    public boolean isAuto(SQLField f) {
        // FIXME test
        if (f.getDefaultValue() == null)
            return false;

        return f.getType().getJavaType() == Integer.class && "YES".equals(f.getMetadata("IS_AUTOINCREMENT"));
    }

    @Override
    public String getAuto() {
        return " int IDENTITY";
    }

    @Override
    public String getDateAndTimeType() {
        return "datetime2";
    }

    @Override
    public String getBooleanType() {
        return "bit";
    }

    @Override
    public String transfDefaultJDBC2SQL(SQLField f) {
        final Object def = f.getDefaultValue();
        if (def == null)
            return null;

        // remove parentheses from ((1))
        String stringDef = def.toString();
        while (stringDef.charAt(0) == '(' && stringDef.charAt(stringDef.length() - 1) == ')')
            stringDef = stringDef.substring(1, stringDef.length() - 1);

        if (f.getType().getJavaType() == Boolean.class) {
            return stringDef.equals("'true'") ? "true" : "false";
        } else {
            return stringDef;
        }
    }

    @Override
    protected String transfDefault(SQLField f, String castless) {
        if (castless != null && f.getType().getJavaType() == Boolean.class) {
            // yes MS has no true/false keywords
            return castless.equals("TRUE") ? "'true'" : "'false'";
        } else
            return castless;
    }

    @Override
    public String disableFKChecks(DBRoot b) {
        return fkChecks(b, false);
    }

    private String fkChecks(final DBRoot b, final boolean enable) {
        final String s = enable ? "with check check constraint all" : "nocheck constraint all";
        return "exec sp_MSforeachtable @command1 = 'ALTER TABLE ? " + s + "' , @whereand = " +
        //
                b.getBase().quoteString("and schema_id = SCHEMA_ID( " + b.getBase().quoteString(b.getName()) + " )");
    }

    @Override
    public String enableFKChecks(DBRoot b) {
        return fkChecks(b, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> normalizeIndexInfo(final Map m) {
        // genuine MS driver
        if (getSystem().getJDBCName().equals("sqlserver"))
            m.put("NON_UNIQUE", ((Number) m.get("NON_UNIQUE")).intValue() != 0);
        return m;
    }

    @Override
    public String getDropIndex(String name, SQLName tableName) {
        return "DROP INDEX " + SQLBase.quoteIdentifier(name) + " on " + tableName.quote() + ";";
    }

    protected String setNullable(SQLField f, boolean b) {
        return SQLSelect.quote("ALTER COLUMN %n SET " + (b ? "" : "NOT") + " NULL", f);
    }

    // FIXME
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
        // FIXME
        // http://ranjithk.com/2010/01/31/script-to-drop-all-objects-of-a-schema/
        return SQLSelect.quote("exec CleanUpSchema %s, 'w' ;", name);
    }

    @Override
    public String getCreateRoot(String name) {
        return SQLSelect.quote("CREATE SCHEMA %i ;", name);
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return null;
    }

    private static final Pattern nullPatrn = Pattern.compile("\\N", Pattern.LITERAL);
    private static final Pattern backSlashPatrn = Pattern.compile("\\\"", Pattern.LITERAL);
    private static final Pattern newlinePatrn = Pattern.compile("\n");
    private static final Pattern newlineAndIDPatrn = Pattern.compile("\n(?=\\p{Digit}+\\|)");

    private static final Pattern commaSepPatrn = Pattern.compile("(?<!\\\\)\",\"");
    private static final Pattern firstLastQuotePatrn = Pattern.compile("(^\")|(\"$)", Pattern.MULTILINE);

    // zero-width lookbehind to handle sequential boolean
    private static final Pattern boolTPatrn = Pattern.compile("(?<=\\|)t\\|");
    private static final Pattern boolFPatrn = Pattern.compile("(?<=\\|)f\\|");
    private static final Pattern boolTEndPatrn = Pattern.compile("\\|t$", Pattern.MULTILINE);
    private static final Pattern boolFEndPatrn = Pattern.compile("\\|f$", Pattern.MULTILINE);

    // 2007-12-21 10:39:09.031+01 with microseconds part being variable length and optional
    private static final Pattern dateWithOffsetPatrn = Pattern.compile("(\\|\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2} \\p{Digit}{2}:\\p{Digit}{2}:\\p{Digit}{2}(.\\p{Digit}{1,3})?)\\+\\p{Digit}{2}");

    @Override
    public void _loadData(final File f, final SQLTable t) throws IOException {
        // FIXME null handling ?
        final String data = FileUtils.read(f, "UTF-8");
        final String sansNull = nullPatrn.matcher(data).replaceAll("\"\"");

        String tmp = sansNull;

        // remove header with column names
        tmp = tmp.substring(tmp.indexOf('\n') + 1, tmp.length());

        // remove pipes in data
        tmp = tmp.replace('|', ' ');
        // remove inner "
        tmp = commaSepPatrn.matcher(tmp).replaceAll("|");
        // remove first and last "
        tmp = firstLastQuotePatrn.matcher(tmp).replaceAll("");
        // remove escape character (only remove \" so we can spot \|)
        tmp = backSlashPatrn.matcher(tmp).replaceAll(String.valueOf('"'));

        // for pg types
        if (true) {
            tmp = boolTPatrn.matcher(tmp).replaceAll("1|");
            tmp = boolFPatrn.matcher(tmp).replaceAll("0|");
            tmp = boolTEndPatrn.matcher(tmp).replaceAll("|1");
            tmp = boolFEndPatrn.matcher(tmp).replaceAll("|0");

            tmp = dateWithOffsetPatrn.matcher(tmp).replaceAll("$1");
        }

        // we can't specify \n as ROWTERMINATOR ms automatically prepends \r
        // http://msdn.microsoft.com/en-us/library/ms191485.aspx
        if (t.isRowable() && t.getOrderedFields().get(0) != t.getKey())
            throw new IllegalArgumentException("MS needs ID first for " + t + " " + t.getOrderedFields());
        String winNL;
        if (t.isRowable()) {
            winNL = newlineAndIDPatrn.matcher(tmp).replaceAll("\r\n");
            // newlineAndIDPatrn doesn't match the last newline
            winNL = winNL.substring(0, winNL.length() - 1) + "\r\n";
        } else {
            winNL = newlinePatrn.matcher(tmp).replaceAll("\r\n");
        }

        if (t.getName().equals("RIGHT"))
            System.err.println("SQLSyntaxMS._loadData()\n\n" + tmp);

        final File temp = File.createTempFile("mssql_loadData", ".txt", new File("."));
        FileUtils.write(winNL, temp, "UTF-16", false);
        checkServerLocalhost(t);
        t.getDBSystemRoot().getDataSource()
                .execute(t.getBase().quote("bulk insert %f from %s with ( DATAFILETYPE='widechar', FIELDTERMINATOR = '|', FIRSTROW=1, KEEPIDENTITY ) ;", t, temp.getAbsolutePath()));
        temp.delete();
    }

    // FIXME
    @Override
    protected void _storeData(final SQLTable t, final File f) {
        checkServerLocalhost(t);
    }

    @Override
    public boolean supportMultiAlterClause() {
        // support multiple if you omit the "add" : ALTER TABLE t add f1 int, f2 bit
        return false;
    }

    @Override
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        return SQLSystem.H2.getSyntax().getNullIsDataComparison(x, eq, y);
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        return "  select name, schema_name(schema_id) as \"schema\", cast(OBJECT_DEFINITION(object_id) as varchar(4096)) as \"src\"\n"
        //
                + "  FROM " + new SQLName(b.getName(), "sys", "objects") + "\n"
                // scalar, inline table-valued, table-valued
                + "  where type IN ('FN', 'IF', 'TF') and SCHEMA_NAME( schema_id ) in (" + quoteStrings(b, schemas) + ") ";
    }

    @Override
    public String getTriggerQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        final String tableWhere = tables == null ? "" : " and tabl.name in (" + quoteStrings(b, tables) + ")";
        // for some reason OBJECT_DEFINITION always returns null
        return "SELECT  trig.name as \"TRIGGER_NAME\", SCHEMA_NAME( tabl.schema_id ) as \"TABLE_SCHEMA\", tabl.name as \"TABLE_NAME\",  null as \"ACTION\", cast(OBJECT_DEFINITION(trig.object_id) as varchar(4096)) as \"SQL\"\n"
                //
                + "FROM " + new SQLName(b.getName(), "sys", "triggers") + " trig\n"
                //
                + "join " + new SQLName(b.getName(), "sys", "objects") + " tabl on trig.parent_id = tabl.object_id\n"
                //
                + "where SCHEMA_NAME( tabl.schema_id ) in (" + quoteStrings(b, schemas) + ") " + tableWhere;
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return SQLBase.quoteStd("DROP TRIGGER %i", new SQLName(t.getTable().getSchema().getName(), t.getName()));
    }

    @Override
    public String getColumnsQuery(SQLBase b, Set<String> schemas, Set<String> tables) {
        // TODO
        return null;
    }

    private final String getInfoSchemaWhere(SQLBase b, final String schemaCol, Set<String> schemas, final String tableCol, Set<String> tables) {
        final String tableWhere = tables == null ? "" : " and " + tableCol + " in (" + quoteStrings(b, tables) + ")";
        return schemaCol + " in ( " + quoteStrings(b, schemas) + ") " + tableWhere;
        // no need of base, since we query the views of the right base
    }

    @Override
    public List<Map<String, Object>> getConstraints(SQLBase b, Set<String> schemas, Set<String> tables) throws SQLException {
        final String where = "where " + getInfoSchemaWhere(b, "SCHEMA_NAME(t.schema_id)", schemas, "t.name", tables);
        final String sel = "SELECT SCHEMA_NAME(t.schema_id) AS \"TABLE_SCHEMA\", t.name AS \"TABLE_NAME\", k.name AS \"CONSTRAINT_NAME\", 'UNIQUE' as \"CONSTRAINT_TYPE\", col_name(c.object_id, c.column_id) AS \"COLUMN_NAME\", c.key_ordinal AS \"ORDINAL_POSITION\"\n"
                + "FROM sys.key_constraints k\n"
                //
                + "JOIN sys.index_columns c ON c.object_id = k.parent_object_id AND c.index_id = k.unique_index_id\n"
                //
                + "JOIN sys.tables t ON t.object_id = k.parent_object_id\n"
                + where
                + " and k.type != 'PK'"
                + "\nUNION ALL\n"
                //
                + "SELECT SCHEMA_NAME(t.schema_id) AS \"TABLE_SCHEMA\", t.name AS \"TABLE_NAME\", k.name AS \"CONSTRAINT_NAME\", 'CHECK' as \"CONSTRAINT_TYPE\", col.name AS \"COLUMN_NAME\", 1 AS \"ORDINAL_POSITION\"\n"
                + "FROM sys.check_constraints k\n"
                //
                + "join sys.tables t on k.parent_object_id = t.object_id\n"
                //
                + "left join sys.columns col on k.parent_column_id = col.column_id and col.object_id = t.object_id\n"
                //
                + where;
        // don't cache since we don't listen on system tables
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> res = (List<Map<String, Object>>) b.getDBSystemRoot().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
        SQLSyntaxMySQL.mergeColumnNames(res);
        return res;
    }

    @Override
    public OutsideClause getSetTableComment(String comment) {
        return null;
    }

    @Override
    public String getConcatOp() {
        return "+";
    }

    @Override
    public String getRegexpOp(boolean negation) {
        // MS needs either the CLR : http://msdn.microsoft.com/en-us/magazine/cc163473.aspx
        // or http://www.codeproject.com/KB/database/xp_pcre.aspx
        return null;
    }
}
