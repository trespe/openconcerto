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
 
 package org.openconcerto.sql.model.graph;

import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;

import java.util.Iterator;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;

/**
 * Classe de base entre les graphes.
 * 
 * @author ILM Informatique 22 décembre 2004
 */
@ThreadSafe
public abstract class BaseGraph {

    @GuardedBy("this")
    private final Graph<SQLTable, Link> graph;

    protected BaseGraph(DirectedGraph<SQLTable, Link> g) {
        this.graph = g;
    }

    protected BaseGraph(UndirectedGraph<SQLTable, Link> g) {
        this.graph = g;
    }

    /**
     * Retourne une chaine représentant ce graphe. D'abord les noeuds puis les liens:
     * 
     * <pre>
     *        A
     *        B
     *        C
     *   
     *        A B   label
     * </pre>
     * 
     * Utile pour visualiser le graphe de la table dans JGraphPad.
     * 
     * @return une représentation de ce graphe.
     */
    public synchronized String dump() {
        StringBuffer sb = new StringBuffer();

        Set vertices = this.graph.vertexSet();
        Iterator i = vertices.iterator();
        while (i.hasNext()) {
            SQLTable table = (SQLTable) i.next();
            sb.append(table.getSQLName()).append(table.hashCode()).append("\n");
        }

        sb.append("\n");

        Set edges = this.graph.edgeSet();
        i = edges.iterator();
        while (i.hasNext()) {
            Link l = (Link) i.next();
            final SQLName src = l.getSource().getSQLName();
            final SQLName dest = l.getTarget().getSQLName();
            sb.append(src).append("\t").append(dest).append("\t").append(l.getLabel().getName()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Retourne toutes les tables qui sont liées de près ou de loin à la table passée.
     * 
     * @param table la table source.
     * @return toutes les tables qui sont liées à la table passée.
     */
    public synchronized Set<SQLTable> getConnectedSet(SQLTable table) {
        final ConnectivityInspector<SQLTable, Link> insp;
        if (this.graph instanceof DirectedGraph)
            insp = new ConnectivityInspector<SQLTable, Link>((DirectedGraph<SQLTable, Link>) this.graph);
        else
            insp = new ConnectivityInspector<SQLTable, Link>((UndirectedGraph<SQLTable, Link>) this.graph);
        return insp.connectedSetOf(table);
    }

    public synchronized final Set<SQLTable> getAllTables() {
        return this.graph.vertexSet();
    }

    protected synchronized final Graph<SQLTable, Link> getGraph() {
        return this.graph;
    }
}
