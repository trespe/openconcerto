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
import org.openconcerto.utils.CollectionUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Toutes les méthodes utilisent des heuristiques. Utiliser par DatabaseGraph pour faire la carte
 * d'une base MySQL.
 */
public class SQLKey {

    static public final String PREFIX = "ID_";

    /**
     * Si le champ passé est une clé.
     * 
     * @param fieldName le champ à tester.
     * @return <code>true</code> si le champ passé est une clé.
     */
    static private boolean isKey(String fieldName) {
        return fieldName.toUpperCase().startsWith(PREFIX);
    }

    /**
     * Retourne les clé étrangères de la table passée.
     * 
     * @param table la table.
     * @return l'ensemble des noms des clés étrangères.
     */
    static Set<String> foreignKeys(SQLTable table) {
        // we used to name the primary key ID_TABLE, so we must not interpret it as a self reference
        // getSole() so we can have join tables (e.g. ID_CONTENU and ID_SITE are the primary key)
        final String pkeyName = CollectionUtils.getSole(table.getPKsNames());

        final Set<String> result = new HashSet<String>();
        final Iterator i = table.getFields().iterator();
        while (i.hasNext()) {
            final String fieldName = ((SQLField) i.next()).getName();
            // inclure les clés sauf les clés primaires
            if (isKey(fieldName) && !fieldName.equals(pkeyName))
                result.add(fieldName);
        }
        return result;
    }

    /**
     * Pour une clé retourne la table correspondante.
     * <p>
     * Attention : cette méthode utilise un heuristique et n'est pas infaillible.
     * </p>
     * 
     * @param key la clé, par exemple "OBSERVATION.ID_ARTICLE_2".
     * @return la table, par exemple "ARTICLE".
     * @throws IllegalArgumentException si le champ passé n'est pas une clé.
     * @throws IllegalStateException si la table ne peut être déterminée.
     * @see #isKey(String)
     */
    static SQLTable keyToTable(SQLField key) {
        SQLTable table = key.getTable();
        String keyName = key.getName();
        if (isKey(keyName)) {
            // remove the keyPrefix
            String rest = keyName.substring(PREFIX.length());
            // remove one by one the last parts
            SQLTable res = null;
            while (res == null && rest.length() > 0) {
                // privilege our own root, then check the rest of the roots
                res = table.getDBRoot().findTable(rest);
                if (res == null) {
                    int last_ = rest.lastIndexOf('_');
                    if (last_ > -1)
                        rest = rest.substring(0, last_);
                    else
                        rest = "";
                }
            }
            if (res == null)
                throw new IllegalStateException("unable to find the table that " + key.getSQLName() + " points to.");
            if (res.getPrimaryKeys().size() != 1)
                throw new IllegalStateException(key + " points to " + res + " which doesn't have 1 primary key.");
            return res;
        } else {
            throw new IllegalArgumentException("passed string is not a key");
        }
    }

}
