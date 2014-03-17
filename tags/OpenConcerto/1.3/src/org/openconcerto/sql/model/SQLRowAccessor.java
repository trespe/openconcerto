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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.convertor.StringClobConvertor;

import java.math.BigDecimal;
import java.sql.Clob;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A class that represent a row of a table. The row might not acutally exists in the database, and
 * it might not define all the fields.
 * 
 * @author Sylvain CUAZ
 */
public abstract class SQLRowAccessor implements SQLData {

    private final SQLTable table;

    protected SQLRowAccessor(SQLTable table) {
        super();
        if (table == null)
            throw new NullPointerException("null SQLTable");
        this.table = table;
    }

    public final SQLTable getTable() {
        return this.table;
    }

    /**
     * Returns the ID of the represented row.
     * 
     * @return the ID, or {@link SQLRow#NONEXISTANT_ID} if this row is not linked to the DB.
     */
    public abstract int getID();

    public abstract Number getIDNumber();

    public final boolean isUndefined() {
        return this.getID() == this.getTable().getUndefinedID();
    }

    /**
     * Creates an SQLRow from these values, without any DB access.
     * 
     * @return an SQLRow with the same values as this.
     */
    public abstract SQLRow asRow();

    /**
     * Creates an SQLRowValues from these values, without any DB access.
     * 
     * @return an SQLRowValues with the same values as this.
     */
    public abstract SQLRowValues asRowValues();

    /**
     * Creates an SQLRowValues with just this ID, and no other values.
     * 
     * @return an empty SQLRowValues.
     */
    public abstract SQLRowValues createEmptyUpdateRow();

    /**
     * Return the fields defined by this instance.
     * 
     * @return a Set of field names.
     */
    public abstract Set<String> getFields();

    public abstract Object getObject(String fieldName);

    /**
     * All objects in this row.
     * 
     * @return an immutable map.
     */
    public abstract Map<String, Object> getAbsolutelyAll();

    /**
     * Retourne le champ nommé <code>field</code> de cette ligne. Cette méthode formate la valeur en
     * fonction de son type, par exemple une date sera localisée.
     * 
     * @param field le nom du champ que l'on veut.
     * @return la valeur du champ sous forme de chaine, ou <code>null</code> si la valeur est NULL.
     */
    public final String getString(String field) {
        String result = null;
        Object obj = this.getObject(field);
        if (obj == null) {
            result = null;
        } else if (obj instanceof Date) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            result = df.format((Date) obj);
        } else if (obj instanceof Clob) {
            try {
                result = StringClobConvertor.INSTANCE.unconvert((Clob) obj);
            } catch (Exception e) {
                e.printStackTrace();
                result = obj.toString();
            }
        } else {
            result = obj.toString();
        }
        return result;
    }

    /**
     * Retourne le champ nommé <code>field</code> de cette ligne.
     * 
     * @param field le nom du champ que l'on veut.
     * @return la valeur du champ sous forme d'int, ou <code>0</code> si la valeur est NULL.
     */
    public final int getInt(String field) {
        return getObjectAs(field, Number.class).intValue();
    }

    public final long getLong(String field) {
        return getObjectAs(field, Number.class).longValue();
    }

    public final float getFloat(String field) {
        return getObjectAs(field, Number.class).floatValue();
    }

    public final Boolean getBoolean(String field) {
        return getObjectAs(field, Boolean.class);
    }

    public final BigDecimal getBigDecimal(String field) {
        return getObjectAs(field, BigDecimal.class);
    }

    public final Calendar getDate(String field) {
        final Date d = this.getObjectAs(field, Date.class);
        if (d == null)
            return null;

        final Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal;
    }

    public final <T> T getObjectAs(String field, Class<T> clazz) {
        T res = null;
        try {
            res = clazz.cast(this.getObject(field));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Impossible d'accéder au champ " + field + " de la ligne " + this + " en tant que " + clazz.getSimpleName(), e);
        }
        return res;
    }

    /**
     * Return the foreign row, if any, for the passed field.
     * 
     * @param fieldName name of the foreign field.
     * @return <code>null</code> if the value of <code>fieldName</code> is <code>null</code>,
     *         otherwise a SQLRowAccessor with the value of <code>fieldName</code> as its ID.
     * @throws IllegalArgumentException if fieldName is not a foreign field.
     */
    public abstract SQLRowAccessor getForeign(String fieldName);

    /**
     * Return the ID of a foreign row.
     * 
     * @param fieldName name of the foreign field.
     * @return the value of <code>fieldName</code>, {@link SQLRow#NONEXISTANT_ID} if
     *         <code>null</code>.
     * @throws IllegalArgumentException if fieldName is not a foreign field.
     */
    public abstract int getForeignID(String fieldName) throws IllegalArgumentException;

    public abstract boolean isForeignEmpty(String fieldName);

    public abstract Collection<? extends SQLRowAccessor> getReferentRows();

    public abstract Collection<? extends SQLRowAccessor> getReferentRows(final SQLField refField);

    public abstract Collection<? extends SQLRowAccessor> getReferentRows(final SQLTable refTable);

    public final Collection<? extends SQLRowAccessor> followLink(final Link l) {
        return this.followLink(l, Direction.ANY);
    }

    /**
     * Return the rows linked to this one by <code>l</code>.
     * 
     * @param l the link to follow.
     * @param direction which way, one can pass {@link Direction#ANY} to infer it except for self
     *        references.
     * @return the rows linked to this one.
     * @see Step#create(SQLTable, SQLField, Direction)
     */
    public abstract Collection<? extends SQLRowAccessor> followLink(final Link l, final Direction direction);

    /**
     * Returns a java object modeling this row.
     * 
     * @return an instance modeling this row or <code>null</code> if there's no class to model this
     *         table.
     * @see org.openconcerto.sql.element.SQLElement#getModelObject(SQLRowAccessor)
     */
    public final Object getModelObject() {
        final SQLElement foreignElement = Configuration.getInstance().getDirectory().getElement(this.getTable());
        return (foreignElement == null) ? null : foreignElement.getModelObject(this);
    }

    public final BigDecimal getOrder() {
        return (BigDecimal) this.getObject(this.getTable().getOrderField().getName());
    }

    public final Calendar getCreationDate() {
        final SQLField f = getTable().getCreationDateField();
        return f == null ? null : this.getDate(f.getName());
    }

    public final Calendar getModificationDate() {
        final SQLField f = getTable().getModifDateField();
        return f == null ? null : this.getDate(f.getName());
    }

    // avoid costly asRow()
    public final boolean equalsAsRow(SQLRowAccessor o) {
        return this.getTable() == o.getTable() && this.getID() == o.getID();
    }

    // avoid costly asRow()
    public final int hashCodeAsRow() {
        return this.getTable().hashCode() + this.getID();
    }
}
