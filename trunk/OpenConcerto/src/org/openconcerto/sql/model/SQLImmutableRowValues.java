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
 
 package org.openconcerto.sql.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SQLImmutableRowValues extends SQLRowAccessor {

    private final SQLRowValues delegate;

    public SQLImmutableRowValues(SQLRowValues delegate) {
        super(delegate.getTable());
        this.delegate = delegate;
    }

    @Override
    public SQLRowValues createEmptyUpdateRow() {
        return this.delegate.createEmptyUpdateRow();
    }

    @Override
    public Collection<SQLRowValues> getReferentRows() {
        return this.delegate.getReferentRows();
    }

    @Override
    public Set<SQLRowValues> getReferentRows(SQLField refField) {
        return this.delegate.getReferentRows(refField);
    }

    @Override
    public Collection<SQLRowValues> getReferentRows(SQLTable refTable) {
        return this.delegate.getReferentRows(refTable);
    }

    public int size() {
        return this.delegate.size();
    }

    @Override
    public final int getID() {
        return this.delegate.getID();
    }

    @Override
    public final Number getIDNumber() {
        return this.delegate.getIDNumber();
    }

    @Override
    public final Object getObject(String fieldName) {
        return this.delegate.getObject(fieldName);
    }

    @Override
    public Map<String, Object> getAbsolutelyAll() {
        return this.delegate.getAbsolutelyAll();
    }

    @Override
    public final SQLRowAccessor getForeign(String fieldName) {
        return this.delegate.getForeign(fieldName);
    }

    @Override
    public boolean isForeignEmpty(String fieldName) {
        return this.delegate.isForeignEmpty(fieldName);
    }

    public boolean isEmptyLink(String fieldName) {
        return this.delegate.isEmptyLink(fieldName);
    }

    public boolean isDefault(String fieldName) {
        return this.delegate.isDefault(fieldName);
    }

    @Override
    public Set<String> getFields() {
        return this.delegate.getFields();
    }

    @Override
    public final SQLRow asRow() {
        return this.delegate.asRow();
    }

    @Override
    public final SQLRowValues asRowValues() {
        return this.delegate.deepCopy();
    }

    public final String printTree() {
        return this.delegate.printTree();
    }

    public final String printGraph() {
        return this.delegate.printGraph();
    }

    public final boolean equalsGraph(SQLRowValues other) {
        return this.delegate.equalsGraph(other);
    }

    @Override
    public SQLTableListener createTableListener(SQLDataListener l) {
        return this.delegate.createTableListener(l);
    }

    @Override
    public boolean equals(Object obj) {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + " on <" + this.delegate.toString() + ">";
    }
}
