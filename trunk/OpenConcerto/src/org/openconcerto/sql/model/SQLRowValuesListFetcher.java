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

import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Construct a list of linked SQLRowValues from one request.
 * 
 * @author Sylvain
 */
public class SQLRowValuesListFetcher {

    // return the referent single link path starting from graph
    private static Path computePath(SQLRowValues graph) {
        // check that there's only one referent for each row
        // (otherwise huge joins, e.g. LOCAL<-CPI,SOURCE,RECEPTEUR,etc.)
        final Path res = new Path(graph.getTable());
        graph.getGraph().walk(graph, res, new ITransformer<State<Path>, Path>() {
            @Override
            public Path transformChecked(State<Path> input) {
                final Collection<SQLRowValues> referentRows = input.getCurrent().getReferentRows();
                if (referentRows.size() > 1) {
                    // remove the foreign rows which are all the same (since they point to
                    // current) so the exn is more legible
                    final List<SQLRowValues> toPrint = SQLRowValues.trim(referentRows);
                    throw new IllegalArgumentException(input.getCurrent() + " is referenced by " + toPrint + "\nat " + input.getPath());
                } else if (referentRows.size() == 0) {
                    input.getAcc().append(input.getPath());
                }
                return input.getAcc();
            }
        }, RecursionType.BREADTH_FIRST, false);
        return res;
    }

    private static Path createPath(final SQLTable t, SQLField... fields) {
        final Path res = new Path(t);
        for (final SQLField f : fields)
            res.add(f);
        return res;
    }

    static private final CollectionMap<Tuple2<Path, Number>, SQLRowValues> createCollectionMap() {
        // we need a List in merge()
        return new CollectionMap<Tuple2<Path, Number>, SQLRowValues>(new ArrayList<SQLRowValues>(8));
    }

    private final SQLRowValues graph;
    private final Path descendantPath;
    private ITransformer<SQLSelect, SQLSelect> selTransf;
    private Integer selID;
    private boolean ordered;
    private SQLRowValues minGraph;
    private boolean includeForeignUndef;
    private SQLSelect frozen;
    // graftPlace -> {referent path -> fetcher}
    private final Map<Path, Map<Path, SQLRowValuesListFetcher>> grafts;

    /**
     * Construct a new instance with the passed graph of SQLRowValues.
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}, eg <code>new
     *        SQLRowValues("SITE").setAllToNull().put("ID_CONTACT", new SQLRowValues("CONTACT"))</code>
     *        to return all sites (with all their fields) with their associated contacts.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph) {
        this(graph, false);
    }

    /**
     * Construct a new instance with the passed graph of SQLRowValues. Eg if <code>graph</code> is a
     * BATIMENT which points to SITE, is pointed by LOCAL, CPI_BT and <code>referents</code> is
     * <code>true</code>, {@link #fetch()} could return
     * 
     * <pre>
     * SITE[2]  BATIMENT[2]     LOCAL[2]    CPI_BT[3]       
     *                                      CPI_BT[2]       
     *                          LOCAL[3]        
     *                          LOCAL[5]    CPI_BT[5]
     * SITE[7]  BATIMENT[3]     LOCAL[4]    CPI_BT[4]       
     * SITE[7]  BATIMENT[4]
     * </pre>
     * 
     * @param graph what SQLRowValues should be returned by {@link #fetch()}, eg <code>new
     *        SQLRowValues("SITE").setAllToNull().put("ID_CONTACT", new SQLRowValues("CONTACT"))</code>
     *        to return all sites (with all their fields) with their associated contacts.
     * @param referents <code>true</code> if referents to <code>graph</code> should also be fetched.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph, final boolean referents) {
        this(graph, referents ? computePath(graph) : null);
    }

    public SQLRowValuesListFetcher(SQLRowValues graph, final SQLField... fields) {
        this(graph, createPath(graph.getTable(), fields));
    }

    /**
     * Construct a new instance.
     * 
     * @param graph graph what SQLRowValues should be returned by {@link #fetch()}.
     * @param referentPath a {@link Path#isSingleLink() single link} path from the primary table,
     *        <code>null</code> meaning don't fetch referent rows.
     */
    public SQLRowValuesListFetcher(SQLRowValues graph, final Path referentPath) {
        super();
        this.graph = graph.deepCopy();
        if (referentPath == null)
            // don't keep unneeded values
            this.graph.clearReferents();
        else if (graph.followPath(referentPath) == null)
            throw new IllegalArgumentException("path is not contained in the passed rowValues : " + referentPath + "\n" + graph.printTree());
        this.descendantPath = referentPath == null || referentPath.length() == 0 ? null : referentPath;
        // followPath() do the following check
        assert this.descendantPath == null || (this.descendantPath.getFirst() == graph.getTable() && this.descendantPath.isSingleLink());
        // always need IDs
        for (final SQLRowValues curr : this.getGraph().getGraph().getItems()) {
            // don't overwrite existing values
            if (!curr.hasID())
                curr.setID(null);
        }

        this.selTransf = null;
        this.selID = null;
        this.ordered = false;
        this.minGraph = null;
        this.includeForeignUndef = false;
        this.frozen = null;
        this.grafts = new HashMap<Path, Map<Path, SQLRowValuesListFetcher>>(4);
    }

    /**
     * Make this instance immutable. Ie all setters will now throw {@link IllegalStateException}.
     * Furthermore the request will be computed now once and for all, so as not to be subject to
     * outside modification by {@link #getSelTransf()}.
     * 
     * @return this.
     */
    public final SQLRowValuesListFetcher freeze() {
        if (!this.isFrozen()) {
            this.frozen = new SQLSelect(this.getReq());
            for (final Map<Path, SQLRowValuesListFetcher> m : this.grafts.values()) {
                for (final SQLRowValuesListFetcher f : m.values())
                    f.freeze();
            }
        }
        return this;
    }

    public final boolean isFrozen() {
        return this.frozen != null;
    }

    private final void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("this has been frozen: " + this);
    }

    public SQLRowValues getGraph() {
        return this.graph;
    }

    /**
     * Whether to include undefined rows (of tables other than the graph's).
     * 
     * @param includeForeignUndef <code>true</code> to include undefined rows.
     */
    public final void setIncludeForeignUndef(boolean includeForeignUndef) {
        this.checkFrozen();
        this.includeForeignUndef = includeForeignUndef;
    }

    /**
     * Require that only rows with values for the full graph are returned. Eg if the graph is CPI ->
     * OBS, setting this to <code>true</code> will excludes CPI without OBS.
     * 
     * @param b <code>true</code> if only full rows should be fetched.
     */
    public final void setFullOnly(boolean b) {
        this.checkFrozen();
        if (b)
            this.minGraph = this.getGraph();
        else
            this.minGraph = null;
    }

    private final boolean requirePath(final Path p) {
        return this.minGraph != null && this.minGraph.followPath(p) != null;
    }

    private boolean fetchReferents() {
        return this.descendantPath != null;
    }

    /**
     * To modify the query before execution.
     * 
     * @param selTransf will be passed the query which has been constructed, and the return value
     *        will be actually executed, can be <code>null</code>.
     */
    public void setSelTransf(ITransformer<SQLSelect, SQLSelect> selTransf) {
        this.checkFrozen();
        this.selTransf = selTransf;
    }

    public final ITransformer<SQLSelect, SQLSelect> getSelTransf() {
        return this.selTransf;
    }

    /**
     * Add a where in {@link #getReq()} to restrict the primary key.
     * 
     * @param selID an ID for the primary key, <code>null</code> to not filter.
     */
    public void setSelID(Integer selID) {
        this.checkFrozen();
        this.selID = selID;
    }

    public final Integer getSelID() {
        return this.selID;
    }

    /**
     * Whether to add ORDER BY in {@link #getReq()}.
     * 
     * @param b <code>true</code> if the query should be ordered.
     */
    public final void setOrdered(final boolean b) {
        this.checkFrozen();
        this.ordered = b;
    }

    public final boolean isOrdered() {
        return this.ordered;
    }

    public final SQLRowValuesListFetcher graft(final SQLRowValuesListFetcher other) {
        return this.graft(other, new Path(getGraph().getTable()));
    }

    public final SQLRowValuesListFetcher graft(final SQLRowValues other, Path graftPath) {
        // with referents otherwise it's useless
        return this.graft(new SQLRowValuesListFetcher(other, true), graftPath);
    }

    /**
     * Graft a fetcher on this graph.
     * 
     * @param other another instance fetching rows of the table at <code>graftPath</code>.
     * @param graftPath a path from this values to where <code>other</code> rows should be grafted.
     * @return the previous fetcher.
     */
    public final SQLRowValuesListFetcher graft(final SQLRowValuesListFetcher other, Path graftPath) {
        checkFrozen();
        if (this == other)
            throw new IllegalArgumentException("trying to graft onto itself");
        if (other.getGraph().getTable() != graftPath.getLast())
            throw new IllegalArgumentException("trying to graft " + other.getGraph().getTable() + " at " + graftPath);
        final SQLRowValues graftPlace = this.getGraph().followPath(graftPath);
        if (graftPlace == null)
            throw new IllegalArgumentException("path doesn't exist: " + graftPath);
        assert graftPath.getLast() == graftPlace.getTable();
        if (other.getGraph().getForeigns().size() > 0)
            throw new IllegalArgumentException("shouldn't have foreign rows");

        final Path descendantPath = computePath(other.getGraph());
        if (descendantPath.length() == 0)
            throw new IllegalArgumentException("empty path");
        // checked by computePath
        assert descendantPath.isSingleLink();
        // eg this is LOCAL* -> BATIMENT -> SITE and CPI -> LOCAL -> BATIMENT* is being grafted
        if (graftPath.length() > 0 && descendantPath.getStep(0).reverse().equals(graftPath.getStep(graftPath.length() - 1)))
            throw new IllegalArgumentException(descendantPath.getStep(0) + " already fetched");
        if (!this.grafts.containsKey(graftPath)) {
            this.grafts.put(graftPath, new HashMap<Path, SQLRowValuesListFetcher>(4));
            // if descendantPath already exists, replace it
        } else if (!this.grafts.get(graftPath).containsKey(descendantPath)) {
            // otherwise check for duplicate path
            for (final Path p : this.grafts.get(graftPath).keySet()) {
                if (p.startsWith(descendantPath) || descendantPath.startsWith(p))
                    throw new IllegalArgumentException(descendantPath + " already grafted at " + graftPath + " : " + p);
            }
        }
        return this.grafts.get(graftPath).put(descendantPath, other);
    }

    public void ungraft() {
        this.ungraft(new Path(getGraph().getTable()));
    }

    public final void ungraft(final Path graftPath) {
        checkFrozen();
        this.grafts.remove(graftPath);
    }

    private final void addFields(final SQLSelect sel, final SQLRowValues vals, final String alias) {
        for (final String fieldName : vals.getFields())
            sel.addSelect(new AliasedField(vals.getTable().getField(fieldName), alias));
    }

    public final SQLSelect getReq() {
        if (this.isFrozen())
            return this.frozen;

        final SQLTable t = this.getGraph().getTable();
        final SQLSelect sel = new SQLSelect(t.getBase());

        if (this.includeForeignUndef) {
            sel.setExcludeUndefined(false);
            sel.setExcludeUndefined(true, t);
        }

        if (this.isOrdered() && t.isOrdered())
            sel.addFieldOrder(t.getOrderField());
        walk(sel, new ITransformer<State<SQLSelect>, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(State<SQLSelect> input) {
                final String alias;
                if (input.getFrom() != null) {
                    alias = getAlias(input.getAcc(), input.getPath());
                    final String aliasPrev = input.getPath().length() == 1 ? null : input.getAcc().followPath(t.getName(), input.getPath().subPath(0, -1));
                    if (input.isBackwards()) {
                        // eg LEFT JOIN loc on loc.ID_BATIMENT = BATIMENT.ID
                        input.getAcc().addBackwardJoin("LEFT", alias, input.getFrom(), aliasPrev);
                        // order is only meaningfull for backwards
                        // as going forward there's at most 1 row for each row in t
                        if (isOrdered())
                            input.getAcc().addOrderSilent(alias);
                    } else {
                        final String joinType = requirePath(input.getPath()) ? "INNER" : "LEFT";
                        input.getAcc().addJoin(joinType, new AliasedField(input.getFrom(), aliasPrev), alias);
                    }

                } else
                    alias = null;
                addFields(input.getAcc(), input.getCurrent(), alias);

                return input.getAcc();
            }

        });
        if (this.selID != null)
            sel.andWhere(new Where(t.getKey(), "=", this.selID));
        return this.getSelTransf() == null ? sel : this.getSelTransf().transformChecked(sel);
    }

    static String getAlias(final SQLSelect sel, final Path path) {
        String res = "tAlias";
        final int stop = path.length();
        for (int i = 0; i < stop; i++) {
            res += "__" + path.getSingleStep(i).getName();
        }
        // needed for backward, otherwise tableAlias__ID_BATIMENT for LOCAL
        res += "__" + path.getTable(stop).getName();
        return sel.getUniqueAlias(res);
    }

    // assure that the graph is explored the same way for the construction of the request
    // and the reading of the resultSet
    private <S> void walk(final S sel, final ITransformer<State<S>, S> transf) {
        // walk through foreign keys and never walk back (use graft())
        this.getGraph().getGraph().walk(this.getGraph(), sel, transf, RecursionType.BREADTH_FIRST, true);
        // walk starting backwards but allowing forwards
        this.getGraph().getGraph().walk(this.getGraph(), sel, new ITransformer<State<S>, S>() {
            @Override
            public S transformChecked(State<S> input) {
                final Path p = input.getPath();
                if (p.getStep(0).isForeign())
                    throw new SQLRowValuesCluster.StopRecurseException().setCompletely(false);
                final Step lastStep = p.getStep(p.length() - 1);
                // if we go backwards it should be from the start (i.e. we can't go backwards, then
                // forwards and backwards again)
                if (!lastStep.isForeign() && !p.isSingleDirection(false))
                    throw new SQLRowValuesCluster.StopRecurseException().setCompletely(false);
                return transf.transformChecked(input);
            }
        }, RecursionType.BREADTH_FIRST, null, false);
    }

    // models the graph, so that we don't have to walk it for each row
    private static final class GraphNode {
        private final SQLTable t;
        private final int fieldCount;
        private final int linkIndex;
        private final Step from;

        private GraphNode(final State<Integer> input) {
            super();
            this.t = input.getCurrent().getTable();
            this.fieldCount = input.getCurrent().size();
            this.linkIndex = input.getAcc();
            final int length = input.getPath().length();
            this.from = length == 0 ? null : input.getPath().getStep(length - 1);
        }

        public final SQLTable getTable() {
            return this.t;
        }

        public final int getFieldCount() {
            return this.fieldCount;
        }

        public final int getLinkIndex() {
            return this.linkIndex;
        }

        public final String getFromName() {
            return this.from.getSingleField().getName();
        }

        public final boolean isBackwards() {
            return !this.from.isForeign(this.from.getSingleField());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.fieldCount;
            result = prime * result + ((this.from == null) ? 0 : this.from.hashCode());
            result = prime * result + this.linkIndex;
            result = prime * result + this.t.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final GraphNode other = (GraphNode) obj;
            return this.fieldCount == other.fieldCount && this.linkIndex == other.linkIndex && this.t.equals(other.t) && CompareUtils.equals(this.from, other.from);
        }

        @Override
        public String toString() {
            final String link = this.from == null ? "" : " linked to " + getLinkIndex() + " by " + this.getFromName() + (this.isBackwards() ? " backwards" : " forewards");
            return this.getFieldCount() + " fields of " + this.getTable() + link;
        }
    }

    static private final class RSH implements ResultSetHandler {
        private final List<String> selectFields;
        private final List<GraphNode> graphNodes;

        private RSH(List<String> selectFields, List<GraphNode> l) {
            this.selectFields = selectFields;
            this.graphNodes = l;
        }

        @Override
        public Object handle(final ResultSet rs) throws SQLException {
            final List<GraphNode> l = this.graphNodes;
            final int graphSize = l.size();
            int nextToLink = 0;
            final List<Future<?>> futures = new ArrayList<Future<?>>();

            final List<SQLRowValues> res = new ArrayList<SQLRowValues>(64);
            final List<List<SQLRowValues>> rows = Collections.synchronizedList(new ArrayList<List<SQLRowValues>>(64));
            // for each rs row, create all SQLRowValues without linking them together
            // if we're multi-threaded, link them in another thread
            while (rs.next()) {
                int rsIndex = 1;

                // MAYBE cancel() futures
                if (Thread.currentThread().isInterrupted())
                    throw new RTInterruptedException("interrupted while fetching");
                final List<SQLRowValues> row = new ArrayList<SQLRowValues>(graphSize);
                for (int i = 0; i < graphSize; i++) {
                    final GraphNode node = l.get(i);
                    final SQLRowValues creatingVals = new SQLRowValues(node.getTable());
                    if (i == 0)
                        res.add(creatingVals);

                    final int stop = rsIndex + node.getFieldCount();
                    for (; rsIndex < stop; rsIndex++) {
                        try {
                            // -1 since rs starts at 1
                            // field names checked below
                            creatingVals.put(this.selectFields.get(rsIndex - 1), rs.getObject(rsIndex), false);
                        } catch (SQLException e) {
                            throw new IllegalStateException("unable to fill " + creatingVals, e);
                        }
                    }
                    row.add(creatingVals);
                }
                rows.add(row);
                // become multi-threaded only for large values
                final int currentCount = rows.size();
                if (currentCount % 1000 == 0) {
                    futures.add(exec.submit(new Linker(l, rows, nextToLink, currentCount)));
                    nextToLink = currentCount;
                }
            }
            final int rowSize = rows.size();
            assert nextToLink > 0 == futures.size() > 0;
            if (nextToLink > 0)
                futures.add(exec.submit(new Linker(l, rows, nextToLink, rowSize)));

            // check field names only once since each row has the same fields
            if (rowSize > 0) {
                final List<SQLRowValues> firstRow = rows.get(0);
                for (int i = 0; i < graphSize; i++) {
                    final SQLRowValues vals = firstRow.get(i);
                    if (!vals.getTable().getFieldsName().containsAll(vals.getFields()))
                        throw new IllegalStateException("field name error : " + vals.getFields() + " not in " + vals.getTable().getFieldsName());
                }
            }

            // either link all rows, or...
            if (nextToLink == 0)
                link(l, rows, 0, rowSize);
            else {
                // ...wait for every one and most importantly check for any exceptions
                try {
                    for (final Future<?> f : futures)
                        f.get();
                } catch (Exception e) {
                    throw new IllegalStateException("couldn't link", e);
                }
            }

            return res;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.graphNodes.hashCode();
            result = prime * result + this.selectFields.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final RSH other = (RSH) obj;
            return this.graphNodes.equals(other.graphNodes) && this.selectFields.equals(other.selectFields);
        }

    }

    /**
     * Execute the request transformed by <code>selTransf</code> and return the result as a list of
     * SQLRowValues. NOTE: this method doesn't use the cache of SQLDataSource.
     * 
     * @return a list of SQLRowValues, one item per row, each item having the same structure as the
     *         SQLRowValues passed to the constructor.
     */
    public final List<SQLRowValues> fetch() {
        return this.fetch(true);
    }

    private final List<SQLRowValues> fetch(final boolean merge) {
        final SQLSelect req = this.getReq();
        // getName() would take 5% of ResultSetHandler.handle()
        final List<String> selectFields = new ArrayList<String>(req.getSelectFields().size());
        for (final SQLField f : req.getSelectFields())
            selectFields.add(f.getName());
        final SQLTable table = getGraph().getTable();

        // create a flat list of the graph nodes, we just need the table, field count and the index
        // in this list of its linked table, eg for CPI -> LOCAL -> BATIMENT -> SITE :
        // <LOCAL,2,0>, <BATIMENT,2,0>, <SITE,5,1>, <CPI,4,0>
        final int graphSize = this.getGraph().getGraph().size();
        final List<GraphNode> l = new ArrayList<GraphNode>(graphSize);
        walk(0, new ITransformer<State<Integer>, Integer>() {
            @Override
            public Integer transformChecked(State<Integer> input) {
                final int index = l.size();
                l.add(new GraphNode(input));
                return index;
            }
        });
        assert l.size() == graphSize : "All nodes weren't explored once : " + l.size() + " != " + graphSize;

        // if we wanted to use the cache, we'd need to copy the returned list and its items (i.e.
        // deepCopy()), since we modify them afterwards. Or perhaps include the code after this line
        // into the result set handler.
        final IResultSetHandler rsh = new IResultSetHandler(new RSH(selectFields, l), false);
        @SuppressWarnings("unchecked")
        final List<SQLRowValues> res = (List<SQLRowValues>) table.getBase().getDataSource().execute(req.asString(), rsh, false);
        // e.g. list of batiment pointing to site
        final List<SQLRowValues> merged = merge && this.fetchReferents() ? merge(res) : res;
        if (this.grafts.size() > 0) {
            for (final Entry<Path, Map<Path, SQLRowValuesListFetcher>> graftPlaceEntry : this.grafts.entrySet()) {
                // e.g. BATIMENT
                final Path graftPlace = graftPlaceEntry.getKey();
                final Path mapPath = new Path(graftPlace.getLast());
                // list of BATIMENT to only fetch what's necessary
                final Set<Number> ids = new HashSet<Number>();
                // byRows is common to all grafts to support CPI -> LOCAL -> BATIMENT and RECEPTEUR
                // -> LOCAL -> BATIMENT (ie avoid duplicate LOCAL)
                // CollectionMap since the same row can be in multiple index of merged, e.g. when
                // fetching *BATIMENT* -> SITE each site will be repeated as many times as it has
                // children and if we want their DOSSIER they must be grafted on each line.
                final CollectionMap<Tuple2<Path, Number>, SQLRowValues> byRows = createCollectionMap();
                for (final SQLRowValues vals : merged) {
                    final SQLRowValues graftPlaceVals = vals.followPath(graftPlace);
                    // happens when grafting on optional row
                    if (graftPlaceVals != null) {
                        ids.add(graftPlaceVals.getIDNumber());
                        byRows.put(Tuple2.create(mapPath, graftPlaceVals.getIDNumber()), graftPlaceVals);
                    }
                }
                assert ids.size() == byRows.size();
                for (final Entry<Path, SQLRowValuesListFetcher> e : graftPlaceEntry.getValue().entrySet()) {
                    // e.g BATIMENT <- LOCAL <- CPI
                    final Path descendantPath = e.getKey();
                    assert descendantPath.getFirst() == graftPlace.getLast() : descendantPath + " != " + graftPlace;
                    final SQLRowValuesListFetcher graft = e.getValue();

                    final SQLSelect toRestore = graft.frozen;
                    graft.frozen = new SQLSelect(graft.getReq()).andWhere(new Where(graft.getGraph().getTable().getKey(), ids));
                    // don't merge then...
                    final List<SQLRowValues> referentVals = graft.fetch(false);
                    graft.frozen = toRestore;
                    // ...but now
                    this.merge(merged, referentVals, byRows, descendantPath);
                }
            }
        }
        return merged;
    }

    // no need to set keep-alive too low, since on finalize() the pool shutdowns itself
    private static final ExecutorService exec = new ThreadPoolExecutor(0, 2, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private static final class Linker implements Callable<Object> {

        private final List<GraphNode> l;
        private final List<List<SQLRowValues>> rows;
        private final int fromIndex;
        private final int toIndex;

        public Linker(final List<GraphNode> l, final List<List<SQLRowValues>> rows, final int first, final int last) {
            super();
            this.l = l;
            this.rows = rows;
            this.fromIndex = first;
            this.toIndex = last;
        }

        @Override
        public Object call() throws Exception {
            link(this.l, this.rows, this.fromIndex, this.toIndex);
            return null;
        }

    }

    private static void link(final List<GraphNode> l, final List<List<SQLRowValues>> rows, final int start, final int stop) {
        final int graphSize = l.size();
        for (int nodeIndex = 1; nodeIndex < graphSize; nodeIndex++) {
            final GraphNode node = l.get(nodeIndex);

            final String fromName = node.getFromName();
            final int linkIndex = node.getLinkIndex();
            final boolean backwards = node.isBackwards();

            for (int i = start; i < stop; i++) {
                final List<SQLRowValues> row = rows.get(i);
                final SQLRowValues creatingVals = row.get(nodeIndex);
                // don't link empty values (LEFT JOIN produces rowValues filled with
                // nulls) to the graph
                if (creatingVals.hasID()) {
                    final SQLRowValues valsToFill;
                    final SQLRowValues valsToPut;
                    if (backwards) {
                        valsToFill = creatingVals;
                        valsToPut = row.get(linkIndex);
                    } else {
                        valsToFill = row.get(linkIndex);
                        valsToPut = creatingVals;
                    }

                    // check is done by updateLinks()
                    valsToFill.put(fromName, valsToPut, false);
                }
            }
        }
    }

    /**
     * Merge a list of fetched rowValues so that remove any duplicated rowValues. Eg, transforms
     * this :
     * 
     * <pre>
     * BATIMENT[2]     LOCAL[2]        CPI_BT[3]       
     * BATIMENT[2]     LOCAL[2]        CPI_BT[2]       
     * BATIMENT[2]     LOCAL[3]        
     * BATIMENT[2]     LOCAL[5]        CPI_BT[5]
     * BATIMENT[3]     LOCAL[4]        CPI_BT[4]       
     * BATIMENT[4]
     * </pre>
     * 
     * into this :
     * 
     * <pre>
     * BATIMENT[2]     LOCAL[2]        CPI_BT[3]       
     *                                 CPI_BT[2]       
     *                 LOCAL[3]        
     *                 LOCAL[5]        CPI_BT[5]
     * BATIMENT[3]     LOCAL[4]        CPI_BT[4]       
     * BATIMENT[4]
     * </pre>
     * 
     * @param l a list of fetched rowValues.
     * @return a smaller list in which all rowValues are unique.
     */
    private final List<SQLRowValues> merge(final List<SQLRowValues> l) {
        return this.merge(l, l, null, this.descendantPath);
    }

    /**
     * Merge a list of rowValues and optionally graft it onto another one.
     * 
     * @param tree the list receiving the graft.
     * @param graft the list being merged and optionally grafted on <code>tree</code>, can be the
     *        same as <code>tree</code>.
     * @param graftPlaceRows if this is a graft the destination rowValues, otherwise
     *        <code>null</code>, this instance will be modified.
     * @param descendantPath the path to merge.
     * @return the merged and grafted values.
     */
    private final List<SQLRowValues> merge(final List<SQLRowValues> tree, final List<SQLRowValues> graft, final CollectionMap<Tuple2<Path, Number>, SQLRowValues> graftPlaceRows, Path descendantPath) {
        final boolean isGraft = graftPlaceRows != null;
        assert (tree != graft) == isGraft : "Trying to graft onto itself";
        final List<SQLRowValues> res = isGraft ? tree : new ArrayList<SQLRowValues>();
        // so that every graft is actually grafted onto the tree
        final CollectionMap<Tuple2<Path, Number>, SQLRowValues> map = isGraft ? graftPlaceRows : createCollectionMap();

        final int stop = descendantPath.length();
        for (final SQLRowValues v : graft) {
            boolean doAdd = true;
            // 2 grafts cannot have the same path, so the last rowValues cannot be merged
            SQLRowValues previous = v.followPath(descendantPath);
            assert previous == null || !map.containsKey(Tuple2.create(descendantPath, previous.getIDNumber())) : "Fetched twice: " + previous;
            for (int i = stop - 1; i >= 0 && doAdd; i--) {
                final Path subPath = descendantPath.subPath(0, i);
                final SQLRowValues desc = v.followPath(subPath);
                if (desc != null) {
                    final Tuple2<Path, Number> row = Tuple2.create(subPath, desc.getIDNumber());
                    if (map.containsKey(row)) {
                        doAdd = false;
                        // previous being null can happen when 2 grafted paths share some steps at
                        // the start, e.g. SOURCE -> LOCAL and CPI -> LOCAL with a LOCAL having a
                        // SOURCE but no CPI
                        if (previous != null) {
                            final List<SQLRowValues> destinationRows = (List<SQLRowValues>) map.getNonNull(row);
                            final int destinationSize = destinationRows.size();
                            assert destinationSize > 0 : "Map contains row but have no corresponding value: " + row;
                            final String ffName = descendantPath.getSingleStep(i).getName();
                            // avoid the first deepCopy() and copy before merging
                            for (int j = 1; j < destinationSize; j++) {
                                previous.deepCopy().put(ffName, destinationRows.get(j));
                            }
                            previous.put(ffName, destinationRows.get(0));
                        }
                    } else {
                        map.put(row, desc);
                    }
                    previous = desc;
                }
            }
            if (doAdd) {
                assert !isGraft : "Adding graft values as tree values";
                res.add(v);
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + this.getGraph() + " with " + this.getSelID() + " and " + this.getSelTransf();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SQLRowValuesListFetcher) {
            final SQLRowValuesListFetcher o = (SQLRowValuesListFetcher) obj;
            // use getReq() to avoid selTransf equality pb (ie we generally use anonymous classes
            // which thus lack equals())
            return this.getReq().equals(o.getReq()) && CompareUtils.equals(this.descendantPath, o.descendantPath) && this.grafts.equals(o.grafts);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.getReq().hashCode();
    }
}
