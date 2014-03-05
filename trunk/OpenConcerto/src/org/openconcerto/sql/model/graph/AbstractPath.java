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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractPath<T extends AbstractPath<T>> {

    public final SQLTable getFirst() {
        return this.getTable(0);
    }

    public final SQLTable getLast() {
        return this.getTable(this.getTables().size() - 1);
    }

    /**
     * La table se trouvant à la position demandée dans ce chemin.
     * 
     * @param i l'index, entre 0 et length() inclus.
     * @return la table se trouvant à la position demandée.
     */
    public final SQLTable getTable(int i) {
        return this.getTables().get(i);
    }

    public abstract List<SQLTable> getTables();

    public final int length() {
        return this.getSteps().size();
    }

    public final T append(Path p) {
        if (this.getLast() != p.getFirst())
            throw new IllegalArgumentException("this ends at " + this.getLast() + " while the other begins at " + p.getFirst());
        return this._append(p);
    }

    protected abstract T _append(Path p);

    abstract T add(Step step);

    /**
     * Add a table at the end of the path. NOTE: the step will be composed of all the foreign fields
     * between {@link #getLast()} and <code>destTable</code>.
     * 
     * @param destTable the table to add.
     * @return an instance with the table added.
     * @throws IllegalArgumentException if <code>destTable</code> has no foreign fields between
     *         itself and the current end of this path.
     */
    public final T add(final SQLTable destTable) {
        return this.add(destTable, Direction.ANY);
    }

    public final T add(final SQLTable destTable, final Direction dir) {
        return this.add(Step.create(getLast(), destTable, dir));
    }

    /**
     * Add a table at the end of the path. NOTE: the step will be composed of all the foreign fields
     * between {@link #getLast()} and <code>tableName</code>.
     * 
     * @param tableName the table name.
     * @return an instance with the table added.
     */
    public final T addTable(final String tableName) {
        return this.addTable(tableName, Direction.ANY, false);
    }

    public final T addTable(final String tableName, final Direction dir, final boolean onlyOne) {
        return this.add(dir, null, tableName, null, onlyOne);
    }

    public final T addTables(String... names) {
        return this.addTables(Arrays.asList(names));
    }

    public abstract T addTables(List<String> names);

    public final T addForeignTable(final String tableName) {
        return this.addForeignTable(tableName, null);
    }

    /**
     * Add a table at the end of the path if there's only one <b>foreign<b> link between the end and
     * it.
     * 
     * @param tableName the table name.
     * @param rootName the name of the table root, <code>null</code> to not use.
     * @return an instance with the table added.
     */
    public final T addForeignTable(final String tableName, final String rootName) {
        return this.add(Direction.FOREIGN, null, tableName, rootName, true);
    }

    /**
     * Add a step to the path.
     * 
     * @param fField a foreign field.
     * @return an instance with the field added.
     * @throws IllegalArgumentException if <code>fField</code> is not a foreign field or if neither
     *         of its ends are the current end of this path.
     */
    public final T add(final SQLField fField) {
        return this.add(fField, Direction.ANY);
    }

    /**
     * Add a step to the path.
     * 
     * @param fField a foreign field.
     * @param direction how to cross <code>fField</code>, <code>ANY</code> to infer it.
     * @return an instance with the field added.
     * @throws IllegalArgumentException if <code>fField</code> is not a foreign field or if neither
     *         of its ends are the current end of this path, or if <code>direction</code> isn't
     *         valid.
     * @see Step#create(SQLTable, SQLField, Direction)
     */
    public final T add(final SQLField fField, final Direction direction) {
        return this.add(Step.create(getLast(), fField, direction));
    }

    /**
     * Add a step to the path.
     * 
     * @param fields fields between the last table and another.
     * @return an instance with the step added.
     */
    public final T addStepWithFields(final Collection<SQLField> fields) {
        final Set<Link> links = new HashSet<Link>(fields.size());
        final DatabaseGraph graph = getFirst().getDBSystemRoot().getGraph();
        for (final SQLField f : fields) {
            links.add(graph.getForeignLink(f));
        }
        return this.add(links);
    }

    /**
     * Add multiple steps to the path.
     * 
     * @param fieldsNames foreign fields.
     * @return an instance with the steps added.
     * @see #addForeignField(String)
     */
    public abstract T addForeignFields(final String... fieldsNames);

    public final T addForeignField(final String fieldName) {
        return this.add(Direction.FOREIGN, fieldName, null, null, true);
    }

    public final T addReferentField(final String fieldName) {
        return this.addReferent(fieldName, null, null);
    }

    public final T addReferentTable(final String tableName) {
        return this.addReferent(null, tableName, null);
    }

    /**
     * Add a table at the end of this path if there's only one link matching the parameters.
     * 
     * @param fieldName the field name, <code>null</code> to not use.
     * @param tableName the name of the added table, <code>null</code> to not use.
     * @param rootName the name of the root of the added table, <code>null</code> to not use.
     * @return an instance with the table added.
     */
    public final T addReferent(final String fieldName, final String tableName, final String rootName) {
        return this.add(Direction.REFERENT, fieldName, tableName, rootName, true);
    }

    /**
     * Add a step to the path.
     * 
     * @param dir the direction of the new step.
     * @param fieldName the field name, <code>null</code> to not use.
     * @param tableName the name of the added table, <code>null</code> to not use.
     * @param rootName the name of the root of the added table, <code>null</code> to not use.
     * @param onlyOne <code>true</code> if one and only one link should match.
     * @return an instance with the step added.
     * @throws IllegalStateException if <code>onlyOne</code> is <code>true</code> and not one and
     *         only one link matching.
     */
    public final T add(final Direction dir, final String fieldName, final String tableName, final String rootName, final boolean onlyOne) {
        final Set<Link> links = this.getLast().getDBSystemRoot().getGraph().getLinks(getLast(), dir, onlyOne, new Link.NamePredicate(getLast(), rootName, tableName, fieldName));
        return this.add(links, dir);
    }

    /**
     * Add a step to the path.
     * 
     * @param links links from the current end of this path to another table.
     * @return an instance with the step added.
     * @throws IllegalArgumentException if <code>links</code> are not all between the current end
     *         and the same other table.
     */
    public final T add(final Collection<Link> links) {
        return this.add(links, Direction.ANY);
    }

    public final T add(final Collection<Link> links, Direction dir) {
        final Step step;
        if (dir == Direction.ANY) {
            step = Step.create(getLast(), links);
        } else {
            step = Step.create(CollectionUtils.fillMap(new HashMap<Link, Direction>(), links, dir));
            if (step.getFrom() != getLast())
                throw new IllegalArgumentException("links from " + step.getFrom() + " not " + getLast());
        }
        return this.add(step);
    }

    /**
     * Ajoute un maillon a la chaine.
     * 
     * @param item un lien.
     * @return an instance with the step added.
     * @throws IllegalArgumentException si aucune des extremités de item n'est connectée à ce
     *         chemin.
     */
    public final T add(Link item) {
        return this.add(item, Direction.ANY);
    }

    public final T add(Link l, Direction direction) {
        return this.add(Step.create(getLast(), l, direction));
    }

    public abstract List<Step> getSteps();
}
