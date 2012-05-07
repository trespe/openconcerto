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
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.Set;

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

    private static final String XML_VERSION = "20120228-1810";
    private static final String FILENAME = "graph.xml";

    private final DBSystemRoot base;
    private final DBRoot context;
    private final List<String> mappedFromFile;
    // cache
    private final Map<SQLTable, Set<Link>> foreignLinks = new HashMap<SQLTable, Set<Link>>();
    private final Map<List<SQLField>, Link> foreignLink = new HashMap<List<SQLField>, Link>();

    /**
     * Crée le graphe de la base passée.
     * 
     * @param root la base dont on veut le graphe.
     * @throws SQLException if an error occurs.
     */
    public DatabaseGraph(DBSystemRoot root) throws SQLException {
        super(new DirectedMultigraph<SQLTable, Link>(Link.class));
        this.base = root;
        this.context = null;
        synchronized (root.getTreeMutex()) {
            this.mappedFromFile = Collections.unmodifiableList(this.mapTables());
        }
    }

    public DatabaseGraph(DatabaseGraph g, DBRoot root) {
        super(g.getGraphP());
        assert g.base == root.getDBSystemRoot();
        this.base = g.base;
        this.context = root;
        this.mappedFromFile = g.mappedFromFile;
    }

    /**
     * The list of roots mapped from file.
     * 
     * @return list of roots mapped from file.
     */
    final List<String> getMappedFromFile() {
        return this.mappedFromFile;
    }

    private final SQLServer getServer() {
        return this.base.getAnc(SQLServer.class);
    }

    private DBRoot getContext() {
        return this.context;
    }

    /**
     * Construit la carte des tables
     * 
     * @return roots loaded from file.
     * @throws SQLException if an error occurs.
     */
    private List<String> mapTables() throws SQLException {
        assert Thread.holdsLock(this.base.getTreeMutex()) : "Cannot graph a changing object";
        List<String> res = Collections.emptyList();
        final Set<SQLTable> tables = this.base.getDescs(SQLTable.class);
        Graphs.addAllVertices(this.getGraphP(), tables);
        final DBItemFileCache dir = this.getFileCache();
        List<String> childrenToFetch = new ArrayList<String>(this.base.getChildrenNames());
        try {
            if (dir != null) {
                Log.get().config("for mapping " + this + " trying xmls in " + dir);
                final long t1 = System.currentTimeMillis();
                res = this.mapFromXML();
                childrenToFetch.removeAll(res);
                final long t2 = System.currentTimeMillis();
                Log.get().config("XML took " + (t2 - t1) + "ms for mapping the graph of " + this.base.getName() + "." + res);
            }
        } catch (Exception e) {
            SQLBase.logCacheError(dir, e);
            this.deleteGraphFiles();
        }
        if (!childrenToFetch.isEmpty()) {
            final long t1 = System.currentTimeMillis();
            for (final String rootName : childrenToFetch) {
                final DBRoot r = this.base.getRoot(rootName);
                for (final SQLTable table : r.getDescs(SQLTable.class)) {
                    this.map(table);
                }
                this.save(r);
            }
            final long t2 = System.currentTimeMillis();
            Log.get().config("JDBC took " + (t2 - t1) + "ms for mapping the graph of " + this.base + "." + childrenToFetch);
        }
        return res;
    }

    private final void addLink(final List<SQLField> from, final List<SQLField> to, String foreignKeyName, Rule updateRule, Rule deleteRule) {
        addLink(new Link(from, to, foreignKeyName, updateRule, deleteRule));
    }

    private final void addLink(final Link l) {
        DirectedEdge.addEdge(this.getGraphP(), l);
    }

    private void map(final SQLTable table) throws SQLException {
        final Set<String> metadataFKs = new HashSet<String>();
        final List importedKeys = this.base.getDataSource().useConnection(new ConnectionHandlerNoSetup<List, SQLException>() {
            @Override
            public List handle(final SQLDataSource ds) throws SQLException {
                final DatabaseMetaData metaData = ds.getConnection().getMetaData();
                return (List) SQLDataSource.ARRAY_LIST_HANDLER.handle(metaData.getImportedKeys(table.getBase().getMDName(), table.getSchema().getName(), table.getName()));
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

            // not by name, postgresql returns lowercase
            // "FKCOLUMN_NAME"
            final String keyName = (String) m[7];
            // "KEY_SEQ"
            final short seq = ((Number) m[8]).shortValue();
            // "PKTABLE_CAT", h2 doesn't support and PKTABLE_CAT is in uppercase, so don't bother
            // just use our base name
            final String foreignCat;
            // postgresql returns null
            if (this.base.getServer().getSQLSystem().isInterBaseSupported() && m[0] != null) {
                foreignCat = (String) m[0];
            } else
                foreignCat = table.getBase().getName();
            // "PKTABLE_SCHEM"
            final String foreignSchema = (String) m[1];
            // "PKTABLE_NAME"
            final String foreignTableName = (String) m[2];
            // "PKCOLUMN_NAME"
            final String foreignTableColName = (String) m[3];
            // "FK_NAME"
            final String foreignKeyName = (String) m[11];

            final SQLField key = table.getField(keyName);
            final SQLSchema schema = table.getBase().getServer().getBase(foreignCat).getSchema(foreignSchema);
            if (schema == null)
                throw new IllegalStateException(key.getSQLName() + " references " + foreignCat + "." + foreignSchema + " which does not exist (probably filtered by DBSystemRoot.getRootsToMap())");
            final SQLTable foreignTable;
            if (this.base.getServer().getSQLSystem() == SQLSystem.MYSQL)
                // MySQL returns all lowercase foreignTableName, see Bug #18446 :
                // INFORMATION_SCHEMA.KEY_COLUMN_USAGE.REFERENCED_TABLE_NAME always lowercase
                foreignTable = getTableIgnoringCase(schema, foreignTableName);
            else
                foreignTable = (SQLTable) schema.getCheckedChild(foreignTableName);

            metadataFKs.add(keyName);
            if (seq == 1) {
                // if we start a new link add the current one
                if (from.size() > 0)
                    addLink(from, to, name, updateRule, deleteRule);
                from.clear();
                to.clear();
            }
            from.add(key);
            to.add(foreignTable.getField(foreignTableColName));
            // "UPDATE_RULE"
            updateRule = Rule.fromShort(((Number) m[9]).shortValue());
            // "DELETE_RULE"
            deleteRule = Rule.fromShort(((Number) m[10]).shortValue());
            name = foreignKeyName;
            // MAYBE DEFERRABILITY
        }
        if (from.size() > 0)
            addLink(from, to, name, updateRule, deleteRule);

        if (Boolean.getBoolean(INFER_FK)) {
            final Set<String> lexicalFKs = SQLKey.foreignKeys(table);
            // already done
            lexicalFKs.removeAll(metadataFKs);
            // MAYBE option to print out foreign keys w/o constraint
            for (final String keyName : lexicalFKs) {
                final SQLField key = table.getField(keyName);
                addLink(singletonList(key), singletonList(SQLKey.keyToTable(key).getKey()), null, null, null);
            }
        }
    }

    private final SQLTable getTableIgnoringCase(final SQLSchema s, String tablename) {
        for (final String tname : s.getTableNames())
            if (tname.equalsIgnoreCase(tablename))
                return s.getTable(tname);
        return null;
    }

    // ** cache

    /**
     * Where xml dumps are saved, always <code>null</code> if "org.openconcerto.sql.structure.useXML" is
     * <code>false</code>.
     * 
     * @return the directory of xmls dumps, <code>null</code> if it can't be found.
     */
    private DBItemFileCache getFileCache() {
        final boolean useXML = Boolean.getBoolean("org.openconcerto.sql.structure.useXML");
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
        if (rootFile == null)
            return false;
        else
            try {
                FileUtils.mkdir_p(rootFile.getParentFile());
                PrintWriter pWriter = new PrintWriter(new FileOutputStream(rootFile));
                pWriter.print("<root codecVersion=\"");
                pWriter.print(XML_VERSION);
                pWriter.print("\"");
                SQLSchema.getVersionAttr(r.getSchema(), pWriter);
                pWriter.println(" >\n");
                for (final SQLTable t : r.getDescs(SQLTable.class)) {
                    final Set<Link> flinks = this.getForeignLinks(t);
                    if (!flinks.isEmpty()) {
                        pWriter.print("<table name=\"");
                        pWriter.print(OUTPUTTER.escapeAttributeEntities(t.getName()));
                        pWriter.println("\">");
                        for (final Link l : flinks) {
                            l.toXML(pWriter);
                        }
                        pWriter.println("</table>");
                    }
                }
                pWriter.println("\n</root>");
                pWriter.close();

                return true;
            } catch (Exception e) {
                Log.get().warning("unable to save files in " + rootFile + "\n" + ExceptionUtils.getStackTrace(e));
                return false;
            }
    }

    /**
     * Loads all necessary saved roots (ie ignore filtered).
     * 
     * @return the root names that were loaded.
     * @throws JDOMException if a file is not valid XML.
     * @throws IOException if a file content is not correct.
     */
    private List<String> mapFromXML() throws JDOMException, IOException {
        final List<String> res = new ArrayList<String>();
        for (final DBItemFileCache cache : getSavedCaches(true)) {
            final Document doc = new SAXBuilder().build(getGraphFile(cache));
            final String fileVersion = doc.getRootElement().getAttributeValue("codecVersion");
            if (!XML_VERSION.equals(fileVersion))
                throw new IOException("wrong version expected " + XML_VERSION + " got: " + fileVersion);
            final String rootName = cache.getName();
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
                    throw new IOException("wrong version expected " + actualVersion + " got: " + xmlVersion);
                for (final Object o : doc.getRootElement().getChildren()) {
                    final Element tableElem = (Element) o;
                    final SQLTable t = r.getTable(tableElem.getAttributeValue("name"));
                    for (final Object lo : tableElem.getChildren()) {
                        final Element linkElem = (Element) lo;
                        addLink(Link.fromXML(t, linkElem));
                    }
                }
                res.add(rootName);
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
    public Where getWhereClause(SQLTable t1, SQLTable t2) {
        return this.getWhereClause(t1, t2, null);
    }

    /**
     * Renvoie la clause WHERE pour faire la jointure en t1 et t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la deuxieme table.
     * @param fields les champs à utiliser, <code>null</code> pour tous.
     * @return le OR des champs passés.
     */
    public Where getWhereClause(SQLTable t1, SQLTable t2, Set<SQLField> fields) {
        Where res = null;
        final Iterator<Where> i = this.getStraightWhereClause(t1, t2, fields).iterator();
        while (i.hasNext()) {
            final Where w = i.next();
            // OR car OBSERVATION.ID_ARTICLE_1,2,3
            res = w.or(res);
        }
        return res;
    }

    /**
     * Renvoie les clauses WHERE pour faire la jointure en t1 et t2.
     * 
     * @param t1 la premiere table.
     * @param t2 la deuxieme table.
     * @param fields les champs à utiliser, <code>null</code> pour tous.
     * @return les WHERE demandés.
     */
    public Set<Where> getStraightWhereClause(SQLTable t1, SQLTable t2, Set<SQLField> fields) {
        final Set<Where> res = new HashSet<Where>();

        for (final Link l : this.getLinks(t1, t2)) {
            final SQLField f = l.getLabel();
            if (fields == null || fields != null && fields.contains(f)) {
                final SQLTable target = l.getTarget();
                res.add(new Where(f, "=", target.getKey()));
            }
        }

        return res;
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

    /**
     * La jointure du chemin passé. Le chemin est une liste de String, chaque chaine doit être soit
     * le nom d'une table, soit le nom complet d'un champ (TABLE.FIELD_NAME). <br/>
     * TODO passer des SQLTable.
     * 
     * @param path le chemin (en String) de la jointure.
     * @return en 0 la clause WHERE, en 1 les tables.
     */
    public Where getJointure(List<String> path) {
        return this.getJointure(Path.create(this.getContext(), path));
    }

    public Where getJointure(Path p) {
        Where res = null;
        for (int i = 1; i <= p.length(); i++) {
            SQLTable previous = p.getTable(i - 1);
            SQLTable table = p.getTable(i);
            Where wc = this.getWhereClause(previous, table, p.getStepFields(i - 1));
            res = wc.and(res);
        }
        return res;
    }

    public Set<Where> getStraightJoin(final Path p) {
        if (p.length() == 0) {
            throw new IllegalArgumentException("Path empty");
        } else if (p.length() == 1) {
            final SQLTable previous = p.getTable(0);
            final SQLTable table = p.getTable(1);
            return this.getStraightWhereClause(previous, table, p.getStepFields(0));
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
