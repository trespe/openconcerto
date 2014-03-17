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
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Tuple2.List2;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * An immutable step in a {@link Path}.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public class Step {

    /**
     * Create a new step crossing <code>fField</code>.
     * 
     * @param start the start of the step.
     * @param fField a foreign field.
     * @param direction how to cross <code>fField</code>, <code>ANY</code> to infer it.
     * @return a new step.
     * @throws IllegalArgumentException if <code>fField</code> is not a foreign field, if neither of
     *         its ends are <code>start</code>, if <code>direction</code> is ANY and
     *         <code>fField</code> points to its source, or if <code>direction</code> is not
     *         <code>ANY</code> and wrong.
     */
    public static final Step create(final SQLTable start, final SQLField fField, final Direction direction) throws IllegalArgumentException {
        final Link l = fField.getDBSystemRoot().getGraph().getForeignLink(fField);
        if (l == null)
            throw new IllegalArgumentException(fField + " is not a foreign field.");
        return create(start, l, direction);
    }

    public static final Step create(final SQLTable start, final Link l, final Direction direction) throws IllegalArgumentException {
        if (l == null)
            throw new NullPointerException("null link");
        // throws an exception if l is not connected to start
        final SQLTable end = l.oppositeVertex(start);

        final Direction computedDirection;
        if (start == end)
            computedDirection = Direction.ANY;
        else
            computedDirection = Direction.fromForeign(l.getSource() == start);

        if (computedDirection == Direction.ANY && direction == Direction.ANY)
            throw new IllegalArgumentException("self reference : " + l + ", you must specify the direction");
        if (direction != Direction.ANY && computedDirection != Direction.ANY && direction != computedDirection)
            throw new IllegalArgumentException("wrong direction: " + direction + ", real is : " + computedDirection);
        final Direction nonNullDir = direction == Direction.ANY ? computedDirection : direction;
        assert nonNullDir != Direction.ANY;

        return new Step(start, l, nonNullDir, end);
    }

    public static final Step create(final SQLTable start, final SQLTable end) {
        return create(start, end, Direction.ANY);
    }

    public static final Step create(final SQLTable start, final SQLTable end, final Direction dir) {
        final Set<Link> set;
        if (dir == Direction.ANY) {
            set = start.getDBSystemRoot().getGraph().getLinks(start, end);
        } else if (dir == Direction.FOREIGN) {
            set = start.getDBSystemRoot().getGraph().getForeignLinks(start, end);
        } else if (dir == Direction.REFERENT) {
            set = start.getDBSystemRoot().getGraph().getForeignLinks(end, start);
        } else {
            throw new IllegalStateException("Unknown direction " + dir);
        }
        if (set.isEmpty())
            throw new IllegalArgumentException("path is broken between " + start + " and " + end + " in direction " + dir);
        if (dir == Direction.ANY) {
            return create(start, set);
        } else {
            final Map<Link, Direction> links = new HashMap<Link, Direction>(set.size());
            for (final Link l : set)
                links.put(l, dir);
            return new Step(start, links, end);
        }
    }

    public static final Step create(final SQLTable start, final Collection<Link> links) {
        if (links.size() == 0)
            throw new IllegalArgumentException("empty fields");
        final SQLTable end = links.iterator().next().oppositeVertex(start);
        if (start == end)
            throw new IllegalArgumentException("start and end are the same: " + links + " the direction can't be inferred");
        final Map<Link, Direction> directions = new HashMap<Link, Direction>();
        for (final Link l : links) {
            if (end != l.oppositeVertex(start))
                throw new IllegalArgumentException("fields do not point to the same table: " + links);
            directions.put(l, Direction.fromForeign(start == l.getSource()));
        }

        return new Step(start, directions, end);
    }

    private static final List2<SQLTable> getStartEnd(final Link l, final Direction dir) {
        if (dir == Direction.ANY)
            throw new IllegalArgumentException("Unspecified direction");
        if (dir == Direction.FOREIGN)
            return new List2<SQLTable>(l.getSource(), l.getTarget());
        else
            return new List2<SQLTable>(l.getTarget(), l.getSource());
    }

    /**
     * Create a new step with the passed links.
     * 
     * @param links the links to cross, direction cannot be {@link Direction#ANY}.
     * @return a new step.
     * @throws IllegalArgumentException if <code>links</code> is empty, if {@link Direction#ANY} is
     *         present, or if not all links share end points.
     */
    public static final Step create(final Map<Link, Direction> links) {
        List2<SQLTable> startEnd = null;
        for (final Entry<Link, Direction> e : links.entrySet()) {
            final List2<SQLTable> currentStartEnd = getStartEnd(e.getKey(), e.getValue());
            if (startEnd == null) {
                startEnd = currentStartEnd;
            } else if (!startEnd.equals(currentStartEnd)) {
                throw new IllegalArgumentException("Start and end tables differ " + startEnd + " != " + currentStartEnd);
            }
        }
        if (startEnd == null)
            throw new IllegalArgumentException("empty links");
        return new Step(startEnd.get0(), links, startEnd.get1());
    }

    private final SQLTable from;
    private final SQLTable to;
    private final Map<Link, Direction> fields;
    // after profiling: doing getStep().iterator().next() costs a lot
    // if there's only one link and it has a single field
    private final SQLField singleField;
    // if all links have single fields
    @GuardedBy("this")
    private Set<SQLField> singleFields;
    @GuardedBy("this")
    private boolean singleFieldsComputed;

    // all constructors are private since they don't fully check the coherence of their parameters
    private Step(final SQLTable start, final Map<Link, Direction> fields, final SQLField singleField, final SQLTable end) {
        assert start != null && end != null;
        assert fields.size() > 0;
        assert !new HashSet<Direction>(fields.values()).contains(Direction.ANY) : "some directions are unknown : " + fields;
        // thread-safe since only mutable attributes are volatile
        assert fields instanceof AbstractMap : "Fields might not be thread-safe";
        this.from = start;
        this.to = end;
        this.fields = Collections.unmodifiableMap(fields);
        this.singleField = singleField;
        if (singleField == null) {
            this.singleFields = null;
            this.singleFieldsComputed = false;
        } else {
            this.singleFields = Collections.singleton(singleField);
            this.singleFieldsComputed = true;
        }
    }

    private Step(final SQLTable start, final Map<Link, Direction> fields, final SQLTable end) {
        this(start, new HashMap<Link, Direction>(fields), fields.size() == 1 ? CollectionUtils.getSole(fields.keySet()).getSingleField() : null, end);
    }

    private Step(final SQLTable start, Link field, final Direction foreign, final SQLTable end) {
        this(start, Collections.singletonMap(field, foreign), field.getSingleField(), end);
    }

    public Step(Step p) {
        this(p.from, p.fields, p.to);
    }

    public final Step reverse() {
        final Map<Link, Direction> reverseFields = new HashMap<Link, Direction>(this.fields.size());
        for (final Entry<Link, Direction> e : this.fields.entrySet()) {
            reverseFields.put(e.getKey(), e.getValue().reverse());
        }
        final Step res = new Step(this.to, reverseFields, this.singleField, this.from);
        // no need to synchronize on res, it isn't yet published
        synchronized (this) {
            res.singleFields = this.singleFields;
            res.singleFieldsComputed = this.singleFieldsComputed;
        }
        return res;
    }

    public final SQLTable getFrom() {
        return this.from;
    }

    public final SQLTable getTo() {
        return this.to;
    }

    public final Set<Link> getLinks() {
        return this.fields.keySet();
    }

    public synchronized final Set<SQLField> getFields() {
        if (!this.singleFieldsComputed) {
            this.singleFields = Link.getSingleFields(this.fields.keySet());
            this.singleFieldsComputed = true;
        }
        return this.singleFields;
    }

    public final SQLField getSingleField() {
        return this.singleField;
    }

    public final Set<Step> getSingleSteps() {
        if (this.singleField != null)
            return Collections.singleton(this);
        final Set<Step> res = new HashSet<Step>(this.fields.size());
        for (final Entry<Link, Direction> e : this.fields.entrySet()) {
            res.add(new Step(this.getFrom(), e.getKey(), e.getValue(), this.getTo()));
        }
        return res;
    }

    /**
     * Whether this step goes through the link <code>f</code> forwards or backwards.
     * 
     * @param f the link.
     * @return <code>true</code> if f is crossed forwards (e.g. going from SITE to CONTACT with
     *         ID_CONTACT_CHEF and ID_CONTACT_BUREAU).
     */
    public final boolean isForeign(final Link f) {
        return this.getDirection(f) == Direction.FOREIGN;
    }

    /**
     * Whether this step goes through the link <code>f</code> forwards or backwards.
     * 
     * @param f the link.
     * @return <code>FOREIGN</code> if f is crossed forwards (e.g. going from SITE to CONTACT with
     *         ID_CONTACT_CHEF and ID_CONTACT_BUREAU), <code>REFERENT</code> otherwise.
     */
    public final Direction getDirection(final Link f) {
        return this.fields.get(f);
    }

    /**
     * Whether this step goes through all of its links forwards or backwards.
     * 
     * @return <code>true</code> if all links are forwards, <code>null</code> if mixed.
     * @see #getDirection()
     */
    public final Boolean isForeign() {
        final Direction soleDir = getDirection();
        return soleDir == Direction.ANY ? null : soleDir == Direction.FOREIGN;
    }

    /**
     * Whether this step goes through all of its links forwards or backwards.
     * 
     * @return <code>FOREIGN</code> or <code>REFERENT</code> if all links go the same way,
     *         <code>ANY</code> if mixed.
     * @see #getDirection(Link)
     */
    public final Direction getDirection() {
        final Direction soleDir = CollectionUtils.getSole(new HashSet<Direction>(this.fields.values()));
        return soleDir == null ? Direction.ANY : soleDir;
    }

    public String toString() {
        return this.getClass().getSimpleName() + " from: " + this.getFrom() + " to: " + this.getTo() + "\n" + this.fields;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Step) {
            final Step o = (Step) obj;
            // no need to compare to, starting from the same point with the same fields lead to the
            // same table
            return this.from.equals(o.from) && this.fields.equals(o.fields);
        } else
            return false;
    }

    /**
     * Returns a hash code value for this step.
     * 
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return this.fields.hashCode();
    }
}
