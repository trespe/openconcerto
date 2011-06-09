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

import org.openconcerto.utils.cc.IPredicate;

import java.util.ArrayList;
import java.util.List;

/**
 * An SQL Identifier, such as a table or a field.
 * 
 * @author Sylvain
 */
public abstract class SQLIdentifier extends DBStructureItemJDBC {

    protected SQLIdentifier(final DBStructureItemJDBC parent, final String name) {
        super(parent, name);
    }

    // ** names

    /**
     * The complete name from the root.
     * 
     * @return the complete name, eg schema.table.field.
     */
    public final SQLName getSQLName() {
        return getSQLName(null);
    }

    /**
     * The name from <code>ancestor</code>.
     * 
     * @param ancestor an ancestor of this, eg table.
     * @return the name relative to <code>ancestor</code>, eg for a field : table.field.
     * @throws IllegalArgumentException if <code>ancestor</code> is not an ancestor of this.
     */
    public final SQLName getSQLName(DBStructureItem ancestor) {
        return this.getSQLName(ancestor, true);
    }

    /**
     * The name from <code>ancestor</code>.
     * 
     * @param ancestor an ancestor of this, <code>null</code> meaning the root, eg base.
     * @param includeAncestor whether to include <code>ancestor</code> in the name (if it's not
     *        <code>null</code>), eg <code>false</code>.
     * @return the name relative to <code>ancestor</code>, eg table.field (base.table.field if
     *         <code>includeAncestor</code> is true).
     * @throws IllegalArgumentException if <code>ancestor</code> is not an ancestor of this.
     */
    public final SQLName getSQLName(DBStructureItem ancestor, boolean includeAncestor) {
        final List<String> res = new ArrayList<String>();
        SQLIdentifier current = this;
        while (current != null && !current.isAlterEgoOf(ancestor)) {
            res.add(0, current.getName());
            current = current.getParent() instanceof SQLIdentifier ? (SQLIdentifier) current.getParent() : null;
        }
        if (current == null && ancestor != null)
            throw new IllegalArgumentException(ancestor + " is not an ancestor of " + this);

        if (includeAncestor && ancestor != null)
            res.add(0, ancestor.getName());
        return new SQLName(res);
    }

    /**
     * The shortest SQLName to identify this from <code>from</code>.
     * 
     * @param from the context, eg "CTech"."SITE".
     * @return the shortest SQLName (but never an empty one), eg "Gestion"."CLIENT" if this is
     *         "Gestion"."CLIENT", or "CATEGORIE" if this is "CTech"."CATEGORIE".
     */
    public final SQLName getContextualSQLName(SQLIdentifier from) {
        final DBStructureItemJDBC common = this.getCommonAncestor(from);
        if (common instanceof SQLIdentifier) {
            return this.getSQLName(common, common == this);
        } else
            return this.getSQLName();
    }

    public final SQLName getSQLNameUntilDBRoot(boolean includeAncestor) {
        return this.getSQLName(DBRoot.class, includeAncestor);
    }

    // not public since there's only 2 DB class : Root & SystemRoot, but SystemRoot doesn't always
    // work as sometimes it is not an identifier (eg MySQL)
    // so use getSQLNameUntilDBRoot()
    final <D extends DBStructureItemDB> SQLName getSQLName(final Class<D> ancestorClass, boolean includeAncestor) {
        final SQLIdentifier ancestor = findAncestor(new IPredicate<SQLIdentifier>() {
            @Override
            public boolean evaluateChecked(SQLIdentifier input) {
                // not getDB(): it avoids inexistant level
                return ancestorClass.isInstance(input.getRawAlterEgo());
            }
        });
        if (ancestor == null)
            throw new IllegalArgumentException("no SQLIdentifier ancestor of " + this + " is of class " + ancestorClass);
        return this.getSQLName(ancestor, includeAncestor);
    }

    private final SQLIdentifier findAncestor(IPredicate<SQLIdentifier> pred) {
        SQLIdentifier current = this;
        while (current != null && !pred.evaluateChecked(current)) {
            current = current.getParent() instanceof SQLIdentifier ? (SQLIdentifier) current.getParent() : null;
        }
        return current;
    }
}
