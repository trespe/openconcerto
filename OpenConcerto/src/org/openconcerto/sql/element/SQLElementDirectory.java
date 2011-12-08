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
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directory of SQLElement by table.
 * 
 * @author Sylvain CUAZ
 */
public final class SQLElementDirectory {

    private final Map<SQLTable, SQLElement> elements;
    private final CollectionMap<String, SQLTable> tableNames;
    private final CollectionMap<Class<? extends SQLElement>, SQLTable> byClass;
    private final List<DirectoryListener> listeners;

    public SQLElementDirectory() {
        this.elements = new HashMap<SQLTable, SQLElement>();
        // to mimic elements behaviour, if we add twice the same table
        // the second one should replace the first one
        this.tableNames = new CollectionMap<String, SQLTable>(HashSet.class);
        this.byClass = new CollectionMap<Class<? extends SQLElement>, SQLTable>(HashSet.class);

        this.listeners = new ArrayList<DirectoryListener>();
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
     */
    public synchronized final void addSQLElement(SQLElement elem) {
        this.elements.put(elem.getTable(), elem);
        this.tableNames.put(elem.getTable().getName(), elem.getTable());
        this.byClass.put(elem.getClass(), elem.getTable());
        for (final DirectoryListener dl : this.listeners) {
            dl.elementAdded(elem);
        }
        String canonicalName = elem.getClass().getCanonicalName();
        if (canonicalName.contains("erp.core") && canonicalName.contains(".element")) {
            int i = canonicalName.indexOf("erp.core") + 9;
            int j = canonicalName.indexOf(".element");
            canonicalName = canonicalName.substring(i, j);
        }
        ElementMapper.getInstance().map(canonicalName + ".element", elem);
        ElementMapper.getInstance().map(canonicalName + ".list.table", elem.getTable().getName());
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
            this.byClass.remove(elem.getClass(), elem.getTable());
            // MAYBE only reset neighbours.
            for (final SQLElement otherElem : this.elements.values())
                otherElem.resetRelationships();
            for (final DirectoryListener dl : this.listeners) {
                dl.elementRemoved(elem);
            }
        }
        return elem;
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
