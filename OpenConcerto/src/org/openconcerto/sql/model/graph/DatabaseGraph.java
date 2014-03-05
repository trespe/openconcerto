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
 * DatabaseGraph created on 13 mai 2004
 */
package org.openconcerto.sql.model.graph;

import static org.openconcerto.xml.JDOMUtils.OUTPUTTER;
import static java.util.Collections.singletonList;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBFileCache;
import org.openconcerto.sql.model.DBItemFileCache;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.LoadingListener.GraphLoadingEvent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.ToRefreshSpec.ToRefreshActual;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.cc.IPredicate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.collections.CollectionUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedMultigraph;

/**
 * Le graphe de la base de donnée. Les noeuds étant des tables, les liens les relations de clefs
 * étrangères. Cette classe utilise SQLKey pour deduire les relations entre tables.
 * 
 * @author ILM Informatique 13 mai 2004
 * @see org.openconcerto.sql.model.graph.SQLKey
 */
@ThreadSafe
public class DatabaseGraph extends BaseGraph {

    /**
     * Whether to infer foreign constraints from fields' names.
     * 
     * @see SQLKey
     */
    public static final String INFER_FK = "org.openconcerto.sql.graph.inferFK";

    private static final String XML_VERSION = "20121024-1614";
    private static final String FILENAME = "graph.xml";

    // passedBase is the base that was passed for the catalog parameter of getImportedKeys() or
    // getExportedKeys()
    public static SQLTable getTableFromJDBCMetaData(final SQLBase passedBase, final String jdbcCat, final String jdbcSchem, final String jdbcName) {
        final SQLServer server = passedBase.getServer();

        final String correctedCat;
        // h2 doesn't support and jdbcCat is in upper case
        // postgresql returns null
        if (server.getSQLSystem().isInterBaseSupported()) {
            correctedCat = jdbcCat;
        } else {
            correctedCat = passedBase.getName();
        }

        final SQLBase base = server.getBase(correctedCat);
        final SQLSchema schema = base == null ? null : base.getSchema(jdbcSchem);
        if (schema == null)
            throw new IllegalStateException("Schema " + correctedCat + "." + jdbcSchem + " does not exist (probably filtered by DBSystemRoot.getRootsToMap())");
        final SQLTable res;
        if (server.getSQLSystem() == SQLSystem.MYSQL)
            // MySQL returns all lowercase foreignTableName, see Bug #18446 :
            // INFORMATION_SCHEMA.KEY_COLUMN_USAGE.REFERENCED_TABLE_NAME always lowercase
            res = getTableIgnoringCase(schema, jdbcName);
        else
            res = (SQLTable) schema.getCheckedChild(jdbcName);
        return res;
    }

    private final DBSystemRoot base;
    @GuardedBy("this")
    private Map<String, Set<String>> mappedFromFile;
    // cache
    @GuardedBy("this")
    private final Map<SQLTable, Set<Link>> foreignLinks = new HashMap<SQLTable, Set<Link>>();
    @GuardedBy("this")
    private final Map<List<SQLField>, Link> foreignLink = new HashMap<List<SQLField>, Link>();

    private final ThreadLocal<Integer> atomicRefreshDepth = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        };
    };
    private final ThreadLocal<ToRefreshSpec> atomicRefreshItems = new ThreadLocal<ToRefreshSpec>() {
        @Override
        protected ToRefreshSpec initialValue() {
            return new ToRefreshSpec();
        }
    };

    /**
     * Crée le graphe de la base passée.
     * 
     * @param root la base dont on veut le graphe.
     */
    public DatabaseGraph(final DBSystemRoot root) {
        super(new DirectedMultigraph<SQLTable, Link>(Link.class));
        this.base = root;
        this.mappedFromFile = null;
    }

    public final void refresh(final TablesMap tablesRefreshed, final boolean readCache) throws SQLException {
        if (inAtomicRefresh()) {
            this.atomicRefreshItems.get().add(tablesRefreshed, readCache);
        } else {
            refresh(new ToRefreshSpec().add(tablesRefreshed, readCache));
        }
    }

    private final void refresh(final ToRefreshSpec toRefresh) throws SQLException {
        synchronized (this.base.getTreeMutex()) {
            synchronized (this) {
                final GraphLoadingEvent evt = new GraphLoadingEvent(this.base).fireEvent();
                try {
                    // TODO limit to file permission
                    this.mappedFromFile = Collections.unmodifiableMap(AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, Set<String>>>() {
                        @Override
                        public Map<String, Set<String>> run() throws SQLException {
                            return mapTables(toRefresh);
                        }
                    }));
                } catch (PrivilegedActionException e) {
                    throw (SQLException) e.getCause();
                } finally {
                    evt.fireFinishingEvent();
                }
            }
        }
    }

    /**
     * Whether this thread is in an atomic refresh. NOTE: if <code>true</code> then the graph won't
     * be up to date.
     * 
     * @return <code>true</code> if this thread is in an atomic refresh.
     */
    public final boolean inAtomicRefresh() {
        return this.atomicRefreshDepth.get().intValue() > 0;
    }

    /**
     * Execute the passed Callable with only one refresh at the end. This method is reentrant.
     * 
     * @param callable what to do.
     * @return the result of <code>callable</code>.
     * @throws SQLException if an error occurs.
     */
    public final <V> V atomicRefresh(final Callable<V> callable) throws SQLException {
        final V res;
        this.atomicRefreshDepth.set(this.atomicRefreshDepth.get().intValue() + 1);
        // this method is useful for grouping multiple changes to the structure, so be sure to
        // prevent other threads from modifying and thus changing the graph
        synchronized (this.base.getTreeMutex()) {
            final int newVal;
            try {
                res = callable.call();
            } catch (Exception e) {
                throw new SQLException("Call failed", e);
            } finally {
                newVal = this.atomicRefreshDepth.get().intValue() - 1;
                this.atomicRefreshDepth.set(newVal);
                assert newVal >= 0;
            }
            if (newVal == 0) {
                final ToRefreshSpec itemsToRefresh = this.atomicRefreshItems.get();
                this.atomicRefreshItems.remove();
                this.atomicRefreshDepth.remove();
                // we need to call refresh() only once to avoid order and cycle issues
                refresh(itemsToRefresh);
            }
        }
        return res;
    }

    /**
     * The list of roots mapped from file.
     * 
     * @return list of roots and tables mapped from file.
     */
    synchronized final Map<String, Set<String>> getMappedFromFile() {
        return this.mappedFromFile;
    }

    private final SQLServer getServer() {
        return this.base.getAnc(SQLServer.class);
    }

    /**
     * Construit la carte des tables
     * 
     * @param toRefreshSpec the roots and tables to refresh.
     * @return roots and tables loaded from file.
     * @throws SQLException if an error occurs.
     */
    private synchronized Map<String, Set<String>> mapTables(final ToRefreshSpec toRefreshSpec) throws SQLException {
        assert Thread.holdsLock(this.base.getTreeMutex()) : "Cannot graph a changing object";
        Map<String, Set<String>> res = new TablesMap();

        final Set<SQLTable> currentTables = this.getAllTables();
        final ToRefreshActual toRefresh = toRefreshSpec.getActual(this.base, currentTables);
        // clear graph and add tables (vertices)
        {
            final Set<SQLTable> newTablesInScope = toRefresh.getNewTablesInScope();
            final Set<SQLTable> oldTablesInScope = toRefresh.getOldTablesInScope();
            // refresh all ?
            final boolean clearGraph = oldTablesInScope.equals(currentTables);

            // clear cache
            synchronized (this) {
                if (clearGraph) {
                    this.foreignLink.clear();
                    this.foreignLinks.clear();
                } else {
                    for (final Iterator<Entry<List<SQLField>, Link>> iter = this.foreignLink.entrySet().iterator(); iter.hasNext();) {
                        final Entry<List<SQLField>, Link> e = iter.next();
                        // don't use e.getValue() since it can be null
                        final SQLTable linkTable = e.getKey().get(0).getTable();
                        if (oldTablesInScope.contains(linkTable))
                            iter.remove();
                    }
                    for (final Iterator<Entry<SQLTable, Set<Link>>> iter = this.foreignLinks.entrySet().iterator(); iter.hasNext();) {
                        final Entry<SQLTable, Set<Link>> e = iter.next();
                        final SQLTable linkTable = e.getKey().getTable();
                        if (oldTablesInScope.contains(linkTable))
                            iter.remove();
                    }
                }
            }

            if (clearGraph) {
                this.getGraphP().removeAllVertices(oldTablesInScope);
                assert this.getGraphP().vertexSet().size() == 0 && this.getGraphP().edgeSet().size() == 0;
            } else {
                // Removing a vertex also removes edges, so check that we also refresh referent
                // tables otherwise they won't have any foreign links anymore which is wrong if
                // removedTable was just renamed
                // Also the cache is only cleared for tables in scope, meaning that the cache for
                // those referent tables will be incoherent with the actual graph
                final Collection<SQLTable> removedTables = org.openconcerto.utils.CollectionUtils.subtract(oldTablesInScope, newTablesInScope);
                for (final SQLTable removedTable : removedTables) {
                    final Set<SQLTable> referentTables = getReferentTables(removedTable);
                    // MAYBE add option to refresh needed tables instead of failing
                    if (!oldTablesInScope.containsAll(referentTables)) {
                        throw new IllegalStateException(removedTable + " has been removed but some of its referents won't be refreshed : "
                                + org.openconcerto.utils.CollectionUtils.subtract(referentTables, oldTablesInScope));
                    }
                }
                this.getGraphP().removeAllVertices(removedTables);

                // remove links that will be refreshed.
                final Set<Link> linksToRemove = new HashSet<Link>();
                for (final SQLTable t : org.openconcerto.utils.CollectionUtils.intersection(oldTablesInScope, newTablesInScope)) {
                    linksToRemove.addAll(this.getGraphP().outgoingEdgesOf(t));
                }
                this.getGraphP().removeAllEdges(linksToRemove);
            }

            // add new tables (and existing but it's OK graph vertices is a set)
            Graphs.addAllVertices(this.getGraphP(), newTablesInScope);
        }
        final TablesMap fromXML = toRefresh.getFromXML();
        final TablesMap fromJDBC = toRefresh.getFromJDBC();
        if (fromXML.size() > 0) {
            final DBItemFileCache dir = this.getFileCache();
            try {
                if (dir != null) {
                    Log.get().config("for mapping " + this + " trying xmls in " + dir);
                    final long t1 = System.currentTimeMillis();
                    res = this.mapFromXML(fromXML);
                    // remove what was loaded
                    fromXML.removeAll(res);
                    final long t2 = System.currentTimeMillis();
                    Log.get().config("XML took " + (t2 - t1) + "ms for mapping the graph of " + this.base.getName() + "." + res);
                }
            } catch (Exception e) {
                SQLBase.logCacheError(dir, e);
                this.deleteGraphFiles();
            }
            // add to JDBC what wasn't loaded
            fromJDBC.addAll(fromXML);
        }
        if (!fromJDBC.isEmpty()) {
            final long t1 = System.currentTimeMillis();
            for (final Entry<String, Set<String>> e : fromJDBC.entrySet()) {
                final String rootName = e.getKey();
                final Set<String> tableNames = e.getValue();
                final DBRoot r = this.base.getRoot(rootName);
                // first try to map the whole root at once
                if (!this.map(r, tableNames)) {
                    // if this isn't supported use standard JDBC
                    for (final String table : tableNames) {
                        this.map(r, table, null);
                    }
                }
                this.save(r);
            }
            final long t2 = System.currentTimeMillis();
            Log.get().config("JDBC took " + (t2 - t1) + "ms for mapping the graph of " + this.base + "." + fromJDBC);
        }
        return res;
    }

    private final void addLink(final List<SQLField> from, final List<SQLField> to, String foreignKeyName, Rule updateRule, Rule deleteRule) {
        addLink(new Link(from, to, foreignKeyName, updateRule, deleteRule));
    }

    private final void addLink(final Link l) {
        DirectedEdge.addEdge(this.getGraphP(), l);
    }

    private boolean map(final DBRoot r, final Set<String> tableNames) throws SQLException {
        // on PG test goes from 75ms to 18ms
        if (tableNames.size() <= 1)
            return false;

        if (r.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            this.map(r, null, tableNames);
            return true;
        } else {
            return false;
        }
    }

    private void map(final DBRoot r, final String tableName, final Set<String> tableNames) throws SQLException {
        // either we refresh the whole root and we must know which tables to use
        // or we refresh only one table and tableNames is useless
        assert tableName == null ^ tableNames == null;
        final CollectionMap<String, String> metadataFKs = new CollectionMap<String, String>(new HashSet<String>());
        final List importedKeys = this.base.getDataSource().useConnection(new ConnectionHandlerNoSetup<List, SQLException>() {
            @Override
            public List handle(final SQLDataSource ds) throws SQLException {
                final DatabaseMetaData metaData = ds.getConnection().getMetaData();
                return (List) SQLDataSource.ARRAY_LIST_HANDLER.handle(metaData.getImportedKeys(r.getBase().getMDName(), r.getSchema().getName(), tableName));
            }
        });
        // accumulators for multi-field foreign key
        final List<SQLField> from = new ArrayList<SQLField>();
        final List<SQLField> to = new ArrayList<SQLField>();
        Rule updateRule = null;
        Rule deleteRule = null;
        String name = null;
        final Iterator ikIter = importedKeys.iterator();
        while (ikIter.hasNext()) {
            final Object[] m = (Object[]) ikIter.next();

            // FKTABLE_SCHEM
            assert CompareUtils.equals(m[5], r.getSchema().getName());
            // FKTABLE_NAME
            final String fkTableName = (String) m[6];
            assert tableName == null || tableName.equals(fkTableName);
            if (tableNames != null && !tableNames.contains(fkTableName))
                continue;
            // not by name, postgresql returns lowercase
            // "FKCOLUMN_NAME"
            final String keyName = (String) m[7];
            // "KEY_SEQ"
            final short seq = ((Number) m[8]).shortValue();
            // "PKCOLUMN_NAME"
            final String foreignTableColName = (String) m[3];
            // "FK_NAME"
            final String foreignKeyName = (String) m[11];

            final SQLField key = r.getTable(fkTableName).getField(keyName);

            final SQLTable foreignTable;
            try {
                foreignTable = getTableFromJDBCMetaData(r.getBase(), (String) m[0], (String) m[1], (String) m[2]);
            } catch (Exception e) {
                throw new IllegalStateException("Could not find what " + key.getSQLName() + " references", e);
            }

            metadataFKs.put(fkTableName, keyName);
            if (seq == 1) {
                // if we start a new link add the current one
                if (from.size() > 0)
                    addLink(from, to, name, updateRule, deleteRule);
                from.clear();
                to.clear();
            }
            from.add(key);
            assert seq == 1 || from.get(from.size() - 2).getTable() == from.get(from.size() - 1).getTable();
            to.add(foreignTable.getField(foreignTableColName));
            assert seq == 1 || to.get(to.size() - 2).getTable() == to.get(to.size() - 1).getTable();

            final Rule prevUpdateRule = updateRule;
            final Rule prevDeleteRule = deleteRule;
            // "UPDATE_RULE"
            updateRule = Rule.fromShort(((Number) m[9]).shortValue());
            // "DELETE_RULE"
            deleteRule = Rule.fromShort(((Number) m[10]).shortValue());
            if (seq > 1) {
                if (prevUpdateRule != updateRule)
                    throw new IllegalStateException("Incoherent update rules " + prevUpdateRule + " != " + updateRule);
                if (prevDeleteRule != deleteRule)
                    throw new IllegalStateException("Incoherent delete rules " + prevDeleteRule + " != " + deleteRule);
            }

            name = foreignKeyName;
            // MAYBE DEFERRABILITY
        }
        if (from.size() > 0)
            addLink(from, to, name, updateRule, deleteRule);

        if (Boolean.getBoolean(INFER_FK)) {
            final Set<String> tables = tableName != null ? Collections.singleton(tableName) : tableNames;
            for (final String tableToInfer : tables) {
                final SQLTable table = r.getTable(tableToInfer);
                final Set<String> lexicalFKs = SQLKey.foreignKeys(table);
                // already done
                lexicalFKs.removeAll(metadataFKs.getNonNull(table.getName()));
                // MAYBE option to print out foreign keys w/o constraint
                for (final String keyName : lexicalFKs) {
                    final SQLField key = table.getField(keyName);
                    addLink(singletonList(key), singletonList(SQLKey.keyToTable(key).getKey()), null, null, null);
                }
            }
        }
    }

    static private final SQLTable getTableIgnoringCase(final SQLSchema s, String tablename) {
        final List<SQLTable> matchingTables = new ArrayList<SQLTable>(4);
        for (final String tname : s.getTableNames())
            if (tname.equalsIgnoreCase(tablename))
                matchingTables.add(s.getTable(tname));
        if (matchingTables.size() == 0)
            // this will throw an exception
            return (SQLTable) s.getCheckedChild(tablename);
        else if (matchingTables.size() == 1)
            return matchingTables.get(0);
        else
            throw new IllegalStateException("More than one table matches " + tablename + " : " + matchingTables);
    }

    // ** cache

    /**
     * Where xml dumps are saved, always <code>null</code> if {@link DBSystemRoot#useCache()} is
     * <code>false</code>.
     * 
     * @return the directory of xmls dumps, <code>null</code> if it can't be found.
     */
    private DBItemFileCache getFileCache() {
        final boolean useXML = this.base.useCache();
        final DBFileCache d = this.getServer().getFileCache();
        if (!useXML || d == null)
            return null;
        else {
            return d.getChild(this.base);
        }
    }

    private final File getRootFile(String root) {
        final DBItemFileCache saveDir = this.getFileCache();
        if (saveDir == null)
            return null;
        return getGraphFile(saveDir.getChild(root));
    }

    private final List<DBItemFileCache> getSavedCaches(boolean withFile) {
        final DBItemFileCache item = this.getFileCache();
        if (item == null)
            return Collections.emptyList();
        else {
            return item.getSavedDesc(DBRoot.class, withFile ? FILENAME : null);
        }
    }

    final void deleteGraphFiles() {
        for (final DBItemFileCache i : this.getSavedCaches(true)) {
            getGraphFile(i).delete();
        }
    }

    final void deleteGraphFile(String rootName) {
        getRootFile(rootName).delete();
    }

    private File getGraphFile(final DBItemFileCache i) {
        return i.getFile(FILENAME);
    }

    boolean save(final DBRoot r) {
        final String rootName = r.getName();
        final File rootFile = this.getRootFile(rootName);
        if (rootFile == null) {
            return false;
        } else {
            assert Thread.holdsLock(this.base.getTreeMutex()) : "Might save garbage if two threads open the same file";
            BufferedWriter pWriter = null;
            try {
                FileUtils.mkdir_p(rootFile.getParentFile());
                pWriter = FileUtils.createXMLWriter(rootFile);
                pWriter.write("<root codecVersion=\"");
                pWriter.write(XML_VERSION);
                pWriter.write("\"");
                SQLSchema.getVersionAttr(r.getSchema(), pWriter);
                pWriter.write(" >\n");
                for (final SQLTable t : r.getDescs(SQLTable.class)) {
                    final Set<Link> flinks = this.getForeignLinks(t);
                    // now that the atomic level is the table we must explicitly record which tables
                    // have no links (to differentiate from tables which we don't know of)
                    pWriter.write("<table name=\"");
                    pWriter.write(OUTPUTTER.escapeAttributeEntities(t.getName()));
                    pWriter.write("\">\n");
                    for (final Link l : flinks) {
                        l.toXML(pWriter);
                    }
                    pWriter.write("</table>\n");
                }
                pWriter.write("\n</root>\n");

                return true;
            } catch (Exception e) {
                Log.get().log(Level.WARNING, "unable to save files in " + rootFile, e);
                return false;
            } finally {
                if (pWriter != null) {
                    try {
                        pWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Loads all passed saved roots.
     * 
     * @param fromXML the roots and tables to refresh.
     * @return the root and tables names that were loaded.
     * @throws JDOMException if a file is not valid XML.
     * @throws IOException if a file content is not correct.
     */
    private TablesMap mapFromXML(final TablesMap fromXML) throws JDOMException, IOException {
        final TablesMap res = new TablesMap();
        for (final DBItemFileCache cache : getSavedCaches(true)) {
            final String rootName = cache.getName();
            if (!fromXML.containsKey(rootName))
                continue;
            final Document doc = new SAXBuilder().build(getGraphFile(cache));
            final String fileVersion = doc.getRootElement().getAttributeValue("codecVersion");
            if (!XML_VERSION.equals(fileVersion))
                throw new IOException("wrong format version, expected " + XML_VERSION + " got: " + fileVersion);
            // if the systemRoot doesn't contain the saved root, it means it is filtered (otherwise
            // it would have been erased) so we don't need to load it
            if (this.base.contains(rootName)) {
                final DBRoot r = (DBRoot) this.base.getCheckedChild(rootName);
                // checking version in XMLStructureSource is not enough since it can happen that
                // a DBFileCache is available before this but after the structure was loaded. In
                // this case no version check has been made and thus this file can be obsolete.
                final String xmlVersion = SQLSchema.getVersion(doc.getRootElement());
                final String actualVersion = r.getSchema().getVersion();
                if (!CompareUtils.equals(xmlVersion, actualVersion))
                    throw new IOException("wrong DB version, expected " + actualVersion + " got: " + xmlVersion);
                final Set<String> fromXMLTableNames = fromXML.get(rootName);
                for (final Object o : doc.getRootElement().getChildren()) {
                    final Element tableElem = (Element) o;
                    final SQLTable t = r.getTable(tableElem.getAttributeValue("name"));
                    if (fromXMLTableNames.contains(t.getName())) {
                        for (final Object lo : tableElem.getChildren()) {
                            final Element linkElem = (Element) lo;
                            addLink(Link.fromXML(t, linkElem));
                        }
                        // t was loaded (even if it had no links)
                        res.add(rootName, t.getName());
                    }
                }
                // rootName was loaded from XML (even if it had no tables)
                if (!res.containsKey(rootName))
                    res.put(rootName, Collections.<String> emptySet());
            }
        }
        return res;
    }

    // From + To

    /**
     * Renvoie tous les liens qui partent ou arrivent sur la table passée.
     * 
     * @param table la table dont on veut connaitre les liens.
     * @return tous les liens qui partent ou arrivent sur cette table.
     */
    public synchronized Set<Link> getAllLinks(SQLTable table) {
        if (table == null)
            throw new NullPointerException();
        return this.getGraphP().edgesOf(table);
    }

    public Set<Link> getLinks(SQLTable table, Direction dir) {
        if (table == null || dir == null)
            throw new NullPointerException();
        if (dir == Direction.ANY)
            return this.getAllLinks(table);
        else if (dir == Direction.REFERENT)
            return this.getReferentLinks(table);
        else
            return this.getForeignLinks(table);
    }

    public Set<Link> getLinks(SQLTable table, Direction dir, final IPredicate<? super Link> pred) {
        final Set<Link> allLinks = this.getLinks(table, dir);
        // don't create instance for nothing
        return pred == null || pred == IPredicate.truePredicate() ? allLinks : org.openconcerto.utils.CollectionUtils.select(allLinks, pred, new HashSet<Link>());
    }

    public Set<Link> getLinksWithOpposite(final SQLTable table, final Direction dir, final String oppositeTableName) {
        return this.getLinks(table, dir, oppositeTableName == null ? null : new Link.NamePredicate(table, oppositeTableName));
    }

    /**
     * Get a single link from/to <code>table</code>.
     * 
     * @param table an end of the link.
     * @param dir should the link start from or end at <code>table</code>.
     * @return the single link.
     * @throws IllegalStateException if not one and only one link matching.
     */
    public Link getLink(final SQLTable table, final Direction dir) {
        return this.getLink(table, dir, null);
    }

    public Link getLink(final SQLTable table, final Direction dir, final IPredicate<? super Link> pred) {
        return this.getLink(table, dir, pred, false);
    }

    public Link getLinkWithOpposite(final SQLTable table, final Direction dir, final String oppositeTableName, final boolean nullIfNone) {
        return this.getLink(table, dir, oppositeTableName == null ? null : new Link.NamePredicate(table, oppositeTableName), nullIfNone);
    }

    /**
     * Get a single link from/to <code>table</code>.
     * 
     * @param table an end of the link.
     * @param dir should the link start from or end at <code>table</code>.
     * @param pred to filter the links from/to <code>table</code>.
     * @param nullIfNone if <code>false</code> this method never returns <code>null</code>, if
     *        <code>true</code> it will return <code>null</code> if no link matches.
     * @throws IllegalStateException if not one and only one link matching.
     * @return the single link, or <code>null</code> if none matched and <code>nullIfNone</code> is
     *         true.
     */
    public Link getLink(final SQLTable table, final Direction dir, final IPredicate<? super Link> pred, final boolean nullIfNone) {
        final Set<Link> res = this.getLinks(table, dir, pred);
        if (res.size() > 1) {
            throw new IllegalStateException("More than one link : " + res);
        } else if (res.size() == 0) {
            if (nullIfNone)
                return null;
            else
                throw new IllegalStateException("No link");
        }
        return res.iterator().next();
    }

    Set<Link> getLinks(SQLTable table, Direction dir, final boolean onlyOne) {
        return this.getLinks(table, dir, onlyOne, null);
    }

    Set<Link> getLinks(SQLTable table, Direction dir, final boolean onlyOne, final IPredicate<? super Link> pred) {
        if (onlyOne)
            // don't want nullIfNone
            return Collections.singleton(this.getLink(table, dir, pred, false));
        else
            return this.getLinks(table, dir, pred);
    }

    // Foreign

    /**
     * Renvoie tous les liens qui partent de la table passée.
     * 
     * @param table la table dont on veut connaitre les liens.
     * @return tous les liens qui partent de cette table.
     */
    public synchronized Set<Link> getForeignLinks(SQLTable table) {
        Set<Link> res = this.foreignLinks.get(table);
        // works because res cannot be null
        if (res == null) {
            res = Collections.unmodifiableSet(this.getGraphP().outgoingEdgesOf(table));
            this.foreignLinks.put(table, res);
        }
        return res;
    }

    public Set<SQLField> getForeignKeys(SQLTable table) {
        return getLabels(this.getForeignLinks(table));
    }

    public final Set<List<SQLField>> getForeignKeysFields(SQLTable table) {
        return getCols(this.getForeignLinks(table));
    }

    /**
     * Return the link corresponding to the passed foreign field.
     * 
     * @param fk a foreign key.
     * @return the Link corresponding to the passed field, or <code>null</code> if it is not a
     *         foreign field.
     */
    public Link getForeignLink(SQLField fk) {
        return this.getForeignLink(singletonList(fk));
    }

    public Link getForeignLink(SQLTable t, List<String> fields) {
        final List<SQLField> fks = new ArrayList<SQLField>(fields.size());
        for (final String s : fields)
            fks.add(t.getField(s));
        return this.getForeignLink(fks);
    }

    public synchronized Link getForeignLink(final List<SQLField> fk) {
        if (fk.size() == 0)
            throw new IllegalArgumentException("empty list");
        // result can be null
        if (!this.foreignLink.containsKey(fk)) {
            this.foreignLink.put(fk, (Link) CollectionUtils.find(this.getForeignLinks(fk.get(0).getTable()), new LabelPredicate(fk)));
        }
        return this.foreignLink.get(fk);
    }

    /**
     * Retourne la table étrangère sur laquelle pointe le champ passé.
     * 
     * @param fk une clef étrangère.
     * @return la table sur laquelle pointe le champ.
     */
    public SQLTable getForeignTable(SQLField fk) {
        final Link l = this.getForeignLink(fk);
        if (l != null)
            return l.getTarget();
        else
            return null;
    }

    /**
     * Retourne les liens de t1 à t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la 2eme table.
     * @return les liens de t1 à t2.
     * @throws NullPointerException if either t1 or t2 is <code>null</code>.
     */
    public synchronized Set<Link> getForeignLinks(SQLTable t1, SQLTable t2) {
        if (t1 == null || t2 == null)
            throw new NullPointerException("t1: " + t1 + ", t2: " + t2);
        return this.getGraphP().getAllEdges(t1, t2);
    }

    public Set<SQLField> getForeignFields(SQLTable t1, SQLTable t2) {
        return getLabels(this.getForeignLinks(t1, t2));
    }

    // Referent

    /**
     * Renvoie tous les liens qui arrivent sur la table passée.
     * 
     * @param table la table dont on veut connaitre les liens.
     * @return tous les liens qui arrivent sur cette table.
     */
    public synchronized Set<Link> getReferentLinks(SQLTable table) {
        return this.getGraphP().incomingEdgesOf(table);
    }

    public Set<SQLField> getReferentKeys(SQLTable table) {
        return getLabels(this.getReferentLinks(table));
    }

    /**
     * Renvoie toutes les tables qui pointent sur la table passée.
     * 
     * @param table la table dont on veut connaitre les référentes.
     * @return toutes les tables qui pointent <code>table</code>.
     */
    public Set<SQLTable> getReferentTables(SQLTable table) {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final Link l : this.getReferentLinks(table))
            res.add(l.getSource());
        return res;
    }

    public Set<SQLTable> findReferentTables(SQLTable table, final String refTable, final List<String> refKeys) {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final Link l : this.getReferentLinks(table)) {
            if (l.getSource().getName().equals(refTable) && (refKeys.isEmpty() || l.getCols().equals(refKeys)))
                res.add(l.getSource());
        }
        return res;
    }

    public SQLTable findReferentTable(SQLTable table, final String refTable, final List<String> refKeys) {
        return org.openconcerto.utils.CollectionUtils.getSole(this.findReferentTables(table, refTable, refKeys));
    }

    /**
     * Find the only referent table named <code>refTable</code>.
     * 
     * @param table a table.
     * @param refTable a table name that should point to <code>table</code>.
     * @param refKeys the names of the fields from <code>refTable</code>, empty meaning don't use
     *        them to filter.
     * @return the only matching table or <code>null</code> (i.e. if there's none or more than one).
     * @see #findReferentTables(SQLTable, String, List)
     */
    public SQLTable findReferentTable(SQLTable table, final String refTable, final String... refKeys) {
        return this.findReferentTable(table, refTable, Arrays.asList(refKeys));
    }

    // Between

    /**
     * Retourne tous les liens entre t1 et t2. C'est à dire les liens qui partent de t1 pour t2 et
     * inversement.
     * 
     * @param t1 la premiere table.
     * @param t2 la 2eme table.
     * @return l'ensemble des liens directs entre les 2 tables.
     */
    public Set<Link> getLinks(SQLTable t1, SQLTable t2) {
        Set<Link> res = new HashSet<Link>(this.getForeignLinks(t1, t2));
        res.addAll(this.getForeignLinks(t2, t1));
        return res;
    }

    /**
     * Renvoie tous les champs entre t1 et t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la 2eme table.
     * @return l'ensemble des champs qui lient les 2 tables.
     * @see #getLinks(SQLTable, SQLTable)
     */
    public Set<SQLField> getFields(SQLTable t1, SQLTable t2) {
        return getLabels(this.getLinks(t1, t2));
    }

    // *** Wheres

    /**
     * Renvoie la clause WHERE pour faire la jointure en t1 et t2. Par exemple entre MISSION et
     * RAPPORT : <br/>
     * RAPPORT.ID_MISSION=MISSION.ID_MISSION OR MISSION.ID_RAPPORT_INITIAL=RAPPORT.ID_RAPPORT. Pour
     * un sous-ensemble des liens, utiliser {@link #getWhereClause(SQLTable, SQLTable, Set)}, pour
     * un seul champ {@link #getWhereClause(SQLField)}.
     * 
     * @param t1 la premiere table.
     * @param t2 la deuxieme table.
     * @return le OR de tous les liens entres les 2 tables.
     */
    public Where getWhereClause(TableRef t1, TableRef t2) {
        return this.getWhereClause(t1, t2, null);
    }

    public Where getWhereClause(final Step step) {
        return this.getWhereClause(step.getFrom(), step.getTo(), step);
    }

    /**
     * Renvoie la clause WHERE pour faire la jointure en t1 et t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la deuxieme table.
     * @param fields les liens à utiliser, <code>null</code> pour tous.
     * @return le OR des champs passés.
     */
    public Where getWhereClause(TableRef t1, TableRef t2, Step fields) {
        // OR car OBSERVATION.ID_ARTICLE_1,2,3
        return Where.or(this.getStraightWhereClause(t1, t2, fields));
    }

    // MAYBE allow to pass self reference links in both directions with a SetMap<Direction, Link>,
    // e.g. find both next and previous mission

    /**
     * Renvoie les clauses WHERE pour faire la jointure en t1 et t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la deuxieme table.
     * @param step les liens à utiliser, <code>null</code> pour tous.
     * @return les WHERE demandés.
     */
    public Set<Where> getStraightWhereClause(TableRef t1, TableRef t2, Step step) {
        if (step == null) {
            step = Step.create(t1.getTable(), t2.getTable());
        } else {
            if (t1 == null)
                t1 = step.getFrom();
            else if (step.getFrom() != t1.getTable())
                throw new IllegalArgumentException("step isn't from t1 " + step);
            if (t2 == null)
                t2 = step.getTo();
            else if (step.getTo() != t2.getTable())
                throw new IllegalArgumentException("step isn'ts to t2 " + step);
        }

        final Set<Where> res = new HashSet<Where>();
        for (final Link l : step.getLinks()) {
            final Direction dir = step.getDirection(l);
            res.add(getWhereClause(t1, t2, l, dir));
        }

        return res;
    }

    public Where getWhereClause(final Link l) {
        return getWhereClause(l.getSource(), l.getTarget(), l, Direction.FOREIGN);
    }

    /**
     * The where clause to join t1 to t2.
     * 
     * @param t1 the first table, can be <code>null</code>, e.g. <code>null</code>.
     * @param t2 the second table, can be <code>null</code>, e.g. "LOCAL loc".
     * @param l a link between <code>t1</code> and <code>t2</code>, e.g. LOCAL.ID_BATIMENT.
     * @param dir how to go through <code>l</code>, either {@link Direction#FOREIGN} or
     *        {@link Direction#REFERENT}, e.g. <code>REFERENT</code>.
     * @return the WHERE, e.g. loc.ID_BATIMENT = BATIMENT.ID.
     */
    public Where getWhereClause(final TableRef t1, final TableRef t2, final Link l, final Direction dir) {
        if (l == null)
            throw new NullPointerException("Null link");
        TableRef src, dest;
        if (dir == Direction.FOREIGN) {
            src = t1;
            dest = t2;
        } else if (dir == Direction.REFERENT) {
            src = t2;
            dest = t1;
        } else {
            throw new IllegalArgumentException("Invalid direction : " + dir);
        }
        if (src == null)
            src = l.getSource();
        else if (src.getTable() != l.getSource())
            throw new IllegalArgumentException("Wrong source table " + src.getTable() + " != " + l.getSource());
        if (dest == null)
            dest = l.getTarget();
        else if (dest.getTable() != l.getTarget())
            throw new IllegalArgumentException("Wrong target table " + dest.getTable() + " != " + l.getTarget());
        final Iterator<SQLField> primaryKeys = dest.getTable().getPrimaryKeys().iterator();
        Where w = null;
        for (final SQLField f : l.getFields()) {
            assert f.getTable() == src.getTable();
            final FieldRef f1 = src.getField(f.getName());
            final FieldRef f2 = dest.getField(primaryKeys.next().getName());
            w = new Where(f1, "=", f2).and(w);
        }
        assert w != null : "Empty fields for " + l;
        assert !primaryKeys.hasNext() : "Mismatch";
        return w;
    }

    /**
     * Renvoie la clause where pour faire la jointure suivant ce champ.
     * 
     * @param f un champ, eg RAPPORT_GENERE.ID_MISSION.
     * @return eg "RAPPORT_GENERE.ID_MISSION=MISSION.ID_MISSION".
     */
    public Where getWhereClause(SQLField f) {
        return new Where(f, "=", this.getForeignTable(f).getKey());
    }

    // *** Jointures

    public Where getJointure(Path p) {
        Where res = null;
        for (int i = 0; i < p.length(); i++) {
            res = this.getWhereClause(p.getStep(i)).and(res);
        }
        return res;
    }

    public Set<Where> getStraightJoin(final Path p) {
        if (p.length() == 0) {
            throw new IllegalArgumentException("Path empty");
        } else if (p.length() == 1) {
            final SQLTable previous = p.getTable(0);
            final SQLTable table = p.getTable(1);
            return this.getStraightWhereClause(previous, table, p.getStep(0));
        } else {
            final Set<Where> res = new HashSet<Where>();

            final Set<Where> wheres = this.getStraightJoin(p.justFirst());
            final Iterator<Where> wheresIter = wheres.iterator();
            while (wheresIter.hasNext()) {
                final Where where = wheresIter.next();
                final Iterator<Where> restIter = this.getStraightJoin(p.minusFirst()).iterator();
                while (restIter.hasNext()) {
                    final Where w = restIter.next();
                    res.add(where.and(w));
                }
            }
            return res;
        }
    }

    public Where getJointure(int ID, Path p) {
        return new Where(p.getFirst().getKey(), "=", ID).and(this.getJointure(p));
    }

    public synchronized GraFFF cloneForFilter(List<SQLTable> linksToRemove) {
        return GraFFF.create(this.getGraphP(), linksToRemove);
    }

    public synchronized GraFFF cloneForFilterKeep(Set<SQLField> linksToKeep) {
        return GraFFF.createKeep(this.getGraphP(), linksToKeep);
    }

    protected DirectedMultigraph<SQLTable, Link> getGraphP() {
        return (DirectedMultigraph<SQLTable, Link>) this.getGraph();
    }

    static public <C extends Collection<String>> C getNames(Collection<Link> links, C fields) {
        for (final Link l : links)
            fields.add(l.getLabel().getName());
        return fields;
    }

    static public Set<String> getNames(Collection<Link> links) {
        return getNames(links, new HashSet<String>());
    }

    /**
     * Add the labels of <code>links</code> to the passed collection.
     * 
     * @param <C> type of collection.
     * @param links a Collection of Link.
     * @param fields a collection where labels are added.
     * @return <code>fields</code>.
     */
    static public <C extends Collection<SQLField>> C getLabels(Collection<Link> links, C fields) {
        for (final Link l : links)
            fields.add(l.getLabel());
        return fields;
    }

    static public Set<SQLField> getLabels(Collection<Link> links) {
        return getLabels(links, new HashSet<SQLField>());
    }

    static public <C extends Collection<List<SQLField>>> C getCols(Collection<Link> links, C fields) {
        for (final Link l : links)
            fields.add(l.getFields());
        return fields;
    }

    static public Set<List<SQLField>> getCols(Collection<Link> links) {
        return getCols(links, new HashSet<List<SQLField>>());
    }

}
