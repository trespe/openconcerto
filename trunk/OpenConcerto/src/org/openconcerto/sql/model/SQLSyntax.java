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

import static org.openconcerto.utils.CollectionUtils.join;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.OutsideClause;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.NetUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * A class that abstract the syntax of different SQL systems. Type is an SQL datatype like 'int' or
 * 'varchar', definition is the type plus default and constraints like 'int default 1 not null
 * unique'.
 * 
 * @author Sylvain
 * 
 */
public abstract class SQLSyntax {

    static public final String ORDER_NAME = "ORDRE";
    static public final String ARCHIVE_NAME = "ARCHIVE";
    static public final String ID_NAME = "ID";
    static private final Map<SQLSystem, SQLSyntax> instances = new HashMap<SQLSystem, SQLSyntax>();
    static public final String DATA_EXT = ".txt";

    static protected final String TS_EXTENDED_JAVA_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS000";
    static protected final String TS_BASIC_JAVA_FORMAT = "yyyyMMdd'T'HHmmss.SSS000";

    static public enum ConstraintType {
        CHECK, FOREIGN_KEY("FOREIGN KEY"), PRIMARY_KEY("PRIMARY KEY"), UNIQUE;

        private final String sqlName;

        private ConstraintType() {
            this(null);
        }

        private ConstraintType(final String n) {
            this.sqlName = n == null ? name() : n;
        }

        public final String getSqlName() {
            return this.sqlName;
        }

        static public ConstraintType find(final String sqlName) {
            for (final ConstraintType c : values())
                if (c.getSqlName().equals(sqlName))
                    return c;
            throw new IllegalArgumentException("Unknown type: " + sqlName);
        }
    }

    static {
        register(new SQLSyntaxPG());
        register(new SQLSyntaxH2());
        register(new SQLSyntaxMySQL());
        register(new SQLSyntaxMS());
    }

    static private void register(SQLSyntax s) {
        instances.put(s.getSystem(), s);
    }

    public final static SQLSyntax get(DBStructureItem<?> sql) {
        return get(sql.getServer().getSQLSystem());
    }

    public final static SQLSyntax get(SQLSystem system) {
        final SQLSyntax res = instances.get(system);
        if (res == null) {
            throw new IllegalArgumentException("unsupported system: " + system);
        }
        return res;
    }

    private final SQLSystem sys;
    protected final CollectionMap<Class, String> typeNames;

    protected SQLSyntax(final SQLSystem sys) {
        this.sys = sys;
        this.typeNames = new CollectionMap<Class, String>(new HashSet<String>(4));
    }

    /**
     * The set of aliases for a particular type.
     * 
     * @param clazz the type, e.g. Integer.class.
     * @return the SQL aliases, e.g. {"integer", "int", "int4"}.
     */
    public final Set<String> getTypeNames(Class clazz) {
        return (Set<String>) this.typeNames.getNonNull(clazz);
    }

    public final SQLSystem getSystem() {
        return this.sys;
    }

    public String getInitRoot(final String name) {
        // by default: nothing
        return "";
    }

    public abstract boolean isAuto(SQLField f);

    public abstract String getAuto();

    public String getIDType() {
        return " int";
    }

    /**
     * A non null primary int key with a default value, without 'PRIMARY KEY'. MySQL needs this when
     * changing a primary column (otherwise: "multiple primary keys").
     * 
     * @return the corresponding definition.
     */
    public String getPrimaryIDDefinitionShort() {
        return this.getAuto();
    }

    /**
     * A non null primary int key with a default value.
     * 
     * @return the corresponding definition.
     */
    public final String getPrimaryIDDefinition() {
        return this.getPrimaryIDDefinitionShort() + " PRIMARY KEY";
    }

    public String getArchiveType() {
        return " int";
    }

    public String getArchiveDefinition() {
        return this.getArchiveType() + " DEFAULT 0 NOT NULL";
    }

    public final String getOrderType() {
        return " DECIMAL(" + getOrderPrecision() + "," + getOrderScale() + ")";
    }

    public final int getOrderPrecision() {
        return 16;
    }

    public final int getOrderScale() {
        return 8;
    }

    public final Object getOrderDefault() {
        return null;
    }

    public final boolean isOrder(final SQLField f) {
        final SQLType type = f.getType();
        if (type.getType() != Types.DECIMAL && type.getType() != Types.NUMERIC)
            return false;
        if (type.getSize() != getOrderPrecision() || ((Number) type.getDecimalDigits()).intValue() != getOrderScale())
            return false;
        // do not check UNIQUE since it might require re-order

        return f.isNullable() && f.getDefaultValue() == getOrderDefault();
    }

    public final String getOrderDefinition() {
        return this.getOrderType() + " DEFAULT " + getOrderDefault() + " UNIQUE";
    }

    /**
     * How to declare a foreign key constraint.
     * 
     * @param constraintPrefix to be prepended to the constraint name, can be empty.
     * @param fk the name of the foreign keys, eg ["ID_SITE"].
     * @param refTable the name of the referenced table, eg CTech.SITE.
     * @param referencedFields the fields in the foreign table, eg ["ID"].
     * @param updateRule the update rule, <code>null</code> means use DB default.
     * @param deleteRule the delete rule, <code>null</code> means use DB default.
     * @return a String declaring that <code>fk</code> points to <code>referencedFields</code>.
     */
    public String getFK(final String constraintPrefix, final List<String> fk, final SQLName refTable, final List<String> referencedFields, final Rule updateRule, final Rule deleteRule) {
        final String onUpdate = updateRule == null ? "" : " ON UPDATE " + getRuleSQL(updateRule);
        final String onDelete = deleteRule == null ? "" : " ON DELETE " + getRuleSQL(deleteRule);
        // a prefix for the constraint name, since in psql constraints are db wide not table wide
        return SQLSelect.quote("CONSTRAINT %i FOREIGN KEY ( " + quoteIdentifiers(fk) + " ) REFERENCES %i ", constraintPrefix + join(fk, "__") + "_fkey", refTable) + "( "
        // don't put ON DELETE CASCADE since it's dangerous, plus MS SQL only supports 1 fk with
        // cascade : http://support.microsoft.com/kb/321843/en-us
                + quoteIdentifiers(referencedFields) + " )" + onUpdate + onDelete;
    }

    protected String getRuleSQL(final Rule r) {
        return r.asString();
    }

    public String getDropFK() {
        return getDropConstraint();
    }

    // to drop a constraint that is not a foreign key, eg unique
    public String getDropConstraint() {
        return "DROP CONSTRAINT ";
    }

    public String getDropPrimaryKey(SQLTable t) {
        return "DROP PRIMARY KEY";
    }

    public abstract String getDropIndex(String name, SQLName tableName);

    public String getCreateIndex(final String indexSuffix, final SQLName tableName, final List<String> fields) {
        // a prefix for the name, since in psql index are schema wide not table wide
        return getCreateIndex(false, tableName.getName() + "_" + join(fields, "__") + indexSuffix, tableName, fields);
    }

    public final String getCreateIndex(final boolean unique, final String indexName, final SQLName tableName, final List<String> fields) {
        // cannot use getCreateIndex(Index i) since Index needs an SQLTable
        final String res = "CREATE" + (unique ? " UNIQUE" : "") + " INDEX " + SQLBase.quoteIdentifier(indexName) + " ON " + tableName.quote();
        return res + " (" + quoteIdentifiers(fields) + ");";
    }

    public List<OutsideClause> getCreateIndexes(final SQLTable t, final IPredicate<Index> pred) throws SQLException {
        final List<Index> indexes = t.getIndexes();
        final List<OutsideClause> res = new ArrayList<OutsideClause>(indexes.size());
        for (final Index i : indexes) {
            if (pred == null || pred.evaluateChecked(i))
                res.add(getCreateIndex(i));
        }
        return res;
    }

    public final OutsideClause getCreateIndex(final Index i) {
        return new OutsideClause() {

            @Override
            public ClauseType getType() {
                return ClauseType.ADD_INDEX;
            }

            @Override
            public String asString(final SQLName tableName) {
                // mysql indexes are by table (eg ORDRE INT UNIQUE is called just "ORDRE"),
                // but pg needs names unique in the schema, so make the index start by the
                // tablename
                final String indexName = getSchemaUniqueName(tableName.getName(), i.getName());
                String res = "CREATE" + (i.isUnique() ? " UNIQUE" : "") + " INDEX " + SQLBase.quoteIdentifier(indexName) + " ";
                final String exprs = join(i.getAttrs(), ", ", new ITransformer<String, String>() {
                    @Override
                    public String transformChecked(String attr) {
                        if (i.getTable().contains(attr))
                            return SQLBase.quoteIdentifier(attr);
                        else
                            // eg lower("field")
                            return attr;
                    }
                });
                res += getCreateIndex("(" + exprs + ")", tableName, i);
                // filter condition or warning if this doesn't support it
                final boolean supported;
                if (i.getFilter() != null && i.getFilter().length() > 0) {
                    res += " WHERE " + i.getFilter();
                    supported = getSystem().isIndexFilterConditionSupported();
                } else {
                    supported = true;
                }
                res += ";";
                if (!supported) {
                    res = "-- filter condition not supported\n-- " + res;
                    Log.get().warning(res);
                }
                return res;
            }
        };
    }

    /**
     * Just the part after "CREATE UNIQUE INDEX foo ".
     * 
     * @param cols the columns of <code>i</code>, since all systems agree avoid duplication, eg
     *        ("f1", "field2").
     * @param tableName the table where the index should be created, eg "root"."t".
     * @param i the index, do not use its table, use <code>tableName</code>.
     * @return the part after "CREATE UNIQUE INDEX foo ".
     */
    protected String getCreateIndex(final String cols, final SQLName tableName, Index i) {
        return i.getTable().getBase().quote("ON %i" + cols, tableName);
    }

    /**
     * Something to be appended to CREATE TABLE statements, like "ENGINE = InnoDB".
     * 
     * @return a String that need to be appended to CREATE TABLE statements.
     */
    public String getCreateTableSuffix() {
        return "";
    }

    public final String getFieldDecl(SQLField f) {
        String res = "";
        final SQLSyntax fs = SQLSyntax.get(f.getServer().getSQLSystem());
        if (fs.isAuto(f))
            res += this.getAuto();
        else {
            final String sqlType = getType(f);
            final String sqlDefault = getDefault(f, sqlType);
            final boolean nullable = getNullable(f);

            res += getFieldDecl(sqlType, sqlDefault, nullable);
        }
        return res;
    }

    public final String getFieldDecl(final String sqlType, final String sqlDefault, final boolean nullable) {
        return sqlType + getDefaultClause(sqlDefault) + getNullableClause(nullable);
    }

    protected final boolean getNullable(SQLField f) {
        // if nullable == null, act like nullable
        return !Boolean.FALSE.equals(f.isNullable());
    }

    public final String getNullableClause(boolean nullable) {
        return nullable ? " " : " NOT NULL ";
    }

    /**
     * The default value for the passed field.
     * 
     * @param f the field.
     * @return the default SQL value, eg "0".
     */
    protected final String getDefault(SQLField f) {
        return this.getDefault(f, getType(f));
    }

    protected final String getDefault(SQLField f, final String sqlType) {
        final SQLSyntax fs = f.getServer().getSQLSystem().getSyntax();
        final String stdDefault = fs.transfDefaultSQL2Common(f);
        if (stdDefault == null || !this.supportsDefault(sqlType))
            return null;
        else {
            // for the field date default '2008-12-30'
            // pg will report a default value of '2008-12-30'::date
            // for the field date default '2008-12-30'::date
            // h2 will report a default value of DATE '2008-12-30'
            // to make comparisons possible we thus remove the unnecessary cast
            final String castless;
            final Tuple2<Boolean, String> cast = fs.getCast();
            if (cast == null)
                castless = stdDefault;
            else
                castless = remove(stdDefault, fs.getTypeNames(f.getType().getJavaType()), cast.get0(), cast.get1());
            return this.transfDefault(f, castless);
        }
    }

    // find a cast with one of the passed strings and remove it
    // e.g. remove("'a'::varchar", ["char", "varchar"], false, "::") yields 'a'
    private static String remove(final String s, final Collection<String> substrings, final boolean leading, final String sep) {
        final String lowerS = s.toLowerCase();
        String typeCast = null;
        for (final String syn : substrings) {
            typeCast = syn.toLowerCase();
            if (leading)
                typeCast = typeCast + sep;
            else
                typeCast = sep + typeCast;
            if (leading ? lowerS.startsWith(typeCast) : lowerS.endsWith(typeCast)) {
                break;
            } else
                typeCast = null;
        }

        if (typeCast == null)
            return s;
        else if (leading)
            return s.substring(typeCast.length());
        else
            return s.substring(0, s.length() - typeCast.length());
    }

    /**
     * Get the default clause.
     * 
     * @param def the default, e.g. "0".
     * @return the default clause, e.g. "DEFAULT 0".
     */
    public final String getDefaultClause(final String def) {
        if (def == null)
            return " ";
        else
            return " DEFAULT " + def;
    }

    public final String getType(SQLField f) {
        final SQLSyntax fs = f.getServer().getSQLSystem().getSyntax();
        final SQLType t = f.getType();

        final String sqlType;
        final String typeName = t.getTypeName().toLowerCase();
        if (typeName.contains("clob")) {
            sqlType = "text";
        } else if (Date.class.isAssignableFrom(t.getJavaType())) {
            // allow getAutoDateType() to return null so that normal systems use normal code path
            // (e.g. to handle TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP)
            if (fs.isAutoDate(f) && this.getAutoDateType(f) != null)
                sqlType = this.getAutoDateType(f);
            else if (typeName.contains("datetime") || typeName.contains("timestamp"))
                sqlType = this.getDateAndTimeType() + (getSystem().isFractionalSecondsSupported() && t.getDecimalDigits() != null ? "(" + t.getDecimalDigits() + ")" : "");
            else
                sqlType = typeName;
        } else if (t.getJavaType() == String.class) {
            if (typeName.contains("text") || typeName.contains("clob")) {
                sqlType = "text";
            } else {
                final String type = typeName.contains("var") ? "varchar" : "char";
                final int size = t.getSize();
                if (size < Integer.MAX_VALUE) {
                    sqlType = type + "(" + size + ")";
                } else {
                    Log.get().warning("Unbounded varchar for " + f.getSQLName());
                    if (this.getSystem() == SQLSystem.MYSQL)
                        throw new IllegalStateException("MySQL doesn't support unbounded varchar and might truncate data if reducing size of " + f.getSQLName());
                    // don't specify size
                    sqlType = type;
                }
            }
        } else if (t.getJavaType() == BigDecimal.class) {
            sqlType = "DECIMAL(" + t.getSize() + "," + t.getDecimalDigits() + ")";
        } else if (t.getJavaType() == Boolean.class) {
            sqlType = getBooleanType();
        } else if (Number.class.isAssignableFrom(t.getJavaType())) {
            if (Double.class.isAssignableFrom(t.getJavaType())) {
                // this is the standard name (the only one accepted by pg)
                sqlType = "double precision";
            } else if (Float.class.isAssignableFrom(t.getJavaType())) {
                // MySQL needs REAL_AS_FLOAT mode (e.g. from ANSI)
                sqlType = "real";
            } else {
                // always remove unsigned they're a pain to maintain across systems
                // use standard SQL types
                sqlType = typeName.replace("unsigned", "").replace("int2", "smallint").replace("int4", "int").replace("int8", "bigint").trim();
            }
        } else {
            sqlType = typeName;
        }
        return sqlType;
    }

    private boolean isAutoDate(SQLField f) {
        if (f.getDefaultValue() == null)
            return false;

        final String def = ((String) f.getDefaultValue()).toLowerCase();
        return Date.class.isAssignableFrom(f.getType().getJavaType()) && (def.contains("now") || def.contains("current_"));
    }

    /**
     * The date type that support a default value, since some systems don't support defaults for all
     * their types. This implementation simply returns <code>null</code>.
     * 
     * @param f the source field.
     * @return the type that support a default value, <code>null</code> to avoid special treatment.
     */
    protected String getAutoDateType(SQLField f) {
        return null;
    }

    /**
     * The type that store both the date and time. This implementation return the SQL standard
     * "timestamp".
     * 
     * @return the type that store both the date and time.
     */
    public String getDateAndTimeType() {
        return "timestamp";
    }

    public String getBooleanType() {
        return "boolean";
    }

    protected boolean supportsDefault(String sqlType) {
        return true;
    }

    /**
     * Should transform the passed "common" default to its corresponding value in this syntax. This
     * implementation returns the passed argument.
     * 
     * @param f the field the default is for.
     * @param castless the common default without a cast, e.g. TRUE.
     * @return the default useable in this, e.g. 'true' or 1.
     */
    protected String transfDefault(SQLField f, final String castless) {
        return castless;
    }

    private static final Set<String> nonStandardTimeFunctions = CollectionUtils.createSet("now()", "transaction_timestamp()", "current_timestamp()");
    /** list of columns identifying a field in the resultSet from information_schema.COLUMNS */
    public static final List<String> INFO_SCHEMA_NAMES_KEYS = Arrays.asList("TABLE_SCHEMA", "TABLE_NAME", "COLUMN_NAME");

    // tries to transform SQL to a dialect that all systems can understand
    private String transfDefaultSQL2Common(SQLField f) {
        final String defaultVal = this.transfDefaultJDBC2SQL(f);
        if (defaultVal != null && Date.class.isAssignableFrom(f.getType().getJavaType()) && nonStandardTimeFunctions.contains(defaultVal.trim().toLowerCase()))
            return "CURRENT_TIMESTAMP";
        else if (defaultVal != null && Boolean.class.isAssignableFrom(f.getType().getJavaType()))
            return defaultVal.toUpperCase();
        else
            return defaultVal;
    }

    public String transfDefaultJDBC2SQL(SQLField f) {
        return (String) f.getDefaultValue();
    }

    /**
     * How casts are written. E.g. if this returns [true, " "] casts look like "integer 4".
     * 
     * @return whether type is written before the value and what string is put between type and
     *         value, <code>null</code> if the syntax do no use casts.
     */
    protected abstract Tuple2<Boolean, String> getCast();

    // JDBC says: ordered by NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION
    public List<Map<String, Object>> getIndexInfo(final SQLTable t) throws SQLException {
        final List<?> indexesInfo = t.getDBSystemRoot().getDataSource().useConnection(new ConnectionHandlerNoSetup<List<?>, SQLException>() {
            @Override
            public List<?> handle(SQLDataSource ds) throws SQLException {
                return (List<?>) SQLDataSource.MAP_LIST_HANDLER.handle(ds.getConnection().getMetaData().getIndexInfo(t.getBase().getMDName(), t.getSchema().getName(), t.getName(), false, false));
            }
        });
        final List<Map<String, Object>> res = new ArrayList<Map<String, Object>>(indexesInfo.size());
        for (final Object o : indexesInfo) {
            final Map<?, ?> m = (Map<?, ?>) o;
            // ignore all null rows ; some systems (e.g. MySQL) return a string instead of short
            if (!String.valueOf(DatabaseMetaData.tableIndexStatistic).equals(m.get("TYPE").toString()))
                res.add(this.normalizeIndexInfo(m));
        }
        return res;
    }

    /**
     * Convert the map returned by JDBC getIndexInfo() to a normalized form. Currently the map
     * returned must have :
     * <dl>
     * <dt>NON_UNIQUE</dt>
     * <dd>Boolean</dd>
     * <dt>COLUMN_NAME</dt>
     * <dd>Non quoted string, eg "ch amp"</dd>
     * </dl>
     * 
     * @param m the values returned by
     *        {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)}.
     * @return a normalized map.
     */
    protected Map<String, Object> normalizeIndexInfo(final Map<?, ?> m) {
        throw new UnsupportedOperationException();
    }

    // copy the passed map and set all keys to upper case
    // (pg returns lower case)
    protected final Map<String, Object> copyIndexInfoMap(final Map<?, ?> m) {
        final Map<String, Object> res = new HashMap<String, Object>(m.size());
        for (final Entry<?, ?> e : m.entrySet())
            res.put(((String) e.getKey()).toUpperCase(), e.getValue());
        return res;
    }

    public abstract String disableFKChecks(DBRoot b);

    public abstract String enableFKChecks(DBRoot b);

    /**
     * Alter clause to change the default.
     * 
     * @param field the field to change.
     * @param defaut the new default value.
     * @return the SQL clause.
     */
    protected final String setDefault(SQLField field, String defaut) {
        if (defaut == null)
            return SQLSelect.quote("ALTER %n DROP DEFAULT", field);
        else
            return SQLSelect.quote("ALTER COLUMN %n SET DEFAULT " + defaut, field);
    }

    /**
     * Alter clauses to transform <code>f</code> into <code>from</code>.
     * 
     * @param f the field to change.
     * @param from the field to copy.
     * @param toTake which properties of <code>from</code> to copy.
     * @return the SQL clauses.
     */
    public final List<String> getAlterField(SQLField f, SQLField from, Set<Properties> toTake) {
        if (toTake.size() == 0)
            return Collections.emptyList();

        final Boolean nullable = toTake.contains(Properties.NULLABLE) ? getNullable(from) : null;
        final String newType;
        if (toTake.contains(Properties.TYPE))
            newType = getType(from);
        // type needed by getDefault()
        else if (toTake.contains(Properties.DEFAULT))
            newType = getType(f);
        else
            newType = null;
        final String newDef = toTake.contains(Properties.DEFAULT) ? getDefault(from, newType) : null;

        return getAlterField(f, toTake, newType, newDef, nullable);
    }

    // cannot rename since some systems won't allow it in the same ALTER TABLE
    public abstract List<String> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable);

    public String getDecimal(int precision, int scale) {
        return " DECIMAL(" + precision + "," + scale + ")";
    }

    public final String getDecimalIntPart(int intPart, int fractionalPart) {
        return getDecimal(intPart + fractionalPart, fractionalPart);
    }

    public abstract String getDropRoot(String name);

    public abstract String getCreateRoot(String name);

    /**
     * Load data from files.
     * 
     * @param dir the directory where the files are located.
     * @param r the root where to load.
     * @param tableNames the tables to load or <code>null</code> to load all files in
     *        <code>dir</code>.
     * @param delete <code>true</code> if tables should be emptied before loading.
     * @throws IOException if an error occurs while reading the files.
     * @throws SQLException if an error occurs while loading data into the database.
     */
    public final void loadData(final File dir, final DBRoot r, final Set<String> tableNames, final boolean delete) throws IOException, SQLException {
        final List<Tuple2<File, SQLTable>> tables = new ArrayList<Tuple2<File, SQLTable>>();
        if (tableNames == null) {
            for (final File f : dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isFile() && f.getName().toLowerCase().endsWith(DATA_EXT);
                }
            })) {
                final String tableName = f.getName().substring(0, f.getName().length() - DATA_EXT.length());
                final SQLTable t = r.getTable(tableName);
                if (t == null)
                    Log.get().warning("table " + tableName + " doesn't exist in " + r);
                else
                    tables.add(Tuple2.create(f, t));
            }
        } else {
            for (final String tableName : tableNames) {
                final File f = new File(dir, tableName + DATA_EXT);
                if (f.exists())
                    tables.add(Tuple2.create(f, r.getTable(tableName)));
                else
                    Log.get().warning(f.getAbsolutePath() + " doesn't exist");
            }
        }
        // only run at the end to avoid being stopped while loading
        r.getBase().getDataSource().execute(disableFKChecks(r));
        for (final Tuple2<File, SQLTable> t : tables)
            loadData(t.get0(), t.get1(), delete, Level.INFO);
        r.getBase().getDataSource().execute(enableFKChecks(r));
    }

    public final void loadData(final File f, final SQLTable t) throws IOException, SQLException {
        this.loadData(f, t, false);
    }

    public final void loadData(final File f, final SQLTable t, final boolean delete) throws IOException, SQLException {
        this.loadData(f, t, delete, null);
    }

    public final void loadData(final File f, final SQLTable t, final boolean delete, final Level level) throws IOException, SQLException {
        if (level != null)
            Log.get().log(level, "loading " + f + " into " + t.getSQLName() + "... ");
        if (delete)
            t.getBase().getDataSource().execute("DELETE FROM " + t.getSQLName().quote());
        _loadData(f, t);
        t.fireTableModified(SQLRow.NONEXISTANT_ID);
        if (level != null)
            Log.get().log(level, "done loading " + f);
    }

    protected abstract void _loadData(File f, SQLTable t) throws IOException, SQLException;

    /**
     * Dump the rows of <code>r</code> to <code>dir</code>. One file per table, named tableName
     * {@link #DATA_EXT} in CSV format (field sep: ",", field delimiter: "\"", line sep: "\n") with
     * the column names on the first line.
     * 
     * @param r the root to dump.
     * @param dir where to dump it.
     * @throws IllegalArgumentException if the server and this jvm aren't on the same machine.
     */
    public final void storeData(final DBRoot r, final File dir) {
        this.storeData(r, null, dir);
    }

    public final void storeData(final DBRoot r, final Set<String> tableNames, final File dir) {
        dir.mkdirs();
        final Map<String, SQLTable> tables = new TreeMap<String, SQLTable>(r.getTablesMap());
        if (tableNames != null)
            tables.keySet().retainAll(tableNames);
        for (final SQLTable t : tables.values()) {
            _storeData(t, new File(dir, t.getName() + DATA_EXT));
        }
    }

    public final void storeData(SQLTable t, File f) {
        this._storeData(t, f);
    }

    protected abstract void _storeData(SQLTable t, File f);

    /**
     * Whether the passed server runs on this machine.
     * 
     * @param s the server to test.
     * @return <code>true</code> if this jvm runs on the same machine than <code>s</code>.
     */
    protected boolean isServerLocalhost(SQLServer s) {
        return NetUtils.isSelfAddr(s.getName());
    }

    protected final void checkServerLocalhost(DBStructureItem<?> t) {
        if (!this.isServerLocalhost(t.getServer()))
            throw new IllegalArgumentException("the server of " + t + " is not this computer: " + t.getServer());
    }

    SQLBase createBase(SQLServer server, String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        return new SQLBase(server, name, login, pass, dsInit);
    }

    /**
     * The function to return the character with the given ASCII code.
     * 
     * @param asciiCode the code, eg 92 for '\\'.
     * @return the sql function, eg char(92).
     */
    public String getChar(int asciiCode) {
        return "char(" + asciiCode + ")";
    }

    /**
     * The SQL operator to concatenate strings. This returns the standard ||.
     * 
     * @return the cat operator.
     */
    public String getConcatOp() {
        return "||";
    }

    public final String getRegexpOp() {
        return this.getRegexpOp(false);
    }

    /**
     * The SQL operator to match POSIX regular expressions.
     * 
     * @param negation <code>true</code> to negate.
     * @return the regexp operator, <code>null</code> if not supported.
     * @see <a
     *      href="http://www.postgresql.org/docs/9.1/static/functions-matching.html#FUNCTIONS-POSIX-REGEXP">postgresql</a>
     */
    public String getRegexpOp(final boolean negation) {
        return negation ? "NOT REGEXP" : "REGEXP";
    }

    /**
     * The SQL needed to create a synonym of <code>t</code> named <code>newName</code>. This can be
     * implemented by updatable views. ATTN for systems using views many restrictions apply (eg no
     * keys, no defaults...).
     * 
     * @param t a table.
     * @param newName the name of the synonym.
     * @return the SQL needed or <code>null</code> if this system doesn't support it.
     */
    public String getCreateSynonym(final SQLTable t, final SQLName newName) {
        return t.getBase().quote("create view %i as select * from %f;", newName, t);
    }

    /**
     * Whether we can put several clauses in one "ALTER TABLE".
     * 
     * @return <code>true</code> if this system supports multiple clauses.
     */
    public boolean supportMultiAlterClause() {
        return true;
    }

    /**
     * Return the SQL clause to compare x and y treating NULL as data.
     * 
     * @param x an sql expression, eg "someField".
     * @param eq <code>true</code> if <code>x</code> and <code>y</code> should be equal, eg
     *        <code>false</code>.
     * @param y an sql expression, eg "1".
     * @return the corresponding clause, eg "someField is distinct from 1".
     */
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        return x + (eq ? " IS NOT DISTINCT FROM " : " IS DISTINCT FROM ") + y;
    }

    public final String getFormatTimestamp(final Timestamp ts, final boolean basic) {
        return this.getFormatTimestamp(SQLBase.quoteStringStd(ts.toString()), basic);
    }

    /**
     * Return the SQL function that format a time stamp to a complete representation. The
     * {@link SimpleDateFormat format} is {@value #TS_EXTENDED_JAVA_FORMAT} : microseconds and no
     * time zone (only format supported by all systems).
     * <p>
     * NOTE : from ISO 8601:2004(E) §4.2.2.4 the decimal sign is included even in basic format.
     * </p>
     * 
     * @param sqlTS an SQL expression of type time stamp.
     * @param basic <code>true</code> if the format should be basic, i.e. with the minimum number of
     *        characters ; <code>false</code> if additional separators must be added (more legible).
     * @return the SQL needed to format the passed parameter.
     */
    public abstract String getFormatTimestamp(final String sqlTS, final boolean basic);

    public final String getInsertOne(final SQLName tableName, final List<String> fields, String... values) {
        return this.getInsertOne(tableName, fields, Arrays.asList(values));
    }

    public final String getInsertOne(final SQLName tableName, final List<String> fields, final List<String> values) {
        return getInsert(tableName, fields, Collections.singletonList(values));
    }

    public final String getInsert(final SQLName tableName, final List<String> fields, final List<List<String>> values) {
        return "INSERT INTO " + tableName.quote() + "(" + quoteIdentifiers(fields) + ") " + getValues(values, fields.size());
    }

    public final String getValues(final List<List<String>> rows) {
        return this.getValues(rows, -1);
    }

    /**
     * Create a VALUES expression.
     * 
     * @param rows the rows with the SQL expression for each cell.
     * @param colCount the number of columns the rows must have, -1 meaning infer it from
     *        <code>rows</code>.
     * @return the VALUES expression, e.g. "VALUES (1, 'one'), (2, 'two'), (3, 'three')".
     */
    public final String getValues(final List<List<String>> rows, int colCount) {
        final int rowCount = rows.size();
        if (rowCount < 1)
            throw new IllegalArgumentException("Empty rows will cause a syntax error");
        if (colCount < 0)
            colCount = rows.get(0).size();
        final StringBuilder sb = new StringBuilder(rowCount * 64);
        final char space = rowCount > 6 ? '\n' : ' ';
        sb.append("VALUES");
        sb.append(space);
        for (final List<String> row : rows) {
            if (row.size() != colCount)
                throw new IllegalArgumentException("Row have wrong size, not " + colCount + " : " + row);
            sb.append("(");
            sb.append(CollectionUtils.join(row, ", "));
            sb.append("),");
            sb.append(space);
        }
        // remove last ", "
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    /**
     * Get a constant table usable as a join.
     * 
     * @param rows the SQL values for the table, e.g. [["1", "'one'"], ["2", "'two'"]].
     * @param alias the table alias, e.g. "t".
     * @param columnsAlias the columns aliases.
     * @return a constant table, e.g. ( VALUES (1, 'one'), (2, 'two') ) as "t" ("n", "name").
     */
    public String getConstantTable(final List<List<String>> rows, final String alias, final List<String> columnsAlias) {
        final int colSize = columnsAlias.size();
        if (colSize < 1)
            throw new IllegalArgumentException("Empty columns will cause a syntax error");
        final StringBuilder sb = new StringBuilder(rows.size() * 64);
        sb.append("( ");
        sb.append(getValues(rows, colSize));
        sb.append(" ) as ");
        sb.append(SQLBase.quoteIdentifier(alias));
        sb.append(" (");
        for (final String colAlias : columnsAlias) {
            sb.append(SQLBase.quoteIdentifier(colAlias));
            sb.append(", ");
        }
        // remove last ", "
        sb.setLength(sb.length() - 2);
        sb.append(")");
        return sb.toString();
    }

    protected final String getTablesMapJoin(final SQLBase b, final TablesMap tables, final String schemaExpr, final String tableExpr) {
        final List<List<String>> rows = new ArrayList<List<String>>();
        for (final Entry<String, Set<String>> e : tables.entrySet()) {
            final String schemaName = b.quoteString(e.getKey());
            if (e.getValue() == null) {
                rows.add(Arrays.asList(schemaName, "NULL"));
            } else {
                for (final String tableName : e.getValue())
                    rows.add(Arrays.asList(schemaName, b.quoteString(tableName)));
            }
        }
        final String tableAlias = "tables";
        final SQLName schemaName = new SQLName(tableAlias, "schema");
        final SQLName tableName = new SQLName(tableAlias, "table");

        final String schemaWhere = schemaExpr + " = " + schemaName.quote();
        final String tableWhere = "(" + tableName.quote() + " is null or " + tableExpr + " = " + tableName.quote() + ")";
        return "INNER JOIN " + getConstantTable(rows, tableAlias, Arrays.asList(schemaName.getName(), tableName.getName())) + " on " + schemaWhere + " and " + tableWhere;
    }

    /**
     * A query to retrieve columns metadata from INFORMATION_SCHEMA. The result must have at least
     * {@link #INFO_SCHEMA_NAMES_KEYS}.
     * 
     * @param b the base.
     * @param tables the tables by schemas names.
     * @return the query to retrieve information about columns.
     */
    public abstract String getColumnsQuery(SQLBase b, TablesMap tables);

    /**
     * Return the query to find the functions. The result must have 3 columns : schema, name and src
     * (this should provide the most information possible, eg just the body, the complete SQL or
     * <code>null</code> if nothing can be found).
     * 
     * @param b the base.
     * @param schemas the schemas we're interested in.
     * @return the query or <code>null</code> if no information can be retrieved.
     */
    public abstract String getFunctionQuery(SQLBase b, Set<String> schemas);

    /**
     * Return the constraints in the passed tables.
     * 
     * @param b the base.
     * @param tables the tables by schemas names.
     * @return a list of map with at least "TABLE_SCHEMA", "TABLE_NAME", "CONSTRAINT_NAME",
     *         "CONSTRAINT_TYPE" and (List of String)"COLUMN_NAMES" keys.
     * @throws SQLException if an error occurs.
     */
    public abstract List<Map<String, Object>> getConstraints(SQLBase b, TablesMap tables) throws SQLException;

    protected static final String quoteStrings(final SQLBase b, Collection<String> c) {
        return CollectionUtils.join(c, ", ", new ITransformer<String, String>() {
            @Override
            public String transformChecked(String s) {
                return b.quoteString(s);
            }
        });
    }

    public static final String quoteIdentifiers(Collection<String> c) {
        return join(c, ", ", new ITransformer<String, String>() {
            @Override
            public String transformChecked(String s) {
                return SQLBase.quoteIdentifier(s);
            }
        });
    }

    public static final String getSchemaUniqueName(final String tableName, final String name) {
        return name.startsWith(tableName) ? name : tableName + "_" + name;
    }

    /**
     * A query to retrieve triggers in the passed schemas and tables. The result must have at least
     * TRIGGER_NAME, TABLE_SCHEMA, TABLE_NAME, ACTION (system dependant, eg "NEW.F = true") and SQL
     * (the SQL needed to create the trigger, can be <code>null</code>).
     * 
     * @param b the base.
     * @param tables the tables by schemas names.
     * @return the query to retrieve triggers.
     * @throws SQLException if an error occurs.
     */
    public abstract String getTriggerQuery(SQLBase b, TablesMap tables) throws SQLException;

    public abstract String getDropTrigger(Trigger t);

    /**
     * The part of an UPDATE query specifying tables and fields to update.
     * 
     * @param t the table whose fields will change.
     * @param tables the other tables of the update.
     * @param setPart the fields of <code>t</code> and their values.
     * @return the SQL specifying how to set the fields.
     * @throws UnsupportedOperationException if this system doesn't support the passed update, eg
     *         multi-table.
     */
    public String getUpdate(SQLTable t, List<String> tables, Map<String, String> setPart) throws UnsupportedOperationException {
        if (tables.size() > 0)
            throw new UnsupportedOperationException();
        return t.getSQLName() + "\nSET " + CollectionUtils.join(setPart.entrySet(), ",\n", new ITransformer<Entry<String, String>, String>() {
            @Override
            public String transformChecked(Entry<String, String> input) {
                return input.getKey() + " = " + input.getValue();
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
                return "COMMENT ON TABLE " + tableName.quote() + " IS " + SQLBase.quoteStringStd(comment) + ";";
            }
        };
    }
}
