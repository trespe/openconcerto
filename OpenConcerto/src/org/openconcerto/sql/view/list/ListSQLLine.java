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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.list.search.SearchQueue;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A line used by SQLTableModelSource, posessing an order and an id. Compare is done on the order.
 * 
 * @author Sylvain
 */
public final class ListSQLLine implements Comparable<ListSQLLine> {

    public static final int indexFromID(final List<ListSQLLine> l, final int id) {
        int foundIndex = -1;
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final int currentID = l.get(i).getID();
            if (currentID == id) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    private final SQLTableModelLinesSource src;
    private final SQLRowValues row;
    private final int id;
    // lists are accessed by Swing (model.getValueAt()) and
    // by the search queue (SearchRunnable#matchFilter(ListSQLLine line))
    private final List<Object> list;
    // count of column values loaded in this.list
    // (to avoid loading debug columns, which took more time than the regular columns, ie more than
    // half the time was passed on almost never displayed values)
    private int loadedCol;
    private final List<Object> pubList;

    public ListSQLLine(SQLTableModelLinesSource src, SQLRowValues row, int id) {
        super();
        this.src = src;
        this.row = row;
        this.id = id;
        this.list = new ArrayList<Object>();
        this.pubList = Collections.unmodifiableList(this.list);
        this.loadedCol = 0;
    }

    // load at least columnCount values
    private synchronized void loadCache(int columnCount) {
        if (this.loadedCol >= columnCount)
            return;

        try {
            final List<SQLTableModelColumn> allCols = this.src.getParent().getAllColumns();
            for (int i = this.loadedCol; i < columnCount; i++)
                this.list.add(allCols.get(i).show(this.row));
            this.loadedCol = columnCount;
        } catch (RuntimeException e) {
            // the list length must be equal to the column count
            // if we're interrupted, come back to a safe state
            this.list.clear();
            this.loadedCol = 0;
            throw e;
        }
    }

    public final SQLTableModelLinesSource getSrc() {
        return this.src;
    }

    public final SQLRowValues getRow() {
        return this.row;
    }

    @Override
    public int compareTo(ListSQLLine o) {
        if (this.src != o.src)
            throw new IllegalArgumentException(this.src + " != " + o.src);
        return this.src.compare(this, o);
    }

    public int getID() {
        return this.id;
    }

    public synchronized List<Object> getList(int columnCount) {
        this.loadCache(columnCount);
        return this.pubList;
    }

    public final void setValueAt(Object obj, int colIndex) {
        this.src.getParent().getColumn(colIndex).put(this, obj);
    }

    public final void updateValueAt(Set<Integer> colIndexes) {
        if (colIndexes.size() == 0)
            return;
        final int max = Collections.max(colIndexes).intValue();
        synchronized (this) {
            final int alreadyLoaded = this.loadedCol;
            this.loadCache(max);
            for (final int colIndex : colIndexes) {
                // no need to update twice colIndex
                if (colIndex < alreadyLoaded)
                    // MAYBE first iterate to fetch the new values and then merge them to list,
                    // otherwise if there's an exn list will be half updated
                    this.list.set(colIndex, this.src.getParent().getColumn(colIndex).show(this.getRow()));
            }
        }
        this.src.fireLineChanged(this.getID(), this, colIndexes);
    }

    public void clearCache() {
        synchronized (this) {
            this.list.clear();
            this.loadedCol = 0;
        }
        this.src.fireLineChanged(this.getID(), this, null);
    }

    /**
     * Load the passed values into this row at the passed path.
     * 
     * @param id ID of vals, needed when vals is <code>null</code>.
     * @param vals values to load, eg CONTACT.NOM = "Dupont".
     * @param p where to load the values, eg "SITE.ID_CONTACT_CHEF".
     */
    void loadAt(int id, SQLRowValues vals, Path p) {
        final String lastReferentField = SearchQueue.getLastReferentField(p);
        // load() empties vals, so getFields() before
        final Set<Integer> indexes = lastReferentField == null ? this.pathToIndex(p, vals.getFields()) : null;
        // replace our values with the new ones
        if (lastReferentField == null) {
            for (final SQLRowValues v : this.getRow().followPath(p, CreateMode.CREATE_NONE, false)) {
                v.load(vals.deepCopy(), null);
            }
        } else {
            // e.g. if p is SITE <- BATIMENT <- LOCAL, lastField is LOCAL.ID_BATIMENT
            // if p is SITE -> CLIENT <- SITE (i.e. siblings of a site), lastField is SITE.ID_CLIENT
            final SQLField lastField = p.getStep(-1).getSingleField();
            final Collection<SQLRowValues> previous;
            if (p.length() > 1 && p.getStep(-2).reverse().equals(p.getStep(-1)))
                previous = this.getRow().followPath(p.minusLast(2), CreateMode.CREATE_NONE, false);
            else
                previous = null;
            // the rows that vals should point to, e.g. BATIMENT or CLIENT
            final Collection<SQLRowValues> targets = this.getRow().followPath(p.minusLast(), CreateMode.CREATE_NONE, false);
            for (final SQLRowValues target : targets) {
                // remove existing referent with the updated ID
                SQLRowValues toRemove = null;
                for (final SQLRowValues toUpdate : target.getReferentRows(lastField)) {
                    // don't back track (in the example a given SITE will be at the primary location
                    // and a second time along its siblings)
                    if ((previous == null || !previous.contains(toUpdate)) && toUpdate.getID() == id) {
                        if (toRemove != null)
                            throw new IllegalStateException("Duplicate IDs " + id + " : " + System.identityHashCode(toRemove) + " and " + System.identityHashCode(toUpdate) + "\n"
                                    + this.getRow().printGraph());
                        toRemove = toUpdate;
                    }
                }
                if (toRemove != null)
                    toRemove.remove(lastField.getName());
                // attach updated values
                if (vals != null && vals.getLong(lastField.getName()) == target.getIDNumber().longValue())
                    vals.deepCopy().put(lastField.getName(), target);
            }
        }
        // update our cache
        if (indexes == null)
            this.clearCache();
        else
            this.updateValueAt(indexes);
    }

    /**
     * Find the columns that use the modifiedFields for their value.
     * 
     * @param p the path to the modified fields, eg "CPI.ID_LOCAL".
     * @param modifiedFields the field modified, eg "DESIGNATION".
     * @return the index of columns using "CPI.ID_LOCAL.DESIGNATION", or null for every columns.
     */
    private Set<Integer> pathToIndex(final Path p, final Collection<String> modifiedFields) {
        if (containsFK(p.getLast(), modifiedFields)) {
            // e.g. CPI.ID_LOCAL, easier to just refresh the whole line, than to search for each
            // column affected (that would mean expanding the FK)
            return null;
        } else {
            final Set<Integer> res = new HashSet<Integer>();
            final Set<FieldPath> modifiedPaths = FieldPath.create(p, modifiedFields);
            final List<? extends SQLTableModelColumn> cols = this.src.getParent().getAllColumns();
            for (int i = 0; i < cols.size(); i++) {
                final SQLTableModelColumn col = cols.get(i);
                if (CollectionUtils.containsAny(col.getPaths(), modifiedPaths))
                    res.add(i);
            }
            return res;
        }
    }

    private static boolean containsFK(final SQLTable t, Collection<String> fields) {
        for (final String f : fields) {
            if (t.getForeignKeys().contains(t.getField(f)))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.row;
    }
}
