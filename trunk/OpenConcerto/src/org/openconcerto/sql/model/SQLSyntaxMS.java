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
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.OutsideClause;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.ProcessStreams;
import org.openconcerto.utils.ProcessStreams.Action;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.StringUtils;
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
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class SQLSyntaxMS extends SQLSyntax {

    SQLSyntaxMS() {
        super(SQLSystem.MSSQL);
        this.typeNames.addAll(Boolean.class, "bit");
        // tinyint is unsigned
        this.typeNames.addAll(Short.class, "smallint", "tinyint");
        this.typeNames.addAll(Integer.class, "int");
        this.typeNames.addAll(Long.class, "bigint");
        this.typeNames.addAll(BigDecimal.class, "decimal", "numeric", "smallmoney", "money");
        this.typeNames.addAll(Float.class, "real");
        this.typeNames.addAll(Double.class, "float");
        this.typeNames.addAll(Timestamp.class, "smalldatetime", "datetime");
        this.typeNames.addAll(java.sql.Date.class, "date");
        this.typeNames.addAll(java.sql.Time.class, "time");
        this.typeNames.addAll(Blob.class, "image",
        // byte[]
                "varbinary", "binary");
        this.typeNames.addAll(Clob.class, "text", "ntext", "unitext");
        this.typeNames.addAll(String.class, "char", "varchar", "nchar", "nvarchar", "unichar", "univarchar");
    }

    @Override
    SQLBase createBase(SQLServer server, String name, final IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        return new MSSQLBase(server, name, systemRootInit, login, pass, dsInit);
    }

    @Override
    public String getInitSystemRoot() {
        final String sql;
        try {
            final String fileContent = FileUtils.readUTF8(SQLSyntaxPG.class.getResourceAsStream("mssql-functions.sql"));
            sql = fileContent.replace("${rootName}", SQLBase.quoteIdentifier("dbo"));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read functions", e);
        }
        return sql;
    }

    @Override
    public boolean isAuto(SQLField f) {
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
    public int getMaximumVarCharLength() {
        // http://msdn.microsoft.com/en-us/library/ms176089(v=sql.105).aspx
        return 8000;
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
    protected String getRuleSQL(final Rule r) {
        // MSSQL doesn't support RESTRICT
        return (r.equals(Rule.RESTRICT) ? Rule.NO_ACTION : r).asString();
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

    @Override
    public List<Map<String, Object>> getIndexInfo(SQLTable t) throws SQLException {
        final String query = "SELECT NULL AS \"TABLE_CAT\", schema_name(t.schema_id) as \"TABLE_SCHEM\", t.name as \"TABLE_NAME\",\n" +
        //
                "~idx.is_unique as \"NON_UNIQUE\", NULL AS \"INDEX_QUALIFIER\", idx.name as \"INDEX_NAME\", NULL as \"TYPE\",\n" +
                //
                "indexCols.key_ordinal as \"ORDINAL_POSITION\", cols.name as \"COLUMN_NAME\",\n" +
                //
                "case when indexCols.is_descending_key = 1 then 'D' else 'A' end as \"ASC_OR_DESC\", null as \"CARDINALITY\", null as \"PAGES\",\n" +
                //
                "filter_definition as \"FILTER_CONDITION\"\n" +
                //
                "  FROM [test].[sys].[objects] t\n" +
                //
                "  join [test].[sys].[indexes] idx on idx.object_id = t.object_id\n" +
                //
                "  join [test].[sys].[index_columns] indexCols on idx.index_id = indexCols.index_id and idx.object_id = indexCols.object_id\n" +
                //
                "  join [test].[sys].[columns] cols on t.object_id = cols.object_id and cols.column_id = indexCols.column_id \n" +
                //
                "  where schema_name(t.schema_id) = " + t.getBase().quoteString(t.getSchema().getName()) + " and t.name = " + t.getBase().quoteString(t.getName()) + "\n"
                //
                + "ORDER BY \"NON_UNIQUE\", \"TYPE\", \"INDEX_NAME\", \"ORDINAL_POSITION\";";
        // don't cache since we don't listen on system tables
        return (List<Map<String, Object>>) t.getDBSystemRoot().getDataSource().execute(query, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
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

    @Override
    public boolean isUniqueException(SQLException exn) {
        return SQLUtils.findWithSQLState(exn).getErrorCode() == 2601;
    }

    @Override
    public Map<ClauseType, List<String>> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final ListMap<ClauseType, String> res = new ListMap<ClauseType, String>();
        if (toAlter.contains(Properties.TYPE) || toAlter.contains(Properties.NULLABLE)) {
            final String newType = toAlter.contains(Properties.TYPE) ? type : getType(f);
            final boolean newNullable = toAlter.contains(Properties.NULLABLE) ? nullable : getNullable(f);
            res.add(ClauseType.ALTER_COL, "ALTER COLUMN " + f.getQuotedName() + " " + getFieldDecl(newType, null, newNullable));
        }
        if (toAlter.contains(Properties.DEFAULT)) {
            final Constraint existingConstraint = f.getTable().getConstraint(ConstraintType.DEFAULT, Arrays.asList(f.getName()));
            if (existingConstraint != null) {
                res.add(ClauseType.DROP_CONSTRAINT, "DROP CONSTRAINT " + SQLBase.quoteIdentifier(existingConstraint.getName()));
            }
            if (defaultVal != null) {
                res.add(ClauseType.ADD_CONSTRAINT, "ADD DEFAULT " + defaultVal + " FOR " + f.getQuotedName());
            }
        }
        return res;
    }

    @Override
    public String getRenameTable(SQLName table, String newName) {
        return "sp_rename " + SQLBase.quoteStringStd(table.quote()) + ", " + SQLBase.quoteStringStd(newName);
    }

    @Override
    public String getDropTableIfExists(SQLName name) {
        final String quoted = name.quote();
        return "IF OBJECT_ID(" + SQLBase.quoteStringStd(quoted) + ", 'U') IS NOT NULL DROP TABLE " + quoted;
    }

    @Override
    public String getDropRoot(String name) {
        // Only works if getInitSystemRoot() was executed
        // http://ranjithk.com/2010/01/31/script-to-drop-all-objects-of-a-schema/
        return "exec CleanUpSchema " + SQLBase.quoteStringStd(name) + ", 'w' ;";
    }

    @Override
    public String getCreateRoot(String name) {
        return "CREATE SCHEMA " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return null;
    }

    @Override
    public void _loadData(final File f, final SQLTable t) throws IOException {
        final String data = FileUtils.readUTF8(f);
        final File temp = File.createTempFile(FileUtils.sanitize("mssql_loadData_" + t.getName()), ".txt");

        // no we cant't use UTF16 since Java write BE and MS ignores the BOM, always using LE.
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), Charset.forName("x-UTF-16LE-BOM")));

        final List<SQLField> fields = t.getOrderedFields();
        final int fieldsCount = fields.size();
        final BitSet booleanFields = new BitSet(fieldsCount);
        int fieldIndex = 0;
        for (final SQLField field : fields) {
            final int type = field.getType().getType();
            booleanFields.set(fieldIndex++, type == Types.BOOLEAN || type == Types.BIT);
        }
        fieldIndex = 0;

        try {
            // skip fields names
            int i = data.indexOf('\n') + 1;
            while (i < data.length()) {
                final String twoChars = i + 2 <= data.length() ? data.substring(i, i + 2) : null;
                if ("\\N".equals(twoChars)) {
                    i += 2;
                } else if ("\"\"".equals(twoChars)) {
                    writer.write("\0");
                    i += 2;
                } else {
                    final Tuple2<String, Integer> unDoubleQuote = StringUtils.unDoubleQuote(data, i);
                    String unquoted = unDoubleQuote.get0();
                    if (booleanFields.get(fieldIndex)) {
                        if (unquoted.equalsIgnoreCase("false")) {
                            unquoted = "0";
                        } else if (unquoted.equalsIgnoreCase("true")) {
                            unquoted = "1";
                        }
                    }
                    writer.write(unquoted);
                    i = unDoubleQuote.get1();
                }
                fieldIndex++;
                if (i < data.length()) {
                    final char c = data.charAt(i);
                    if (c == ',') {
                        writer.write(FIELD_DELIM);
                        i++;
                    } else if (c == '\n') {
                        writer.write(ROW_DELIM);
                        i++;
                        if (fieldIndex != fieldsCount)
                            throw new IOException("Expected " + fieldsCount + " fields but got : " + fieldIndex);
                        fieldIndex = 0;
                    } else {
                        throw new IOException("Unexpected character after field : " + c);
                    }
                }
            }
            if (fieldIndex != 0 && fieldIndex != fieldsCount)
                throw new IOException("Expected " + fieldsCount + " fields but got : " + fieldIndex);
        } finally {
            writer.close();
        }

        execute_bcp(t, false, temp);
        temp.delete();

        // MAYBE when on localhost, remove the bcp requirement (OTOH bcp should already be
        // installed, just perhaps not in the path)
        // checkServerLocalhost(t);
        // "bulk insert " + t.getSQL() + " from " + b.quoteString(temp.getAbsolutePath()) +
        // " with ( DATAFILETYPE='widechar', FIELDTERMINATOR = " + b.quoteString(FIELD_DELIM)
        // + ", ROWTERMINATOR= " + b.quoteString(ROW_DELIM) +
        // ", FIRSTROW=1, KEEPIDENTITY, KEEPNULLS ) ;"
    }

    private static final String FIELD_DELIM = "<|!!|>";
    private static final String ROW_DELIM = "...#~\n~#...";

    protected void execute_bcp(final SQLTable t, final boolean dump, final File f) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder("bcp");
        pb.command().add(t.getSQLName().quote());
        pb.command().add(dump ? "out" : "in");
        pb.command().add(f.getAbsolutePath());
        // UTF-16LE with a BOM
        pb.command().add("-w");
        pb.command().add("-t" + FIELD_DELIM);
        pb.command().add("-r" + ROW_DELIM);
        // needed if table name is a keyword (e.g. RIGHT)
        pb.command().add("-q");
        pb.command().add("-S" + t.getServer().getName());
        pb.command().add("-U" + t.getDBSystemRoot().getDataSource().getUsername());
        pb.command().add("-P" + t.getDBSystemRoot().getDataSource().getPassword());
        if (!dump) {
            // retain null
            pb.command().add("-k");
            // keep identity
            pb.command().add("-E");
        }

        final Process p = pb.start();
        ProcessStreams.handle(p, Action.REDIRECT);
        try {
            final int returnCode = p.waitFor();
            if (returnCode != 0)
                throw new IOException("Did not finish correctly : " + returnCode + "\n" + pb.command());
        } catch (InterruptedException e) {
            throw new RTInterruptedException(e);
        }
    }

    // For bcp : http://www.microsoft.com/en-us/download/details.aspx?id=16978
    @Override
    protected void _storeData(final SQLTable t, final File f) throws IOException {
        final File tmpFile = File.createTempFile(FileUtils.sanitize("mssql_dump_" + t.getName()), ".dat");
        execute_bcp(t, true, tmpFile);
        final int readerBufferSize = 32768;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tmpFile), StringUtils.UTF16), readerBufferSize);
        final List<SQLField> orderedFields = t.getOrderedFields();
        final int fieldsCount = orderedFields.size();
        final String cols = CollectionUtils.join(orderedFields, ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return SQLBase.quoteIdentifier(input.getName());
            }
        });
        final FileOutputStream outs = new FileOutputStream(f);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(outs, StringUtils.UTF8));
            writer.write(cols);
            writer.write('\n');
            final StringBuilder sb = new StringBuilder(readerBufferSize * 2);
            String row = readUntil(reader, sb, ROW_DELIM);
            final Pattern fieldPattern = Pattern.compile(FIELD_DELIM, Pattern.LITERAL);
            while (row != null) {
                if (row.length() > 0) {
                    // -1 to have every (even empty) field
                    final String[] fields = fieldPattern.split(row, -1);
                    if (fields.length != fieldsCount)
                        throw new IOException("Invalid fields count, expected " + fieldsCount + " but was " + fields.length + "\n" + row);
                    int i = 0;
                    for (final String field : fields) {
                        final String quoted;
                        if (field.length() == 0) {
                            quoted = "\\N";
                        } else if (field.equals("\0")) {
                            quoted = "\"\"";
                        } else {
                            quoted = StringUtils.doubleQuote(field);
                        }
                        writer.write(quoted);
                        if (++i < fieldsCount)
                            writer.write(',');
                    }
                    writer.write('\n');
                }
                row = readUntil(reader, sb, ROW_DELIM);
            }
        } finally {
            tmpFile.delete();
            if (writer != null)
                writer.close();
            else
                outs.close();
            reader.close();
        }
    }

    private String readUntil(BufferedReader reader, StringBuilder sb, String rowDelim) throws IOException {
        if (sb.capacity() == 0)
            return null;
        final int existing = sb.indexOf(rowDelim);
        if (existing >= 0) {
            final String res = sb.substring(0, existing);
            sb.delete(0, existing + rowDelim.length());
            return res;
        } else {
            final char[] buffer = new char[sb.capacity() / 3];
            final int readCount = reader.read(buffer);
            if (readCount <= 0) {
                final String res = sb.toString();
                sb.setLength(0);
                sb.trimToSize();
                assert sb.capacity() == 0;
                return res;
            } else {
                sb.append(buffer, 0, readCount);
                return readUntil(reader, sb, rowDelim);
            }
        }
    }

    @Override
    public boolean supportMultiAlterClause() {
        // support multiple if you omit the "add" : ALTER TABLE t add f1 int, f2 bit
        return false;
    }

    @Override
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        final String nullSafe = x + " = " + y + " or ( " + x + " is null and " + y + " is null)";
        if (eq)
            return nullSafe;
        else
            return x + " <> " + y + " or (" + x + " is null and " + y + " is not null) " + " or (" + x + " is not null and " + y + " is null) ";
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
    public String getTriggerQuery(SQLBase b, TablesMap tables) {
        // for some reason OBJECT_DEFINITION always returns null
        return "SELECT  trig.name as \"TRIGGER_NAME\", SCHEMA_NAME( tabl.schema_id ) as \"TABLE_SCHEMA\", tabl.name as \"TABLE_NAME\",  null as \"ACTION\", cast(OBJECT_DEFINITION(trig.object_id) as varchar(4096)) as \"SQL\"\n"
                //
                + "FROM " + new SQLName(b.getName(), "sys", "triggers") + " trig\n"
                //
                + "join " + new SQLName(b.getName(), "sys", "objects") + " tabl on trig.parent_id = tabl.object_id\n"
                // requested tables
                + getTablesMapJoin(b, tables, "SCHEMA_NAME( tabl.schema_id )", "tabl.name");
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return "DROP TRIGGER " + new SQLName(t.getTable().getSchema().getName(), t.getName()).quote();
    }

    @Override
    public String getColumnsQuery(SQLBase b, TablesMap tables) {
        return "SELECT TABLE_SCHEMA as \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", TABLE_NAME as \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", COLUMN_NAME as \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , CHARACTER_SET_NAME as \"CHARACTER_SET_NAME\", COLLATION_NAME as \"COLLATION_NAME\" from INFORMATION_SCHEMA.COLUMNS\n" +
                // requested tables
                getTablesMapJoin(b, tables, "TABLE_SCHEMA", "TABLE_NAME");
    }

    @Override
    public List<Map<String, Object>> getConstraints(SQLBase b, TablesMap tables) throws SQLException {
        final String where = getTablesMapJoin(b, tables, "SCHEMA_NAME(t.schema_id)", "t.name");
        final String sel = "SELECT SCHEMA_NAME(t.schema_id) AS \"TABLE_SCHEMA\", t.name AS \"TABLE_NAME\", k.name AS \"CONSTRAINT_NAME\", case k.type when 'UQ' then 'UNIQUE' when 'PK' then 'PRIMARY KEY' end as \"CONSTRAINT_TYPE\", col_name(c.object_id, c.column_id) AS \"COLUMN_NAME\", c.key_ordinal AS \"ORDINAL_POSITION\", null AS [DEFINITION]\n"
                + "FROM sys.key_constraints k\n"
                //
                + "JOIN sys.index_columns c ON c.object_id = k.parent_object_id AND c.index_id = k.unique_index_id\n"
                //
                + "JOIN sys.tables t ON t.object_id = k.parent_object_id\n"
                + where
                + "\nUNION ALL\n"
                //
                + "SELECT SCHEMA_NAME(t.schema_id) AS \"TABLE_SCHEMA\", t.name AS \"TABLE_NAME\", k.name AS \"CONSTRAINT_NAME\", 'CHECK' as \"CONSTRAINT_TYPE\", col.name AS \"COLUMN_NAME\", 1 AS \"ORDINAL_POSITION\", k.[definition] AS [DEFINITION]\n"
                + "FROM sys.check_constraints k\n"
                //
                + "join sys.tables t on k.parent_object_id = t.object_id\n"
                //
                + "left join sys.columns col on k.parent_column_id = col.column_id and col.object_id = t.object_id\n"
                //
                + where
                + "\nUNION ALL\n"
                //
                + "SELECT SCHEMA_NAME(t.schema_id) AS [TABLE_SCHEMA], t.name AS [TABLE_NAME], k.name AS [CONSTRAINT_NAME], 'DEFAULT' as [CONSTRAINT_TYPE], col.name AS [COLUMN_NAME], 1 AS [ORDINAL_POSITION], k.[definition] AS [DEFINITION]\n"
                + "FROM sys.[default_constraints] k\n"
                //
                + "JOIN sys.tables t ON t.object_id = k.parent_object_id\n"
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

    @Override
    public String getFormatTimestamp(String sqlTS, boolean basic) {
        final String extended = "CONVERT(nvarchar(30), CAST(" + sqlTS + " as datetime), 126) + '000'";
        if (basic) {
            return "replace( replace( " + extended + ", '-', ''), ':' , '' )";
        } else {
            return extended;
        }
    }
}
