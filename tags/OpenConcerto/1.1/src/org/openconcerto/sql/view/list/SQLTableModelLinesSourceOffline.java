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

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lines are stored in SQLRowValues commited to the database on demand. This class is thread-safe.
 * 
 * @author Sylvain
 */
public class SQLTableModelLinesSourceOffline extends SQLTableModelLinesSource {

    private final SQLTableModelSourceOffline parent;
    private final List<ListSQLLine> lines;
    // since new lines have no database ID, give them a virtual one
    private int freeID;
    private final Map<Integer, ListSQLLine> id2line;
    // values that can be modified
    private final SQLRowValues modifiableVals;
    // original value for modified lines
    private final Map<ListSQLLine, SQLRowValues> dbVals;
    // removed lines
    private final Set<ListSQLLine> deleted;

    {
        this.lines = new LinkedList<ListSQLLine>();
        // the firsts are used in other part of the fwk
        this.freeID = SQLRow.MIN_VALID_ID - 10;
        this.id2line = new HashMap<Integer, ListSQLLine>();
    }

    public SQLTableModelLinesSourceOffline(SQLTableModelSourceOffline parent, ITableModel model) {
        super(model);
        this.parent = parent;
        this.modifiableVals = this.getParent().getElem().getPrivateGraph();
        this.dbVals = new IdentityHashMap<ListSQLLine, SQLRowValues>();
        this.deleted = new HashSet<ListSQLLine>();
        this.reset();
    }

    @Override
    public final SQLTableModelSourceOffline getParent() {
        return this.parent;
    }

    public synchronized final void add(SQLRowValues vals) {
        // make sure every needed path is there
        vals.grow(this.getParent().getFetcher().getGraph(), false);
        final ListSQLLine newLine = this.createLine(vals);
        // fire
        if (newLine != null)
            newLine.clearCache();
    }

    /**
     * Loose any change and refetch from the database.
     */
    public synchronized final void reset() {
        this.lines.clear();
        this.id2line.clear();
        this.dbVals.clear();
        this.deleted.clear();

        for (final SQLRowValues r : this.getParent().getFetcher().fetch())
            this.add(r);
    }

    public synchronized final ListSQLLine rm(int index) {
        return this.rm(this.lines.get(index));
    }

    private synchronized ListSQLLine rm(final ListSQLLine line) {
        if (line != null) {
            this._rm(line);
            // add to a list of id to archive if it's in the db
            if (line.getRow().hasID())
                this.deleted.add(line);
            // fire
            this.fireLineChanged(line.getID(), null, null);
        }
        return line;
    }

    private synchronized void _rm(final ListSQLLine l) {
        if (l != null) {
            this.lines.remove(l);
            this.id2line.remove(l.getID());
        }
    }

    public synchronized List<ListSQLLine> getAll() {
        return Collections.unmodifiableList(this.lines);
    }

    public synchronized ListSQLLine get(final int id) {
        final SQLRowValuesListFetcher f = new SQLRowValuesListFetcher(this.getParent().getFetcher().getGraph());
        f.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                if (ListSQLRequest.lockSelect)
                    input.addWaitPreviousWriteTXTable(getParent().getPrimaryTable().getName());
                final Where w = new Where(getParent().getPrimaryTable().getKey(), "=", id);
                return getParent().getFetcher().getSelTransf().transformChecked(input).andWhere(w);
            }
        });
        // since we use "=" pk, either 1 or 0
        final SQLRowValues row = CollectionUtils.getSole(f.fetch());
        // TODO warn if overwriting changes
        if (row == null)
            // if this id is not part of us, rm from our list
            this._rm(this.id2line.get(id));
        return this.createLine(row);
    }

    @Override
    protected synchronized void lineCreated(ListSQLLine res) {
        super.lineCreated(res);
        this.id2line.put(res.getID(), res);
        this.lines.add(res);
    }

    protected synchronized int getID(SQLRowValues r) {
        return r.hasID() ? r.getID() : this.freeID--;
    }

    @Override
    public synchronized int compare(ListSQLLine l1, ListSQLLine l2) {
        if (l1 == l2)
            return 0;
        for (final ListSQLLine l : this.lines) {
            if (l == l1)
                return -1;
            else if (l == l2)
                return 1;
        }
        throw new IllegalArgumentException("neither " + l1 + " nor " + l2 + " in " + this);
    }

    @Override
    public void commit(ListSQLLine l, Path path, SQLRowValues vals) {
        checkCanModif(l, path);
        l.loadAt(vals, path);
    }

    private synchronized void checkCanModif(ListSQLLine l, Path path) {
        if (this.modifiableVals.followPath(path) == null)
            throw new IllegalArgumentException("can only modify " + this.modifiableVals);
        // if l isn't in the db, no need to update, the new values will be inserted
        if (l.getRow().hasID() && !this.dbVals.containsKey(l)) {
            // copy the initial state
            this.dbVals.put(l, l.getRow().deepCopy());
        }
    }

    // no need to synch : modifiableVals is final
    public void changeFK(ListSQLLine l, Path p, int id) {
        checkCanModif(l, p.subPath(0, p.length() - 1));
        if (this.modifiableVals.followPath(p) != null)
            throw new IllegalArgumentException("can only modify a foreign key of " + this.modifiableVals);

        // executer la requete dans la updateQueue
        this.getModel().getUpdateQ().put(new ChangeFKRunnable(l, p, id));
    }

    /**
     * Make all changes applied to this persistent.
     * 
     * @throws SQLException if an error occurs.
     */
    public synchronized final void commit() throws SQLException {
        // insert, copy since we will remove some of the lines
        for (final ListSQLLine l : CopyUtils.copy(this.lines))
            if (!l.getRow().hasID()) {
                // its virtual ID is no longer valid, so remove the line
                this.rm(l);
                // the commit will trigger a #get(ID) that will determine if the row is to be added
                // only commit modified values, avoid updating each local, batiment, etc
                l.getRow().prune(this.modifiableVals).commit();
            }

        // update
        for (final Map.Entry<ListSQLLine, SQLRowValues> e : this.dbVals.entrySet())
            this.getParent().getElem().update(e.getValue(), e.getKey().getRow()).exec();
        this.dbVals.clear();

        // delete
        for (final ListSQLLine l : this.deleted)
            getParent().getElem().archive(l.getRow().getID());
        this.deleted.clear();
    }
}
