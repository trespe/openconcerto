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

import org.openconcerto.utils.CollectionMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Un ensemble de champs SQL. Les champs sont indexés par table, et on peut donc connaître
 * l'ensemble des tables des champs ou encore avoir tous les champs pour une table.
 * 
 * @see #getTables()
 * @see #getFields(SQLTable)
 * @author ILM Informatique 12 mai 2004
 */
public class SQLFieldsSet {

    // SQLTable => {SQLField}
    private final CollectionMap<SQLTable, SQLField> tables;
    private String name;

    /**
     * Crée un ensemble vide.
     */
    public SQLFieldsSet() {
        this(new HashSet<SQLField>());
    }

    /**
     * Crée un ensemble composé des champs passés.
     * 
     * @param fields un ensemble de SQLField, l'ensemble n'est pas modifié.
     */
    public SQLFieldsSet(final Collection<SQLField> fields) {
        this.tables = new CollectionMap<SQLTable, SQLField>(LinkedHashSet.class);
        this.setFields(fields);
    }

    private void setFields(final Collection<SQLField> fields) {
        this.tables.clear();
        for (final SQLField field : fields) {
            this.add(field);
        }
    }

    /**
     * Ajoute un champ.
     * 
     * @param field le champ a ajouté.
     */
    public final void add(final SQLField field) {
        this.tables.put(field.getTable(), field);
    }

    public final void retain(final SQLTable t) {
        this.tables.keySet().retainAll(Collections.singleton(t));
    }

    /**
     * Retourne tous les champs de cet ensemble appartenant à la table passée.
     * 
     * @param table la table dont on veut les champs.
     * @return l'ensemble des champs appartenant à la table.
     */
    public final Set<SQLField> getFields(final SQLTable table) {
        return (Set<SQLField>) this.tables.getNonNull(table);
    }

    public final Set<SQLField> getFields(final String table) {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final SQLTable t : getTables())
            if (t.getName().equals(table))
                res.addAll(this.getFields(t));
        return res;
    }

    public final Set<String> getFieldsNames(final SQLTable table) {
        final Set<String> res = new HashSet<String>();
        for (final SQLField f : this.getFields(table))
            res.add(f.getName());
        return res;
    }

    /**
     * Retourne toutes les tables des champs.
     * 
     * @return l'ensemble des SQLTable.
     */
    public final Set<SQLTable> getTables() {
        return Collections.unmodifiableSet(this.tables.keySet());
    }

    /**
     * Retourne toutes les champs.
     * 
     * @return l'ensemble des SQLField.
     */
    public final Set<SQLField> asSet() {
        return new HashSet<SQLField>(this.tables.values());
    }

    public final void setName(final String string) {
        this.name = string;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.name;
    }

}
