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

import org.openconcerto.sql.model.SQLRowValuesCluster.State;
import org.openconcerto.sql.model.SQLRowValuesCluster.ValueChangeListener;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.request.Inserter.ReturnMode;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.NumberUtils;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.IdentitySet;
import org.openconcerto.utils.cc.LinkedIdentitySet;
import org.openconcerto.utils.cc.TransformedMap;
import org.openconcerto.utils.convertor.NumberConvertor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class that represent a row of a table that can be modified before being inserted or updated.
 * The row might not actually exists in the database, and it might not define all the fields. One
 * can put SQLRowValues as a foreign key value, so that it will be inserted as well.
 * 
 * @author Sylvain CUAZ
 * @see #load(SQLRowAccessor, Set)
 * @see #put(String, Object)
 * @see #insert()
 * @see #update(int)
 */
public final class SQLRowValues extends SQLRowAccessor {

    public static enum ForeignCopyMode {
        /**
         * Copy no SQLRowValues.
         */
        NO_COPY,
        /**
         * Copy the id of SQLRowValues if any, otherwise don't copy anything. This keeps the maximum
         * of information without any foreign rowValues.
         */
        COPY_ID_OR_RM,
        /**
         * Copy the id of SQLRowValues if any, otherwise copy the row. This keeps all the
         * information.
         */
        COPY_ID_OR_ROW,
        /**
         * Copy every SQLRowValues.
         */
        COPY_ROW
    }

    static public enum CreateMode {
        /**
         * Never create rows.
         */
        CREATE_NONE,
        /**
         * For multi-link step, create one row with all links.
         */
        CREATE_ONE,
        /**
         * For multi-link step, create one row for each link.
         */
        CREATE_MANY
    }

    public static final Object SQL_DEFAULT = new Object() {
        @Override
        public String toString() {
            return SQLRowValues.class.getSimpleName() + ".SQL_DEFAULT";
        }
    };
    /**
     * Empty foreign field value.
     * 
     * @see #putEmptyLink(String)
     */
    public static final Object SQL_EMPTY_LINK = new Object() {
        @Override
        public String toString() {
            return SQLRowValues.class.getSimpleName() + ".SQL_EMPTY_LINK";
        }
    };

    private static boolean checkValidity = true;

    public static void setValidityChecked(final boolean b) {
        checkValidity = b;
    }

    /**
     * Whether or not {@link #getInvalid()} is called before each data modification.
     * 
     * @return <code>true</code> if the validity is checked.
     */
    public static boolean isValidityChecked() {
        return checkValidity;
    }

    private static final boolean DEFAULT_ALLOW_BACKTRACK = true;

    private final Map<String, Object> values;
    private final Map<String, SQLRowValues> foreigns;
    private final CollectionMap<SQLField, SQLRowValues> referents;
    private SQLRowValuesCluster graph;
    private CollectionMap<SQLField, ReferentChangeListener> referentsListener;

    public SQLRowValues(SQLTable t) {
        super(t);
        // use LinkedHashSet so that the order is preserved, see #walkFields()
        // don't use value too low for initialCapacity otherwise rehash operations
        this.values = new LinkedHashMap<String, Object>(8);
        this.foreigns = new HashMap<String, SQLRowValues>(4);
        // extend (rather than use another constructor) for performance
        // eg 20% gained when fetching 45000 rows
        this.referents = new CollectionMap<SQLField, SQLRowValues>(4) {
            @SuppressWarnings("unchecked")
            @Override
            public Collection<SQLRowValues> createCollection(Collection coll) {
                // use LinkedHashSet so that the order is preserved, eg we can iterate over LOCALs
                // pointing to a BATIMENT with consistent and predictable (insertion-based) order.
                // use IdentitySet to be able to put two equal instances
                return coll == null ? new LinkedIdentitySet<SQLRowValues>() : new LinkedIdentitySet<SQLRowValues>(coll);
            }
        };
        // no used much so lazy init
        this.referentsListener = null;
        this.graph = new SQLRowValuesCluster(this);
    }

    public SQLRowValues(SQLTable t, Map<String, ?> values) {
        this(t);
        this.setAll(values);
    }

    public SQLRowValues(SQLRowValues vals) {
        this(vals, ForeignCopyMode.COPY_ROW);
    }

    /**
     * Create a new instance with the same values. If <code>copyForeigns</code> is <code>true</code>
     * the new instance will have exactly the same values, ie it will point to the same
     * SQLRowValues. If <code>copyForeigns</code> is <code>false</code> all SQLRowValues will be
     * left out.
     * 
     * @param vals the instance to copy.
     * @param copyForeigns whether to copy foreign SQLRowValues.
     */
    public SQLRowValues(SQLRowValues vals, ForeignCopyMode copyForeigns) {
        this(vals.getTable());
        // setAll() takes care of foreigns and referents
        this.setAll(vals.getAllValues(copyForeigns));
    }

    @Override
    public SQLRowValues createEmptyUpdateRow() {
        return new SQLRowValues(this.getTable()).setID(this.getID());
    }

    /**
     * Copy this rowValues and all others connected to it. Ie contrary to
     * {@link #SQLRowValues(SQLRowValues)} the result will not point to the same rowValues, but to
     * copy of them.
     * 
     * @return a copy of this.
     */
    public final SQLRowValues deepCopy() {
        return this.getGraph().deepCopy(this);
    }

    // *** graph

    private synchronized void updateLinks(String fieldName, Object old, Object value) {
        // try to avoid getTable().getField() (which takes 1/3 of put() for nothing when there is no
        // rowvalues)
        final boolean oldRowVals = old instanceof SQLRowValues;
        final boolean newRowVals = value instanceof SQLRowValues;
        if (!oldRowVals && !newRowVals)
            return;

        final SQLField f = this.getTable().getField(fieldName);

        if (oldRowVals) {
            final SQLRowValues vals = (SQLRowValues) old;
            vals.referents.remove(f, this);
            this.foreigns.remove(fieldName);
            assert this.graph == vals.graph;
            this.graph.remove(this, f, vals);
            vals.fireRefChange(f, false, this);
        }
        if (newRowVals) {
            final SQLRowValues vals = (SQLRowValues) value;
            vals.referents.put(f, this);
            this.foreigns.put(fieldName, vals);
            this.graph.add(this, f, vals);
            assert this.graph == vals.graph;
            vals.fireRefChange(f, true, this);
        }
    }

    public synchronized final SQLRowValuesCluster getGraph() {
        return this.graph;
    }

    public final <T> void walkGraph(T acc, ITransformer<State<T>, T> closure) {
        this.getGraph().walk(this, acc, closure);
    }

    /**
     * Walk through the fields of the rowValues in order. Eg if you added DESIGNATION, ID_BATIMENT
     * pointing to {DESIGNATION}, then INCLURE, <code>closure</code> would be called with
     * LOCAL.DESIGNATION, LOCAL.ID_BATIMENT.DESIGNATION, LOCAL.INCLURE. This can't be done using
     * {@link SQLRowValuesCluster#walk(SQLRowValues, Object, ITransformer, RecursionType)} since it
     * walks through rowValues so if you use {@link RecursionType#BREADTH_FIRST} you'll be passed
     * LOCAL, then BATIMENT and the reverse if you use {@link RecursionType#DEPTH_FIRST}.
     * 
     * @param closure what to do on each field.
     */
    public final void walkFields(IClosure<FieldPath> closure) {
        this.walkFields(closure, false);
    }

    public final void walkFields(IClosure<FieldPath> closure, final boolean includeFK) {
        this.getGraph().walkFields(this, closure, includeFK);
    }

    public final SQLRowValues prune(SQLRowValues graph) {
        return this.getGraph().prune(this, graph);
    }

    /**
     * Fetch if necessary and store in this the foreign row.
     * 
     * @param fk a foreign key, eg "ID_FAMILLE_2".
     * @return the foreign row, eg FAMILLE[1].
     */
    public final SQLRowValues grow(String fk) {
        final Object val = this.getContainedObject(fk);
        // if fk is in our map with a null value, nothing to grow
        if (val != null && !(val instanceof SQLRowValues)) {
            final SQLRowValues vals = new SQLRowValues(this.getTable());
            vals.putRowValues(fk).setAllToNull();
            this.grow(vals, true);
        }
        return (SQLRowValues) this.getForeign(fk);
    }

    public final SQLRowValues grow(SQLRowValues graph) {
        return this.grow(graph, true);
    }

    /**
     * Grow this rowValues to match the passed graph. If this was /RECEPTEUR/ : {DESIGNATION="des";
     * ID_LOCAL=2} and <code>graph</code> is /RECEPTEUR/ : {DESIGNATION=null; ID_LOCAL:
     * /LOCAL/:{DESIGNATION=null}}, then now this is /RECEPTEUR/ : {DESIGNATION="des"; ID_LOCAL:
     * /LOCAL/:{ID=2, DESIGNATION="local"}}
     * 
     * @param graph the target graph.
     * @param checkFields <code>true</code> if missing fields should be fetched.
     * @return this.
     * @throws IllegalArgumentException if this couldn't be grown.
     */
    public final SQLRowValues grow(SQLRowValues graph, final boolean checkFields) {
        graph.getGraph().grow(graph, this, checkFields);
        return this;
    }

    public final boolean graphContains(SQLRowValues graph) {
        return this.getGraph().contains(this, graph) == null;
    }

    void setGraph(SQLRowValuesCluster g) {
        assert g != null;
        this.graph = g;
    }

    final Map<String, SQLRowValues> getForeigns() {
        return Collections.unmodifiableMap(this.foreigns);
    }

    final Map<SQLField, SQLRowValues> getForeignsBySQLField() {
        return new TransformedMap<String, SQLField, SQLRowValues>(this.getForeigns(), new ITransformer<String, SQLField>() {
            @Override
            public SQLField transformChecked(String input) {
                return getTable().getField(input);
            }
        }, new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return input.getName();
            }
        });
    }

    final CollectionMap<SQLField, SQLRowValues> getReferents() {
        return this.referents;
    }

    @Override
    public Collection<SQLRowValues> getReferentRows() {
        // remove the backdoor since values() returns a view
        // remove duplicates (e.g. this is a CONTACT referenced by ID_CONTACT_RAPPORT &
        // ID_CONTACT_RDV from the same site)
        return this.referents.createCollection(this.referents.values());
    }

    @Override
    public Set<SQLRowValues> getReferentRows(SQLField refField) {
        return (Set<SQLRowValues>) this.referents.getNonNull(refField);
    }

    @Override
    public Collection<SQLRowValues> getReferentRows(SQLTable refTable) {
        // remove duplicates
        final Collection<SQLRowValues> res = this.referents.createCollection(null);
        assert res.isEmpty();
        for (final Map.Entry<SQLField, Collection<SQLRowValues>> e : this.referents.entrySet()) {
            if (e.getKey().getTable().equals(refTable))
                res.addAll(e.getValue());
        }
        return res;
    }

    /**
     * Remove all links pointing to this.
     * 
     * @return this.
     */
    public final SQLRowValues clearReferents() {
        return this.changeReferents(null, false);
    }

    public final SQLRowValues removeReferents(final SQLField f) {
        // don't use changeReferents() as it's less optimal
        for (final SQLRowValues ref : new ArrayList<SQLRowValues>(this.getReferentRows(f))) {
            ref.remove(f.getName());
        }
        return this;
    }

    public final SQLRowValues retainReferents(final SQLField f) {
        return this.changeReferents(f, true);
    }

    private final SQLRowValues changeReferents(final SQLField f, final boolean retain) {
        if (f != null || !retain) {
            // copy otherwise ConcurrentModificationException
            for (final Entry<SQLField, Collection<SQLRowValues>> e : CopyUtils.copy(this.getReferents()).entrySet()) {
                if (f == null || e.getKey().equals(f) != retain) {
                    for (final SQLRowValues ref : e.getValue()) {
                        ref.put(e.getKey().getName(), null);
                    }
                }
            }
        }
        return this;
    }

    public SQLRowValues retainReferent(SQLRowValues toRetain) {
        return this.retainReferents(Collections.singleton(toRetain));
    }

    public SQLRowValues retainReferents(Collection<SQLRowValues> toRetain) {
        toRetain = CollectionUtils.toIdentitySet(toRetain);
        // copy otherwise ConcurrentModificationException
        for (final Entry<SQLField, Collection<SQLRowValues>> e : CopyUtils.copy(this.getReferents()).entrySet()) {
            for (final SQLRowValues ref : e.getValue()) {
                if (!toRetain.contains(ref))
                    ref.put(e.getKey().getName(), null);
            }
        }
        return this;
    }

    // *** get

    public int size() {
        return this.values.size();
    }

    @Override
    public final int getID() {
        final Number res = this.getIDNumber();
        if (res != null)
            return res.intValue();
        else
            return SQLRow.NONEXISTANT_ID;
    }

    @Override
    public final Number getIDNumber() {
        final Object res = this.getObject(this.getTable().getKey().getName());
        if (res instanceof Number) {
            return (Number) res;
        } else
            return null;
    }

    @Override
    public final Object getObject(String fieldName) {
        return this.values.get(fieldName);
    }

    @Override
    public Map<String, Object> getAbsolutelyAll() {
        return getAllValues(ForeignCopyMode.COPY_ROW);
    }

    protected final Map<String, Object> getAllValues(ForeignCopyMode copyForeigns) {
        final Map<String, Object> toAdd;
        if (copyForeigns == ForeignCopyMode.COPY_ROW || this.foreigns.size() == 0) {
            toAdd = this.values;
        } else {
            final Set<Entry<String, Object>> entrySet = this.values.entrySet();
            toAdd = new LinkedHashMap<String, Object>(entrySet.size());
            for (final Map.Entry<String, Object> e : entrySet) {
                if (!(e.getValue() instanceof SQLRowValues)) {
                    toAdd.put(e.getKey(), e.getValue());
                } else if (copyForeigns != ForeignCopyMode.NO_COPY) {
                    final SQLRowValues foreign = (SQLRowValues) e.getValue();
                    if (foreign.hasID())
                        toAdd.put(e.getKey(), foreign.getIDNumber());
                    else if (copyForeigns == ForeignCopyMode.COPY_ID_OR_ROW)
                        toAdd.put(e.getKey(), foreign);
                }
            }
        }
        return Collections.unmodifiableMap(toAdd);
    }

    /**
     * Return the foreign row, if any, for the passed field.
     * 
     * @param fieldName name of the foreign field.
     * @return if <code>null</code> or a SQLRowValues one was put at <code>fieldName</code>, return
     *         it ; else assume that an ID was put at <code>fieldName</code> and return a new SQLRow
     *         with it.
     * @throws IllegalArgumentException if fieldName is not a foreign field or if it isn't contained
     *         in this instance.
     * @throws ClassCastException if the value is neither a SQLRowValues, nor <code>null</code> nor
     *         a Number.
     */
    @Override
    public final SQLRowAccessor getForeign(String fieldName) throws IllegalArgumentException, ClassCastException {
        // keep getForeignTable at the 1st line since it does the check
        final SQLTable foreignTable = this.getForeignTable(fieldName);
        final Object val = this.getContainedObject(fieldName);
        if (val instanceof SQLRowAccessor) {
            return (SQLRowAccessor) val;
        } else if (val == null) {
            // since we used getContainedObject(), it means that a null was put in our map, not that
            // fieldName wasn't there
            return null;
        } else if (this.isDefault(fieldName)) {
            throw new IllegalStateException(fieldName + " is DEFAULT");
        } else {
            return new SQLRow(foreignTable, this.getInt(fieldName));
        }
    }

    private Object getContainedObject(String fieldName) throws IllegalArgumentException {
        if (!this.values.containsKey(fieldName))
            throw new IllegalArgumentException("Field " + fieldName + " not present in this : " + this.getFields());
        return this.values.get(fieldName);
    }

    /**
     * Returns the foreign table of <i>fieldName</i>.
     * 
     * @param fieldName the name of a foreign field, eg "ID_ARTICLE_2".
     * @return the table the field points to (never <code>null</code>), eg |ARTICLE|.
     * @throws IllegalArgumentException if <i>fieldName</i> is not a foreign field.
     */
    private final SQLTable getForeignTable(String fieldName) throws IllegalArgumentException {
        final DatabaseGraph graph = this.getTable().getDBSystemRoot().getGraph();
        final SQLTable foreignTable = graph.getForeignTable(this.getTable().getField(fieldName));
        if (foreignTable == null)
            throw new IllegalArgumentException(fieldName + " is not a foreign key of " + this.getTable());
        return foreignTable;
    }

    @Override
    public boolean isForeignEmpty(String fieldName) {
        // keep getForeignTable at the 1st line since it does the check
        final SQLTable foreignTable = this.getForeignTable(fieldName);
        final Object val = this.getContainedObject(fieldName);
        final Number id = val instanceof SQLRowValues ? ((SQLRowValues) val).getIDNumber() : (Number) val;
        final Number undefID = foreignTable.getUndefinedIDNumber();
        return NumberUtils.areNumericallyEqual(id, undefID);
    }

    @Override
    public int getForeignID(String fieldName) {
        final SQLRowAccessor foreign = getForeign(fieldName);
        return foreign == null ? SQLRow.NONEXISTANT_ID : foreign.getID();
    }

    public boolean isDefault(String fieldName) {
        return SQL_DEFAULT.equals(this.getObject(fieldName));
    }

    /**
     * Retourne les champs spécifiés par cette instance.
     * 
     * @return l'ensemble des noms des champs.
     */
    @Override
    public Set<String> getFields() {
        return Collections.unmodifiableSet(this.values.keySet());
    }

    /**
     * Whether this row has a Number for the primary key.
     * 
     * @return <code>true</code> if the value of the primary key is a number.
     */
    public final boolean hasID() {
        return this.getIDNumber() != null;
    }

    @Override
    public final SQLRow asRow() {
        if (!this.hasID())
            throw new IllegalStateException(this + " has no ID");
        return new SQLRow(this.getTable(), this.getAllValues(ForeignCopyMode.COPY_ID_OR_RM));
    }

    @Override
    public final SQLRowValues asRowValues() {
        return this;
    }

    // *** set

    /**
     * Retains only the fields in this that are contained in the specified collection. In other
     * words, removes all of its elements that are not contained in the specified collection.
     * 
     * @param fields collection containing elements to be retained, <code>null</code> meaning all.
     * @return this.
     */
    public final SQLRowValues retainAll(Collection<String> fields) {
        return this.changeFields(fields, true);
    }

    private final SQLRowValues changeFields(Collection<String> fields, final boolean retain) {
        // retains all == no-op
        if (retain && fields == null)
            return this;
        // clear all on an empty values == no-op
        if (!retain && fields == null && this.size() == 0)
            return this;

        final Set<String> toRm = new HashSet<String>(this.values.keySet());
        // fields == null => !retain => clear()
        if (fields != null) {
            if (retain) {
                toRm.removeAll(fields);
            } else {
                toRm.retainAll(fields);
            }
        }
        // nothing to change
        if (toRm.isEmpty())
            return this;
        // handle links
        final Set<String> fks = getTable().getForeignKeysNames();
        for (final String fieldName : toRm) {
            if (fks.contains(fieldName))
                this._put(fieldName, null);
        }
        if (fields == null) {
            assert !retain && toRm.equals(this.values.keySet());
            this.values.clear();
        } else {
            this.values.keySet().removeAll(toRm);
        }
        this.getGraph().fireModification(this, toRm);
        return this;
    }

    /**
     * Removes from this all fields that are contained in the specified collection.
     * 
     * @param fields collection containing elements to be removed, <code>null</code> meaning all.
     * @return this.
     */
    public final SQLRowValues removeAll(Collection<String> fields) {
        return this.changeFields(fields, false);
    }

    public final void remove(String field) {
        // check arg & handle links
        this.put(field, null);
        // really remove
        this.values.remove(field);
    }

    public final void clear() {
        this.removeAll(null);
    }

    public final void clearPrimaryKeys() {
        this.clearPrimaryKeys(this.values);
        // by definition primary keys are not foreign keys, so no need to updateLinks()
    }

    private Map<String, Object> clearPrimaryKeys(final Map<String, Object> values) {
        return clearFields(values, this.getTable().getPrimaryKeys());
    }

    private Map<String, Object> clearFields(final Map<String, Object> values, final Set<SQLField> fields) {
        return changeFields(values, fields, false);
    }

    private Map<String, Object> changeFields(final Map<String, Object> values, final Set<SQLField> fields, final boolean retain) {
        final Iterator<String> iter = values.keySet().iterator();
        while (iter.hasNext()) {
            final String fieldName = iter.next();
            if (fields.contains(this.getTable().getField(fieldName)) ^ retain)
                iter.remove();
        }
        return values;
    }

    // puts

    public SQLRowValues put(String fieldName, Object value) {
        return this.put(fieldName, value, true);
    }

    // table.contains() can take up to 35% of this method
    SQLRowValues put(String fieldName, Object value, final boolean checkName) {
        if (checkName && !this.getTable().contains(fieldName))
            throw new IllegalArgumentException(fieldName + " is not in table " + this.getTable());
        _put(fieldName, value);
        this.getGraph().fireModification(this, fieldName, value);
        return this;
    }

    private void _put(String fieldName, Object value) {
        if (value == SQL_EMPTY_LINK)
            // keep getForeignTable since it does the check
            value = this.getForeignTable(fieldName).getUndefinedIDNumber();
        // use assertion since check() is not perfect
        assert check(fieldName, value);
        this.updateLinks(fieldName, this.values.put(fieldName, value), value);
    }

    private boolean check(String fieldName, Object value) {
        if (value == null || value == SQL_DEFAULT)
            return true;
        final SQLField field = getTable().getField(fieldName);
        final Class<?> javaType = field.getType().getJavaType();
        // createStatement() does some conversion so don't be too strict
        if (value instanceof Number) {
            checkGroup(Number.class, value, field, javaType);
        } else if (value instanceof Date) {
            checkGroup(Date.class, value, field, javaType);
        } else if (value instanceof SQLRowValues) {
            if (!getTable().getForeignKeys().contains(field))
                throw new IllegalArgumentException("Since value is a SQLRowValues, expected a foreign key but got " + field);
        } else if (!javaType.isInstance(value))
            throw new IllegalArgumentException("Wrong type for " + field + ", expected " + javaType + " but got " + value.getClass());
        return true;
    }

    private void checkGroup(final Class<?> superClass, final Object value, final SQLField field, final Class<?> javaType) {
        if (superClass.isInstance(value)) {
            if (!superClass.isAssignableFrom(javaType))
                throw new IllegalArgumentException("Incompatible type for " + field + ", expected " + javaType + " but got " + value.getClass() + "(" + superClass + ")");
        } else {
            throw new IllegalStateException("value is not an instance of the superclass for " + field);
        }
    }

    public SQLRowValues put(String fieldName, int value) {
        return this.put(fieldName, Integer.valueOf(value));
    }

    public SQLRowValues putDefault(String fieldName) {
        return this.put(fieldName, SQL_DEFAULT);
    }

    /**
     * To empty a foreign key.
     * 
     * @param fieldName the name of the foreign key to empty.
     * @return this.
     */
    public SQLRowValues putEmptyLink(String fieldName) {
        return this.put(fieldName, SQL_EMPTY_LINK);
    }

    /**
     * Set a new {@link SQLRowValues} as the value of <code>fieldName</code>. ATTN contrary to many
     * methods this one do not return <code>this</code>.
     * 
     * @param fieldName the name of a foreign field.
     * @return the newly created values.
     * @throws IllegalArgumentException if <code>fieldName</code> is not a foreign field.
     */
    public final SQLRowValues putRowValues(String fieldName) throws IllegalArgumentException {
        // getForeignTable checks
        final SQLRowValues vals = new SQLRowValues(this.getForeignTable(fieldName));
        this.put(fieldName, vals);
        return vals;
    }

    /**
     * Set the order of this row so that it will be just after/before <code>r</code>. NOTE: this may
     * reorder the table to make room.
     * 
     * @param r the row to be next to.
     * @param after whether this row will be before or after <code>r</code>.
     * @return this.
     */
    public SQLRowValues setOrder(SQLRow r, boolean after) {
        return this.setOrder(r, after, ReOrder.DISTANCE.movePointRight(2).intValue(), 0);
    }

    private SQLRowValues setOrder(SQLRow r, boolean after, int nbToReOrder, int nbReOrdered) {
        final BigDecimal freeOrder = r.getOrder(after);
        final String orderName = this.getTable().getOrderField().getName();
        if (freeOrder != null)
            return this.put(orderName, freeOrder);
        else if (nbReOrdered > r.getTable().getRowCount()) {
            throw new IllegalStateException("cannot reorder " + r.getTable().getSQLName());
        } else {
            // make room
            try {
                ReOrder.create(this.getTable(), r.getOrder().intValue() - (nbToReOrder / 2), nbToReOrder).exec();
            } catch (SQLException e) {
                throw ExceptionUtils.createExn(IllegalStateException.class, "reorder failed for " + this.getTable() + " at " + r.getOrder(), e);
            }
            r.fetchValues();
            return this.setOrder(r, after, nbToReOrder * 10, nbToReOrder);
        }
    }

    public final SQLRowValues setID(Number id) {
        // faster
        return this.setID(id, false);
    }

    /***
     * Set the {@link #getIDNumber() ID} of this row. Convert is useful to compare a row created in
     * Java and a row returned from the database, since in Java the ID will be an integer whereas
     * the DB can return anything.
     * 
     * @param id the new ID.
     * @param convert <code>true</code> if <code>id</code> should be converted to type of the
     *        primary key.
     * @return this.
     */
    public final SQLRowValues setID(Number id, final boolean convert) {
        final SQLField key = this.getTable().getKey();
        if (convert)
            id = NumberConvertor.convert(id, key.getType().getJavaType().asSubclass(Number.class));

        return this.put(key.getName(), id);
    }

    public final SQLRowValues setAll(Map<String, ?> m) {
        return this.loadAll(m, true);
    }

    public final SQLRowValues putAll(Map<String, ?> m) {
        return this.loadAll(m, false);
    }

    private final SQLRowValues loadAll(Map<String, ?> m, final boolean clear) {
        if (!this.getTable().getFieldsName().containsAll(m.keySet()))
            throw new IllegalArgumentException("fields " + m.keySet() + " are not a subset of " + this.getTable() + " : " + this.getTable().getFieldsName());
        if (clear)
            clear();
        for (final Map.Entry<String, ?> e : m.entrySet()) {
            this._put(e.getKey(), e.getValue());
        }
        this.getGraph().fireModification(this, m);
        return this;
    }

    public final SQLRowValues putNulls(String... fields) {
        return this.putNulls(Arrays.asList(fields), false);
    }

    /**
     * Set the passed fields to <code>null</code>.
     * 
     * @param fields which fields to put.
     * @param ignoreInexistant <code>true</code> if non existing field should be ignored,
     *        <code>false</code> will throw an exception if a field doesn't exist.
     * @return this.
     */
    public final SQLRowValues putNulls(Collection<String> fields, final boolean ignoreInexistant) {
        final Map<String, Object> m = new HashMap<String, Object>(fields.size());
        final Set<String> fieldsName = getTable().getFieldsName();
        // keep order
        for (final String fn : new LinkedHashSet<String>(fields)) {
            if (!ignoreInexistant || fieldsName.contains(fn))
                m.put(fn, null);
        }
        return this.putAll(m);
    }

    /**
     * Set all the fields (including primary and foreign keys) of this row to <code>null</code>.
     * 
     * @return this.
     */
    public final SQLRowValues setAllToNull() {
        final Map<String, Object> nullMap = new HashMap<String, Object>();
        for (final SQLField f : this.getTable().getFields()) {
            nullMap.put(f.getName(), null);
        }
        return this.setAll(nullMap);
    }

    // listener

    public class ReferentChangeEvent extends EventObject {

        private final SQLField f;
        private final SQLRowValues vals;
        private final boolean put;

        public ReferentChangeEvent(SQLField f, boolean put, SQLRowValues vals) {
            super(SQLRowValues.this);
            assert f != null && f.getDBSystemRoot().getGraph().getForeignTable(f) == getSource().getTable() && f.getTable() == vals.getTable();
            this.f = f;
            this.put = put;
            this.vals = vals;
        }

        // eg SITE[2]
        @Override
        public SQLRowValues getSource() {
            return (SQLRowValues) super.getSource();
        }

        // eg ID_SITE
        public final SQLField getField() {
            return this.f;
        }

        // eg BATIMENT[3]
        public final SQLRowValues getChangedReferent() {
            return this.vals;
        }

        // true if getChangedReferent() is a new referent of getSource(), false if it has been
        // removed from getSource()
        public final boolean isAddition() {
            return this.put;
        }

        public final boolean isRemoval() {
            return !this.isAddition();
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + (this.isAddition() ? " added" : " removed") + " on field " + getField() + " from " + this.getSource().asRow() + " : " + getChangedReferent();
        }
    }

    public static interface ReferentChangeListener extends EventListener {

        void referentChange(ReferentChangeEvent evt);

    }

    /**
     * Adds a listener to referent rows.
     * 
     * @param field the referent field to listen to, <code>null</code> meaning all.
     * @param l the listener.
     */
    public final void addReferentListener(SQLField field, ReferentChangeListener l) {
        if (this.referentsListener == null)
            this.referentsListener = new CollectionMap<SQLField, ReferentChangeListener>(new ArrayList<ReferentChangeListener>());
        this.referentsListener.put(field, l);
    }

    public final void removeReferentListener(SQLField field, ReferentChangeListener l) {
        if (this.referentsListener != null) {
            this.referentsListener.remove(field, l);
        }
    }

    private void fireRefChange(SQLField f, boolean put, SQLRowValues vals) {
        // only create event if needed
        if (this.referentsListener != null || this.getGraph().referentFireNeeded(put)) {
            final ReferentChangeEvent evt = new ReferentChangeEvent(f, put, vals);
            if (this.referentsListener != null) {
                for (final ReferentChangeListener l : this.referentsListener.getNonNull(f))
                    l.referentChange(evt);
                for (final ReferentChangeListener l : this.referentsListener.getNonNull(null))
                    l.referentChange(evt);
            }
            this.getGraph().fireModification(evt);
        }
    }

    public final void addValueListener(ValueChangeListener l) {
        this.getGraph().addValueListener(this, l);
    }

    public final void removeValueListener(ValueChangeListener l) {
        this.getGraph().removeValueListener(this, l);
    }

    @Override
    public final Collection<SQLRowValues> followLink(final Link l, final Direction direction) {
        return this.followPath(Path.get(getTable()).add(l, direction), CreateMode.CREATE_NONE, false);
    }

    /**
     * Create the necessary SQLRowValues so that the graph of this row goes along the passed path.
     * 
     * @param p the path of SQLRowValues, eg "LOCAL.ID_BATIMENT,BATIMENT.ID_SITE".
     * @return the SQLRowValues at the end of the path, eg a SQLRowValues on /SITE/.
     */
    public final SQLRowValues assurePath(final Path p) {
        return this.followPath(p, true);
    }

    /**
     * Return the row at the end of passed path.
     * 
     * @param p the path to follow, e.g. SITE,SITE.ID_CONTACT_CHEF.
     * @return the row at the end or <code>null</code> if none exists, e.g. SQLRowValues on
     *         /CONTACT/.
     */
    public final SQLRowValues followPath(final Path p) {
        return this.followPath(p, false);
    }

    private final SQLRowValues followPath(final Path p, final boolean create) {
        return followPathToOne(p, create ? CreateMode.CREATE_ONE : CreateMode.CREATE_NONE, DEFAULT_ALLOW_BACKTRACK);
    }

    /**
     * Follow path to at most one row.
     * 
     * @param p the path to follow.
     * @param create if and how to create new rows.
     * @param allowBackTrack <code>true</code> to allow encountering the same row more than once.
     * @return the destination row or <code>null</code> if none exists and <code>create</code> was
     *         {@link CreateMode#CREATE_NONE}
     * @see #followPath(Path, CreateMode, boolean, boolean)
     */
    public final SQLRowValues followPathToOne(final Path p, final CreateMode create, final boolean allowBackTrack) {
        final Collection<SQLRowValues> res = this.followPath(p, create, true, allowBackTrack);
        // since we passed onlyOne=true
        assert res.size() <= 1;
        return CollectionUtils.getSole(res);
    }

    /**
     * Return the rows at the end of the passed path.
     * 
     * @param path a path, e.g. SITE, BATIMENT, LOCAL.
     * @return the existing rows at the end of <code>path</code>, never <code>null</code>, e.g.
     *         [LOCAL[3], LOCAL[5]].
     */
    public final Collection<SQLRowValues> getDistantRows(final Path path) {
        return followPath(path, CreateMode.CREATE_NONE, false);
    }

    public final Collection<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean onlyOne) {
        return followPath(p, create, onlyOne, DEFAULT_ALLOW_BACKTRACK);
    }

    /**
     * Follow path through the graph.
     * 
     * @param p the path to follow.
     * @param create if and how to create new rows.
     * @param onlyOne <code>true</code> if this method should return at most one row.
     * @param allowBackTrack <code>true</code> to allow encountering the same row more than once.
     * @return the destination rows, can be empty.
     * @throws IllegalArgumentException if <code>p</code> doesn't start with this table.
     * @throws IllegalStateException if <code>onlyOne</code> and there's more than one row on the
     *         path.
     */
    public final Collection<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean onlyOne, final boolean allowBackTrack) throws IllegalArgumentException, IllegalStateException {
        return followPath(p, create, onlyOne, allowBackTrack ? null : new LinkedIdentitySet<SQLRowValues>());
    }

    private final IdentitySet<SQLRowValues> followPath(final Path p, final CreateMode create, final boolean onlyOne, final IdentitySet<SQLRowValues> beenThere) {
        if (p.getFirst() != this.getTable())
            throw new IllegalArgumentException("path " + p + " doesn't start with us " + this);
        if (p.length() > 0) {
            // fail-fast : avoid creating rows
            if (onlyOne && create == CreateMode.CREATE_MANY && !p.isSingleLink())
                throw new IllegalStateException("more than one link with " + create + " and onlyOne : " + p);

            // Set is needed when a row is multi-linked to another (to avoid calling recursively
            // followPath() on the same instance)
            // IdentitySet is needed since multiple rows can be equal, e.g. empty rows :
            // SITE -- chef -> CONTACT
            // _____-- rapport -> CONTACT
            final Set<SQLRowValues> next = new LinkedIdentitySet<SQLRowValues>();
            final Step firstStep = p.getStep(0);
            final Set<Link> ffs = firstStep.getLinks();
            for (final Link l : ffs) {
                final SQLField ff = l.getLabel();
                if (firstStep.isForeign(l)) {
                    final Object fkValue = this.getObject(ff.getName());
                    if (fkValue instanceof SQLRowValues && (beenThere == null || !beenThere.contains(fkValue))) {
                        next.add((SQLRowValues) fkValue);
                    } else if (create == CreateMode.CREATE_ONE || create == CreateMode.CREATE_MANY) {
                        assert create == CreateMode.CREATE_MANY || (create == CreateMode.CREATE_ONE && next.size() <= 1);
                        final SQLRowValues nextOne;
                        if (create == CreateMode.CREATE_ONE && next.size() == 1) {
                            nextOne = next.iterator().next();
                        } else {
                            nextOne = new SQLRowValues(ff.getDBSystemRoot().getGraph().getForeignTable(ff));
                            // keep the id, if present
                            if (fkValue instanceof Number)
                                nextOne.setID((Number) fkValue);
                            next.add(nextOne);
                        }
                        this.put(ff.getName(), nextOne);
                    }
                } else {
                    final Set<SQLRowValues> referentRows = this.getReferentRows(ff);
                    final Set<SQLRowValues> validReferentRows;
                    if (beenThere == null || beenThere.size() == 0) {
                        validReferentRows = referentRows;
                    } else {
                        validReferentRows = new LinkedIdentitySet<SQLRowValues>(referentRows);
                        validReferentRows.removeAll(beenThere);
                    }
                    if (validReferentRows.size() > 0) {
                        next.addAll(validReferentRows);
                    } else if (create == CreateMode.CREATE_ONE || create == CreateMode.CREATE_MANY) {
                        assert create == CreateMode.CREATE_MANY || (create == CreateMode.CREATE_ONE && next.size() <= 1);
                        final SQLRowValues nextOne;
                        if (create == CreateMode.CREATE_ONE && next.size() == 1) {
                            nextOne = next.iterator().next();
                        } else {
                            nextOne = new SQLRowValues(ff.getTable());
                            next.add(nextOne);
                        }
                        nextOne.put(ff.getName(), this);
                    }
                }
            }
            if (onlyOne && next.size() > 1)
                throw new IllegalStateException("more than one row and onlyOne=true : " + next);

            // see comment above for IdentitySet
            final IdentitySet<SQLRowValues> res = new LinkedIdentitySet<SQLRowValues>();
            for (final SQLRowValues n : next) {
                final IdentitySet<SQLRowValues> newBeenThere;
                if (beenThere == null) {
                    newBeenThere = null;
                } else {
                    newBeenThere = new LinkedIdentitySet<SQLRowValues>(beenThere);
                    final boolean added = newBeenThere.add(this);
                    assert added;
                }
                res.addAll(n.followPath(p.minusFirst(), create, onlyOne, newBeenThere));
            }
            return res;
        } else {
            return CollectionUtils.createIdentitySet(this);
        }
    }

    public final SQLRowValues flatten() {
        return this.flatten(false);
    }

    /**
     * Replace each foreign rowValues with its ID.
     * 
     * @param rmNoID <code>true</code> if a rowValues without an ID should be removed.
     * @return this.
     */
    public final SQLRowValues flatten(boolean rmNoID) {
        final Map<String, SQLRowValues> copy = new HashMap<String, SQLRowValues>(this.getForeigns());
        for (final Map.Entry<String, SQLRowValues> e : copy.entrySet()) {
            if (e.getValue().hasID())
                this.put(e.getKey(), e.getValue().getID());
            else if (rmNoID)
                this.remove(e.getKey());
        }
        return this;
    }

    // *** load

    public void loadAbsolutelyAll(SQLRow row) {
        this.setAll(row.getAbsolutelyAll());
    }

    /**
     * Load values from the passed row (and remove them if possible).
     * 
     * @param row the row to load values from.
     * @param fieldsNames what fields to load, <code>null</code> meaning all.
     */
    public void load(SQLRowAccessor row, final Set<String> fieldsNames) {
        // make sure we only define keys that row has
        // allow load( {'A':a, 'B':b}, {'A', 'B', 'C' } ) to not define 'C' to null
        final Map<String, Object> m = new HashMap<String, Object>(row.getAbsolutelyAll());
        if (fieldsNames != null)
            m.keySet().retainAll(fieldsNames);

        // rm the added fields otherwise this and row will be linked
        // eg load LOCAL->BATIMENT into a LOCAL will result in the BATIMENT
        // being pointed to by both LOCAL
        if (row instanceof SQLRowValues)
            ((SQLRowValues) row).removeAll(m.keySet());

        // put after remove so that this graph never contains row (and thus avoids unneeded events)
        this.putAll(m);
    }

    // *** modify

    void checkValidity() {
        if (!checkValidity)
            return;
        // this checks archived which the DB doesn't with just foreign constraints
        final Object[] pb = this.getInvalid();
        if (pb != null)
            throw new IllegalStateException("can't update " + this + " : the field " + pb[0] + " points to " + pb[1]);
    }

    /**
     * Renvoie le premier pb dans les valeurs. C'est à dire la première clef externe qui pointe sur
     * une ligne non valide.
     * 
     * @return <code>null</code> si pas de pb, sinon un Object[] :
     *         <ol>
     *         <li>en 0 le nom du champ posant pb, eg "ID_OBSERVATION_2"</li>
     *         <li>en 1 une SQLRow décrivant le pb, eg "(OBSERVATION[123])"</li>
     *         </ol>
     */
    public synchronized Object[] getInvalid() {
        final Set<SQLField> fk = this.getTable().getForeignKeys();
        for (final String fieldName : this.values.keySet()) {
            final SQLField field = this.getTable().getField(fieldName);
            // verifie l'intégrité (a rowValues is obviously correct, as is EMPTY,
            // DEFAULT is the responsability of the DB)
            final Object fieldVal = this.getObject(fieldName);
            if (fk.contains(field) && fieldVal != SQL_DEFAULT && !(fieldVal instanceof SQLRowValues)) {
                final SQLRow pb = this.getTable().checkValidity(field.getName(), (Number) fieldVal);
                if (pb != null)
                    return new Object[] { fieldName, pb };
            }
        }
        return null;
    }

    // * insert

    /**
     * Insert a new line (strips the primary key, it must be db generated and strips order, added at
     * the end).
     * 
     * @return the newly inserted line, or <code>null</code> if the table has not exactly one
     *         primary key.
     * @throws SQLException if an error occurs while inserting.
     * @throws IllegalStateException if the ID of the new line cannot be retrieved.
     */
    public synchronized SQLRow insert() throws SQLException {
        // remove unwanted fields, keep ARCHIVE
        return this.insert(false, false);
    }

    /**
     * Insert a new line verbatim. ATTN the primary key must not exist.
     * 
     * @return the newly inserted line, or <code>null</code> if the table has not exactly one
     *         primary key.
     * @throws SQLException if an error occurs while inserting.
     * @throws IllegalStateException if the ID of the new line cannot be retrieved.
     */
    public synchronized SQLRow insertVerbatim() throws SQLException {
        return this.insert(true, true);
    }

    public synchronized SQLRow insert(final boolean insertPK, final boolean insertOrder) throws SQLException {
        this.getGraph().store(new SQLRowValuesCluster.Insert(insertPK, insertOrder));
        return this.getGraph().getRow(this);
    }

    SQLTableEvent insertJustThis(final Set<SQLField> autoFields) throws SQLException {
        final Map<String, Object> copy = this.clearFields(new HashMap<String, Object>(this.values), autoFields);

        try {
            final Tuple2<List<String>, Number> fieldsAndID = this.getTable().getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Tuple2<List<String>, Number>, SQLException>() {
                @Override
                public Tuple2<List<String>, Number> handle(SQLDataSource ds) throws SQLException {
                    final Tuple2<PreparedStatement, List<String>> pStmt = createInsertStatement(getTable(), copy);
                    try {
                        final Number newID = insert(pStmt.get0(), getTable());
                        // MAYBE keep the pStmt around while values.keySet() doesn't change
                        pStmt.get0().close();
                        return Tuple2.create(pStmt.get1(), newID);
                    } catch (Exception e) {
                        throw new SQLException("Unable to insert " + pStmt.get0(), e);
                    }
                }
            });

            assert this.getTable().isRowable() == (fieldsAndID.get1() != null);
            if (this.getTable().isRowable()) {
                // pour pouvoir avoir les valeurs des champs non précisés
                return new SQLTableEvent(getChangedRow(fieldsAndID.get1().intValue()), Mode.ROW_ADDED, fieldsAndID.get0());
            } else
                return new SQLTableEvent(getTable(), SQLRow.NONEXISTANT_ID, Mode.ROW_ADDED, fieldsAndID.get0());
        } catch (SQLException e) {
            throw new SQLException("unable to insert " + this + " using " + copy, e);
        }
    }

    private SQLRow getChangedRow(final int newID) {
        // don't read the cache since no event has been fired yet
        // don't write to it since the transaction isn't committed yet, so other threads
        // should not see the new values.
        return new SQLRow(getTable(), newID).fetchValues(false);
    }

    // * update

    public SQLRow update() throws SQLException {
        if (!hasID()) {
            throw new IllegalStateException("can't update no ID specified, use update(int)");
        }
        return this.update(this.getID());
    }

    public SQLRow update(final int id) throws SQLException {
        this.put(this.getTable().getKey().getName(), id);
        return this.commit();
    }

    /**
     * Permet de mettre à jour une ligne existante avec les valeurs courantes.
     * 
     * @param id l'id à mettre à jour.
     * @return the updated row.
     * @throws SQLException si pb lors de la maj.
     */
    SQLTableEvent updateJustThis(final int id) throws SQLException {
        if (id == this.getTable().getUndefinedID()) {
            throw new IllegalArgumentException("can't update undefined");
        }
        // clear primary key, otherwise we might end up with :
        // UPDATE TABLE SET ID=123,DESIGNATION='aa' WHERE id=456
        // which will delete ID 456, and possibly cause a conflict with preexisting ID 123
        final Map<String, Object> updatedValues = this.clearPrimaryKeys(new HashMap<String, Object>(this.values));

        final List<String> updatedCols = this.getTable().getDBSystemRoot().getDataSource().useConnection(new ConnectionHandlerNoSetup<List<String>, SQLException>() {
            @Override
            public List<String> handle(SQLDataSource ds) throws SQLException {
                final Tuple2<PreparedStatement, List<String>> pStmt = createUpdateStatement(getTable(), updatedValues, id);
                final long timeMs = System.currentTimeMillis();
                final long time = System.nanoTime();
                pStmt.get0().executeUpdate();
                final long afterExecute = System.nanoTime();
                // logging after closing fails to get the connection info
                SQLRequestLog.log(pStmt.get0(), "rowValues.update()", timeMs, time, afterExecute, afterExecute, afterExecute, afterExecute, System.nanoTime());
                pStmt.get0().close();
                return pStmt.get1();
            }
        });

        return new SQLTableEvent(getChangedRow(id), Mode.ROW_UPDATED, updatedCols);
    }

    // * commit

    /**
     * S'assure que ces valeurs arrivent dans la base. Si la ligne possède un ID équivaut à update()
     * sinon insert().
     * 
     * @return the affected row.
     * @throws SQLException
     */
    public SQLRow commit() throws SQLException {
        this.getGraph().store(SQLRowValuesCluster.StoreMode.COMMIT);
        return this.getGraph().getRow(this);
    }

    SQLTableEvent commitJustThis() throws SQLException {
        if (!hasID()) {
            return this.insertJustThis(Collections.<SQLField> emptySet());
        } else
            return this.updateJustThis(this.getID());
    }

    /**
     * Returns a string representation of this (excluding any foreign or referent rows).
     * 
     * @return a compact representation of this.
     * @see #printGraph()
     */
    @Override
    public String toString() {
        String result = this.getClass().getSimpleName() + " on " + this.getTable() + " : {";
        result += CollectionUtils.join(this.values.entrySet(), ", ", new ITransformer<Entry<String, ?>, String>() {
            public String transformChecked(final Entry<String, ?> e) {
                final String className = e.getValue() == null ? "" : "(" + e.getValue().getClass() + ")";
                final String value;
                // avoid infinite loop (and overly verbose string)
                if (e.getValue() instanceof SQLRowValues) {
                    final SQLRowValues foreignVals = (SQLRowValues) e.getValue();
                    if (foreignVals == SQLRowValues.this) {
                        value = "this";
                    } else if (foreignVals.hasID()) {
                        value = foreignVals.getIDNumber().toString();
                    } else {
                        // so that if the same vals is referenced multiple times, we can see it
                        value = "@" + System.identityHashCode(foreignVals);
                    }
                } else
                    value = String.valueOf(e.getValue());
                return e.getKey() + "=" + value + className;
            }
        });
        result += "}";
        return result;
    }

    /**
     * Return a graphical representation (akin to the result of a query) of the tree rooted at
     * <code>this</code>.
     * 
     * @return a string representing the rows pointing to this.
     * @see SQLRowValuesCluster#printTree(SQLRowValues, int)
     */
    public final String printTree() {
        return this.getGraph().printTree(this, 16);
    }

    /**
     * Return the list of all nodes and their links.
     * 
     * @return a string representing the graph of this.
     */
    public final String printGraph() {
        return this.getGraph().printNodes();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SQLRowValues) {
            final SQLRowValues o = (SQLRowValues) obj;
            return this.getTable().equals(o.getTable()) && this.values.equals(o.values);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.getTable().hashCode();
    }

    /**
     * Indicates whether some other graph is "equal to" this one.
     * 
     * @param other another rowValues.
     * @return <code>true</code> if both graph are equals.
     * @see #getGraphFirstDifference(SQLRowValues)
     */
    public final boolean equalsGraph(final SQLRowValues other) {
        return this.getGraphFirstDifference(other) == null;
    }

    /**
     * Return the first difference between this graph and another.
     * 
     * @param other another instance.
     * @return the first difference, <code>null</code> if equals.
     */
    public final String getGraphFirstDifference(final SQLRowValues other) {
        return this.getGraph().getFirstDifference(this, other);
    }

    final boolean equalsJustThis(final SQLRowValues o) {
        // NO_COPY since foreign rows are handled by SQLRowValuesCluster.equals()
        // LinkedHashMap.equals() does not compare the order of entries, which is fine since
        // inserting doesn't change with the order of fields
        return this.getTable().equals(o.getTable()) && new SQLRowValues(this, ForeignCopyMode.NO_COPY).values.equals(new SQLRowValues(o, ForeignCopyMode.NO_COPY).values);
    }

    // *** static

    static private Tuple2<PreparedStatement, List<String>> createInsertStatement(final SQLTable table, Map<String, Object> values) throws SQLException {
        final Tuple2<List<String>, List<Object>> l = CollectionUtils.mapToLists(values);
        final List<String> fieldsNames = l.get0();
        final List<Object> vals = l.get1();

        addMetadata(fieldsNames, vals, table.getCreationUserField(), getUser());
        addMetadata(fieldsNames, vals, table.getCreationDateField(), new Timestamp(System.currentTimeMillis()));

        return createStatement(table, fieldsNames, vals, true);
    }

    static private Tuple2<PreparedStatement, List<String>> createUpdateStatement(SQLTable table, Map<String, Object> values, int id) throws SQLException {
        final Tuple2<List<String>, List<Object>> l = CollectionUtils.mapToLists(values);
        final List<String> fieldsNames = l.get0();
        final List<Object> vals = l.get1();

        vals.add(new Integer(id));
        return createStatement(table, fieldsNames, vals, false);
    }

    static private void addMetadata(List<String> fieldsNames, List<Object> values, SQLField field, Object fieldValue) throws SQLException {
        if (field != null) {
            // TODO updateVerbatim to force a value
            final int index = fieldsNames.indexOf(field.getName());
            if (index < 0) {
                // ajout au dbt car le where du UPDATE a besoin de l'ID en dernier
                fieldsNames.add(0, field.getName());
                values.add(0, fieldValue);
            } else {
                values.set(index, fieldValue);
            }
        }
    }

    static private Object getUser() {
        final int userID = UserManager.getUserID();
        return userID < SQLRow.MIN_VALID_ID ? SQL_DEFAULT : userID;
    }

    /**
     * Create a prepared statement.
     * 
     * @param table the table to change.
     * @param fieldsNames the columns names of <code>table</code>.
     * @param values their values.
     * @param insert whether to insert or update.
     * @return the new statement and its columns.
     * @throws SQLException if an error occurs.
     */
    static private Tuple2<PreparedStatement, List<String>> createStatement(SQLTable table, List<String> fieldsNames, List<Object> values, boolean insert) throws SQLException {
        addMetadata(fieldsNames, values, table.getModifUserField(), getUser());
        addMetadata(fieldsNames, values, table.getModifDateField(), new Timestamp(System.currentTimeMillis()));

        final PreparedStatement pStmt;
        final String tableQuoted = table.getSQLName().quote();
        String req = (insert ? "INSERT INTO " : "UPDATE ") + tableQuoted + " ";
        if (insert) {
            assert fieldsNames.size() == values.size();
            // remove DEFAULT since they are useless and prevent us from using
            // INSERT INTO "TABLEAU_ELECTRIQUE" ("ID_OBSERVATION", ...) select DEFAULT, ?,
            // MAX("ORDRE") + 1 FROM "TABLEAU_ELECTRIQUE"
            for (int i = values.size() - 1; i >= 0; i--) {
                if (values.get(i) == SQL_DEFAULT) {
                    fieldsNames.remove(i);
                    values.remove(i);
                }
            }
            assert fieldsNames.size() == values.size();

            // ajout de l'ordre
            final SQLField order = table.getOrderField();
            final boolean selectOrder;
            if (order != null && !fieldsNames.contains(order.getName())) {
                // si l'ordre n'est pas spécifié, ajout à la fin
                fieldsNames.add(order.getName());
                selectOrder = true;
            } else {
                selectOrder = false;
            }

            if (fieldsNames.size() == 0 && table.getServer().getSQLSystem() != SQLSystem.MYSQL) {
                // "LOCAL" () VALUES () is a syntax error on PG
                req += "DEFAULT VALUES";
            } else {
                req += "(" + CollectionUtils.join(fieldsNames, ", ", new ITransformer<String, String>() {
                    public String transformChecked(String input) {
                        return SQLBase.quoteIdentifier(input);
                    }
                }) + ")";
                // no DEFAULT thus only ?
                final String questionMarks = CollectionUtils.join(Collections.nCopies(values.size(), "?"), ", ");
                if (selectOrder) {
                    // needed since VALUES ( (select MAX("ORDRE") from "LOCAL") ) on MySQL yield
                    // "You can't specify target table 'LOCAL' for update in FROM clause"
                    req += " select ";
                    req += questionMarks;
                    if (values.size() > 0)
                        req += ", ";
                    // COALESCE for empty tables, MIN_ORDER + 1 since MIN_ORDER cannot be moved
                    req += "COALESCE(MAX(" + SQLBase.quoteIdentifier(order.getName()) + "), " + ReOrder.MIN_ORDER + ") + 1 FROM " + tableQuoted;
                } else {
                    req += " VALUES (";
                    req += questionMarks;
                    req += ")";
                }
            }
            pStmt = createInsertStatement(req, table);
        } else {
            // ID at the end
            assert fieldsNames.size() == values.size() - 1;
            final List<String> fieldAndValues = new ArrayList<String>(fieldsNames.size());
            final ListIterator<String> iter = fieldsNames.listIterator();
            while (iter.hasNext()) {
                final String fieldName = iter.next();
                final SQLField field = table.getField(fieldName);
                final Object value = values.get(iter.previousIndex());
                // postgresql doesn't support prefixing fields with their tables in an update
                fieldAndValues.add(SQLBase.quoteIdentifier(field.getName()) + "= " + getFieldValue(value));
            }

            req += "SET " + CollectionUtils.join(fieldAndValues, ", ");
            req += " WHERE " + table.getKey().getFieldRef() + "= ?";
            final Connection c = table.getBase().getDataSource().getConnection();
            pStmt = c.prepareStatement(req);
        }
        // set fields values
        int i = 0;
        for (final Object value : values) {
            // nothing to set if there's no corresponding '?'
            if (value != SQL_DEFAULT) {
                final Object toIns;
                if (value instanceof SQLRowValues) {
                    // TODO if we already point to some row, archive it
                    toIns = ((SQLRowValues) value).insert().getIDNumber();
                } else
                    toIns = value;
                // sql index start at 1
                if (toIns instanceof Date) {
                    // to convert from java.util to java.sql, needed for pg and MS
                    pStmt.setObject(i + 1, new Timestamp(((Date) toIns).getTime()));
                } else
                    pStmt.setObject(i + 1, toIns);
                i++;
            }
        }
        return Tuple2.create(pStmt, fieldsNames);
    }

    private static String getFieldValue(final Object value) {
        return value == SQL_DEFAULT ? "DEFAULT" : "?";
    }

    @Override
    public SQLTableModifiedListener createTableListener(SQLDataListener l) {
        return new SQLTableListenerData<SQLRowValues>(this, l);
    }

    public String dumpValues() {
        String result = "[";
        result += CollectionUtils.join(this.values.entrySet(), ", ", new ITransformer<Entry<String, ?>, String>() {
            public String transformChecked(final Entry<String, ?> e) {
                return e.getValue().toString();
            }
        });

        return result + "]";

    }

    // *** static

    /**
     * Create an insert statement which can provide the inserted ID.
     * 
     * @param req the INSERT sql.
     * @param table the table where the row will be inserted.
     * @return a new <code>PreparedStatement</code> object, containing the pre-compiled SQL
     *         statement, that will have the capability of returning the primary key.
     * @throws SQLException if a database access error occurs.
     * @see #insert(PreparedStatement, SQLTable)
     */
    static public final PreparedStatement createInsertStatement(String req, final SQLTable table) throws SQLException {
        final boolean rowable = table.isRowable();
        final boolean isPG = table.getServer().getSQLSystem() == SQLSystem.POSTGRESQL;
        if (rowable && isPG)
            req += " RETURNING " + SQLBase.quoteIdentifier(table.getKey().getName());
        final Connection c = table.getDBSystemRoot().getDataSource().getConnection();
        final int returnGenK = rowable && !isPG && c.getMetaData().supportsGetGeneratedKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
        return c.prepareStatement(req, returnGenK);
    }

    /**
     * Execute the passed INSERT statement and return the ID of the new row.
     * 
     * @param pStmt an INSERT statement (should have been obtained using
     *        {@link #createInsertStatement(String, SQLTable)}).
     * @param table the table where the row will be inserted.
     * @return the new ID.
     * @throws SQLException if the insertion fails.
     */
    static public final Number insert(final PreparedStatement pStmt, final SQLTable table) throws SQLException {
        final long timeMs = System.currentTimeMillis();

        final long time = System.nanoTime();
        pStmt.execute();
        final long afterExecute = System.nanoTime();

        final Number newID;
        if (table.isRowable()) {
            final ResultSet rs;
            if (table.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
                // uses RETURNING
                rs = pStmt.getResultSet();
            } else {
                rs = pStmt.getGeneratedKeys();
            }
            try {
                if (rs.next()) {
                    newID = (Number) rs.getObject(1);
                } else
                    throw new IllegalStateException("no keys have been autogenerated for the successfully executed statement :" + pStmt);
            } catch (SQLException exn) {
                throw new IllegalStateException("can't get autogenerated keys for the successfully executed statement :" + pStmt);
            }
        } else {
            newID = null;
        }
        final long afterHandle = System.nanoTime();
        SQLRequestLog.log(pStmt, "rowValues.insert()", timeMs, time, afterExecute, afterExecute, afterExecute, afterHandle, System.nanoTime());

        return newID;
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the inserted IDs, or <code>null</code> if <code>t</code> is not
     *         {@link SQLTable#isRowable() rowable}.
     * @throws SQLException if an error occurs while inserting.
     */
    @SuppressWarnings("unchecked")
    public static final List<Number> insertIDs(final SQLTable t, final String sql) throws SQLException {
        final boolean rowable = t.isRowable();
        final Insertion<?> res = insert(t, sql, rowable ? ReturnMode.FIRST_FIELD : ReturnMode.NO_FIELDS);
        if (rowable)
            return ((Insertion<Number>) res).getRows();
        else
            return null;
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return an object to always know the insertion count and possibly the inserted primary keys.
     * @throws SQLException if an error occurs while inserting.
     */
    @SuppressWarnings("unchecked")
    public static final Insertion<Object[]> insert(final SQLTable t, final String sql) throws SQLException {
        return (Insertion<Object[]>) insert(t, sql, ReturnMode.ALL_FIELDS);
    }

    /**
     * Insert rows in the passed table. Should be faster than other insert methods since it doesn't
     * fetch primary keys.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the insertion count.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final int insertCount(final SQLTable t, final String sql) throws SQLException {
        return insert(t, sql, ReturnMode.NO_FIELDS).getCount();
    }

    // if scalar is null primary keys aren't fetched
    private static final Insertion<?> insert(final SQLTable t, final String sql, final ReturnMode mode) throws SQLException {
        return new Inserter(t).insert(sql, mode, true);
    }

    /**
     * Insert rows in the passed table.
     * 
     * @param t a table, eg /LOCAL/.
     * @param sql the sql specifying the data to be inserted, eg ("DESIGNATION") VALUES('A'), ('B').
     * @return the inserted rows (with no values, ie a call to a getter will trigger a db access),
     *         or <code>null</code> if <code>t</code> is not {@link SQLTable#isRowable() rowable}.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final List<SQLRow> insertRows(final SQLTable t, final String sql) throws SQLException {
        final List<Number> ids = insertIDs(t, sql);
        if (ids == null)
            return null;
        final List<SQLRow> res = new ArrayList<SQLRow>(ids.size());
        for (final Number id : ids)
            res.add(new SQLRow(t, id.intValue()));
        return res;
    }

    // MAYBE add insertFromSelect(SQLTable, SQLSelect) if aliases are kept in SQLSelect (so that we
    // can map arbitray expressions to fields in the destination table)
    public static final int insertFromTable(final SQLTable dest, final SQLTable src) throws SQLException {
        return insertFromTable(dest, src, src.getChildrenNames());
    }

    /**
     * Copy all rows from <code>src</code> to <code>dest</code>.
     * 
     * @param dest the table where rows will be inserted.
     * @param src the table where rows will be selected.
     * @param fieldsNames the fields to use.
     * @return the insertion count.
     * @throws SQLException if an error occurs while inserting.
     */
    public static final int insertFromTable(final SQLTable dest, final SQLTable src, final Set<String> fieldsNames) throws SQLException {
        if (dest.getDBSystemRoot() != src.getDBSystemRoot())
            throw new IllegalArgumentException("Tables are not on the same system root : " + dest.getSQLName() + " / " + src.getSQLName());
        if (!dest.getChildrenNames().containsAll(fieldsNames))
            throw new IllegalArgumentException("Destination table " + dest.getSQLName() + " doesn't contain all fields of the source " + src + " : " + fieldsNames);

        final List<SQLField> fields = new ArrayList<SQLField>(fieldsNames.size());
        for (final String fName : fieldsNames)
            fields.add(src.getField(fName));
        final SQLSelect sel = new SQLSelect(true);
        sel.addAllSelect(fields);
        final String colNames = "(" + CollectionUtils.join(fields, ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return SQLBase.quoteIdentifier(input.getName());
            }
        }) + ") ";
        return insertCount(dest, colNames + sel.asString());
    }

    /**
     * Trim a collection of SQLRowValues.
     * 
     * @param graphs the rowValues to trim.
     * @return a copy of <code>graphs</code> without any linked SQLRowValues.
     */
    public static final List<SQLRowValues> trim(final Collection<SQLRowValues> graphs) {
        final List<SQLRowValues> res = new ArrayList<SQLRowValues>(graphs.size());
        for (final SQLRowValues r : graphs)
            res.add(trim(r));
        return res;
    }

    public static final SQLRowValues trim(final SQLRowValues r) {
        return new SQLRowValues(r, ForeignCopyMode.COPY_ID_OR_RM);
    }
}
