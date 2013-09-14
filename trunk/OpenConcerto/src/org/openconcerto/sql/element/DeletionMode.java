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
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

/**
 * Permet de modifier une seule ligne (et ces privates).
 * 
 * @author Sylvain CUAZ
 */
public abstract class DeletionMode {

    public abstract String getName();

    public abstract boolean destroyInformation();

    public String toString() {
        return "DeletionMode[" + this.getName() + "]";
    }

    private void execute(SQLElement elem, int id) throws SQLException {
        this.execute(elem, new SQLRow(elem.getTable(), id));
    }

    public synchronized void execute(SQLElement elem, SQLRowAccessor ra) throws SQLException {
        final int id = ra.getID();
        final SQLRow r = ra.asRow();
        Log.get().fine(this + " on " + elem + "/" + id);
        if (r.isUndefined())
            throw new IllegalArgumentException(this + " can't " + this.getName() + " the undefined");

        if (this.destroyInformation()) {
            this.updateDB(elem, id);
            this.updatePrivates(elem, r);
        } else {
            this.updatePrivates(elem, r);
            this.updateDB(elem, id);
        }
    }

    public abstract void fireChange(SQLRowAccessor row);

    protected final SQLRowValues getUpdateClause(SQLElement elem, boolean archive) {
        final Object newVal;
        if (Boolean.class.equals(elem.getTable().getArchiveField().getType().getJavaType()))
            newVal = archive;
        else
            newVal = archive ? 1 : 0;
        return new SQLRowValues(elem.getTable()).put(elem.getTable().getArchiveField().getName(), newVal);
    }

    protected final String getWhereClause(SQLElement elem, int id) {
        return " WHERE " + new Where(elem.getTable().getKey(), "=", id).getClause();
    }

    protected abstract void updateDB(SQLElement elem, int id) throws SQLException;

    private void updatePrivates(SQLElement elem, SQLRow row) throws SQLException {
        final Set<String> pff = elem.getPrivateForeignFields();
        if (pff.size() > 0) {
            // fetch only if needed
            if (!row.getFields().containsAll(pff))
                row.fetchValues();
            for (final String foreignField : pff) {
                if (!row.isForeignEmpty(foreignField))
                    // don't use getForeign() as it can do a request, whereas execute() only does
                    // one if necessary
                    this.execute(elem.getPrivateElement(foreignField), row.getInt(foreignField));
            }
        }
    }

    //

    public static DeletionMode ArchiveMode = new DeletionMode() {

        @Override
        public String getName() {
            return "archive";
        }

        @Override
        public boolean destroyInformation() {
            return true;
        }

        @Override
        protected void updateDB(SQLElement elem, int id) throws SQLException {
            this.getUpdateClause(elem, true).update(id);
        }

        @Override
        public void fireChange(SQLRowAccessor row) {
            row.getTable().fireTableModified(row.getID(), Collections.singleton(row.getTable().getArchiveField().getName()));
        }

    };

    public static DeletionMode DeleteMode = new DeletionMode() {

        @Override
        public String getName() {
            return "delete";
        }

        @Override
        public boolean destroyInformation() {
            return true;
        }

        @Override
        protected void updateDB(SQLElement elem, int id) throws SQLException {
            // Supression d'un enregistrement de la table.
            final String req = "DELETE FROM  " + elem.getTable().getSQLName().quote() + getWhereClause(elem, id);
            elem.getTable().getBase().getDataSource().execute(req);
        }

        @Override
        public void fireChange(SQLRowAccessor row) {
            row.getTable().fireRowDeleted(row.getID());
        }

    };

    public static DeletionMode UnArchiveMode = new DeletionMode() {

        @Override
        public String getName() {
            return "unarchive";
        }

        @Override
        public boolean destroyInformation() {
            return false;
        }

        @Override
        protected void updateDB(SQLElement elem, int id) throws SQLException {
            this.getUpdateClause(elem, false).update(id);
        }

        @Override
        public void fireChange(SQLRowAccessor row) {
            row.getTable().fireTableModified(row.getID(), Collections.singleton(row.getTable().getArchiveField().getName()));
        }

    };

}
