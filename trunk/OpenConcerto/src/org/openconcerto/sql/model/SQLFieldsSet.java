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
 
 /*
 * SQLFieldsSet created on 12 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.utils.CollectionMap2Itf.SetMapItf;
import org.openconcerto.utils.SetMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * Un ensemble de champs SQL. Les champs sont indexés par table, et on peut donc connaître
 * l'ensemble des tables des champs ou encore avoir tous les champs pour une table.
 * 
 * @see #getTables()
 * @see #getFields(SQLTable)
 * @author ILM Informatique 12 mai 2004
 */
@Immutable
public class SQLFieldsSet {
    static private final SQLFieldsSet EMPTY = new SQLFieldsSet(SetMap.<SQLTable, SQLField> empty(), true);

    static public SQLFieldsSet empty() {
        return EMPTY;
    }

    static public SQLFieldsSet create(final SQLTable t, final String... names) {
        final SetMapItf<SQLTable, SQLField> res = createMap();
        for (final String name : names) {
            res.add(t, t.getField(name));
        }
        return new SQLFieldsSet(res, false);
    }

    static public SQLFieldsSet create(final Map<SQLTable, ? extends Collection<SQLField>> fields) {
        final SetMapItf<SQLTable, SQLField> res = createMap();
        res.merge(fields);
        return new SQLFieldsSet(res, false);
    }

    static private final SetMapItf<SQLTable, SQLField> toSetMap(final Collection<SQLField> fields) {
        final SetMapItf<SQLTable, SQLField> res = createMap();
        for (final SQLField f : fields)
            res.add(f.getTable(), f);
        return res;
    }

    static private SetMapItf<SQLTable, SQLField> createMap() {
        return new SetMap<SQLTable, SQLField>() {
            @Override
            public Set<SQLField> createCollection(Collection<? extends SQLField> v) {
                final LinkedHashSet<SQLField> res = new LinkedHashSet<SQLField>(8);
                res.addAll(v);
                return res;
            }
        };
    }

    static public final Set<String> getNames(final Collection<SQLField> fields) {
        final Set<String> res = new HashSet<String>(fields.size());
        for (final SQLField f : fields)
            res.add(f.getName());
        return res;
    }

    private final SetMapItf<SQLTable, SQLField> tables;

    /**
     * Crée un ensemble composé des champs passés.
     * 
     * @param fields un ensemble de SQLField, l'ensemble n'est pas modifié.
     */
    public SQLFieldsSet(final Collection<SQLField> fields) {
        this(toSetMap(fields), false);
    }

    private SQLFieldsSet(final SetMapItf<SQLTable, SQLField> fields, final boolean unmodif) {
        this.tables = unmodif ? fields : SetMap.unmodifiableMap(fields);
    }

    public final SetMapItf<SQLTable, SQLField> getFields() {
        return this.tables;
    }

    /**
     * Retourne tous les champs de cet ensemble appartenant à la table passée.
     * 
     * @param table la table dont on veut les champs.
     * @return l'ensemble des champs appartenant à la table.
     */
    public final Set<SQLField> getFields(final SQLTable table) {
        return this.tables.getNonNull(table);
    }

    public final Set<SQLField> getFields(final String table) {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final SQLTable t : getTables())
            if (t.getName().equals(table))
                res.addAll(this.getFields(t));
        return res;
    }

    public final Set<String> getFieldsNames(final SQLTable table) {
        return getNames(this.getFields(table));
    }

    /**
     * Retourne toutes les tables des champs.
     * 
     * @return l'ensemble des SQLTable.
     */
    public final Set<SQLTable> getTables() {
        return this.tables.keySet();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.tables;
    }

}
