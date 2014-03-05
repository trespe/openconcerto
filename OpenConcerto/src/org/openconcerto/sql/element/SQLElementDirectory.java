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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.i18n.LocalizedInstances;
import org.openconcerto.utils.i18n.Phrase;
import org.openconcerto.utils.i18n.TranslationManager;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jdom.JDOMException;

/**
 * Directory of SQLElement by table.
 * 
 * @author Sylvain CUAZ
 */
public final class SQLElementDirectory {

    public static final String BASENAME = SQLElementNames.class.getSimpleName();
    private static final LocalizedInstances<SQLElementNames> LOCALIZED_INSTANCES = new LocalizedInstances<SQLElementNames>(SQLElementNames.class, TranslationManager.getControl()) {
        @Override
        protected SQLElementNames createInstance(String bundleName, Locale candidate, Class<?> cl) throws IOException {
            final InputStream ins = cl.getResourceAsStream('/' + getControl().toResourceName(bundleName, "xml"));
            if (ins == null)
                return null;
            final SQLElementNamesFromXML res = new SQLElementNamesFromXML(candidate);
            try {
                res.load(ins);
            } catch (JDOMException e) {
                throw new IOException("Invalid XML", e);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                ins.close();
            }
            return res;
        }
    };

    private final Map<SQLTable, SQLElement> elements;
    private final CollectionMap<String, SQLTable> tableNames;
    private final CollectionMap<String, SQLTable> byCode;
    private final CollectionMap<Class<? extends SQLElement>, SQLTable> byClass;
    private final List<DirectoryListener> listeners;

    private String phrasesPkgName;
    private final Map<String, SQLElementNames> elementNames;

    public SQLElementDirectory() {
        this.elements = new HashMap<SQLTable, SQLElement>();
        // to mimic elements behaviour, if we add twice the same table
        // the second one should replace the first one
        this.tableNames = new CollectionMap<String, SQLTable>(HashSet.class);
        this.byCode = new CollectionMap<String, SQLTable>(HashSet.class);
        this.byClass = new CollectionMap<Class<? extends SQLElement>, SQLTable>(HashSet.class);

        this.listeners = new ArrayList<DirectoryListener>();

        this.phrasesPkgName = null;
        this.elementNames = new HashMap<String, SQLElementNames>();
    }

    private static <K> SQLTable getSoleTable(CollectionMap<K, SQLTable> m, K key) throws IllegalArgumentException {
        final Collection<SQLTable> res = m.getNonNull(key);
        if (res.size() > 1)
            throw new IllegalArgumentException(key + " is not unique: " + CollectionUtils.join(res, ",", new ITransformer<SQLTable, SQLName>() {
                @Override
                public SQLName transformChecked(SQLTable input) {
                    return input.getSQLName();
                }
            }));
        return CollectionUtils.getSole(res);
    }

    public synchronized final void putAll(SQLElementDirectory o) {
        for (final SQLElement elem : o.getElements()) {
            if (!this.contains(elem.getTable()))
                this.addSQLElement(elem);
        }
    }

    /**
     * Add an element by creating it with the no-arg constructor. If the element cannot find its
     * table and thus raise DBStructureItemNotFound, the exception is logged.
     * 
     * @param element the element to add.
     */
    public final void addSQLElement(final Class<? extends SQLElement> element) {
        try {
            this.addSQLElement(element.getConstructor().newInstance());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof DBStructureItemNotFound) {
                Log.get().config("ignore inexistent tables: " + e.getCause().getLocalizedMessage());
                return;
            }
            throw new IllegalArgumentException("ctor failed", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("no-arg ctor failed", e);
        }
    }

    /**
     * Adds an already instantiated element.
     * 
     * @param elem the SQLElement to add.
     * @return the previously added element.
     */
    public synchronized final SQLElement addSQLElement(SQLElement elem) {
        final SQLElement res = this.removeSQLElement(elem.getTable());
        this.elements.put(elem.getTable(), elem);
        this.tableNames.put(elem.getTable().getName(), elem.getTable());
        this.byCode.put(elem.getCode(), elem.getTable());
        this.byClass.put(elem.getClass(), elem.getTable());
        for (final DirectoryListener dl : this.listeners) {
            dl.elementAdded(elem);
        }
        elem.setDirectory(this);
        return res;
    }

    public synchronized final boolean contains(SQLTable t) {
        return this.elements.containsKey(t);
    }

    public synchronized final SQLElement getElement(SQLTable t) {
        return this.elements.get(t);
    }

    /**
     * Search for a table whose name is <code>tableName</code>.
     * 
     * @param tableName a table name, e.g. "ADRESSE".
     * @return the corresponding SQLElement, or <code>null</code> if there is no table named
     *         <code>tableName</code>.
     * @throws IllegalArgumentException if more than one table match.
     */
    public synchronized final SQLElement getElement(String tableName) {
        return this.getElement(getSoleTable(this.tableNames, tableName));
    }

    /**
     * Search for an SQLElement whose class is <code>clazz</code>.
     * 
     * @param <S> type of SQLElement
     * @param clazz the class.
     * @return the corresponding SQLElement, or <code>null</code> if none can be found.
     * @throws IllegalArgumentException if there's more than one match.
     */
    public synchronized final <S extends SQLElement> S getElement(Class<S> clazz) {
        return clazz.cast(this.getElement(getSoleTable(this.byClass, clazz)));
    }

    public synchronized final SQLElement getElementForCode(String code) {
        return this.getElement(getSoleTable(this.byCode, code));
    }

    public synchronized final Set<SQLTable> getTables() {
        return this.getElementsMap().keySet();
    }

    public synchronized final Collection<SQLElement> getElements() {
        return this.getElementsMap().values();
    }

    public final Map<SQLTable, SQLElement> getElementsMap() {
        return Collections.unmodifiableMap(this.elements);
    }

    /**
     * Remove the passed instance. NOTE: this method only remove the specific instance passed, so
     * it's a conditional <code>removeSQLElement(elem.getTable())</code>.
     * 
     * @param elem the instance to remove.
     * @see #removeSQLElement(SQLTable)
     */
    public synchronized void removeSQLElement(SQLElement elem) {
        if (this.getElement(elem.getTable()) == elem)
            this.removeSQLElement(elem.getTable());
    }

    /**
     * Remove the element for the passed table.
     * 
     * @param t the table to remove.
     * @return the removed element, can be <code>null</code>.
     */
    public synchronized SQLElement removeSQLElement(SQLTable t) {
        final SQLElement elem = this.elements.remove(t);
        if (elem != null) {
            this.tableNames.remove(elem.getTable().getName(), elem.getTable());
            this.byCode.remove(elem.getCode(), elem.getTable());
            this.byClass.remove(elem.getClass(), elem.getTable());
            // MAYBE only reset neighbours.
            for (final SQLElement otherElem : this.elements.values())
                otherElem.resetRelationships();
            elem.setDirectory(null);
            for (final DirectoryListener dl : this.listeners) {
                dl.elementRemoved(elem);
            }
        }
        return elem;
    }

    public synchronized final void initL18nPackageName(final String baseName) {
        if (this.phrasesPkgName != null)
            throw new IllegalStateException("Already initialized : " + this.getL18nPackageName());
        this.phrasesPkgName = baseName;
    }

    public synchronized final String getL18nPackageName() {
        return this.phrasesPkgName;
    }

    protected synchronized final SQLElementNames getElementNames(final String pkgName, final Locale locale, final Class<?> cl) {
        if (pkgName == null)
            return null;
        final char sep = ' ';
        final String key = pkgName + sep + locale.toString();
        assert pkgName.indexOf(sep) < 0 : "ambiguous key : " + key;
        SQLElementNames res = this.elementNames.get(key);
        if (res == null) {
            final List<SQLElementNames> l = LOCALIZED_INSTANCES.createInstances(pkgName + "." + BASENAME, locale, cl).get1();
            if (!l.isEmpty()) {
                for (int i = 1; i < l.size(); i++) {
                    l.get(i - 1).setParent(l.get(i));
                }
                res = l.get(0);
            }
            this.elementNames.put(key, res);
        }
        return res;
    }

    /**
     * Search a name for the passed instance and the {@link TM#getTranslationsLocale() current
     * locale}. Search for {@link SQLElementNames} using {@link LocalizedInstances} and
     * {@link SQLElementNamesFromXML} first in {@link SQLElement#getL18nPackageName()} then in
     * {@link #getL18nPackageName()}. E.g. this could load SQLElementNames_en.class and
     * SQLElementNames_en_UK.xml.
     * 
     * @param elem the element.
     * @return the name if found, <code>null</code> otherwise.
     */
    public final Phrase getName(final SQLElement elem) {
        final String elemBaseName = elem.getL18nPackageName();
        final String pkgName = elemBaseName == null ? getL18nPackageName() : elemBaseName;
        final SQLElementNames elementNames = getElementNames(pkgName, TM.getInstance().getTranslationsLocale(), elem.getL18nClass());
        return elementNames == null ? null : elementNames.getName(elem);
    }

    public synchronized final void addListener(DirectoryListener dl) {
        this.listeners.add(dl);
    }

    public synchronized final void removeListener(DirectoryListener dl) {
        this.listeners.remove(dl);
    }

    static public interface DirectoryListener {
        void elementAdded(SQLElement elem);

        void elementRemoved(SQLElement elem);
    }
}
