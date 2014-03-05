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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;

/**
 * Un graphe pour le filtre (graF For Filter :-). Il est non dirigé.
 * 
 * @author ILM Informatique 22 décembre 2004
 * @see org.openconcerto.sql.model.graph.DatabaseGraph
 */
public class GraFFF extends BaseGraph {

    static GraFFF create(DirectedMultigraph<SQLTable, Link> g, List<SQLTable> linksToRemove) {
        final DirectedMultigraph<SQLTable, Link> pg = (DirectedMultigraph<SQLTable, Link>) g.clone();
        Iterator<SQLTable> i = linksToRemove.iterator();
        while (i.hasNext()) {
            final SQLTable t1 = i.next();
            final SQLTable t2 = i.next();
            if (t1 != null && t2 != null)
                pg.removeAllEdges(t1, t2);
            else {
                Log.get().config("cannot remove links between " + t1 + " and " + t2);
            }
        }
        return new GraFFF(pg);
    }

    static GraFFF createKeep(DirectedMultigraph<SQLTable, Link> g, Set<SQLField> linksToKeep) {
        final DirectedMultigraph<SQLTable, Link> pg = (DirectedMultigraph<SQLTable, Link>) g.clone();
        for (final Link l : new HashSet<Link>(pg.edgeSet())) {
            if (!linksToKeep.contains(l.getSingleField()))
                pg.removeEdge(l);
        }
        return new GraFFF(pg);
    }

    /**
     * Crée le graphe de la base passée.
     * 
     * @param g
     */
    private GraFFF(DirectedMultigraph<SQLTable, Link> g) {
        super(new AsUndirectedGraph<SQLTable, Link>(g));
    }

    /**
     * The tables below <code>t</code>.
     * 
     * @param t a table, eg /LOCAL/.
     * @param parentF its parent field, can be <code>null</code>, eg |ID_BATIMENT|.
     * @return all tables below <code>t</code>, eg {/RECEPTEUR/, /CPI/}.
     */
    public final Set<SQLTable> getDescTables(final SQLTable t, final SQLField parentF) {
        final Set<SQLTable> res = new HashSet<SQLTable>();
        this.getDescTables(t, parentF, res);
        return res;
    }

    private final void getDescTables(final SQLTable t, final SQLField parentF, final Set<SQLTable> beenThere) {
        beenThere.add(t);
        for (final Link l : this.getGraph().edgesOf(t)) {
            // works since this graph is a tree, and each table has only one parent field.
            if (!beenThere.contains(l.oppositeVertex(t)) && (parentF == null || !parentF.equals(l.getSingleField()))) {
                getDescTables(l.oppositeVertex(t), parentF, beenThere);
            }
        }
    }

    /**
     * Renvoie le plus court chemin entre 2 tables.
     * 
     * @param src la table de départ.
     * @param dest la table d'arrivée.
     * @return un Path du départ à l'arrivée.
     */
    public Path getShortestPath(SQLTable src, SQLTable dest) {
        return Path.create(src, DijkstraShortestPath.findPathBetween(this.getGraph(), src, dest));
    }

    /**
     * Trouve tous les chemins entre from et to.
     * 
     * @param from la table de départ.
     * @param to la table d'arrivée.
     * @return un ensemble de Path.
     */
    public Set<Path> findAllPath(SQLTable from, SQLTable to) {
        return this.findAllPath(from, to, Path.get(from));
    }

    private Set<Path> findAllPath(SQLTable from, SQLTable to, Path been) {
        if (from == to) {
            return Collections.singleton(been);
        } else {
            final Set<Path> res = new HashSet<Path>();
            // pour chaque lien qui part de from
            for (final Link l : this.getGraph().edgesOf(from)) {
                // on trouve le prochain noeud
                final SQLTable neighbour = l.oppositeVertex(from);
                // on essaye si y on pas déjà été
                if (!been.getTables().contains(neighbour)) {
                    // on avance d'un cran
                    Path newBeen = been.add(l);
                    res.addAll(this.findAllPath(neighbour, to, newBeen));
                }
            }
            return res;
        }
    }

}
