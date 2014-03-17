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

import org.openconcerto.sql.model.SQLTable;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.IntegerNameProvider;

/**
 * Classe de base entre les graphes.
 * 
 * @author ILM Informatique 22 décembre 2004
 */
@ThreadSafe
public abstract class BaseGraph {

    static private final Comparator<SQLTable> TABLE_COMP = new Comparator<SQLTable>() {
        @Override
        public int compare(SQLTable o1, SQLTable o2) {
            return o1.getSQLName().quote().compareTo(o2.getSQLName().quote());
        }
    };
    static private final Comparator<Link> LINK_COMP = new Comparator<Link>() {
        @Override
        public int compare(Link o1, Link o2) {
            final int srcRes = TABLE_COMP.compare(o1.getSource(), o2.getSource());
            if (srcRes != 0)
                return srcRes;
            final int destRes = TABLE_COMP.compare(o1.getTarget(), o2.getTarget());
            if (destRes != 0)
                return destRes;
            return o1.getCols().toString().compareTo(o2.getCols().toString());
        }
    };

    @GuardedBy("this")
    private final Graph<SQLTable, Link> graph;

    protected BaseGraph(DirectedGraph<SQLTable, Link> g) {
        this.graph = g;
    }

    protected BaseGraph(UndirectedGraph<SQLTable, Link> g) {
        this.graph = g;
    }

    /**
     * Export this graph to the Trivial Graph Format. Utile pour visualiser le graphe de la base
     * dans yEd.
     * 
     * @return a TGF string.
     */
    public String toTGF() {
        // used TGF since :
        // GmlExporter only use toString() on vertex and edges.
        // GraphMLExporter is customizable but files created by jgrapht, yEd or gephi are not
        // compatible (i.e. labels use different attributes)
        return this.print(true);
    }

    /**
     * A string representing this graph.
     * 
     * <pre>
     *        A
     *        B
     *        C
     *        #   
     *        A B   label
     * </pre>
     * 
     * @return a string representation.
     */
    public String print() {
        return this.print(false);
    }

    private synchronized String print(final boolean tgf) {
        final StringBuffer sb = new StringBuffer();

        final IntegerNameProvider<SQLTable> p = tgf ? new IntegerNameProvider<SQLTable>() : null;

        final Set<SQLTable> vertices = new TreeSet<SQLTable>(TABLE_COMP);
        vertices.addAll(this.graph.vertexSet());
        for (final SQLTable table : vertices) {
            if (tgf)
                sb.append(p.getVertexName(table)).append('\t');
            sb.append(table.getSQLName()).append('\n');
        }

        sb.append("#\n");

        final Set<Link> edges = new TreeSet<Link>(LINK_COMP);
        edges.addAll(this.graph.edgeSet());
        for (final Link l : edges) {
            final String src;
            final String dest;
            if (tgf) {
                src = p.getVertexName(l.getSource());
                dest = p.getVertexName(l.getTarget());
            } else {
                src = l.getSource().getSQLName().quote();
                dest = l.getContextualName().quote();
            }
            sb.append(src).append('\t').append(dest).append('\t').append(l.getCols()).append('\n');
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
