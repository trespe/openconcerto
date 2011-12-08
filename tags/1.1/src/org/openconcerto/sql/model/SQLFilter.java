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
 * SQLFilter created on 10 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.graph.GraFFF;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Un filtre SQL, un ensemble de tables avec les ID sélectionnés.
 * 
 * @author ILM Informatique 10 mai 2004
 */
public final class SQLFilter {

    /**
     * Create a filter based on parent-child relationships.
     * 
     * @param root the root to filter.
     * @param dir the relationships.
     * @return the corresponding filter.
     */
    static public SQLFilter create(DBSystemRoot root, SQLElementDirectory dir) {
        final Collection<SQLElement> elements = dir.getElements();
        // everyone has one parent
        final Set<SQLField> toKeep = new HashSet<SQLField>(elements.size());

        for (final SQLElement elem : elements) {
            final String parentFF = elem.getParentForeignField();
            if (parentFF != null)
                toKeep.add(elem.getTable().getField(parentFF));
        }
        // NOTE, if filtering on privates is needed (eg OBSERVATION)
        // first find its parents (eg CIRCUIT, MACHINE, etc.)
        // (don't just add elem.getPrivateForeignFields(), it can short circuit the
        // parent relationship, BAT<-LOCAL<-MACHINE<-MACHINE_ITEM and all point to OBS,
        // then to filter MACHINE_ITEM from BAT we will pass through OBS)

        return new SQLFilter(dir, root.getGraph().cloneForFilterKeep(toKeep));
    }

    // *** instance members

    private final SQLElementDirectory dir;
    private final GraFFF filterGraph;
    private final List<Set<SQLRow>> filteredIDs;
    private final List<SQLFilterListener> listeners;

    public SQLFilter(SQLElementDirectory dir, final GraFFF filterGraph) {
        this.dir = dir;
        this.filterGraph = filterGraph;
        this.filteredIDs = new ArrayList<Set<SQLRow>>();
        this.listeners = new ArrayList<SQLFilterListener>();
    }

    /**
     * The path from the passed table to the filtered row.
     * 
     * @param tableToDisplay la table que l'on veut filtrer.
     * @return the path from the passed table to the filter, <code>null</code> if there's no filter
     *         or the filter isn't linked to the table.
     */
    public Path getPath(SQLTable tableToDisplay) {
        Path res = null;
        if (getDepth() > 0) {
            final Set<Path> paths = this.getPaths(tableToDisplay, this.getLeafTable());
            if (!paths.isEmpty())
                // getPaths() start from the filter, we want to start from tableToDisplay
                res = paths.iterator().next().reverse();
        }
        return res;
    }

    /**
     * Retourne l'ensemble des chemins entre les 2 tables. Permet de presonnaliser, par exemple pour
     * les observations findAllPath au lieu de getShortestPath.
     * 
     * @param tableToDisplay la table à afficher, eg OBSERVATION.
     * @param filterTable la table filtrée, eg SITE.
     * @return un ensemble de Path.
     */
    private Set<Path> getPaths(SQLTable tableToDisplay, SQLTable filterTable) {
        final Set<Path> paths;
        // TODO renvoie un Where enorme
        // if (tableToDisplay.getName().equals("OBSERVATION"))
        // paths = this.filterGraph.findAllPath(filterTable, tableToDisplay);
        // else {
        // ATTN ne marche que s'il n'y a qu'1 seul lien entre les tables, sinon
        // comme entre ARTICLE et OBSERVATION_TYPE, il en prend 1 au hasard, ex ID_ARTICLE_2
        Path shortestPath = this.filterGraph.getShortestPath(filterTable, tableToDisplay);
        if (shortestPath == null)
            paths = Collections.emptySet();
        else
            paths = Collections.singleton(shortestPath);
        // }
        return paths;
    }

    /**
     * Filtre la table passée suivant l'ID passé.
     * 
     * @param table LOCAL
     * @param ID 3
     */
    public void setFilteredID(SQLTable table, Integer ID) {
        this.setFiltered(Collections.singletonList(Collections.singleton(new SQLRow(table, ID))));
    }

    public void setFiltered(List<Set<SQLRow>> r) {
        // whether this is already filtered on r
        if (r.equals(this.filteredIDs))
            return;

        final int prevDepth = getDepth();
        final SQLTable prevTable = this.getLeafTable();
        this.filteredIDs.clear();
        this.filteredIDs.addAll(r);
        // find the table the closest to the root
        final SQLTable broadestTable = prevDepth < getDepth() ? prevTable : this.getLeafTable();
        this.fireConnected(broadestTable);
    }

    private int getDepth() {
        return this.filteredIDs.size();
    }

    public final Set<SQLRow> getLeaf() {
        return CollectionUtils.getFirst(this.filteredIDs);
    }

    private final SQLTable getLeafTable() {
        final Set<SQLRow> leaf = getLeaf();
        return leaf == null ? null : leaf.iterator().next().getTable();
    }

    private final void fireConnected(final SQLTable table) {
        final Set<SQLTable> connectedSet;
        if (table == null)
            connectedSet = this.filterGraph.getAllTables();
        else {
            // getFieldRaw since it can be inexistant
            final String parentForeignField = this.dir.getElement(table).getParentForeignField();
            // 5x faster than getElement(table).getDescendantTables()
            connectedSet = this.filterGraph.getDescTables(table, table.getFieldRaw(parentForeignField));
        }

        for (final SQLFilterListener l : this.listeners) {
            l.filterChanged(connectedSet);
        }
    }

    public String toString() {
        return "SQLFilter on: " + this.filteredIDs;
    }

    public void addListener(SQLFilterListener l) {
        this.listeners.add(l);
    }

    public void rmListener(SQLFilterListener l) {
        this.listeners.remove(l);
    }

}
