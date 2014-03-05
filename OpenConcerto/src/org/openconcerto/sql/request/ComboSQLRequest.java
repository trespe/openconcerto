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
 
 package org.openconcerto.sql.request;

/**
 * @author ILM Informatique
 */
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.TransfFieldExpander;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// final: use setSelectTransf()
public final class ComboSQLRequest extends FilteredFillSQLRequest {

    static private final SQLCache<CacheKey, List<IComboSelectionItem>> cache = new SQLCache<CacheKey, List<IComboSelectionItem>>(60, -1, "items of " + ComboSQLRequest.class);

    // encapsulate all values that can change the result
    private static final class CacheKey extends Tuple3<SQLRowValuesListFetcher, IClosure<IComboSelectionItem>, String> {
        public CacheKey(SQLRowValuesListFetcher a, String fieldSeparator, String undefLabel, IClosure<IComboSelectionItem> c) {
            super(a, c, fieldSeparator + undefLabel);
        }
    };

    public static enum KeepMode {
        /**
         * Only the ID is kept.
         */
        NONE,
        /**
         * Only the {@link SQLRow} is kept.
         */
        ROW,
        /**
         * The full {@link SQLRowValues graph} is kept.
         */
        GRAPH
    }

    private static final String SEP_CHILD = " ◄ ";
    private static String SEP_FIELD;
    private static Comparator<? super IComboSelectionItem> DEFAULT_COMPARATOR;

    /**
     * Set the default {@link #setFieldSeparator(String) field separator}.
     * 
     * @param separator the default separator to use from now on.
     */
    public static void setDefaultFieldSeparator(String separator) {
        SEP_FIELD = separator;
    }

    public static void setDefaultItemsOrder(final Comparator<? super IComboSelectionItem> comp) {
        DEFAULT_COMPARATOR = comp;
    }

    static {
        setDefaultFieldSeparator(" | ");
        setDefaultItemsOrder(null);
    }

    // immutable
    private List<SQLField> comboFields;
    private final TransfFieldExpander exp;

    private String fieldSeparator = SEP_FIELD;
    private String undefLabel;
    private KeepMode keepRows;
    private IClosure<IComboSelectionItem> customizeItem;

    private List<Path> order;
    private Comparator<? super IComboSelectionItem> itemsOrder;

    public ComboSQLRequest(SQLTable table, List<String> l) {
        this(table, l, null);
    }

    public ComboSQLRequest(SQLTable table, List<String> l, Where where) {
        super(table, where);
        this.undefLabel = null;
        // don't use memory
        this.keepRows = KeepMode.NONE;
        this.customizeItem = null;
        this.order = null;
        this.itemsOrder = DEFAULT_COMPARATOR;
        this.exp = new TransfFieldExpander(new ITransformer<SQLField, List<SQLField>>() {
            @Override
            public List<SQLField> transformChecked(SQLField fk) {
                final SQLTable foreignTable = fk.getDBSystemRoot().getGraph().getForeignTable(fk);
                return Configuration.getInstance().getDirectory().getElement(foreignTable).getComboRequest().getFields();
            }
        });
        this.setFields(l);
    }

    // public since this class is final (otherwise should be protected)
    public ComboSQLRequest(ComboSQLRequest c) {
        super(c);
        this.exp = new TransfFieldExpander(c.exp);
        this.comboFields = c.comboFields;
        this.order = c.order == null ? null : new ArrayList<Path>(c.order);

        this.fieldSeparator = c.fieldSeparator;
        this.undefLabel = c.undefLabel;
        this.keepRows = c.keepRows;
        this.customizeItem = c.customizeItem;
    }

    public final void setFields(Collection<String> fields) {
        final List<SQLField> l = new ArrayList<SQLField>();
        for (final String fName : fields) {
            l.add(this.getPrimaryTable().getField(fName));
        }
        setSQLFieldsUnsafe(l);
    }

    public final void setSQLFields(Collection<SQLField> fields) {
        for (final SQLField f : fields)
            if (f.getTable() != getPrimaryTable())
                throw new IllegalArgumentException("Not in " + getPrimaryTable() + " : " + f);
        setSQLFieldsUnsafe(new ArrayList<SQLField>(fields));
    }

    private void setSQLFieldsUnsafe(List<SQLField> fields) {
        this.comboFields = Collections.unmodifiableList(fields);
        this.clearGraph();
    }

    /**
     * Set the label of the undefined row. If <code>null</code> (the default) then the undefined
     * will not be fetched, otherwise it will and its label will be <code>undefLabel</code>.
     * 
     * @param undefLabel the new label, can be <code>null</code>.
     */
    public final void setUndefLabel(final String undefLabel) {
        this.undefLabel = undefLabel;
    }

    public final String getUndefLabel() {
        return this.undefLabel;
    }

    public final void setItemCustomizer(IClosure<IComboSelectionItem> customizeItem) {
        this.customizeItem = customizeItem;
    }

    /**
     * Retourne le comboItem correspondant à cet ID.
     * 
     * @param id l'id voulu de la primary table.
     * @return l'élément correspondant s'il existe et n'est pas archivé, <code>null</code>
     *         autrement.
     */
    public final IComboSelectionItem getComboItem(int id) {
        final SQLRowValues res = this.getValues(id);
        return res == null ? null : createItem(res);
    }

    public final List<IComboSelectionItem> getComboItems() {
        return this.getComboItems(true);
    }

    public final List<IComboSelectionItem> getComboItems(final boolean readCache) {
        if (this.getFields().isEmpty())
            throw new IllegalStateException("Empty fields");

        // freeze the fetcher otherwise it will change with the filter
        // and that will cause the cache to fail
        final SQLRowValuesListFetcher comboSelect = this.getFetcher(null).freeze();

        final CacheKey cacheKey = new CacheKey(comboSelect, this.fieldSeparator, this.undefLabel, this.customizeItem);
        if (readCache) {
            final CacheResult<List<IComboSelectionItem>> l = cache.check(cacheKey);
            if (l.getState() == CacheResult.State.INTERRUPTED)
                throw new RTInterruptedException("interrupted while waiting for the cache");
            else if (l.getState() == CacheResult.State.VALID)
                return l.getRes();
        }

        try {
            final List<IComboSelectionItem> result = new ArrayList<IComboSelectionItem>();
            // SQLRowValuesListFetcher don't cache
            for (final SQLRowValues vals : comboSelect.fetch()) {
                if (Thread.currentThread().isInterrupted())
                    throw new RTInterruptedException("interrupted in fill");
                result.add(createItem(vals));
            }
            if (this.itemsOrder != null)
                Collections.sort(result, this.itemsOrder);

            cache.put(cacheKey, result, this.getTables());

            return result;
        } catch (RuntimeException exn) {
            // don't use finally, otherwise we'll do both put() and rmRunning()
            cache.removeRunning(cacheKey);
            throw exn;
        }
    }

    @Override
    protected final SQLSelect transformSelect(SQLSelect sel) {
        sel.setExcludeUndefined(this.undefLabel == null, getPrimaryTable());
        return super.transformSelect(sel);
    }

    @Override
    protected final List<Path> getOrder() {
        if (this.order != null)
            return this.order;

        // order the combo by ancestors
        final List<Tuple2<Path, List<FieldPath>>> expandGroupBy = this.getShowAs().expandGroupBy(getFields());
        final List<Path> res = new ArrayList<Path>(expandGroupBy.size());
        for (final Tuple2<Path, List<FieldPath>> ancestor : expandGroupBy)
            res.add(0, ancestor.get0());
        return res;
    }

    /**
     * Change the ordering of this combo. By default this is ordered by ancestors.
     * 
     * @param l the list of tables, <code>null</code> to restore the default.
     */
    public final void setOrder(List<Path> l) {
        this.order = l;
        this.clearGraph();
    }

    public final void setNaturalItemsOrder(final boolean b) {
        this.setItemsOrder(b ? CompareUtils.<IComboSelectionItem> naturalOrder() : null);
    }

    /**
     * Set the in-memory sort on items.
     * 
     * @param comp how to sort items, <code>null</code> meaning don't sort (i.e. only
     *        {@link #getOrder() SQL order} will be used).
     */
    public final void setItemsOrder(final Comparator<? super IComboSelectionItem> comp) {
        this.itemsOrder = comp;
    }

    public final Comparator<? super IComboSelectionItem> getItemsOrder() {
        return this.itemsOrder;
    }

    private final IComboSelectionItem createItem(final SQLRowValues rs) {
        final String desc;
        if (this.undefLabel != null && rs.getID() == getPrimaryTable().getUndefinedID())
            desc = this.undefLabel;
        else
            desc = CollectionUtils.join(this.getShowAs().expandGroupBy(this.getFields()), SEP_CHILD, new ITransformer<Tuple2<Path, List<FieldPath>>, Object>() {
                public Object transformChecked(Tuple2<Path, List<FieldPath>> ancestorFields) {
                    final List<String> filtered = CollectionUtils.transformAndFilter(ancestorFields.get1(), new ITransformer<FieldPath, String>() {
                        // no need to keep this Transformer in an attribute
                        // even when creating one per line it's the same speed
                        public String transformChecked(FieldPath input) {
                            return getFinalValueOf(input, rs);
                        }
                    }, IPredicate.notNullPredicate(), new ArrayList<String>());
                    return CollectionUtils.join(filtered, ComboSQLRequest.this.fieldSeparator);
                }
            });
        final IComboSelectionItem res;
        if (this.keepRows == KeepMode.GRAPH)
            res = new IComboSelectionItem(rs, desc);
        else if (this.keepRows == KeepMode.ROW)
            res = new IComboSelectionItem(rs.asRow(), desc);
        else
            res = new IComboSelectionItem(rs.getID(), desc);
        if (this.customizeItem != null)
            this.customizeItem.executeChecked(res);
        return res;
    }

    protected final TransfFieldExpander getShowAs() {
        return this.exp;
    }

    /**
     * Renvoie la valeur du champ sous forme de String. De plus est sensé faire quelques
     * conversions, eg traduire les booléens en "oui" "non".
     * 
     * @param element le champ dont on veut la valeur.
     * @param rs un resultSet contenant le champ demandé.
     * @return la valeur du champ en String.
     */
    protected static String getFinalValueOf(FieldPath element, SQLRowValues rs) {
        String result = element.getString(rs);
        // TODO
        // if (element.getType() == "FLOAT") {
        // result = result.replace('.', ',');
        // } else if (element.getType() == "BOOL") {
        // result = result.equals("0") ? "non" : "oui";
        // }
        return result;
    }

    public final List<SQLField> getFields() {
        return this.comboFields;
    }

    /**
     * Set the string that is used to join the fields of a row.
     * 
     * @param string the new separator, e.g. " | ".
     */
    public final void setFieldSeparator(String string) {
        this.fieldSeparator = string;
    }

    /**
     * Characters that may not be displayed correctly by all fonts.
     * 
     * @return characters that may not be displayed correctly.
     */
    public String getSeparatorsChars() {
        return SEP_CHILD + this.fieldSeparator;
    }

    /**
     * Whether {@link IComboSelectionItem items} retain their rows.
     * 
     * @param b <code>true</code> if the rows should be retained.
     * @see IComboSelectionItem#getRow()
     */
    public final void keepRows(boolean b) {
        this.keepRows(b ? KeepMode.ROW : KeepMode.NONE);
    }

    public final void keepRows(final KeepMode mode) {
        this.keepRows = mode;
    }
}
