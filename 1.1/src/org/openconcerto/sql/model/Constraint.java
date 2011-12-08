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

import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.xml.JDOMUtils;
import org.openconcerto.xml.XMLCodecUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

public final class Constraint {

    @SuppressWarnings("unchecked")
    public static Constraint fromXML(final SQLTable t, Element elem) {
        return new Constraint(t, elem.getAttributeValue("name"), (Map<String, Object>) XMLCodecUtils.decode1((Element) elem.getChildren().get(0)));
    }

    private final SQLTable t;
    private final String name;
    private final Map<String, Object> m;
    private String xml = null;

    private Constraint(final SQLTable t, final String name, final Map<String, Object> row) {
        this.t = t;
        this.name = name;
        this.m = row;
    }

    Constraint(final SQLTable t, final Map<String, Object> row) {
        this.t = t;
        this.name = (String) row.remove("CONSTRAINT_NAME");
        this.m = new HashMap<String, Object>(row);
        this.m.remove("TABLE_SCHEMA");
        this.m.remove("TABLE_NAME");
    }

    public final SQLTable getTable() {
        return this.t;
    }

    public final String getName() {
        return this.name;
    }

    public final ConstraintType getType() {
        return ConstraintType.find((String) this.m.get("CONSTRAINT_TYPE"));
    }

    /**
     * The fields' names used by this constraint.
     * 
     * @return the fields' names.
     */
    @SuppressWarnings("unchecked")
    public final List<String> getCols() {
        return (List<String>) this.m.get("COLUMN_NAMES");
    }

    public String toXML() {
        // this is immutable so only compute once the XML
        if (this.xml == null)
            this.xml = "<constraint name=\"" + JDOMUtils.OUTPUTTER.escapeAttributeEntities(getName()) + "\" >" + XMLCodecUtils.encodeSimple(this.m) + "</constraint>";
        return this.xml;
    }

    // ATTN don't use name since it can be auto-generated (eg by a UNIQUE field)
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Constraint) {
            final Constraint o = (Constraint) obj;
            return this.m.equals(o.m);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.m.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + this.getName() + " " + this.m;
    }
}
