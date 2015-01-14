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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLElement.ReferenceAction;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowMode;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesCluster;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Cache several trees of rows (a row and its descendants).
 * 
 * @author Sylvain
 */
public final class TreesOfSQLRows {

    public static final TreesOfSQLRows createFromIDs(final SQLElement elem, final Collection<? extends Number> ids) {
        final List<SQLRow> rows = new ArrayList<SQLRow>(ids.size());
        for (final Number id : ids)
            rows.add(elem.getTable().getRow(id.intValue()));
        return new TreesOfSQLRows(elem, rows);
    }

    private static String createRestrictDesc(SQLElement refElem, SQLRowAccessor refVals, Link fk) {
        final String rowDesc = refElem != null ? refElem.getDescription(refVals.asRow()) : refVals.asRow().toString();
        final String fieldLabel = Configuration.getTranslator(fk.getSource()).getLabelFor(fk.getLabel());
        final String fieldS = fieldLabel != null ? fieldLabel : fk.getLabel().getName();
        // la tâche du 26/05 ne peut perdre son champ UTILISATEUR
        return TM.getTM().trM("sqlElement.linkCantBeCut", CollectionUtils.createMap("row", refVals.asRow(), "rowDesc", rowDesc, "fieldLabel", fieldS));
    }

    private final SQLElement elem;
    private final Map<SQLRow, SQLRowValues> trees;
    private SetMap<SQLField, SQLRow> externReferences;

    public TreesOfSQLRows(final SQLElement elem, SQLRow row) {
        this(elem, Collections.singleton(row));
    }

    public TreesOfSQLRows(final SQLElement elem, final Collection<? extends SQLRowAccessor> rows) {
        super();
        this.elem = elem;
        this.trees = new HashMap<SQLRow, SQLRowValues>(rows.size());
        for (final SQLRowAccessor r : rows) {
            this.elem.check(r);
            this.trees.put(r.asRow(), null);
        }
        this.externReferences = null;
    }

    public final SQLElement getElem() {
        return this.elem;
    }

    public final Set<SQLRow> getRows() {
        return this.trees.keySet();
    }

    public final Map<SQLRow, SQLRowValues> getTrees() throws SQLException {
        if (this.externReferences == null) {
            final Tuple2<Map<SQLRow, SQLRowValues>, SetMap<SQLField, SQLRow>> expand = this.expand();
            assert expand.get0().keySet().equals(this.trees.keySet());
            this.trees.putAll(expand.get0());
            this.externReferences = expand.get1();
        }
        return Collections.unmodifiableMap(this.trees);
    }

    public final Set<SQLRowValuesCluster> getClusters() throws SQLException {
        final Set<SQLRowValuesCluster> res = Collections.newSetFromMap(new IdentityHashMap<SQLRowValuesCluster, Boolean>());
        for (final SQLRowValues r : this.getTrees().values()) {
            // trees can be linked together
            res.add(r.getGraph());
        }
        return res;
    }

    private final Tuple2<Map<SQLRow, SQLRowValues>, SetMap<SQLField, SQLRow>> expand() throws SQLException {
        final Map<Integer, SQLRowValues> valsMap = new HashMap<Integer, SQLRowValues>();
        final Map<SQLRow, SQLRowValues> hasBeen = new HashMap<SQLRow, SQLRowValues>();
        final SetMap<SQLField, SQLRow> toCut = new SetMap<SQLField, SQLRow>();

        // fetch privates of root rows
        final SQLRowValues privateGraph = this.getElem().getPrivateGraph(EnumSet.of(VirtualFields.FOREIGN_KEYS));
        final Privates privates;
        if (privateGraph.getGraph().size() > 1) {
            privates = new Privates(hasBeen, toCut);
            final Set<Number> ids = new HashSet<Number>();
            for (final SQLRow r : this.getRows()) {
                ids.add(r.getIDNumber());
            }
            final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(privateGraph);
            fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    return input.andWhere(new Where(privateGraph.getTable().getKey(), ids));
                }
            });
            final Set<SQLRow> rowsFetched = new HashSet<SQLRow>();
            for (final SQLRowValues newVals : fetcher.fetch()) {
                valsMap.put(newVals.getID(), newVals);
                rowsFetched.add(newVals.asRow());
                privates.collect(newVals);
            }
            if (!rowsFetched.equals(this.getRows()))
                throw new IllegalStateException("Some rows are missing : " + rowsFetched + "\n" + this.getRows());
        } else {
            privates = null;
        }

        final Map<SQLRow, SQLRowValues> res = new HashMap<SQLRow, SQLRowValues>();
        for (final SQLRow r : this.getRows()) {
            SQLRowValues vals = valsMap.get(r.getID());
            // when there's no private to fetch
            if (vals == null) {
                assert privates == null;
                vals = r.createUpdateRow();
                valsMap.put(r.getID(), vals);
            }
            hasBeen.put(r, vals);
            res.put(r, vals);
        }
        expand(getElem().getTable(), valsMap, hasBeen, toCut, false);
        if (privates != null)
            privates.expand();
        return Tuple2.create(res, toCut);
    }

    // NOTE using a collection of vals changed the time it took to archive a site (736 01) from 225s
    // to 5s.
    /**
     * Expand the passed values by going through referent keys.
     * 
     * @param t the table, eg /LOCAL/.
     * @param valsMap the values to expand, eg {3=>LOCAL(3)->BAT(4), 12=>LOCAL(12)->BAT(4)}.
     * @param hasBeen the rows alredy expanded, eg {BAT[4], LOCAL[3], LOCAL[12]}.
     * @param toCut the links to cut, eg {|BAT.ID_PRECEDENT|=> [BAT[2]]}.
     * @param ignorePrivateParentRF <code>true</code> if
     *        {@link SQLElement#getPrivateParentReferentFields() private links} should be ignored.
     * @throws SQLException if a link is {@link ReferenceAction#RESTRICT}.
     */
    private final void expand(final SQLTable t, final Map<Integer, SQLRowValues> valsMap, final Map<SQLRow, SQLRowValues> hasBeen, final SetMap<SQLField, SQLRow> toCut,
            final boolean ignorePrivateParentRF) throws SQLException {
        if (valsMap.size() == 0)
            return;

        final Privates privates = new Privates(hasBeen, toCut);
        final Set<SQLField> privateParentRF = ignorePrivateParentRF ? this.getElem().getElement(t).getPrivateParentReferentFields() : null;
        for (final Link link : t.getDBSystemRoot().getGraph().getReferentLinks(t)) {
            if (ignorePrivateParentRF && privateParentRF.contains(link.getLabel())) {
                // if we did fetch the referents rows, they would be contained in hasBeen
                continue;
            }
            // eg "ID_LOCAL"
            final String ffName = link.getLabel().getName();
            final SQLElement refElem = this.elem.getElementLenient(link.getSource());
            // play it safe
            final ReferenceAction action = refElem != null ? refElem.getActions().get(ffName) : ReferenceAction.RESTRICT;
            if (action == null) {
                throw new IllegalStateException("Null action for " + refElem + " " + ffName);
            }
            final Map<Integer, SQLRowValues> next = new HashMap<Integer, SQLRowValues>();
            final SQLRowValues graphToFetch;
            if (action == ReferenceAction.CASCADE) {
                // otherwise we would need to find and expand the parent rows of referents
                if (refElem.getPrivateParentReferentFields().size() > 0)
                    throw new UnsupportedOperationException("Cannot cascade to private element " + refElem + " from " + link);
                graphToFetch = refElem.getPrivateGraph(EnumSet.of(VirtualFields.FOREIGN_KEYS));
            } else {
                graphToFetch = new SQLRowValues(link.getSource()).put(ffName, null);
            }
            final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(graphToFetch);
            fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    // eg where RECEPTEUR.ID_LOCAL in (3,12)
                    return input.andWhere(new Where(link.getLabel(), valsMap.keySet()));
                }
            });
            for (final SQLRowValues newVals : fetcher.fetch()) {
                final SQLRow r = newVals.asRow();
                final boolean already = hasBeen.containsKey(r);
                switch (action) {
                case RESTRICT:
                    throw new SQLException(createRestrictDesc(refElem, newVals, link));
                case CASCADE:
                    if (!already) {
                        // walk before linking to existing graph
                        privates.collect(newVals);
                        // link with existing graph, eg RECEPTEUR(235)->LOCAL(3)
                        newVals.put(ffName, valsMap.get(newVals.getInt(ffName)));
                        hasBeen.put(r, newVals);
                        next.put(newVals.getID(), newVals);
                    }
                    break;
                case SET_EMPTY:
                    // if the row should be archived no need to cut any of its links
                    if (!already)
                        toCut.add(link.getLabel(), r);
                    break;
                }
                // if already expanded just link and do not add to next
                if (already) {
                    hasBeen.get(r).put(ffName, valsMap.get(newVals.getInt(ffName)));
                }
            }

            expand(fetcher.getGraph().getTable(), next, hasBeen, toCut, false);
        }
        privates.expand();
        // if the row has been added to the graph (by another link) no need to cut any of its links
        final Iterator<Entry<SQLField, Set<SQLRow>>> iter = toCut.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<SQLField, Set<SQLRow>> e = iter.next();
            final String fieldName = e.getKey().getName();
            final Iterator<SQLRow> iter2 = e.getValue().iterator();
            while (iter2.hasNext()) {
                final SQLRow rowToCut = iter2.next();
                final SQLRowValues inGraphRowToCut = hasBeen.get(rowToCut);
                if (inGraphRowToCut != null) {
                    // remove from toCut
                    iter2.remove();
                    // add link
                    final SQLRowValues dest = hasBeen.get(rowToCut.getForeignRow(fieldName, SQLRowMode.NO_CHECK));
                    if (dest == null)
                        throw new IllegalStateException("destination of link to cut " + fieldName + " not found for " + rowToCut);
                    inGraphRowToCut.put(fieldName, dest);
                }
            }
            if (e.getValue().isEmpty())
                iter.remove();
        }
    }

    private final class Privates {
        private final Map<SQLRow, SQLRowValues> hasBeen;
        private final SetMap<SQLField, SQLRow> toCut;
        private final Map<SQLTable, Map<Integer, SQLRowValues>> privateRows;

        public Privates(final Map<SQLRow, SQLRowValues> hasBeen, final SetMap<SQLField, SQLRow> toCut) {
            this.hasBeen = hasBeen;
            this.toCut = toCut;
            this.privateRows = new HashMap<SQLTable, Map<Integer, SQLRowValues>>();
        }

        private void collect(final SQLRowValues mainRow) {
            for (final SQLRowValues privateVals : mainRow.getGraph().getItems()) {
                if (privateVals != mainRow) {
                    // since newVals isn't in, its privates can't
                    assert !this.hasBeen.containsKey(privateVals.asRow());
                    Map<Integer, SQLRowValues> map = this.privateRows.get(privateVals.getTable());
                    if (map == null) {
                        map = new HashMap<Integer, SQLRowValues>();
                        this.privateRows.put(privateVals.getTable(), map);
                    }
                    map.put(privateVals.getID(), privateVals);
                }
            }
        }

        private void expand() throws SQLException {
            for (final Entry<SQLTable, Map<Integer, SQLRowValues>> e : this.privateRows.entrySet()) {
                TreesOfSQLRows.this.expand(e.getKey(), e.getValue(), this.hasBeen, this.toCut, true);
            }
        }
    }

    // ***

    /**
     * Put all the rows of the trees (except the roots) in a map by table.
     * 
     * @return the descendants by table.
     * @throws SQLException if the trees could not be fetched.
     */
    public final Map<SQLTable, List<SQLRowAccessor>> getDescendantsByTable() throws SQLException {
        final ListMap<SQLTable, SQLRowAccessor> res = new ListMap<SQLTable, SQLRowAccessor>();
        final Set<SQLRow> roots = this.getRows();
        for (final SQLRowValuesCluster c : this.getClusters()) {
            for (final SQLRowValues v : c.getItems()) {
                if (!roots.contains(v.asRow()))
                    res.add(v.getTable(), v);
            }
        }
        return res;
    }

    // * extern

    /**
     * Return the rows that point to these trees.
     * 
     * @return the rows by referent field.
     * @throws SQLException if the trees could not be fetched.
     */
    public final Map<SQLField, Set<SQLRow>> getExternReferences() throws SQLException {
        // force compute
        getTrees();
        return this.externReferences;
    }

    public final SortedMap<SQLField, Integer> getExternReferencesCount() throws SQLException {
        final SortedMap<SQLField, Integer> res = new TreeMap<SQLField, Integer>(new Comparator<SQLField>() {
            @Override
            public int compare(SQLField o1, SQLField o2) {
                return o1.getSQLName().toString().compareTo(o2.getSQLName().toString());
            }
        });
        for (final Map.Entry<SQLField, Set<SQLRow>> e : this.getExternReferences().entrySet()) {
            res.put(e.getKey(), e.getValue().size());
        }
        return res;
    }
}
