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

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * Un chemin dans une base de donnée. Un chemin est composé de tables, d'autre part les tables sont
 * reliées entre elle par un sous-ensemble des champs les reliant dans le graphe. This class is
 * immutable and thus thread-safe. Also methods that modify a path always return a new instance. If
 * a few modifications are needed consider using {@link #toBuilder()}.
 * 
 * @author ILM Informatique 4 oct. 2004
 */
@Immutable
public final class Path extends AbstractPath<Path> {

    /**
     * Create a path from tables.
     * 
     * @param root the root of the first table.
     * @param path a list tables names.
     * @return the created path.
     * @see #addTable(String)
     */
    static public Path createFromTables(DBRoot root, List<String> path) {
        final SQLTable first = root.getTable(path.get(0));
        return new PathBuilder(first).addTables(path.subList(1, path.size())).build();
    }

    private static final int CACHE_INITIAL_CAPACITY = 8;
    private static final float CACHE_LOAD_FACTOR = 0.75f;
    private static final int CACHE_MAX_SIZE = (int) (CACHE_LOAD_FACTOR * CACHE_INITIAL_CAPACITY * 8);
    static private final Map<SQLTable, Path> EMPTY_PATHS = new LinkedHashMap<SQLTable, Path>(CACHE_INITIAL_CAPACITY, CACHE_LOAD_FACTOR, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<SQLTable, Path> eldest) {
            return size() > CACHE_MAX_SIZE;
        }
    };

    static public Path get(SQLTable first) {
        synchronized (EMPTY_PATHS) {
            Path res = EMPTY_PATHS.get(first);
            if (res == null) {
                res = new Path(first);
                EMPTY_PATHS.put(first, res);
            }
            return res;
        }
    }

    /**
     * Crée un chemin à partir d'une liste de Link.
     * 
     * @param first la premiere table.
     * @param links les liens.
     * @return le chemin correspondant, ou <code>null</code> si links est <code>null</code>.
     */
    static public Path create(SQLTable first, List<Link> links) {
        // besoin de spécifier le premier, eg BATIMENT.ID_SITE => quel sens ?
        if (links == null)
            return null;
        final PathBuilder builder = new PathBuilder(first);
        for (final Link l : links)
            builder.add(l);
        return builder.build();
    }

    static private final <T> List<T> copy(List<T> l) {
        return Collections.unmodifiableList(new ArrayList<T>(l));
    }

    // arguments not checked for coherence
    static Path create(final List<SQLTable> tables, final List<Step> steps, final List<SQLField> singleFields) {
        return new Path(copy(tables), copy(steps), copy(singleFields));
    }

    private final List<SQLTable> tables;
    private final List<Step> fields;
    // after profiling: doing getStep().iterator().next() costs a lot
    private final List<SQLField> singleFields;

    public Path(SQLTable start) {
        this.tables = Collections.singletonList(start);
        this.fields = Collections.emptyList();
        this.singleFields = Collections.emptyList();
    }

    public Path(Step step) {
        this.tables = Arrays.asList(step.getFrom(), step.getTo());
        this.fields = Collections.singletonList(step);
        this.singleFields = Collections.singletonList(step.getSingleField());
    }

    // arguments not checked for coherence, nor immutability
    private Path(final List<SQLTable> tables, final List<Step> steps, final List<SQLField> singleFields) {
        this.tables = tables;
        this.fields = steps;
        this.singleFields = singleFields;
    }

    public final PathBuilder toBuilder() {
        return new PathBuilder(this);
    }

    public final Path reverse() {
        final int stepsCount = this.fields.size();
        if (stepsCount == 0)
            return this;

        final List<SQLTable> tables = new ArrayList<SQLTable>(stepsCount + 1);
        final List<Step> steps = new ArrayList<Step>(stepsCount);
        final List<SQLField> singleFields = new ArrayList<SQLField>(stepsCount);

        for (int i = stepsCount - 1; i >= 0; i--) {
            final Step reversedStep = this.fields.get(i).reverse();
            tables.add(reversedStep.getFrom());
            steps.add(reversedStep);
            singleFields.add(reversedStep.getSingleField());
        }
        tables.add(this.getFirst());
        return new Path(Collections.unmodifiableList(tables), Collections.unmodifiableList(steps), Collections.unmodifiableList(singleFields));
    }

    public Path minusFirst() {
        return this.subPath(1, this.length());
    }

    public Path minusLast() {
        return this.minusLast(1);
    }

    public Path minusLast(final int count) {
        return this.subPath(0, this.length() - count);
    }

    public Path subPath(int fromIndex) {
        return this.subPath(fromIndex, this.length());
    }

    /**
     * Returns a copy of the portion of this path between the specified <tt>fromIndex</tt>,
     * inclusive, and <tt>toIndex</tt>, exclusive. (If <tt>fromIndex</tt> and <tt>toIndex</tt> are
     * equal, the returned list is empty). Indices can be negative to count from the end, see
     * {@link CollectionUtils#getValidIndex(List, int)}
     * 
     * @param fromIndex low endpoint (inclusive) of the subPath
     * @param toIndex high endpoint (exclusive) of the subPath
     * @return the specified range within this path
     */
    public Path subPath(int fromIndex, int toIndex) {
        fromIndex = CollectionUtils.getValidIndex(this.fields, fromIndex);
        toIndex = CollectionUtils.getValidIndex(this.fields, toIndex);
        if (fromIndex == 0 && toIndex == this.length())
            return this;
        return new Path(this.tables.subList(fromIndex, toIndex + 1), this.fields.subList(fromIndex, toIndex), this.singleFields.subList(fromIndex, toIndex));
    }

    public Path justFirst() {
        return subPath(0, 1);
    }

    @Override
    public List<SQLTable> getTables() {
        return this.tables;
    }

    /**
     * Append <code>p</code> to this path.
     * 
     * @param p the path to append.
     * @return a new path.
     * @throws IllegalArgumentException if <code>p</code> doesn't begin where this ends.
     */
    @Override
    protected final Path _append(Path p) {
        final int thisLength = this.length();
        final int oLength = p.length();
        if (thisLength == 0)
            return p;
        else if (oLength == 0)
            return this;

        final int stepsCount = thisLength + oLength;
        final List<SQLTable> tables = new ArrayList<SQLTable>(stepsCount + 1);
        final List<Step> steps = new ArrayList<Step>(stepsCount);
        final List<SQLField> singleFields = new ArrayList<SQLField>(stepsCount);

        // subList() since the last table will be added from p
        tables.addAll(this.tables.subList(0, thisLength));
        tables.addAll(p.tables);
        steps.addAll(this.fields);
        steps.addAll(p.fields);
        singleFields.addAll(this.singleFields);
        singleFields.addAll(p.singleFields);

        return new Path(Collections.unmodifiableList(tables), Collections.unmodifiableList(steps), Collections.unmodifiableList(singleFields));
    }

    @Override
    final Path add(Step step) {
        return this.append(new Path(step));
    }

    @Override
    public final List<Step> getSteps() {
        return this.fields;
    }

    @Override
    public Path addTables(List<String> names) {
        return toBuilder().addTables(names).build();
    }

    /**
     * Add multiple steps to the path.
     * 
     * @param fieldsNames foreign fields.
     * @return a new path.
     * @see #addForeignField(String)
     */
    @Override
    public final Path addForeignFields(final String... fieldsNames) {
        return toBuilder().addForeignFields(fieldsNames).build();
    }

    /**
     * The step connecting the table i to i+1.
     * 
     * @param i the step asked, from 0 to {@link #length()} -1 or negative (the last step being -1).
     * @return the requested step.
     */
    public final Step getStep(int i) {
        return this.fields.get(CollectionUtils.getValidIndex(this.fields, i, true));
    }

    /**
     * The fields connecting the table i to i+1.
     * 
     * @param i the index of the step.
     * @return the fields connecting the table i to i+1.
     * @see #getStep(int)
     */
    public final Set<SQLField> getStepFields(int i) {
        return this.getStep(i).getFields();
    }

    /**
     * Return the field connecting the table i to i+1, or <code>null</code> if there is more than
     * one (ie {@link #isSingleLink()} is <code>false</code>).
     * 
     * @param i the step asked, from 0 to {@link #length()} -1.
     * @return the field connecting the table i to i+1, or <code>null</code> if there is more than
     *         one.
     */
    public SQLField getSingleStep(int i) {
        return this.singleFields.get(i);
    }

    /**
     * Return the fields connecting the tables.
     * 
     * @return a list of fields and <code>null</code>s.
     * @see #getSingleStep(int)
     */
    public final List<SQLField> getSingleSteps() {
        return this.singleFields;
    }

    /**
     * Whether there's one and only one field between each table.
     * 
     * @return <code>true</code> if there's exactly 1 field between each table.
     */
    public boolean isSingleLink() {
        for (final SQLField step : this.singleFields) {
            if (step == null)
                return false;
        }
        return true;
    }

    public final Set<Path> getSingleLinkPaths() {
        if (this.length() == 0)
            return Collections.singleton(this);

        final Set<Path> res = new HashSet<Path>();
        for (final Path p : this.subPath(1).getSingleLinkPaths()) {
            for (final Step s : this.getStep(0).getSingleSteps()) {
                res.add(new PathBuilder(s).append(p).build());
            }
        }
        return res;
    }

    /**
     * Whether the step <code>i</code> is backwards.
     * 
     * @param i the index of the step.
     * @return <code>true</code> if all fields are backwards (eg going from CONTACT to SITE with
     *         ID_CONTACT_CHEF and ID_CONTACT_BUREAU), <code>null</code> if mixed (eg going from
     *         CONTACT to SITE with SITE.ID_CONTACT_CHEF and CONTACT.ID_SITE), <code>false</code>
     *         otherwise.
     */
    public final Boolean isBackwards(final int i) {
        final Boolean foreign = this.getStep(i).isForeign();
        return foreign == null ? null : !foreign;
    }

    /**
     * The direction of all the steps.
     * 
     * @return <code>null</code> if this is empty or not all steps' directions are equal, otherwise
     *         the direction of all the steps (i.e. {@link Direction#ANY} if they're all mixed).
     * @see Step#getDirection()
     */
    public final Direction getDirection() {
        final Set<Direction> directions = new HashSet<Link.Direction>(this.fields.size());
        for (final Step s : this.fields) {
            directions.add(s.getDirection());
        }
        return CollectionUtils.getSole(directions);
    }

    /**
     * Whether the direction of this path is <code>dir</code>.
     * 
     * @param dir a direction, not <code>null</code>.
     * @return <code>true</code> if this is empty or if the direction of all steps is
     *         <code>dir</code>.
     * @see #getDirection()
     */
    public final boolean isDirection(Direction dir) {
        if (dir == null)
            throw new NullPointerException("Null direction");
        return this.length() == 0 || this.getDirection() == dir;
    }

    /**
     * Whether all steps are in the same (non-mixed) direction.
     * 
     * @return <code>null</code> if at least one step is mixed, <code>true</code> if all steps are
     *         in the same direction (be it backwards or forwards), <code>false</code> if some steps
     *         are forwards and some are backwards.
     */
    final Boolean isSingleDirection() {
        return this.isSingleDirection(null);
    }

    /**
     * Whether all steps are in the passed direction.
     * 
     * @param foreign <code>true</code> if all steps should be forwards.
     * @return <code>null</code> if at least one step is mixed, <code>true</code> if all steps are
     *         in the same direction as <code>foreign</code>.
     */
    final Boolean isSingleDirection(final boolean foreign) {
        return this.isSingleDirection(Boolean.valueOf(foreign));
    }

    // don't expose null since it could be confused as meaning "all steps have mixed directions"
    private final Boolean isSingleDirection(final Boolean foreign) {
        Boolean dir = foreign;
        for (final Step s : this.fields) {
            final Boolean stepIsForeign = s.isForeign();
            if (stepIsForeign == null)
                return null;
            else if (dir == null)
                dir = stepIsForeign;
            else if (!dir.equals(stepIsForeign))
                return false;
        }
        return true;
    }

    public String toString() {
        return "Path\n\tTables: " + this.tables + "\n\tLinks:" + this.fields;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Path) {
            final Path o = (Path) obj;
            // no need to compare tables, starting from the same point the same fields lead to the
            // same table
            return this.getFirst().equals(o.getFirst()) && this.fields.equals(o.fields);
        } else
            return false;
    }

    /**
     * Returns a hash code value for this path. NOTE: as with any list, the hashCode change when its
     * items do.
     * 
     * @return a hash code value for this object.
     * @see java.util.List#hashCode()
     */
    @Override
    public int hashCode() {
        return this.fields.hashCode();
    }

    public boolean startsWith(Path other) {
        return this.length() >= other.length() && this.subPath(0, other.length()).equals(other);
    }
}
