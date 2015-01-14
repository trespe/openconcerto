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
 
 package org.openconcerto.sql.changer.convert;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.SetMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Merge rows of the passed table. I.e. it updates each referencing link, and finally archive the
 * duplicate rows.
 * 
 * @author Sylvain
 */
public class MergeRows extends Changer<SQLTable> {

    public static final String FIELDS_TO_COMPARE_PROP = "fieldsToCompare";

    private final Set<String> fieldsToCompare;

    public MergeRows(final DBSystemRoot b) {
        super(b);
        this.fieldsToCompare = new HashSet<String>();
    }

    public final void setFieldsToCompare(final Collection<String> fieldsToCompare) {
        this.fieldsToCompare.clear();
        this.fieldsToCompare.addAll(fieldsToCompare);
    }

    @Override
    protected Class<? extends DBStructureItem<?>> getMaxLevel() {
        // fields are by table
        return SQLTable.class;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        final String prop = System.getProperty(FIELDS_TO_COMPARE_PROP);
        if (prop != null && prop.length() != 0) {
            this.setFieldsToCompare(Arrays.asList(prop.split(",")));
        }
    }

    protected SQLRowValues createGraph(final SQLTable t) {
        final SQLRowValues res = new SQLRowValues(t).putNulls(this.fieldsToCompare, false);
        if (t.isArchivable())
            res.putNulls(t.getArchiveField().getName());
        return res;
    }

    protected SQLRowValuesListFetcher createFetcher(final SQLTable t) {
        // ordered : predictable and repeatable
        return SQLRowValuesListFetcher.create(this.createGraph(t), true);
    }

    protected boolean shouldMerge(SQLRowValues r1, SQLRowValues r2) {
        // would merge every row
        if (this.fieldsToCompare.size() == 0)
            return false;
        for (final String f : this.fieldsToCompare) {
            // two empty values generally don't mean that rows should merge
            if (!isEqualNonEmptyValue(r1, r2, f))
                return false;
        }
        return true;
    }

    protected boolean isEqualNonEmptyValue(final SQLRowAccessor r1, final SQLRowAccessor r2, final String f) {
        return isEqualValue(r1, r2, f, true);
    }

    protected boolean isEqualValue(final SQLRowAccessor r1, final SQLRowAccessor r2, final String f, final boolean nonEmpty) {
        if (!r1.getFields().contains(f) || !r2.getFields().contains(f))
            throw new IllegalStateException("Missing " + f);
        final Object normalized1 = normalize(f, r1.getObject(f));
        final Object normalized2 = normalize(f, r2.getObject(f));
        if (!CompareUtils.equals(normalized1, normalized2))
            return false;
        return !nonEmpty || !(isEmpty(f, normalized1) && isEmpty(f, normalized2));
    }

    protected Object normalize(String fieldName, Object o) {
        if (o instanceof String) {
            return ((String) o).trim().toLowerCase();
        }
        return o;
    }

    protected boolean isEmpty(String f, Object normalized) {
        return normalized == null || "".equals(normalized);
    }

    // leave emptyUpdateRow empty to update nothing
    // the first row will remain unarchived
    protected void mergeFields(final SQLRowValues emptyUpdateRow, final List<SQLRow> rows) {
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        if (!t.getDBSystemRoot().isMappingAllRoots())
            throw new IllegalStateException("Not mapping all roots means not all referent tables can be found and thus leave unarchived rows pointing to archived rows of " + t);
        this.getStream().println("merging rows of " + t.getSQLName() + " using " + this.fieldsToCompare + "... ");

        final SQLRowValuesListFetcher fetcher = this.createFetcher(t);
        this.getStream().println("fetcher : " + fetcher.getGraph().printGraph());
        final List<SQLRowValues> rows = fetcher.fetch();
        if (rows.size() <= 1)
            return;

        final ListMap<SQLRow, SQLRow> toMerge = new ListMap<SQLRow, SQLRow>();
        final Set<SQLRow> unicity = new HashSet<SQLRow>();
        for (final SQLRowValues r1 : rows) {
            final Iterator<SQLRowValues> iter = rows.iterator();
            // since shouldMerge() is transitive, if shouldMerge(r1, r2) then shouldMerge(r1, x) for
            // each x in toMerge.get(r2)
            List<SQLRow> transitiveRows = Collections.emptyList();
            boolean done = false;
            while (iter.hasNext() && !done) {
                final SQLRowValues r2 = iter.next();
                final SQLRow r2Row = r2.asRow();
                // only need to check the bottom half of the matrix since shouldMerge() is
                // commutative
                done = r2 == r1;
                if (!done && !transitiveRows.contains(r2Row) && shouldMerge(r1, r2)) {
                    // since we are in the bottom half, r2 is before r1 in rows
                    toMerge.add(r2Row, r1.asRow());
                    assert unicity.add(r1.asRow());
                    transitiveRows = toMerge.get(r2Row);
                }
            }
        }
        if (toMerge.size() == 0) {
            getStream().println("No duplicates found");
            return;
        }
        getStream().println("Found " + toMerge.size() + " duplicates :");
        getStream().println(toMerge);

        final List<List<String>> mappingRows = new ArrayList<List<String>>();
        final List<Number> toArchive = new ArrayList<Number>();
        // merge fields
        final List<SQLRowValues> toUpdate = new ArrayList<SQLRowValues>();
        final List<SQLRow> tmpDups = new ArrayList<SQLRow>(16);
        final SQLRowValues tmpVals = new SQLRowValues(t);
        for (final Entry<SQLRow, List<SQLRow>> e : toMerge.entrySet()) {
            final SQLRow mergeDest = e.getKey();
            tmpDups.clear();
            tmpDups.add(mergeDest);
            tmpDups.addAll(e.getValue());
            tmpVals.clear();
            this.mergeFields(tmpVals, tmpDups);
            if (tmpVals.size() > 0) {
                final SQLRowValues toUpdateVals = tmpVals.deepCopy();
                toUpdateVals.setPrimaryKey(mergeDest);
                toUpdate.add(toUpdateVals);
            }
            for (final SQLRow rowToMerge : e.getValue()) {
                mappingRows.add(Arrays.asList(rowToMerge.getID() + "", mergeDest.getID() + ""));
                toArchive.add(rowToMerge.getIDNumber());
            }
        }

        getStream().println("To update:\n" + toUpdate);

        if (this.isDryRun())
            return;

        final String contantTableName = "mergeMapping";
        final String constantTable = getSyntax().getConstantTable(mappingRows, contantTableName, Arrays.asList("OLD_ID", "NEW_ID"));

        SQLUtils.executeAtomic(getDS(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                // merge fields
                for (final SQLRowValues v : toUpdate) {
                    v.commit();
                }

                final SetMap<SQLTable, String> toFire = new SetMap<SQLTable, String>();
                for (final Link refLink : t.getDBSystemRoot().getGraph().getReferentLinks(t)) {
                    getStream().println("updating : " + refLink.getSource().getSQLName() + " " + refLink.getCols());
                    final UpdateBuilder updateBuilder = new UpdateBuilder(refLink.getSource());
                    updateBuilder.addVirtualJoin(constantTable, contantTableName, true, "OLD_ID", refLink.getSingleField().getName());
                    updateBuilder.setFromVirtualJoinField(refLink.getSingleField().getName(), contantTableName, "NEW_ID");
                    getDS().execute(updateBuilder.asString());
                    toFire.add(refLink.getSource(), refLink.getSingleField().getName());
                }

                // checked at the method start
                assert t.getDBSystemRoot().isMappingAllRoots() : "Some rows might still point to us";
                final UpdateBuilder archiveUpdate = new UpdateBuilder(t);
                archiveUpdate.setObject(t.getArchiveField(), 1);
                archiveUpdate.setWhere(new Where(t.getKey(), toArchive));
                getStream().println("archiving : " + archiveUpdate.asString());
                getDS().execute(archiveUpdate.asString());
                toFire.add(t, t.getArchiveField().getName());

                for (final Entry<SQLTable, Set<String>> e : toFire.entrySet()) {
                    e.getKey().fireTableModified(SQLRow.NONEXISTANT_ID, e.getValue());
                }

                return null;
            }
        });
    }
}
