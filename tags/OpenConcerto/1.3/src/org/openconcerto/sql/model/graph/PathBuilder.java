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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allow to build efficiently a {@link Path}. This class is not thread-safe but allow to apply
 * modifications to a path without creating a new instance for each one of them.
 * 
 * @author Sylvain
 * @see #build()
 */
public class PathBuilder extends AbstractPath<PathBuilder> {

    private final List<SQLTable> tables;
    private final List<Step> fields;
    private final List<SQLField> singleFields;

    public PathBuilder(SQLTable start) {
        this.tables = new ArrayList<SQLTable>();
        this.fields = new ArrayList<Step>();
        this.singleFields = new ArrayList<SQLField>();
        this.tables.add(start);
    }

    public PathBuilder(Step step) {
        this.tables = new ArrayList<SQLTable>();
        this.tables.add(step.getFrom());
        this.tables.add(step.getTo());
        this.fields = new ArrayList<Step>();
        this.fields.add(step);
        this.singleFields = new ArrayList<SQLField>();
        this.singleFields.add(step.getSingleField());
    }

    public PathBuilder(Path p) {
        this.tables = new ArrayList<SQLTable>(p.getTables());
        // ok since Step is immutable
        this.fields = new ArrayList<Step>(p.getSteps());
        this.singleFields = new ArrayList<SQLField>(p.getSingleSteps());
    }

    public final Path build() {
        return Path.create(this.tables, this.fields, this.singleFields);
    }

    @Override
    public List<SQLTable> getTables() {
        return Collections.unmodifiableList(this.tables);
    }

    @Override
    protected final PathBuilder _append(final Path p) {    
        this.fields.addAll(p.getSteps());
        this.singleFields.addAll(p.getSingleSteps());
        this.tables.addAll(p.getTables().subList(1, p.getTables().size()));
        return this;
    }

    @Override
    final PathBuilder add(Step step) {
        assert step.getFrom() == this.getLast() : "broken path";
        this.fields.add(step);
        this.singleFields.add(step.getSingleField());
        this.tables.add(step.getTo());
        return this;
    }

    @Override
    public PathBuilder addTables(List<String> names) {
        PathBuilder res = this;
        for (final String name : names)
            res = res.addTable(name);
        return res;
    }

    @Override
    public PathBuilder addForeignFields(String... fieldsNames) {
        PathBuilder res = this;
        for (final String name : fieldsNames)
            res = res.addForeignField(name);
        return res;
    }

    @Override
    public final List<Step> getSteps() {
        return Collections.unmodifiableList(this.fields);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + this.tables + "\n\tLinks:" + this.fields;
    }
}
