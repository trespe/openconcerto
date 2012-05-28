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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.PolymorphFK;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Transformer;

/**
 * A class whose state comes from a row.
 * 
 * @author Sylvain CUAZ
 */
public abstract class RowBacked {

    protected static abstract class PropExtractor implements Transformer {

        @Override
        public final Object transform(Object input) {
            return this.extract((SQLRowAccessor) input);
        }

        public abstract Object extract(SQLRowAccessor r);
    }

    private final SQLRowAccessor r;
    private final Map<String, PropExtractor> propExtractors;
    private final Map<String, Object> values;

    public RowBacked(SQLRowAccessor r) {
        this.r = r;
        this.propExtractors = new HashMap<String, PropExtractor>();
        this.values = new HashMap<String, Object>();

        for (final PolymorphFK f : PolymorphFK.findPolymorphFK(this.getTable())) {
            this.addPolymorphFK(f);
        }
    }

    /**
     * Get a property value. If there's an extractor use it, otherwise there's must be a field named
     * <code>propName</code>. If this field is a foreign key and there's a model object return it
     * otherwise return the foreign row. If this field is not a foreign key just return its value.
     * 
     * @param propName the property we're interested in.
     * @return the corresponding value.
     */
    public final Object get(String propName) {
        if (this.values.containsKey(propName))
            return this.values.get(propName);
        else {
            final Object res;
            if (this.propExtractors.containsKey(propName))
                res = this.propExtractors.get(propName).extract(this.getRow());
            else if (!this.getTable().contains(propName))
                throw new IllegalArgumentException(propName + " is neither a property of " + this + " nor a field of " + this.getTable());
            else if (this.getTable().getForeignKeys().contains(this.getTable().getField(propName))) {
                if (this.getRow().isForeignEmpty(propName))
                    res = null;
                else {
                    final SQLRowAccessor foreign = this.getRow().getForeign(propName);
                    final Object modelObj = getModelObject(foreign);
                    if (modelObj != null)
                        res = modelObj;
                    else
                        res = foreign;
                }
            } else
                res = this.getRow().getObject(propName);
            this.values.put(propName, res);
            return res;
        }
    }

    protected final void putExtractor(String name, PropExtractor extr) {
        this.propExtractors.put(name, extr);
    }

    /**
     * Adds a polymorph link, ie a link to a hierarchy of tables. For example the ORIGIN of a
     * CIRCUIT can be an ALTERNATEUR or another CIRCUIT. There must be a field named
     * <code>name</code>_TABLE and one named <code>name</code>_ID.
     * 
     * @param name the name of the link, eg "ORIGIN".
     */
    protected final void addPolymorphFK(final String name) {
        this.addPolymorphFK(new PolymorphFK(this.getTable(), name));
    }

    protected final void addPolymorphFK(final PolymorphFK fk) {
        this.putExtractor(fk.getName(), new PropExtractor() {
            @Override
            public Object extract(SQLRowAccessor r) {
                final String tableName = r.getString(fk.getTableField().getName());
                final SQLTable foreignT = tableName == null ? null : r.getTable().getBase().getTable(tableName);
                if (foreignT == null)
                    return null;
                else {
                    final int foreignID = r.getInt(fk.getIdField().getName());
                    final SQLRow foreignR = foreignT.getRow(foreignID);
                    if (foreignR == null)
                        throw new IllegalStateException("incoherent base, fk " + fk + " of " + r);
                    return foreignT.getRow(foreignID).getModelObject();
                }
            }
        });
    }

    public final SQLTable getTable() {
        return this.getRow().getTable();
    }

    public final SQLBase getBase() {
        return this.getTable().getBase();
    }

    protected final SQLRowAccessor getRow() {
        return this.r;
    }

    public final SQLRow getSQLRow() {
        return this.getRow().asRow();
    }

    public final int getID() {
        return getRow().getID();
    }

    public RowBacked getParent() {
        final SQLRowAccessor parent = this.getRow().getForeign(this.getElement().getParentForeignField());
        return (RowBacked) getModelObject(parent);
    }

    private final SQLElement getElement() {
        return getElement(this.getTable());
    }

    private static final SQLElement getElement(SQLTable t) {
        return Configuration.getInstance().getDirectory().getElement(t);
    }

    private static Object getModelObject(final SQLRowAccessor r) {
        return getElement(r.getTable()).getModelObject(r);
    }
}
