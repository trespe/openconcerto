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
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.List;

// TODO écouter les changements des tables affichées
// final: use setSelectTransf()
public final class ComboSQLRequest extends FilteredFillSQLRequest {

    static private final SQLCache<CacheKey, List<IComboSelectionItem>> cache = new SQLCache<CacheKey, List<IComboSelectionItem>>(60, -1, "items of " + ComboSQLRequest.class);

    // encapsulate all values that can change the result
    private static final class CacheKey extends Tuple3<SQLRowValuesListFetcher, IClosure<IComboSelectionItem>, String> {
        public CacheKey(SQLRowValuesListFetcher a, String fieldSeparator, String undefLabel, IClosure<IComboSelectionItem> c) {
            super(a, c, fieldSeparator + undefLabel);
        }
    };

    private static final String SEP_CHILD = " ◄ ";
    private static String SEP_FIELD;

    /**
     * Set the default {@link #setFieldSeparator(String) field separator}.
     * 
     * @param separator the default separator to use from now on.
     */
    public static void setDefaultFieldSeparator(String separator) {
        SEP_FIELD = separator;
    }

    static {
        setDefaultFieldSeparator(" | ");
    }

    // pour la combo
    private final List<SQLField> comboFields;
    private final TransfFieldExpander exp;

    private String fieldSeparator = SEP_FIELD;
    private String undefLabel;
    private IClosure<IComboSelectionItem> customizeItem;

    private List<Path> order;

    public ComboSQLRequest(SQLTable table, List<String> l) {
        this(table, l, null);
    }

    public ComboSQLRequest(SQLTable table, List<String> l, Where where) {
        super(table, where);
        this.undefLabel = null;
        this.customizeItem = null;
        this.order = null;
        this.exp = new TransfFieldExpander(new ITransformer<SQLField, List<SQLField>>() {
            @Override
            public List<SQLField> transformChecked(SQLField fk) {
                final SQLTable foreignTable = fk.getDBSystemRoot().getGraph().getForeignTable(fk);
                return Configuration.getInstance().getDirectory().getElement(foreignTable).getComboRequest().getFields();
            }
        });
        this.comboFields = new ArrayList<SQLField>();

        for (final String fName : l) {
            this.addItemToCombo(fName);
        }
    }

    public ComboSQLRequest(ComboSQLRequest c) {
        super(c);
        this.exp = new TransfFieldExpander(c.exp);
        this.comboFields = new ArrayList<SQLField>(c.comboFields);
        this.order = c.order == null ? null : new ArrayList<Path>(c.order);
    }

    private void addItemToCombo(String field) {
        this.comboFields.add(this.getPrimaryTable().getField(field));
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
        if (this.comboFields.isEmpty())
            throw new IllegalStateException("La liste des items listitems est vide!! Ils faut utiliser addComboItem...");

        // freeze the fetcher otherwise it will change with the filter
        // and that will cause the cache to fail
        final SQLRowValuesListFetcher comboSelect = this.getFetcher(null).freeze();

        final CacheKey cacheKey = new CacheKey(comboSelect, this.fieldSeparator, this.undefLabel, this.customizeItem);
        final CacheResult<List<IComboSelectionItem>> l = cache.check(cacheKey);
        if (l.getState() == CacheResult.State.INTERRUPTED)
            throw new RTInterruptedException("interrupted while waiting for the cache");
        else if (l.getState() == CacheResult.State.VALID)
            return l.getRes();

        try {
            final List<IComboSelectionItem> result = new ArrayList<IComboSelectionItem>();
            for (final SQLRowValues vals : comboSelect.fetch()) {
                if (Thread.currentThread().isInterrupted())
                    throw new RTInterruptedException("interrupted in fill");
                result.add(createItem(vals));
            }

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
    }

    private final IComboSelectionItem createItem(final SQLRowValues rs) {
        final String desc;
        if (this.undefLabel != null && rs.getID() == getPrimaryTable().getUndefinedID())
            desc = this.undefLabel;
        else
            desc = CollectionUtils.join(this.exp.expandGroupBy(this.getFields()), SEP_CHILD, new ITransformer<Tuple2<Path, List<FieldPath>>, Object>() {
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
        final IComboSelectionItem res = new IComboSelectionItem(rs.getID(), desc);
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
}
