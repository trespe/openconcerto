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
 * DataBase created on 4 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.StructureSource.PrechangeException;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.change.CollectionChangeEventCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Une base de donnée SQL. Une base est unique, pour obtenir une instance il faut passer par
 * SQLServer. Une base permet d'accéder aux tables qui la composent, ainsi qu'à son graphe.
 * 
 * @author ILM Informatique 4 mai 2004
 * @see org.openconcerto.sql.model.SQLServer#getBase(String)
 * @see #getTable(String)
 * @see #getGraph()
 */
public class SQLBase extends SQLIdentifier {

    /**
     * Boolean system property, if <code>true</code> then the structure and the graph of SQL base
     * will be loaded from XML instead of JDBC.
     */
    public static final String STRUCTURE_USE_XML = "org.openconcerto.sql.structure.useXML";
    /**
     * Boolean system property, if <code>true</code> then schemas and tables can be dropped,
     * otherwise {@link #fetchTables()} will throw an exception.
     */
    public static final String ALLOW_OBJECT_REMOVAL = "org.openconcerto.sql.identifier.allowRemoval";

    // null is a valid name (MySQL doesn't support schemas)
    private final Map<String, SQLSchema> schemas;
    private int[] dbVersion;

    /**
     * Crée une base dans <i>server </i> nommée <i>name </i>.
     * <p>
     * Note: ne pas utiliser ce constructeur, utiliser {@link SQLServer#getBase(String)}
     * </p>
     * 
     * @param server son serveur.
     * @param name son nom.
     * @param login the login.
     * @param pass the password.
     */
    SQLBase(SQLServer server, String name, String login, String pass) {
        this(server, name, login, pass, null);
    }

    /**
     * Creates a base in <i>server</i> named <i>name</i>.
     * <p>
     * Note: don't use this constructor, use {@link SQLServer#getBase(String)}
     * </p>
     * 
     * @param server its server.
     * @param name its name.
     * @param login the login.
     * @param pass the password.
     * @param dsInit to initialize the datasource before any request (eg setting jdbc properties),
     *        can be <code>null</code>.
     */
    SQLBase(SQLServer server, String name, String login, String pass, IClosure<SQLDataSource> dsInit) {
        super(server, name);
        if (name == null)
            throw new NullPointerException("null base");
        this.schemas = new HashMap<String, SQLSchema>();
        this.dbVersion = null;

        // if this is the systemRoot we must init the datasource to be able to loadTables()
        final DBSystemRoot sysRoot = this.getDBSystemRoot();
        if (sysRoot.getJDBC() == this)
            sysRoot.setDS(login, pass, dsInit);

        try {
            loadTables(null, true);
        } catch (SQLException e) {
            throw new IllegalStateException("could not load " + this, e);
        }
    }

    @Override
    protected void onDrop() {
        // allow schemas (and their descendants) to be gc'd even we aren't
        this.schemas.clear();
        super.onDrop();
    }

    private final Set<String> loadTables(final Set<String> childrenNames, boolean inCtor) throws SQLException {
        final DBItemFileCache dir = getFileCache();
        Map<String, SQLSchema> newStruct = null;
        if (dir != null) {
            try {
                Log.get().config("for mapping " + this + " trying xmls in " + dir);
                final long t1 = System.currentTimeMillis();
                // don't call refreshTables() with XML :
                // say you have one schema "s" and its file is missing or corrupted
                // refreshTables(XML) will drop it from our children
                // then we will call refreshTables(JDBC) and it will be re-added
                // => so we removed our child for nothing (firing unneeded events, rendering java
                // objects useless and possibly destroying the systemRoot path)
                final XMLStructureSource xmlStructSrc = new XMLStructureSource(this, childrenNames);
                xmlStructSrc.init();
                newStruct = xmlStructSrc.getNewStructure();
                final long t2 = System.currentTimeMillis();
                Log.get().config("XML took " + (t2 - t1) + "ms for mapping " + this.getName() + "." + xmlStructSrc.getSchemas());
            } catch (Exception e) {
                Log.get().info("invalid files in " + dir + "\n" + ExceptionUtils.getStackTrace(e));
                // delete all files not just structure, since every information about obsolete
                // schemas is obsolete
                // delete all schemas, otherwise if afterwards we load one file it might be valid
                // alone but we know that along with its siblings it's not
                dir.delete();
                if (!(e instanceof PrechangeException) && !inCtor) {
                    throw new IllegalStateException("could not load XMLs", e);
                }
                if (inCtor) {
                    // remove successfully created schemas and descendants
                    // no need to drop them since nobody holds a reference
                    this.schemas.clear();
                }
                // if it was a PrechangeException, schemas weren't changed
            }
        }

        final long t1 = System.currentTimeMillis();
        // always do the fetchTables() since XML do nothing anymore
        final JDBCStructureSource jdbcStructSrc = this.fetchTablesP(childrenNames, newStruct);
        final long t2 = System.currentTimeMillis();
        Log.get().config("JDBC took " + (t2 - t1) + "ms for mapping " + this.getName() + "." + jdbcStructSrc.getSchemas());
        return jdbcStructSrc.getSchemas();
    }

    public final void fetchTables() throws SQLException {
        this.fetchTables(null);
    }

    /**
     * Load the structure from JDBC.
     * 
     * @param childrenNames which children to refresh.
     * @throws SQLException if an error occurs.
     * @see DBSystemRoot#refetch(Set)
     */
    public void fetchTables(Set<String> childrenNames) throws SQLException {
        this.fetchTablesP(childrenNames, null);
    }

    // no need to clear the graph in the ctor
    private JDBCStructureSource fetchTablesP(Set<String> childrenNames, Map<String, SQLSchema> newStruct) throws SQLException {
        return this.refreshTables(new JDBCStructureSource(this, childrenNames, newStruct));
    }

    public final Set<String> loadTables() throws SQLException {
        return this.loadTables(null);
    }

    /**
     * Tries to load the structure from XMLs, if that fails fallback to JDBC.
     * 
     * @param childrenNames which children to refresh.
     * @return schemas loaded with JDBC.
     * @throws SQLException if an error occurs in JDBC.
     */
    public Set<String> loadTables(Set<String> childrenNames) throws SQLException {
        return this.loadTables(childrenNames, false);
    }

    synchronized private final <T extends Exception, S extends StructureSource<T>> S refreshTables(final S src) throws T {
        src.init();

        // refresh schemas
        final Set<String> newSchemas = src.getTotalSchemas();
        final Set<String> currentSchemas = src.getSchemasToRefresh();
        mustContain(this, newSchemas, currentSchemas, "schemas");
        // remove all schemas that are not there anymore
        for (final String schema : CollectionUtils.substract(currentSchemas, newSchemas)) {
            final CollectionChangeEventCreator c = this.createChildrenCreator();
            this.schemas.remove(schema).dropped();
            this.fireChildrenChanged(c);
        }
        // delete the saved schemas that we could have fetched, but haven't
        // (schemas that are not in scope are simply ignored, NOT deleted)
        for (final DBItemFileCache savedSchema : this.getSavedCaches(false)) {
            if (src.isInTotalScope(savedSchema.getName()) && !newSchemas.contains(savedSchema.getName())) {
                savedSchema.delete();
            }
        }
        // clearNonPersistent (will be recreated by fillTables())
        for (final String schema : CollectionUtils.inter(currentSchemas, newSchemas)) {
            this.getSchema(schema).clearNonPersistent();
        }
        // create the new ones
        for (final String schema : newSchemas) {
            this.createAndGetSchema(schema);
        }

        // refresh tables
        final Set<SQLName> newTableNames = src.getTotalTablesNames();
        final Set<SQLName> currentTables = src.getTablesToRefresh();
        // we can only add, cause instances of SQLTable are everywhere
        mustContain(this, newTableNames, currentTables, "tables");
        // remove dropped tables
        for (final SQLName tableName : CollectionUtils.substract(currentTables, newTableNames)) {
            final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
            s.rmTable(tableName.getName());
        }
        // clearNonPersistent
        for (final SQLName tableName : CollectionUtils.inter(newTableNames, currentTables)) {
            final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
            s.getTable(tableName.getName()).clearNonPersistent();
        }
        // create new table descendants (including empty tables)
        for (final SQLName tableName : CollectionUtils.substract(newTableNames, currentTables)) {
            final SQLSchema s = this.getSchema(tableName.getItemLenient(-2));
            s.addTable(tableName.getName());
        }

        // fill with columns
        src.fillTables();
        // if necessary create the metadata table and insert the version
        for (final String sn : src.getSchemas()) {
            final SQLSchema s = this.getSchema(sn);
            if (s.getVersion() == null)
                try {
                    s.updateVersion();
                } catch (SQLException e) {
                    // tant pis, les metadata ne sont pas nécessaires
                    e.printStackTrace();
                }
        }

        // don't signal our systemRoot if our server doesn't yet reference us,
        // otherwise the server will create another instance and enter an infinite loop
        // if the server doesn't reference us, putBase() will be called and it will do
        // #descendantsChanged()
        if (this.getServer().isCreated(this.getName()))
            this.getDBSystemRoot().descendantsChanged();
        src.save();
        return src;
    }

    static <T> void mustContain(final DBStructureItemJDBC c, final Set<T> newC, final Set<T> oldC, final String name) {
        if (Boolean.getBoolean(ALLOW_OBJECT_REMOVAL))
            return;

        final Set<T> diff = CollectionUtils.contains(newC, oldC);
        if (diff != null)
            throw new IllegalStateException("some " + name + " were removed in " + c + ": " + diff);
    }

    public final String getURL() {
        return this.getServer().getURL(this.getName());
    }

    /**
     * Return the field named <i>fieldName </i> in this base.
     * 
     * @param fieldName the fully qualified name of the field.
     * @return the matching field or null if none exists.
     */
    public SQLField getField(String fieldName) {
        String[] parts = fieldName.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException(fieldName + " is not a fully qualified name (like TABLE.FIELD_NAME).");
        }
        String table = parts[0];
        String field = parts[1];
        if (!this.containsTable(table))
            return null;
        else
            return this.getTable(table).getField(field);
    }

    /**
     * Return the table named <i>tablename </i> in this base.
     * 
     * @param tablename the name of the table.
     * @return the matching table or null if none exists.
     */
    public SQLTable getTable(String tablename) {
        return this.getTable(SQLName.parse(tablename));
    }

    public SQLTable getTable(SQLName n) {
        if (n.getItemCount() == 0 || n.getItemCount() > 2)
            throw new IllegalArgumentException("'" + n + "' is not a dotted tablename");

        if (n.getItemCount() == 1) {
            return this.findTable(n.getName());
        } else {
            final SQLSchema s = this.getSchema(n.getFirst());
            if (s == null)
                return null;
            else
                return s.getTable(n.getName());
        }
    }

    private SQLTable findTable(String name) {
        final DBRoot guessed = this.guessDBRoot();
        return guessed == null ? this.getDBSystemRoot().findTable(name) : guessed.findTable(name);
    }

    /**
     * Return whether this base contains the table.
     * 
     * @param tableName the name of the table.
     * @return true if the tableName exists.
     */
    public boolean containsTable(String tableName) {
        return contains(SQLName.parse(tableName));
    }

    private boolean contains(final SQLName n) {
        return this.getTable(n) != null;
    }

    /**
     * Return the tables in the default schema.
     * 
     * @return an unmodifiable Set of the tables' names.
     */
    public Set<String> getTableNames() {
        return this.getDefaultSchema().getTableNames();
    }

    /**
     * Return the tables in the default schema.
     * 
     * @return a Set of SQLTable.
     */
    public Set<SQLTable> getTables() {
        return this.getDefaultSchema().getTables();
    }

    // *** all*

    public Set<SQLName> getAllTableNames() {
        final Set<SQLName> res = new HashSet<SQLName>();
        for (final SQLTable t : this.getAllTables()) {
            res.add(t.getSQLName(this, false));
        }
        return res;
    }

    public Set<SQLTable> getAllTables() {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final SQLSchema s : this.getSchemas()) {
            res.addAll(s.getTables());
        }
        return res;
    }

    // *** schemas

    @Override
    public SQLIdentifier getChild(String name) {
        return this.getSchema(name);
    }

    @Override
    public Set<String> getChildrenNames() {
        return this.schemas.keySet();
    }

    public final Set<SQLSchema> getSchemas() {
        return new HashSet<SQLSchema>(this.schemas.values());
    }

    public final SQLSchema getSchema(String name) {
        return this.schemas.get(name);
    }

    /**
     * The current default schema.
     * 
     * @return the default schema or <code>null</code>.
     */
    final SQLSchema getDefaultSchema() {
        if (this.schemas.size() == 0) {
            return null;
        } else if (this.schemas.size() == 1) {
            return this.schemas.values().iterator().next();
        } else if (this.getServer().getSQLSystem().getLevel(DBRoot.class) == HierarchyLevel.SQLSCHEMA)
            return (SQLSchema) this.getDBSystemRoot().getDefaultRoot().getJDBC();
        else
            throw new IllegalStateException();
    }

    private SQLSchema createAndGetSchema(String name) {
        SQLSchema res = this.getSchema(name);
        if (res == null) {
            res = new SQLSchema(this, name);
            final CollectionChangeEventCreator c = this.createChildrenCreator();
            this.schemas.put(name, res);
            this.fireChildrenChanged(c);
        }
        return res;
    }

    public final DBRoot guessDBRoot() {
        if (this.getDBRoot() != null)
            return this.getDBRoot();
        else
            return this.getDBSystemRoot().getDefaultRoot();
    }

    public DatabaseGraph getGraph() {
        if (this.getDBRoot() == null)
            return this.getDBSystemRoot().getGraph();
        else
            return this.getDBRoot().getGraph();
    }

    /**
     * Vérifie l'intégrité de la base. C'est à dire que les clefs étrangères pointent sur des lignes
     * existantes. Cette méthode renvoie une Map dont les clefs sont les tables présentant des
     * inconsistences. Les valeurs de cette Map sont des List de SQLRow.
     * 
     * @return les inconsistences.
     * @see SQLTable#checkIntegrity()
     */
    public Map<SQLTable, List> checkIntegrity() {
        Map<SQLTable, List> inconsistencies = new HashMap<SQLTable, List>();
        for (final SQLTable table : this.getAllTables()) {
            List tableInc = table.checkIntegrity();
            if (tableInc.size() > 0)
                inconsistencies.put(table, tableInc);
        }
        return inconsistencies;
    }

    /**
     * Exécute la requête dans le contexte de cette base et retourne le résultat. Le résultat d'une
     * insertion étant les clefs auto-générées, eg le nouvel ID.
     * 
     * @deprecated use getDataSource()
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see java.sql.Statement#getGeneratedKeys()
     */
    public ResultSet execute(String query) {
        return this.getDataSource().executeRaw(query);
    }

    public SQLDataSource getDataSource() {
        return this.getDBSystemRoot().getDataSource();
    }

    /**
     * Retourne le champ nommé field, en s'assurant qu'il existe.
     * 
     * @param field le nom du champ voulu.
     * @return le champ correspondant, jamais <code>null</code>.
     * @throws IllegalArgumentException si field n'existe pas.
     */
    public SQLField getFieldChecked(String field) {
        SQLField f = this.getField(field);
        if (f == null) {
            throw new IllegalArgumentException(field + " n'existe pas dans la base " + this);
        }
        return f;
    }

    public String toString() {
        return this.getName();
    }

    // ** metadata

    // will throw an exn if SQLSchema.METADATA_TABLENAME does not exist
    String getFwkMetadata(String schema, String name) {
        final SQLName tableName = new SQLName(this.getName(), schema, SQLSchema.METADATA_TABLENAME);
        final String sel = SQLSelect.quote("SELECT \"VALUE\" FROM %i WHERE \"NAME\"= %s", tableName, name);
        // don't use cache since setFwkMetadata() might not have a SQLTable to fire
        return (String) this.getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));
    }

    public final String getMDName() {
        return this.getServer().getSQLSystem().getMDName(this.getName());
    }

    public int[] getVersion() throws SQLException {
        if (this.dbVersion == null) {
            this.dbVersion = this.getDataSource().useConnection(new ConnectionHandlerNoSetup<int[], SQLException>() {
                @Override
                public int[] handle(SQLDataSource ds) throws SQLException, SQLException {
                    final DatabaseMetaData md = ds.getConnection().getMetaData();
                    return new int[] { md.getDatabaseMajorVersion(), md.getDatabaseMinorVersion() };
                }
            });
        }
        return this.dbVersion;
    }

    // ** files

    private static final String FILENAME = "structure.xml";

    static final boolean isSaved(final SQLServer s, final String base, final String schema) {
        return s.getFileCache().getChild(base, schema).getFile(SQLBase.FILENAME).exists();
    }

    /**
     * Where xml dumps are saved, always <code>null</code> if "org.openconcerto.sql.structure.useXML" is
     * <code>false</code>.
     * 
     * @return the directory of xmls dumps, <code>null</code> if it can't be found.
     */
    private final DBItemFileCache getFileCache() {
        final boolean useXML = Boolean.getBoolean(STRUCTURE_USE_XML);
        final DBFileCache fileCache = this.getServer().getFileCache();
        if (!useXML || fileCache == null)
            return null;
        else {
            return fileCache.getChild(this.getName());
        }
    }

    final File getSchemaFile(String schema) {
        final DBItemFileCache item = this.getFileCache();
        if (item == null)
            return null;
        return item.getChild(schema).getFile(FILENAME);
    }

    private final List<DBItemFileCache> getSavedCaches(boolean withStruct) {
        final DBItemFileCache item = this.getFileCache();
        if (item == null)
            return Collections.emptyList();
        else {
            return item.getSavedDesc(SQLSchema.class, withStruct ? FILENAME : null);
        }
    }

    final List<String> getSavedSchemas() {
        final List<String> res = new ArrayList<String>();
        for (final DBItemFileCache schemaF : this.getSavedCaches(true)) {
            res.add(schemaF.getName());
        }
        return res;
    }

    final boolean isSaved(String schema) {
        return isSaved(this.getServer(), this.getName(), schema);
    }

    /**
     * Deletes all files containing information about this base's structure.
     */
    public void deleteStructureFiles() {
        for (final DBItemFileCache f : this.getSavedCaches(true)) {
            f.getFile(FILENAME).delete();
        }
    }

    boolean save(String schemaName) {
        final File schemaFile = this.getSchemaFile(schemaName);
        if (schemaFile == null)
            return false;
        else
            try {
                FileUtils.mkdir_p(schemaFile.getParentFile());
                final SQLSchema schema = this.getSchema(schemaName);
                PrintWriter pWriter = new PrintWriter(new FileOutputStream(schemaFile));
                pWriter.println("<root codecVersion=\"" + XMLStructureSource.version + "\" >\n" + schema.toXML() + "\n</root>");
                pWriter.close();

                return true;
            } catch (Exception e) {
                Log.get().warning("unable to save files in " + schemaFile + "\n" + ExceptionUtils.getStackTrace(e));
                return false;
            }
    }

    // *** quoting

    // * quote

    /**
     * Quote %-escaped parameters. %% : %, %s : {@link #quoteString(String)}, %i : an identifier
     * string, if it's a SQLName calls {@link SQLName#quote()} else {@link #quoteIdentifier(String)}
     * , %f or %n : respectively fullName and name of an SQLIdentifier of a DBStructureItem.
     * 
     * @param pattern a string with %, eg "SELECT * FROM %n where %f like '%%a%%'".
     * @param params the parameters, eg [ /TENSION/, |TENSION.LABEL| ].
     * @return pattern with % replaced, eg SELECT * FROM "TENSION" where "TENSION.LABEL" like '%a%'.
     */
    public final String quote(final String pattern, Object... params) {
        return quote(this, pattern, params);
    }

    public final static String quoteStd(final String pattern, Object... params) {
        return quote(null, pattern, params);
    }

    static private final Pattern percent = Pattern.compile("%.");

    private final static String quote(final SQLBase b, final String pattern, Object... params) {
        final Matcher m = percent.matcher(pattern);
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        int lastAppendPosition = 0;
        while (m.find()) {
            final String replacement;
            final char modifier = m.group().charAt(m.group().length() - 1);
            if (modifier == '%') {
                replacement = "%";
            } else {
                final Object param = params[i++];
                if (modifier == 's') {
                    replacement = quoteString(b, param.toString());
                } else if (modifier == 'i') {
                    if (param instanceof SQLName)
                        replacement = ((SQLName) param).quote();
                    else
                        replacement = quoteIdentifier(param.toString());
                } else {
                    final SQLIdentifier ident = (SQLIdentifier) ((DBStructureItem) param).getJDBC();
                    if (modifier == 'f') {
                        replacement = ident.getSQLName().quote();
                    } else if (modifier == 'n')
                        replacement = quoteIdentifier(ident.getName());
                    else
                        throw new IllegalArgumentException("unknown modifier: " + modifier);
                }
            }

            // do NOT use appendReplacement() (and appendTail()) since it parses \ and $
            // Append the intervening text
            sb.append(pattern.subSequence(lastAppendPosition, m.start()));
            // Append the match substitution
            sb.append(replacement);
            lastAppendPosition = m.end();
        }
        sb.append(pattern.substring(lastAppendPosition));
        return sb.toString();
    }

    // * quoteString

    /**
     * Quote an sql string specifically for this base.
     * 
     * @param s an arbitrary string, eg "salut\ l'ami".
     * @return the quoted form, eg "'salut\\ l''ami'".
     * @see #quoteStringStd(String)
     */
    public String quoteString(String s) {
        return quoteStringStd(s);
    }

    static private final Pattern singleQuote = Pattern.compile("'");

    /**
     * Quote an sql string the standard way. See section 4.1.2.1. String Constants of postgresql
     * documentation.
     * 
     * @param s an arbitrary string, eg "salut\ l'ami".
     * @return the quoted form, eg "'salut\ l''ami'".
     */
    public final static String quoteStringStd(String s) {
        return "'" + singleQuote.matcher(s).replaceAll("''") + "'";
    }

    public final static String quoteString(SQLBase b, String s) {
        return b == null ? quoteStringStd(s) : b.quoteString(s);
    }

    // * quoteIdentifier

    static private final Pattern doubleQuote = Pattern.compile("\"");

    /**
     * Quote a sql identifier to prevent it from being folded and allow any character.
     * 
     * @param identifier a SQL identifier, eg 'My"Table'.
     * @return the quoted form, eg '"My""Table"'.
     */
    public static final String quoteIdentifier(String identifier) {
        return '"' + doubleQuote.matcher(identifier).replaceAll("\"\"") + '"';
    }
}
