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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.element.SQLElementDirectory.DirectoryListener;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Class to obtain a RowItemDesc from a table and a name.
 * 
 * @author ilm 22 nov. 2004
 * @see #getDescFor(SQLTable, String)
 */
@ThreadSafe
public class SQLFieldTranslator {

    // OK since RowItemDesc is immutable
    /**
     * Instance representing "no description".
     */
    public static final RowItemDesc NULL_DESC = new RowItemDesc(null, null);

    private static final String METADATA_TABLENAME = SQLSchema.FWK_TABLENAME_PREFIX + "RIV_METADATA";

    /**
     * Use the code and not the table name, since the same table might be used differently at
     * different times (e.g. dropped then recreated some time later with a different purpose). Or
     * conversely, a table might get renamed.
     */
    private static final String ELEM_FIELDNAME = "ELEMENT_CODE";
    private static final String COMP_FIELDNAME = "COMPONENT_CODE";
    private static final String ITEM_FIELDNAME = "ITEM";

    private static final String DOC_FIELDNAME = "DOCUMENTATION";
    private static final String COL_TITLE_FIELDNAME = "COLUMN_TITLE";
    private static final String LABEL_FIELDNAME = "LABEL";

    private static final String CORE_VARIANT = "CORE";
    private static final String DB_VARIANT = "DB";

    static public SQLTable getMetaTable(final DBRoot root) throws SQLException {
        if (!root.contains(METADATA_TABLENAME)) {
            final SQLCreateTable createValueT = new SQLCreateTable(root, METADATA_TABLENAME);
            createValueT.setPlain(true);
            createValueT.addColumn(SQLSyntax.ID_NAME, createValueT.getSyntax().getPrimaryIDDefinition());
            final String nullableVarChar = "varchar(" + Preferences.MAX_KEY_LENGTH + ")";
            createValueT.addColumn(ELEM_FIELDNAME, nullableVarChar);
            createValueT.addColumn(COMP_FIELDNAME, nullableVarChar);
            createValueT.addColumn(ITEM_FIELDNAME, nullableVarChar + " NOT NULL");
            createValueT.addUniqueConstraint("uniq", Arrays.asList(ELEM_FIELDNAME, COMP_FIELDNAME, ITEM_FIELDNAME));
            createValueT.addVarCharColumn(LABEL_FIELDNAME, 256);
            createValueT.addVarCharColumn(COL_TITLE_FIELDNAME, 256);
            createValueT.addVarCharColumn(DOC_FIELDNAME, Preferences.MAX_VALUE_LENGTH, true);
            root.createTable(createValueT);
        }
        return root.getTable(METADATA_TABLENAME);
    }

    public static RowItemDesc getDefaultDesc(SQLField f) {
        String name = f.getName(), label = null;
        if (f.isPrimaryKey())
            label = "ID";
        else if (f.getTable().getForeignKeys().contains(f))
            name = name.startsWith(SQLKey.PREFIX) ? name.substring(SQLKey.PREFIX.length()) : name;
        if (label == null)
            label = cleanupName(name);
        return new RowItemDesc(label, label);
    }

    private static String cleanupName(final String name) {
        return StringUtils.firstUpThenLow(name).replace('_', ' ');
    }

    public static RowItemDesc getDefaultDesc(SQLTable t, final String name) {
        if (t.contains(name))
            return getDefaultDesc(t.getField(name));

        final String label = cleanupName(name);
        return new RowItemDesc(label, label);
    }

    // Instance members

    // { SQLTable -> { compCode, variant, item -> RowItemDesc }}
    @GuardedBy("this")
    private final Map<SQLTable, Map<List<String>, RowItemDesc>> translation;
    private final SQLTable table;
    private final SQLElementDirectory dir;
    @GuardedBy("this")
    private final Set<String> unknownCodes;

    {
        this.translation = new HashMap<SQLTable, Map<List<String>, RowItemDesc>>();
        this.unknownCodes = new HashSet<String>();
    }

    public SQLFieldTranslator(DBRoot base) {
        this(base, null);
    }

    public SQLFieldTranslator(DBRoot base, InputStream inputStream) {
        this(base, inputStream, null);
    }

    /**
     * Create a new instance.
     * 
     * @param root the default root for tables.
     * @param inputStream the XML, can be <code>null</code>.
     * @param dir the directory where to look for tables not in <code>root</code>, can be
     *        <code>null</code>.
     */
    public SQLFieldTranslator(DBRoot root, InputStream inputStream, SQLElementDirectory dir) {
        try {
            this.table = getMetaTable(root);
        } catch (SQLException e) {
            throw new IllegalStateException("Couldn't get the meta table", e);
        }
        this.dir = dir;
        this.dir.addListener(new DirectoryListener() {
            @Override
            public void elementRemoved(SQLElement elem) {
                // nothing, SQLElement is not required (only needed for code)
            }

            @Override
            public void elementAdded(SQLElement elem) {
                final boolean isUnknown;
                synchronized (SQLFieldTranslator.this) {
                    isUnknown = SQLFieldTranslator.this.unknownCodes.contains(elem.getCode());
                }
                if (isUnknown) {
                    fetch(Collections.singleton(elem.getCode()));
                }
            }
        });
        if (inputStream != null)
            this.load(root, inputStream);
        fetchAndPut(this.table, null);
    }

    /**
     * Add all translations of <code>o</code> to this, note that if a table is present in both this
     * and <code>o</code> its translations won't be changed.
     * 
     * @param o another SQLFieldTranslator to add.
     */
    public void putAll(SQLFieldTranslator o) {
        if (o == this)
            return;
        final int thisHash = System.identityHashCode(this);
        final int oHash = System.identityHashCode(o);
        final SQLFieldTranslator o1, o2;
        if (thisHash < oHash) {
            o1 = this;
            o2 = o;
        } else if (thisHash > oHash) {
            o1 = o;
            o2 = this;
        } else {
            throw new IllegalStateException("Hash equal");
        }
        synchronized (o1) {
            synchronized (o2) {
                CollectionUtils.addIfNotPresent(this.translation, o.translation);
            }
        }
    }

    public void load(DBRoot b, File file) {
        try {
            load(b, new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Element> getChildren(final Element elem) {
        return elem.getChildren();
    }

    /**
     * Load more translations.
     * 
     * @param b the default root for tables.
     * @param inputStream the XML.
     */
    public void load(DBRoot b, InputStream inputStream) {
        this.load(b, CORE_VARIANT, inputStream);
    }

    /**
     * Load translations from the passed stream.
     * 
     * @param b the default root for tables.
     * @param variant the variant to use.
     * @param inputStream the XML.
     * @return the loaded tables and the names not found (and thus not loaded).
     */
    public Tuple2<Set<SQLTable>, Set<String>> load(DBRoot b, final String variant, InputStream inputStream) {
        if (inputStream == null)
            throw new NullPointerException("inputStream is null");
        final Set<SQLTable> res = new HashSet<SQLTable>();
        final Set<String> notFound = new HashSet<String>();
        try {
            final Document doc = new SAXBuilder().build(inputStream);
            // System.out.println("Base de donnée:"+base);
            for (final Element elem : getChildren(doc.getRootElement())) {
                final String elemName = elem.getName().toLowerCase();
                final DBRoot root;
                final List<Element> tableElems;
                if (elemName.equals("table")) {
                    root = b;
                    tableElems = Collections.singletonList(elem);
                } else if (elemName.equals("root")) {
                    root = b.getDBSystemRoot().getRoot(elem.getAttributeValue("name"));
                    tableElems = getChildren(elem);
                } else {
                    root = null;
                    tableElems = null;
                }
                if (tableElems != null) {
                    for (final Element tableElem : tableElems) {
                        final Tuple2<String, SQLTable> t = load(root, variant, tableElem, true);
                        if (t.get1() == null) {
                            notFound.add(t.get0());
                        } else {
                            res.add(t.get1());
                        }
                    }
                }
            }
            // load() returns null if no table is found
            assert !res.contains(null);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Tuple2.create(res, notFound);
    }

    private Tuple2<String, SQLTable> load(DBRoot b, final String variant, final Element tableElem, final boolean lenient) {
        final String tableName = tableElem.getAttributeValue("name");
        SQLTable table = b.getTable(tableName);
        if (table == null && this.dir != null && this.dir.getElement(tableName) != null)
            table = this.dir.getElement(tableName).getTable();
        if (table != null) {
            for (final Element elem : getChildren(tableElem)) {
                final String elemName = elem.getName().toLowerCase();
                if (elemName.equals("field")) {
                    this.load(table, SQLElement.DEFAULT_COMP_ID, variant, elem);
                } else if (elemName.equals("component")) {
                    final String compCode = elem.getAttributeValue("code");
                    for (final Element fieldElem : getChildren(elem)) {
                        this.load(table, compCode, variant, fieldElem);
                    }
                }
            }
        } else if (lenient) {
            // allow to supply the union all tables and ignore those that aren't in a given base
            Log.get().config("Ignore loading of inexistent table " + tableName);
        } else {
            throw new IllegalStateException("Table not found : " + tableName);
        }
        return Tuple2.create(tableName, table);
    }

    private void load(final SQLTable table, final String compCode, final String variant, final Element fieldElem) {
        final String name = fieldElem.getAttributeValue("name");
        final String label = fieldElem.getAttributeValue("label");
        final String title = fieldElem.getAttributeValue("titlelabel", label);
        final String documentation = fieldElem.getText();
        this.setDescFor(table, compCode, variant, name, new RowItemDesc(label, title, documentation));
    }

    public final void fetch(final Set<String> codes) {
        this.fetchAndPut(this.table, codes);
    }

    private List<SQLRow> fetchOnly(final SQLTable table, final Where w) {
        return SQLRowListRSH.execute(new SQLSelect().addSelectStar(table).setWhere(w));
    }

    private void fetchAndPut(final SQLTable table, final Set<String> codes) {
        final Where w;
        if (codes == null) {
            w = null;
            this.removeTranslation((SQLTable) null, null, DB_VARIANT, null);
        } else {
            w = new Where(table.getField(ELEM_FIELDNAME), codes);
            for (final String elementCode : codes)
                this.removeTranslation(this.dir.getElementForCode(elementCode).getTable(), null, DB_VARIANT, null);
        }
        for (final SQLRow r : fetchOnly(table, w)) {
            final String elementCode = r.getString(ELEM_FIELDNAME);
            // needed since tables can be loaded at any time in SQLElementDirectory
            // MAYBE use code as the map key instead of SQLTable
            final SQLElement elem = this.dir.getElementForCode(elementCode);
            if (elem != null) {
                final String componentCode = r.getString(COMP_FIELDNAME);
                final String item = r.getString(ITEM_FIELDNAME);
                final RowItemDesc desc = new RowItemDesc(r.getString(LABEL_FIELDNAME), r.getString(COL_TITLE_FIELDNAME), r.getString(DOC_FIELDNAME));
                synchronized (this) {
                    putTranslation(elem.getTable(), componentCode, DB_VARIANT, item, desc);
                    this.unknownCodes.remove(elementCode);
                }
            } else {
                synchronized (this) {
                    this.unknownCodes.add(elementCode);
                }
            }
        }
    }

    private synchronized final Map<List<String>, RowItemDesc> getMap(final SQLTable t) {
        Map<List<String>, RowItemDesc> elemMap = this.translation.get(t);
        if (elemMap == null) {
            elemMap = new HashMap<List<String>, RowItemDesc>();
            this.translation.put(t, elemMap);
        }
        return elemMap;
    }

    private synchronized final void putTranslation(SQLTable t, String compCode, String variant, String item, RowItemDesc desc) {
        if (t == null)
            throw new IllegalArgumentException("Table cannot be null");
        // needed by remove()
        if (compCode == null || variant == null || item == null)
            throw new IllegalArgumentException("Values cannot be null");
        this.getMap(t).put(Arrays.asList(compCode, variant, item), desc);
    }

    private synchronized final void removeTranslation(SQLTable t, String compCode, String variant, String name) {
        // null means match everything, OK since we test in putTranslation() that we don't contain
        // null values
        if (t == null) {
            for (final Map<List<String>, RowItemDesc> m : this.translation.values()) {
                this.removeTranslation(m, compCode, variant, name);
            }
        } else {
            this.removeTranslation(this.translation.get(t), compCode, variant, name);
        }
    }

    private synchronized void removeTranslation(Map<List<String>, RowItemDesc> m, String compCode, String variant, String name) {
        if (m == null)
            return;

        if (compCode == null && variant == null && name == null) {
            m.clear();
        } else if (compCode != null && variant != null && name != null) {
            m.remove(Arrays.asList(compCode, variant, name));
        } else {
            final Iterator<List<String>> iter = m.keySet().iterator();
            while (iter.hasNext()) {
                final List<String> l = iter.next();
                if ((compCode == null || compCode.equals(l.get(0))) && (variant == null || variant.equals(l.get(1))) && (name == null || name.equals(l.get(2))))
                    iter.remove();
            }
        }
    }

    private synchronized final RowItemDesc getTranslation(SQLTable t, String compCode, String variant, String item) {
        return this.getMap(t).get(Arrays.asList(compCode, variant, item));
    }

    private final RowItemDesc getTranslation(SQLTable t, String compCodeArg, List<String> variants, String name) {
        final LinkedList<String> ll = new LinkedList<String>(variants);
        ll.addFirst(DB_VARIANT);
        ll.addLast(CORE_VARIANT);

        final String[] compCodes;
        if (compCodeArg == SQLElement.DEFAULT_COMP_ID)
            compCodes = new String[] { SQLElement.DEFAULT_COMP_ID };
        else
            compCodes = new String[] { compCodeArg, SQLElement.DEFAULT_COMP_ID };
        for (final String compCode : compCodes) {
            for (final String variant : ll) {
                final RowItemDesc labeledField = this.getTranslation(t, compCode, variant, name);
                if (labeledField != null)
                    return labeledField;
            }
        }
        return null;
    }

    public RowItemDesc getDescFor(SQLTable t, String name) {
        return getDescFor(t, SQLElement.DEFAULT_COMP_ID, name);
    }

    /**
     * Find the description for the passed item. This method will search for an SQLElement for
     * <code>t</code> to get its {@link SQLElement#getMDPath() variants path}.
     * 
     * @param t the table.
     * @param compCode the component code.
     * @param name the item name.
     * @return the first description that matches the parameters, never <code>null</code> but
     *         {@link #NULL_DESC}.
     * @see #getDescFor(SQLTable, String, List, String)
     */
    public RowItemDesc getDescFor(SQLTable t, String compCode, String name) {
        final SQLElement element = this.dir == null ? null : this.dir.getElement(t);
        final List<String> variants = element == null ? Collections.<String> emptyList() : element.getMDPath();
        return getDescFor(t, compCode, variants, name);
    }

    public RowItemDesc getDescFor(final String elementCode, String compCode, String name) {
        return this.getDescFor(elementCode, compCode, null, name);
    }

    public RowItemDesc getDescFor(final String elementCode, String compCode, List<String> variants, String name) {
        final SQLElement elem = this.dir.getElementForCode(elementCode);
        if (variants == null)
            variants = elem.getMDPath();
        return this.getDescFor(elem.getTable(), compCode, variants, name);
    }

    /**
     * Find the description for the passed item. This method will try {compCode, variant, item}
     * first with the DB variant, then with each passed variant and last with the default variant,
     * until one description is found. If none is found, it will retry with the code
     * {@link SQLElement#DEFAULT_COMP_ID}. If none is found, it will retry all the above after
     * having refreshed its cache from the DB.
     * 
     * @param t the table.
     * @param compCodeArg the component code.
     * @param variants the variants to search, not <code>null</code> but can be empty.
     * @param name the item name.
     * @return the first description that matches the parameters, never <code>null</code> but
     *         {@link #NULL_DESC}.
     */
    public RowItemDesc getDescFor(SQLTable t, String compCodeArg, List<String> variants, String name) {
        RowItemDesc labeledField = this.getTranslation(t, compCodeArg, variants, name);
        // if nothing found, re-fetch from the DB
        if (labeledField == null && this.dir.getElement(t) != null) {
            this.fetchAndPut(this.table, Collections.singleton(this.dir.getElement(t).getCode()));
            labeledField = this.getTranslation(t, compCodeArg, variants, name);
        }
        if (labeledField == null) {
            // we didn't find a requested item
            Log.get().info("unknown item " + name + " in " + t);
            return NULL_DESC;
        } else {
            return labeledField;
        }
    }

    private RowItemDesc getDescFor(SQLField f) {
        return this.getDescFor(f.getTable(), f.getName());
    }

    public String getLabelFor(SQLField f) {
        return this.getDescFor(f).getLabel();
    }

    public String getTitleFor(SQLField f) {
        return this.getDescFor(f).getTitleLabel();
    }

    public final void setDescFor(final SQLTable table, final String componentCode, final String variant, final String name, final RowItemDesc desc) {
        if (DB_VARIANT.equals(variant))
            throw new IllegalArgumentException("Use storeDescFor()");
        putTranslation(table, componentCode, variant, name, desc);
    }

    public final void removeDescFor(SQLTable t, String compCode, String variant, String name) {
        if (DB_VARIANT.equals(variant))
            throw new IllegalArgumentException("Cannot remove DB values, use deleteDescFor()");
        this.removeTranslation(t, compCode, variant, name);
    }

    public final void storeDescFor(final String elementCode, final String componentCode, final String name, final RowItemDesc desc) throws SQLException {
        this.storeDescFor(this.dir.getElementForCode(elementCode).getTable(), componentCode, name, desc);
    }

    public final void storeDescFor(final SQLTable table, final String componentCode, final String name, final RowItemDesc desc) throws SQLException {
        final String elementCode = this.dir.getElement(table).getCode();
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put(ELEM_FIELDNAME, elementCode);
        m.put(COMP_FIELDNAME, componentCode);
        m.put(ITEM_FIELDNAME, name);
        final SQLTable mdT = this.table;
        SQLUtils.executeAtomic(this.table.getDBSystemRoot().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                final List<SQLRow> existing = fetchOnly(mdT, Where.and(mdT, m));
                assert existing.size() <= 1 : "Unique constraint failed for " + m;
                final SQLRowValues vals;
                if (existing.size() == 0)
                    vals = new SQLRowValues(mdT, m);
                else
                    vals = existing.get(0).asRowValues();
                vals.put(LABEL_FIELDNAME, desc.getLabel());
                vals.put(COL_TITLE_FIELDNAME, desc.getTitleLabel());
                vals.put(DOC_FIELDNAME, desc.getDocumentation());
                vals.commit();
                putTranslation(table, componentCode, DB_VARIANT, name, desc);
                return null;
            }
        });
    }

    public final void deleteDescFor(final SQLTable elemTable, final String componentCode, final String name) throws SQLException {
        Where w = null;
        if (elemTable != null)
            w = new Where(this.table.getField(ELEM_FIELDNAME), "=", this.dir.getElement(elemTable).getCode()).and(w);
        if (componentCode != null)
            w = new Where(this.table.getField(COMP_FIELDNAME), "=", componentCode).and(w);
        if (name != null)
            w = new Where(this.table.getField(ITEM_FIELDNAME), "=", name).and(w);
        final String whereString = w == null ? "" : " where " + w.getClause();
        try {
            this.table.getDBSystemRoot().getDataSource().execute("DELETE FROM " + this.table.getSQLName().quote() + whereString);
        } catch (Exception e) {
            throw new SQLException(e);
        }
        this.removeTranslation(elemTable, componentCode, DB_VARIANT, name);
    }
}
