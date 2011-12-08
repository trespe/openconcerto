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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.Constraint;
import org.openconcerto.sql.model.DBFileCache;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IClosure;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Diff {

    /**
     * If <code>true</code> the table changes will be outputted sequentially. This is more legible
     * but cannot be executed when cycles exist.
     */
    public static final String SIMPLE_SEQ = "org.openconcerto.sql.Diff.simpleSeq";
    private static final String ROOTS_TO_MAP = "rootsToMap";

    private static void usage() {
        System.out.println("Usage: " + Diff.class.getName() + " url1 url2 [tableName]...");
        System.out.println("Outputs SQL statements to patch url1. That is if you execute the statements on url1 it will become url2.");
        System.out.println("System properties: " + ROOTS_TO_MAP + "=list of roots to map");
    }

    public static void main(String[] args) throws URISyntaxException {
        if (args.length < 2) {
            usage();
            System.exit(1);
        }
        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");
        // for caching the db
        System.setProperty(DBFileCache.APP_NAME, Diff.class.getName());

        final SQL_URL url1 = SQL_URL.create(args[0]);
        final SQL_URL url2 = SQL_URL.create(args[1]);
        final List<String> tables = args.length < 3 ? null : Arrays.asList(args).subList(2, args.length);

        final DBRoot root1 = getRoot(url1);
        final DBRoot root2 = getRoot(url2);
        final Diff diff = new Diff(root1, root2);
        if (tables == null)
            System.out.println(diff.compute());
        else
            System.out.println(diff.compute(tables));
        root1.getServer().destroy();
        root2.getServer().destroy();
    }

    private static DBRoot getRoot(final SQL_URL url1) {
        final DBSystemRoot sysRoot = SQLServer.create(url1, SQLRow.toList(System.getProperty(ROOTS_TO_MAP, "")), new IClosure<SQLDataSource>() {
            @Override
            public void executeChecked(SQLDataSource input) {
                input.addConnectionProperty("allowMultiQueries", "true");
            }
        });
        return sysRoot.getRoot(url1.getRootName());
    }

    private static String getDesc(final DBRoot root) {
        final SQL_URL url = root.getURL();
        if (url != null) {
            return url.asString();
        } else {
            return "root " + SQLBase.quoteIdentifier(root.getName()) + " of " + root.getDBSystemRoot().getDataSource().getUrl();
        }
    }

    private final DBRoot a, b;

    public Diff(DBRoot a, DBRoot b) {
        super();
        this.a = a;
        this.b = b;
    }

    private final String getHeader(final Collection<String> tableName) {
        final String t = tableName == null ? "" : "/" + tableName;
        return "-- To change " + getDesc(this.a) + t + "\n-- into " + getDesc(this.b) + t + "\n-- \n";
    }

    private Set<String> getTablesUnion() {
        return CollectionUtils.union(this.a.getChildrenNames(), this.b.getChildrenNames());
    }

    public final String compute() {
        return this.getHeader(null) + this.computeBody(getTablesUnion());
    }

    public final List<ChangeTable<?>> getChangeTables() {
        return this.getChangeTables(getTablesUnion());
    }

    public final List<ChangeTable<?>> getChangeTables(final Collection<String> tables) {
        final List<ChangeTable<?>> l = new ArrayList<ChangeTable<?>>();
        for (final String table : tables) {
            final ChangeTable<?> compute = this.computeP(table);
            if (compute != null) {
                l.add(compute);
            }
        }
        return l;
    }

    private final String computeBody(final Collection<String> tables) {
        final List<ChangeTable<?>> l = getChangeTables(tables);
        if (Boolean.getBoolean(SIMPLE_SEQ)) {
            final StringBuilder sb = new StringBuilder();
            for (final ChangeTable<?> compute : l) {
                sb.append("\n-- " + compute.getName() + "\n");
                sb.append(compute.asString(this.a.getName()));
                sb.append("\n");
            }
            return sb.toString();
        } else
            return ChangeTable.catToString(l, this.a.getName());
    }

    public final String compute(final Collection<String> tableName) {
        return this.getHeader(tableName) + this.computeBody(tableName);
    }

    private final ChangeTable<?> computeP(final String tableName) {
        final boolean inA = this.a.contains(tableName);
        final boolean inB = this.b.contains(tableName);
        if (!inA && !inB)
            return null;
        else if (inA && !inB)
            return new DropTable(this.a.getTable(tableName));
        else if (!inA && inB) {
            final SQLTable bT = this.b.getTable(tableName);
            return bT.getCreateTable(this.a.getServer().getSQLSystem());
        } else {
            // in both
            final SQLTable aT = this.a.getTable(tableName);
            final SQLTable bT = this.b.getTable(tableName);
            if (aT.equalsDesc(bT))
                return null;
            else {
                final AlterTable alterTable = new AlterTable(aT);
                {
                    final Set<String> aFields = aT.getFieldsName();
                    final Set<String> bFields = bT.getFieldsName();
                    for (final String rm : CollectionUtils.substract(aFields, bFields)) {
                        alterTable.dropColumn(rm);
                    }
                    for (final String added : CollectionUtils.substract(bFields, aFields)) {
                        alterTable.addColumn(bT.getField(added));
                    }
                    for (final String common : CollectionUtils.inter(aFields, bFields)) {
                        final SQLField aF = aT.getField(common);
                        final SQLField bF = bT.getField(common);
                        final Map<Properties, String> diff = aF.getDiffMap(bF, bT.getServer().getSQLSystem(), true);
                        alterTable.alterColumn(common, bF, diff.keySet());
                    }
                }
                if (!aT.getPKsNames().equals(bT.getPKsNames()))
                    throw new UnsupportedOperationException(tableName + " has pks diff: " + aT.getPKsNames() + " != " + bT.getPKsNames());

                // foreign keys
                {
                    final DatabaseGraph graph = this.a.getDBSystemRoot().getGraph();
                    final DatabaseGraph bGraph = this.b.getDBSystemRoot().getGraph();
                    final Set<String> aFKs = new SQLFieldsSet(aT.getForeignKeys()).getFieldsNames(aT);
                    final Set<String> bFKs = new SQLFieldsSet(bT.getForeignKeys()).getFieldsNames(bT);
                    for (final String rm : CollectionUtils.substract(aFKs, bFKs)) {
                        final Link foreignLink = graph.getForeignLink(aT.getField(rm));
                        if (foreignLink.getName() == null)
                            throw new IllegalStateException(foreignLink + " is not a real constraint, use AddFK");
                        alterTable.dropForeignConstraint(foreignLink.getName());
                    }
                    for (final String added : CollectionUtils.substract(bFKs, aFKs)) {
                        final Link link = bGraph.getForeignLink(bT.getField(added));
                        alterTable.addForeignConstraint(link, false);
                    }
                    for (final String common : CollectionUtils.inter(aFKs, bFKs)) {
                        final SQLName aPath = graph.getForeignLink(aT.getField(common)).getContextualName();
                        final SQLName bPath = bGraph.getForeignLink(bT.getField(common)).getContextualName();
                        if (!aPath.equals(bPath))
                            throw new UnsupportedOperationException(common + " is different: " + aPath + " != " + bPath);
                    }
                }

                // indexes
                try {
                    // order irrelevant
                    final Set<Index> aIndexes = new HashSet<Index>(aT.getIndexes());
                    final Set<Index> bIndexes = new HashSet<Index>(bT.getIndexes());
                    for (final Index rm : CollectionUtils.substract(aIndexes, bIndexes)) {
                        alterTable.dropIndex(rm.getName());
                    }
                    for (final Index added : CollectionUtils.substract(bIndexes, aIndexes)) {
                        // TODO mv system.autoCreatesFKIndex() of getCreateTable() into ChangeTable
                        // (ok in MySQL : the explicit CREATE INDEXs replace the implicit ones)
                        alterTable.addIndex(added);
                    }
                } catch (SQLException e) {
                    throw new UnsupportedOperationException("couldn't get indexes", e);
                }

                // constraints
                {
                    final Set<Constraint> aConstr = aT.getConstraints();
                    final Set<Constraint> bConstr = bT.getConstraints();
                    for (final Constraint rm : CollectionUtils.substract(aConstr, bConstr)) {
                        alterTable.dropConstraint(rm.getName());
                    }
                    for (final Constraint added : CollectionUtils.substract(bConstr, aConstr)) {
                        if (added.getType() == ConstraintType.UNIQUE)
                            alterTable.addUniqueConstraint(added.getName(), added.getCols());
                        else
                            throw new UnsupportedOperationException("unsupported constraint: " + added);
                    }
                }

                return alterTable;
            }
        }
    }

}
