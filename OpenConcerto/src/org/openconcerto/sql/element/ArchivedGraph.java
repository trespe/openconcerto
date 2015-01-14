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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Allow to find which rows must be unarchived to maintain coherency.
 * 
 * @author Sylvain
 */
final class ArchivedGraph {

    static private final EnumSet<VirtualFields> ARCHIVE_AND_FOREIGNS = EnumSet.of(VirtualFields.FOREIGN_KEYS, VirtualFields.ARCHIVE);

    private final SQLElementDirectory dir;
    private final SQLRowValues graph;
    // indexed nodes of graph
    private final Map<SQLRow, SQLRowValues> graphRows;
    // rows that haven't been processed
    private final Set<SQLRow> toExpand;

    /**
     * Create a new instance.
     * 
     * @param dir the directory.
     * @param graph the rows (without privates) to unarchive. This object will be modified.
     */
    ArchivedGraph(final SQLElementDirectory dir, final SQLRowValues graph) {
        if (dir == null)
            throw new NullPointerException("Null SQLElementDirectory");
        this.dir = dir;
        this.graph = graph;
        this.graphRows = new HashMap<SQLRow, SQLRowValues>();
        for (final SQLRowValues v : this.graph.getGraph().getItems()) {
            final SQLRowValues prev = this.graphRows.put(v.asRow(), v);
            if (prev != null)
                throw new IllegalStateException("Duplicated row : " + v.asRow());
        }
        assert this.graphRows.size() == this.graph.getGraph().size();
        this.toExpand = new HashSet<SQLRow>(this.graphRows.keySet());
    }

    private final SQLElement getElement(final SQLTable t) {
        return this.dir.getElement(t);
    }

    private void expandPrivates() {
        final SetMap<SQLTable, Number> idsToExpandPrivate = new SetMap<SQLTable, Number>();
        for (final SQLRow toExpPrivate : this.toExpand) {
            idsToExpandPrivate.add(toExpPrivate.getTable(), toExpPrivate.getIDNumber());
        }
        for (final Entry<SQLTable, Set<Number>> e : idsToExpandPrivate.entrySet()) {
            final SQLElement elem = getElement(e.getKey());
            final Set<Number> ids = e.getValue();
            final SQLRowValues privateGraph = elem.getPrivateGraph(ARCHIVE_AND_FOREIGNS);
            if (privateGraph.getGraph().size() > 1) {
                final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(privateGraph, false);
                setWhereAndArchivePolicy(fetcher, ids, ArchiveMode.BOTH);
                final List<SQLRowValues> fetchedRows = fetcher.fetch();
                assert fetchedRows.size() == ids.size();
                for (final SQLRowValues valsFetched : fetchedRows) {
                    // attach to existing graph
                    // need to copy since we modify the graph when loading values
                    for (final SQLRowValues v : new ArrayList<SQLRowValues>(valsFetched.getGraph().getItems())) {
                        final SQLRow row = v.asRow();
                        if (v == valsFetched) {
                            final SQLRowValues toExpandVals = this.graphRows.get(row);
                            toExpandVals.load(valsFetched, null);
                            assert valsFetched.getFields().isEmpty();
                            for (final Entry<SQLField, ? extends Collection<SQLRowValues>> refEntry : new ListMap<SQLField, SQLRowValues>(valsFetched.getReferentsMap()).entrySet()) {
                                for (final SQLRowValues ref : refEntry.getValue()) {
                                    ref.put(e.getKey().getName(), toExpandVals);
                                }
                            }
                            assert valsFetched.getGraph().size() == 1;
                        } else {
                            this.toExpand.add(row);
                            this.graphRows.put(row, v);
                        }
                    }
                }
            }
        }
    }

    final SQLRowValues expand() {
        // expand once the privates (in the rest of this method they are fetched alongside the
        // main row)
        expandPrivates();

        while (!this.toExpand.isEmpty()) {
            // find required archived rows
            final SetMap<SQLTable, Number> toFetch = new SetMap<SQLTable, Number>();
            final SetMap<SQLTable, Number> privateToFetch = new SetMap<SQLTable, Number>();
            final Map<SQLTable, Set<Link>> foreignFields = new HashMap<SQLTable, Set<Link>>();
            final SetMap<SQLRow, String> nonEmptyFieldsPointingToPrivates = new SetMap<SQLRow, String>();
            for (final SQLRow rowToExpand : this.toExpand) {
                final SQLTable t = rowToExpand.getTable();
                Set<Link> ffs = foreignFields.get(t);
                if (ffs == null) {
                    ffs = t.getDBSystemRoot().getGraph().getForeignLinks(t);
                    foreignFields.put(t, ffs);
                }
                for (final Link ff : ffs) {
                    final String fieldName = ff.getLabel().getName();
                    if (!rowToExpand.isForeignEmpty(fieldName)) {
                        final SQLRow foreignRow = new SQLRow(ff.getTarget(), rowToExpand.getInt(fieldName));
                        final SQLRowValues existingRow = this.graphRows.get(foreignRow);
                        if (existingRow != null) {
                            this.graphRows.get(rowToExpand).put(fieldName, existingRow);
                        } else {
                            final SQLElement elem = getElement(foreignRow.getTable());
                            final SetMap<SQLTable, Number> map;
                            if (elem.getPrivateParentReferentFields().size() > 0) {
                                // if foreignRow is part of private graph, fetch it later from
                                // the main row
                                nonEmptyFieldsPointingToPrivates.add(rowToExpand, fieldName);
                                map = privateToFetch;
                            } else {
                                map = toFetch;
                            }
                            map.add(foreignRow.getTable(), foreignRow.getIDNumber());
                        }
                    }
                }
            }

            final Map<SQLRow, SQLRowValues> archivedForeignRows = fetch(toFetch);

            // attach to existing graph
            final Map<SQLRow, SQLRowValues> added = new HashMap<SQLRow, SQLRowValues>();
            for (final SQLRow rowToExpand : this.toExpand) {
                final SQLTable t = rowToExpand.getTable();
                final Set<Link> ffs = foreignFields.get(t);
                assert ffs != null;
                for (final Link ff : ffs) {
                    final String fieldName = ff.getLabel().getName();
                    if (!rowToExpand.isForeignEmpty(fieldName)) {
                        final SQLRow foreignRow = new SQLRow(ff.getTarget(), rowToExpand.getInt(fieldName));
                        final SQLRowValues valsFetched = archivedForeignRows.get(foreignRow);
                        // null meaning excluded because it wasn't archived or points to a
                        // private
                        if (valsFetched != null && valsFetched.isArchived()) {
                            attach(rowToExpand, fieldName, valsFetched, added);
                            // rows were fetched from a different link
                            nonEmptyFieldsPointingToPrivates.remove(rowToExpand, fieldName);
                            privateToFetch.remove(foreignRow.getTable(), foreignRow.getIDNumber());
                        }
                    }
                }
            }

            // only referenced archived rows
            final Map<SQLRow, SQLRowValues> privateFetched = fetch(privateToFetch);
            toFetch.clear();
            for (final SQLRow r : privateFetched.keySet()) {
                final SQLRowAccessor privateRoot = getElement(r.getTable()).getPrivateRoot(r, ArchiveMode.BOTH);
                toFetch.add(privateRoot.getTable(), privateRoot.getIDNumber());
            }
            // then fetch private graph (even if the private row referenced is archived its main
            // row might not be)
            final Map<SQLRow, SQLRowValues> mainRowFetched = fetch(toFetch, ArchiveMode.BOTH);
            // attach to existing graph
            for (final Entry<SQLRow, Set<String>> e : nonEmptyFieldsPointingToPrivates.entrySet()) {
                final SQLRow rowToExpand = e.getKey();
                final SQLTable t = rowToExpand.getTable();
                for (final String fieldName : e.getValue()) {
                    assert !rowToExpand.isForeignEmpty(fieldName);
                    final SQLRow foreignRow = new SQLRow(t.getForeignTable(fieldName), rowToExpand.getInt(fieldName));
                    final SQLRowValues valsFetched = mainRowFetched.get(foreignRow);
                    if (valsFetched != null) {
                        // since we kept only archived ones in privateFetched
                        assert valsFetched.isArchived();
                        attach(rowToExpand, fieldName, valsFetched, added);
                    }
                }
            }

            this.toExpand.clear();
            this.toExpand.addAll(added.keySet());
        }
        return this.graph;
    }

    // add a link through fieldName and if this joins 2 graphs, index the new rows.
    private void attach(final SQLRow rowToExpand, final String fieldName, final SQLRowValues valsFetched, final Map<SQLRow, SQLRowValues> added) {
        final boolean alreadyLinked = this.graph.getGraph() == valsFetched.getGraph();
        this.graphRows.get(rowToExpand).put(fieldName, valsFetched);
        if (!alreadyLinked) {
            // put all values of valsFetched
            for (final SQLRowValues v : valsFetched.getGraph().getItems()) {
                final SQLRow row = v.asRow();
                added.put(row, v);
                this.graphRows.put(row, v);
            }
        }
        assert this.graphRows.size() == this.graph.getGraph().size();
    }

    private void setWhereAndArchivePolicy(final SQLRowValuesListFetcher fetcher, final Set<Number> ids, final ArchiveMode archiveMode) {
        for (final SQLRowValuesListFetcher f : fetcher.getFetchers(true).allValues()) {
            f.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (f == fetcher) {
                        input.andWhere(new Where(fetcher.getGraph().getTable().getKey(), ids));
                    }
                    input.setArchivedPolicy(archiveMode);
                    return input;
                }
            });
        }
    }

    private Map<SQLRow, SQLRowValues> fetch(final SetMap<SQLTable, Number> toFetch) {
        return this.fetch(toFetch, ArchiveMode.ARCHIVED);
    }

    // fetch the passed rows (and their privates if the table is a main one)
    private Map<SQLRow, SQLRowValues> fetch(final SetMap<SQLTable, Number> toFetch, final ArchiveMode archiveMode) {
        final Map<SQLRow, SQLRowValues> res = new HashMap<SQLRow, SQLRowValues>();
        for (final Entry<SQLTable, Set<Number>> e : toFetch.entrySet()) {
            final Set<Number> ids = e.getValue();
            final SQLTable table = e.getKey();
            final SQLElement elem = getElement(table);
            final SQLRowValuesListFetcher fetcher;
            // don't fetch partial data
            if (elem.getPrivateParentReferentFields().isEmpty())
                fetcher = SQLRowValuesListFetcher.create(elem.getPrivateGraph(ARCHIVE_AND_FOREIGNS));
            else
                fetcher = new SQLRowValuesListFetcher(new SQLRowValues(table).putNulls(table.getFieldsNames(ARCHIVE_AND_FOREIGNS)));
            setWhereAndArchivePolicy(fetcher, ids, archiveMode);
            for (final SQLRowValues fetchedVals : fetcher.fetch()) {
                for (final SQLRowValues v : fetchedVals.getGraph().getItems()) {
                    final SQLRow r = v.asRow();
                    res.put(r, v);
                    assert !this.graphRows.containsKey(r) : "already in graph : " + r;
                }
            }
        }
        return res;
    }
}
