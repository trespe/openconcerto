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
import org.openconcerto.sql.element.SQLElement.ReferenceAction;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

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
        return rowDesc + " ne peut perdre son champ " + fieldS;
    }

    private final SQLElement elem;
    private final Map<SQLRow, SQLRowValues> trees;
    private CollectionMap<SQLField, SQLRow> externReferences;

    public TreesOfSQLRows(final SQLElement elem, SQLRow row) {
        this(elem, Collections.singleton(row));
    }

    public TreesOfSQLRows(final SQLElement elem, final Collection<SQLRow> rows) {
        super();
        this.elem = elem;
        this.trees = new HashMap<SQLRow, SQLRowValues>(rows.size());
        for (final SQLRow r : rows) {
            this.elem.check(r);
            this.trees.put(r, null);
        }
        this.externReferences = null;
    }

    public final SQLElement getElem() {
        return this.elem;
    }

    public final Set<SQLRow> getRows() {
        return this.trees.keySet();
    }

    public final Set<SQLRowValues> getTrees() throws SQLException {
        final Set<SQLRowValues> res = new HashSet<SQLRowValues>();
        for (final SQLRow r : this.getRows()) {
            res.add(this.getTree(r));
        }
        return res;
    }

    private final SQLRowValues getTree(SQLRow r) throws SQLException {
        if (!this.trees.containsKey(r))
            throw new IllegalArgumentException();
        SQLRowValues res = this.trees.get(r);
        if (res == null) {
            final Tuple2<SQLRowValues, CollectionMap<SQLField, SQLRow>> expand = this.expand(r);
            res = expand.get0();
            this.trees.put(r, res);
            if (this.externReferences == null)
                // allow to specify the attributes of the map only once
                this.externReferences = expand.get1();
            else
                this.externReferences.merge(expand.get1());
        }
        return res;
    }

    private final Tuple2<SQLRowValues, CollectionMap<SQLField, SQLRow>> expand(SQLRow r) throws SQLException {
        final SQLRowValues vals = r.createUpdateRow();
        final Set<SQLRow> hasBeen = new HashSet<SQLRow>();
        hasBeen.add(r);
        final CollectionMap<SQLField, SQLRow> toCut = new CollectionMap<SQLField, SQLRow>(new HashSet<SQLRow>());
        expand(vals.getTable(), Collections.singletonMap(vals.getID(), vals), hasBeen, toCut);
        return Tuple2.create(vals, toCut);
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
     * @throws SQLException if a link is {@link ReferenceAction#RESTRICT}.
     */
    private final void expand(final SQLTable t, final Map<Integer, SQLRowValues> valsMap, final Set<SQLRow> hasBeen, final CollectionMap<SQLField, SQLRow> toCut) throws SQLException {
        if (valsMap.size() == 0)
            return;

        for (final Link link : t.getDBSystemRoot().getGraph().getReferentLinks(t)) {
            // eg "ID_LOCAL"
            final String ffName = link.getLabel().getName();
            final SQLElement refElem = this.elem.getElementLenient(link.getSource());
            // play it safe
            final ReferenceAction action = refElem != null ? refElem.getActions().get(ffName) : ReferenceAction.RESTRICT;

            final Map<Integer, SQLRowValues> next = new HashMap<Integer, SQLRowValues>();
            final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(new SQLRowValues(link.getSource()).put(ffName, null));
            fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    // eg where RECEPTEUR.ID_LOCAL in (3,12)
                    return input.andWhere(new Where(link.getLabel(), valsMap.keySet()));
                }
            });
            for (final SQLRowValues newVals : fetcher.fetch()) {
                final SQLRow r = newVals.asRow();
                switch (action) {
                case RESTRICT:
                    throw new SQLException(createRestrictDesc(refElem, newVals, link));
                case CASCADE:
                    if (!hasBeen.contains(r)) {
                        // link with existing graph, eg RECEPTEUR(235)->LOCAL(3)
                        newVals.put(ffName, valsMap.get(newVals.getInt(ffName)));
                        hasBeen.add(r);
                        next.put(newVals.getID(), newVals);
                    }
                    break;
                case SET_EMPTY:
                    // if the row should be archived no need to cut any of its links
                    if (!hasBeen.contains(r))
                        toCut.put(link.getLabel(), r);
                    break;
                }
            }

            expand(fetcher.getGraph().getTable(), next, hasBeen, toCut);
        }
        // if the row should be archived no need to cut any of its links
        final Iterator<Entry<SQLField, Collection<SQLRow>>> iter = toCut.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<SQLField, Collection<SQLRow>> e = iter.next();
            e.getValue().removeAll(hasBeen);
            if (e.getValue().isEmpty())
                iter.remove();
        }
    }

    // ***

    /**
     * Whether these trees contains a row with the same ID.
     * 
     * @param r the row to search.
     * @return <code>true</code> if there's a rowValues with the same ID as <code>r</code>.
     * @throws SQLException if the trees could not be fetched.
     */
    public final boolean contains(SQLRow r) throws SQLException {
        for (final SQLRowValues g : getTrees()) {
            for (final SQLRowValues desc : g.getGraph().getItems()) {
                if (r.equals(desc.asRow()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Put all the rows of the trees (except the roots) in a map by table.
     * 
     * @return the descendants by table.
     * @throws SQLException if the trees could not be fetched.
     */
    public final CollectionMap<SQLTable, SQLRowAccessor> getDescendantsByTable() throws SQLException {
        final CollectionMap<SQLTable, SQLRowAccessor> res = new CollectionMap<SQLTable, SQLRowAccessor>();
        for (final SQLRowValues graph : this.getTrees()) {
            graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
                @Override
                public Object transformChecked(State<Object> input) {
                    if (graph != input.getCurrent())
                        res.put(input.getCurrent().getTable(), input.getCurrent());
                    return null;
                }
            }, RecursionType.BREADTH_FIRST, false);
        }
        return res;
    }

    /**
     * Returns the descendants as a list that is ordered "leaves-first", ie the last item is a root.
     * 
     * @return a flat list of the descendants.
     * @throws SQLException if the trees could not be fetched.
     * 
     */
    public final List<SQLRowAccessor> getFlatDescendants() throws SQLException {
        final List<SQLRowAccessor> res = new ArrayList<SQLRowAccessor>();
        for (final SQLRowValues graph : this.getTrees()) {
            graph.getGraph().walk(graph, null, new ITransformer<State<Object>, Object>() {
                @Override
                public Object transformChecked(State<Object> input) {
                    res.add(input.getCurrent());
                    return null;
                }
            }, RecursionType.DEPTH_FIRST, false);
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
    public final CollectionMap<SQLField, SQLRow> getExternReferences() throws SQLException {
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
        for (final Map.Entry<SQLField, Collection<SQLRow>> e : this.getExternReferences().entrySet()) {
            res.put(e.getKey(), e.getValue().size());
        }
        return res;
    }
}
