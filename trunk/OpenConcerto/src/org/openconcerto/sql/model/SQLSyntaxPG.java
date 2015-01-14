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

import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLTable.SQLIndex;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.dbcp.DelegatingConnection;
import org.postgresql.PGConnection;

/**
 * To require SSL, set the "ssl" connection property to "true" (note: for now any value, including
 * "false" enables it), to disable server validation set "sslfactory" to
 * "org.postgresql.ssl.NonValidatingFactory". To check the connection status, install the
 * contrib/sslinfo extension and execute "select ssl_is_used();". SSL Compression might be supported
 * if we can find a good sslfactory : see this <a
 * href="http://archives.postgresql.org/pgsql-general/2010-08/thrd5.php#00003">thread</a>.
 * <p>
 * To enable SSL on the server see http://www.postgresql.org/docs/current/static/ssl-tcp.html
 * (already set up on Ubuntu).
 * <p>
 * 
 * @author Sylvain CUAZ
 */
class SQLSyntaxPG extends SQLSyntax {

    // From http://www.postgresql.org/docs/9.0/interactive/multibyte.html
    static final short MAX_BYTES_PER_CHAR = 4;
    // http://www.postgresql.org/docs/9.0/interactive/datatype-character.html
    private static final short MAX_LENGTH_BYTES = 4;
    // https://wiki.postgresql.org/wiki/FAQ#What_is_the_maximum_size_for_a_row.2C_a_table.2C_and_a_database.3F
    private static final int MAX_FIELD_SIZE = 1024 * 1024 * 1024;

    private static final int MAX_VARCHAR_L = (MAX_FIELD_SIZE - MAX_LENGTH_BYTES) / MAX_BYTES_PER_CHAR;

    SQLSyntaxPG() {
        super(SQLSystem.POSTGRESQL);
        this.typeNames.addAll(Boolean.class, "boolean", "bool", "bit");
        this.typeNames.addAll(Short.class, "smallint");
        this.typeNames.addAll(Integer.class, "integer", "int", "int4");
        this.typeNames.addAll(Long.class, "bigint", "int8");
        this.typeNames.addAll(BigDecimal.class, "decimal", "numeric");
        this.typeNames.addAll(Float.class, "real", "float4");
        this.typeNames.addAll(Double.class, "double precision", "float8");
        // since 7.3 default is without timezone
        this.typeNames.addAll(Timestamp.class, "timestamp", "timestamp without time zone");
        this.typeNames.addAll(java.util.Date.class, "time", "time without time zone", "date");
        this.typeNames.addAll(Blob.class, "bytea");
        this.typeNames.addAll(Clob.class, "varchar", "char", "character varying", "character", "text");
        this.typeNames.addAll(String.class, "varchar", "char", "character varying", "character", "text");
    }

    public String getInitRoot(final String name) {
        final String sql;
        try {
            final String fileContent = FileUtils.readUTF8(SQLSyntaxPG.class.getResourceAsStream("pgsql-functions.sql"));
            sql = fileContent.replace("${rootName}", SQLBase.quoteIdentifier(name));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read functions", e);
        }
        return sql;
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return Tuple2.create(false, "::");
    }

    public String getIDType() {
        return " int";
    }

    @Override
    public boolean isAuto(SQLField f) {
        return f.getType().getTypeName().equalsIgnoreCase("serial");
    }

    public String getAuto() {
        return " serial";
    }

    @Override
    public int getMaximumVarCharLength() {
        return MAX_VARCHAR_L;
    }

    private String changeFKChecks(DBRoot r, final String action) {
        String res = r.getBase().quote("select %i.getTables(%s, '.*', 'tables_changeFKChecks');", r.getName(), r.getName());
        res += r.getBase().quote("select %i.setTrigger('" + action + "', 'tables_changeFKChecks');", r.getName());
        res += "close \"tables_changeFKChecks\";";
        return res;
    }

    @Override
    public String disableFKChecks(DBRoot b) {
        // MAYBE return "SET CONSTRAINTS ALL DEFERRED";
        // NOTE: CASCADE, SET NULL, SET DEFAULT cannot be deferred (as of 9.1)
        return this.changeFKChecks(b, "DISABLE");
    }

    @Override
    public String enableFKChecks(DBRoot b) {
        // MAYBE return "SET CONSTRAINTS ALL IMMEDIATE";
        return this.changeFKChecks(b, "ENABLE");
    }

    @SuppressWarnings("unchecked")
    @Override
    // override since pg driver do not return FILTER_CONDITION
    public List<Map<String, Object>> getIndexInfo(SQLTable t) throws SQLException {
        final String query = "SELECT NULL AS \"TABLE_CAT\",  n.nspname as \"TABLE_SCHEM\",\n"
        //
                + "ct.relname as \"TABLE_NAME\", NOT i.indisunique AS \"NON_UNIQUE\",\n"
                //
                + "NULL AS \"INDEX_QUALIFIER\", ci.relname as \"INDEX_NAME\",\n"
                //
                + "NULL as \"TYPE\", col.attnum as \"ORDINAL_POSITION\",\n"
                //
                + "CASE WHEN i.indexprs IS NULL THEN col.attname ELSE pg_get_indexdef(ci.oid,col.attnum,false) END AS \"COLUMN_NAME\",\n"
                //
                + "NULL AS \"ASC_OR_DESC\", ci.reltuples as \"CARDINALITY\", ci.relpages as \"PAGES\",\n"
                //
                + "pg_get_expr(i.indpred,ct.oid) as \"FILTER_CONDITION\"\n"
                //
                + "FROM pg_catalog.pg_class ct\n"
                //
                + "     JOIN pg_catalog.pg_namespace n ON n.oid = ct.relnamespace\n"
                //
                + "     JOIN pg_catalog.pg_index i ON ct.oid=i.indrelid\n"
                //
                + "     JOIN pg_catalog.pg_class ci ON ci.oid=i.indexrelid\n"
                //
                + "     JOIN pg_catalog.pg_attribute col ON col.attrelid = ci.oid\n"
                //
                + "WHERE ci.relkind IN ('i','') AND n.nspname <> 'pg_catalog' AND n.nspname !~ '^pg_toast'\n"
                //
                + " AND n.nspname = " + t.getBase().quoteString(t.getSchema().getName()) + " AND ct.relname = " + t.getBase().quoteString(t.getName()) + "\n"
                //
                + "ORDER BY \"NON_UNIQUE\", \"TYPE\", \"INDEX_NAME\", \"ORDINAL_POSITION\";";
        // don't cache since we don't listen on system tables
        return (List<Map<String, Object>>) t.getDBSystemRoot().getDataSource().execute(query, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
    }

    protected String setNullable(SQLField f, boolean b) {
        return "ALTER COLUMN " + f.getQuotedName() + " " + (b ? "DROP" : "SET") + " NOT NULL";
    }

    @Override
    public Map<ClauseType, List<String>> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final List<String> res = new ArrayList<String>();
        if (toAlter.contains(Properties.NULLABLE))
            res.add(this.setNullable(f, nullable));
        final String newType;
        if (toAlter.contains(Properties.TYPE)) {
            newType = type;
            res.add("ALTER COLUMN " + f.getQuotedName() + " TYPE " + newType);
        } else
            newType = getType(f);
        if (toAlter.contains(Properties.DEFAULT))
            res.add(this.setDefault(f, defaultVal));
        return ListMap.singleton(ClauseType.ALTER_COL, res);
    }

    @Override
    public String getDropRoot(String name) {
        return "DROP SCHEMA IF EXISTS " + SQLBase.quoteIdentifier(name) + " CASCADE ;";
    }

    @Override
    public String getCreateRoot(String name) {
        return "CREATE SCHEMA " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    public String getDropPrimaryKey(SQLTable t) {
        return getDropConstraint() + SQLBase.quoteIdentifier(t.getConstraint(ConstraintType.PRIMARY_KEY, t.getPKsNames()).getName());
    }

    @Override
    public String getDropIndex(String name, SQLName tableName) {
        return "DROP INDEX IF EXISTS " + new SQLName(tableName.getItemLenient(-2), name).quote() + " ;";
    }

    @Override
    protected String getCreateIndex(final String cols, final SQLName tableName, SQLIndex i) {
        final String method = i.getMethod() != null ? " USING " + i.getMethod() : "";
        return "ON " + tableName.quote() + " " + method + cols;
    }

    @SuppressWarnings("unused")
    private final String getIndexesReq(String schema, String tablePattern) {
        return "SELECT pg_catalog.pg_get_indexdef(i.indexrelid), c2.relname, i.indisunique, i.indisclustered, i.indisvalid" +
        // FROM
                " FROM pg_catalog.pg_class c" +
                //
                " LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace" +
                //
                " LEFT JOIN pg_catalog.pg_index i ON c.oid = i.indrelid" +
                //
                " LEFT JOIN pg_catalog.pg_class c2 ON i.indexrelid = c2.oid" +
                // WHERE
                " WHERE c.relname ~ '" + tablePattern + "' and n.nspname = '" + schema + "'" + " and i.indisprimary = FALSE;";
    }

    protected boolean supportsPGCast() {
        return true;
    }

    private static final Pattern NOW_PTRN = Pattern.compile("\\(?'now'::text\\)?(::timestamp)");

    @Override
    public String transfDefaultJDBC2SQL(SQLField f) {
        if (f.getDefaultValue() != null && Date.class.isAssignableFrom(f.getType().getJavaType())) {
            // pg returns ('now'::text)::timestamp without time zone for CURRENT_TIMESTAMP
            // replace() handles complex defaults, e.g. now + '00:00:10'::interval
            return NOW_PTRN.matcher(f.getDefaultValue().toString()).replaceAll("CURRENT_TIMESTAMP$1");
        } else {
            return super.transfDefaultJDBC2SQL(f);
        }
    }

    @Override
    public void _loadData(final File f, final SQLTable t) throws IOException, SQLException {
        final String copy = "COPY " + t.getSQLName().quote() + " FROM STDIN " + getDataOptions(t.getBase()) + ";";
        final Number count = t.getDBSystemRoot().getDataSource().useConnection(new ConnectionHandlerNoSetup<Number, IOException>() {
            @Override
            public Number handle(SQLDataSource ds) throws SQLException, IOException {
                FileInputStream in = null;
                try {
                    in = new FileInputStream(f);
                    final Connection conn = ((DelegatingConnection) ds.getConnection()).getInnermostDelegate();
                    return ((PGConnection) conn).getCopyAPI().copyIn(copy, in);
                } finally {
                    if (in != null)
                        in.close();
                }
            }
        });

        final SQLName seq = FixSerial.getPrimaryKeySeq(t);
        // no need to alter sequence if nothing was inserted (can be -1 in old pg)
        // also avoid NULL for empty tables and thus arbitrary start constant
        if (count.intValue() != 0 && seq != null) {
            t.getDBSystemRoot().getDataSource().execute(t.getBase().quote("select %n.\"alterSeq\"( %s, 'select max(%n)+1 from %f');", t.getDBRoot(), seq, t.getKey(), t));
        }
    }

    private static String getDataOptions(final SQLBase b) {
        return " WITH NULL " + b.quoteString("\\N") + " CSV HEADER QUOTE " + b.quoteString("\"") + " ESCAPE AS " + b.quoteString("\\");
    }

    @Override
    protected void _storeData(final SQLTable t, final File f) throws IOException {
        // if there's no fields, there's no data
        if (t.getFields().size() == 0)
            return;

        final String cols = CollectionUtils.join(t.getOrderedFields(), ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField f) {
                return SQLBase.quoteIdentifier(f.getName());
            }
        });
        // you can't specify line separator to pg, so use STDOUT as it always use \n
        try {
            final String sql = "COPY (" + selectAll(t).asString() + ") to STDOUT " + getDataOptions(t.getBase()) + " FORCE QUOTE " + cols + " ;";
            t.getDBSystemRoot().getDataSource().useConnection(new ConnectionHandlerNoSetup<Number, IOException>() {
                @Override
                public Number handle(SQLDataSource ds) throws SQLException, IOException {
                    final Connection conn = ((DelegatingConnection) ds.getConnection()).getInnermostDelegate();
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(f);
                        return ((PGConnection) conn).getCopyAPI().copyOut(sql, out);
                    } finally {
                        if (out != null)
                            out.close();
                    }
                }
            });
        } catch (Exception e) {
            throw new IOException("unable to store " + t + " into " + f, e);
        }
    }

    static SQLSelect selectAll(final SQLTable t) {
        final SQLSelect sel = new SQLSelect(true);
        for (final SQLField field : t.getOrderedFields()) {
            // MySQL despite accepting 'boolean', 'true' and 'false' keywords doesn't really
            // support booleans
            if (field.getType().getJavaType() == Boolean.class)
                sel.addRawSelect("cast(" + field.getFieldRef() + " as integer)", field.getName());
            else
                sel.addSelect(field);
        }
        return sel;
    }

    @Override
    public String getChar(int asciiCode) {
        return "chr(" + asciiCode + ")";
    }

    @Override
    public String getRegexpOp(boolean negation) {
        return negation ? "!~" : "~";
    }

    @Override
    public String getFormatTimestamp(String sqlTS, boolean basic) {
        return "to_char(cast(" + sqlTS + " as timestamp), " + SQLBase.quoteStringStd(basic ? "YYYYMMDD\"T\"HH24MISS.US" : "YYYY-MM-DD\"T\"HH24:MI:SS.US") + ")";
    }

    @Override
    public SQLBase createBase(SQLServer server, String name, final IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        return new PGSQLBase(server, name, systemRootInit, login, pass, dsInit);
    }

    @Override
    public final String getCreateSynonym(final SQLTable t, final SQLName newName) {
        String res = super.getCreateSynonym(t, newName);

        // in postgresql 8.3 views are not updatable, need to write rules
        final List<SQLField> fields = t.getOrderedFields();
        final List<String> setL = new ArrayList<String>(fields.size());
        final List<String> insFieldsL = new ArrayList<String>(fields.size());
        final List<String> insValuesL = new ArrayList<String>(fields.size());
        for (final SQLField f : fields) {
            final String name = t.getBase().quote("%n", f);
            final String newDotName = t.getBase().quote("NEW.%n", f, f);
            // don't add isAuto to ins
            if (!this.isAuto(f)) {
                insFieldsL.add(name);
                insValuesL.add(newDotName);
            }
            setL.add(name + " = " + newDotName);
        }
        final String set = "set " + CollectionUtils.join(setL, ", ");
        final String insFields = "(" + CollectionUtils.join(insFieldsL, ", ") + ") ";
        final String insValues = "VALUES(" + CollectionUtils.join(insValuesL, ", ") + ") ";

        // rule names are unique by table
        res += t.getBase().quote("CREATE or REPLACE RULE \"_updView_\" AS ON UPDATE TO %i\n" + "DO INSTEAD UPDATE %f \n" + set + "where %n=OLD.%n\n" + "RETURNING %f.*;", newName, t, t.getKey(),
                t.getKey(), t);
        res += t.getBase().quote("CREATE or REPLACE RULE \"_delView_\" AS ON DELETE TO %i\n" + "DO INSTEAD DELETE FROM %f \n where %n=OLD.%n\n" + "RETURNING %f.*;", newName, t, t.getKey(),
                t.getKey(), t);
        res += t.getBase().quote("CREATE or REPLACE RULE \"_insView_\" AS ON INSERT TO %i\n" + "DO INSTEAD INSERT INTO %f" + insFields + " " + insValues + "RETURNING %f.*;", newName, t, t);

        return res;
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        return "SELECT ROUTINE_SCHEMA as \"schema\", ROUTINE_NAME as \"name\", ROUTINE_DEFINITION as \"src\" FROM \"information_schema\".ROUTINES where ROUTINE_CATALOG='" + b.getMDName()
                + "' and ROUTINE_SCHEMA in (" + quoteStrings(b, schemas) + ")";
    }

    @Override
    public String getTriggerQuery(SQLBase b, TablesMap tables) throws SQLException {
        return "SELECT tgname as \"TRIGGER_NAME\", n.nspname as \"TABLE_SCHEMA\", c.relname as \"TABLE_NAME\", tgfoid as \"ACTION\", pg_get_triggerdef(t.oid) as \"SQL\" \n" +
        // from
                "FROM pg_catalog.pg_trigger t\n" +
                // table
                "INNER JOIN pg_catalog.pg_class c on t.tgrelid = c.oid\n" +
                // schema
                "INNER JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" +
                // requested tables
                getTablesMapJoin(b, tables, "n.nspname", "c.relname") +
                // where
                "\nwhere not t." + (b.getVersion()[0] >= 9 ? "tgisinternal" : "tgisconstraint");
    }

    @Override
    public String getColumnsQuery(SQLBase b, TablesMap tables) {
        return "SELECT TABLE_SCHEMA as \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", TABLE_NAME as \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", COLUMN_NAME as \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , CHARACTER_SET_NAME as \"CHARACTER_SET_NAME\", COLLATION_NAME as \"COLLATION_NAME\" from INFORMATION_SCHEMA.COLUMNS\n" +
                // requested tables
                getTablesMapJoin(b, tables, "TABLE_SCHEMA", "TABLE_NAME");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConstraints(SQLBase b, TablesMap tables) throws SQLException {
        final String sel = "select nsp.nspname as \"TABLE_SCHEMA\", rel.relname as \"TABLE_NAME\", c.conname as \"CONSTRAINT_NAME\", c.oid as cid, \n"
                + "case c.contype when 'u' then 'UNIQUE' when 'c' then 'CHECK' when 'f' then 'FOREIGN KEY' when 'p' then 'PRIMARY KEY' end as \"CONSTRAINT_TYPE\", att.attname as \"COLUMN_NAME\", pg_get_constraintdef(c.oid) as \"DEFINITION\","
                + "c.conkey as \"colsNum\", att.attnum as \"colNum\"\n"
                // from
                + "from pg_catalog.pg_constraint c\n" + "join pg_namespace nsp on nsp.oid = c.connamespace\n" + "left join pg_class rel on rel.oid = c.conrelid\n"
                + "left join pg_attribute att on  att.attrelid = c.conrelid and att.attnum = ANY(c.conkey)\n"
                // requested tables
                + getTablesMapJoin(b, tables, "nsp.nspname", "rel.relname")
                // order
                + "\norder by nsp.nspname, rel.relname, c.conname";
        // don't cache since we don't listen on system tables
        final List<Map<String, Object>> res = sort((List<Map<String, Object>>) b.getDBSystemRoot().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false)));
        // ATTN c.conkey are not column indexes since dropped attribute are not deleted
        // so we must join pg_attribute to find out column names
        SQLSyntaxMySQL.mergeColumnNames(res);
        return res;
    }

    // pg has no ORDINAL_POSITION and no indexOf() function (except in contrib) so we can't ORDER
    // BY in SQL, we have to do it in java
    private List<Map<String, Object>> sort(final List<Map<String, Object>> sortedByConstraint) {
        final List<Map<String, Object>> res = new ArrayList<Map<String, Object>>(sortedByConstraint.size());
        final Comparator<Map<String, Object>> comp = new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return CompareUtils.compareInt(getIndex(o1), getIndex(o2));
            }

            // index of the passed column in the constraint
            private final int getIndex(Map<String, Object> o) {
                final int colNum = ((Number) o.get("colNum")).intValue();
                try {
                    final Integer[] array = (Integer[]) ((Array) o.get("colsNum")).getArray();
                    for (int i = 0; i < array.length; i++) {
                        if (array[i].intValue() == colNum)
                            return i;
                    }
                    throw new IllegalStateException(colNum + " was not found in " + Arrays.toString(array));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // use the oid of pg to identify constraints (otherwise we'd have to compare the fully
        // qualified name of the constraint)
        int prevID = -1;
        final List<Map<String, Object>> currentConstr = new ArrayList<Map<String, Object>>();
        for (final Map<String, Object> m : sortedByConstraint) {
            final int currentID = ((Number) m.get("cid")).intValue();
            // at each change of constraint, sort its columns
            if (currentConstr.size() > 0 && currentID != prevID) {
                res.addAll(sort(currentConstr, comp));
                currentConstr.clear();
            }
            currentConstr.add(m);
            prevID = currentID;
        }
        res.addAll(sort(currentConstr, comp));

        return res;
    }

    private final List<Map<String, Object>> sort(List<Map<String, Object>> currentConstr, final Comparator<Map<String, Object>> comp) {
        Collections.sort(currentConstr, comp);
        for (int i = 0; i < currentConstr.size(); i++) {
            currentConstr.get(i).put("ORDINAL_POSITION", i + 1);
            // remove columns only needed to sort
            currentConstr.get(i).remove("cid");
            currentConstr.get(i).remove("colNum");
            currentConstr.get(i).remove("colsNum");
        }
        return currentConstr;
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return "DROP TRIGGER " + SQLBase.quoteIdentifier(t.getName()) + " on " + t.getTable().getSQLName().quote();
    }
}
