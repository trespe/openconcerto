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

import org.openconcerto.xml.JDOMUtils;
import org.openconcerto.xml.XMLCodecUtils;

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;

import org.jdom2.Element;

@Immutable
public final class Trigger {

    @SuppressWarnings("unchecked")
    public static Trigger fromXML(final SQLTable t, Element elem) {
        return new Trigger(t, elem.getAttributeValue("name"), (Map<String, Object>) XMLCodecUtils.decode1(elem.getChildren().get(0)));
    }

    private final SQLTable t;
    private final String name;
    private final Map<String, Object> m;
    @GuardedBy("this")
    private String xml = null;

    private Trigger(final SQLTable t, final String name, final Map<String, Object> row) {
        this.t = t;
        this.name = name;
        this.m = row;
    }

    Trigger(final SQLTable t, final Map<String, Object> row) {
        this.t = t;
        this.m = new HashMap<String, Object>(row);
        this.name = (String) this.m.remove("TRIGGER_NAME");
    }

    Trigger(final SQLTable t, final Trigger trigger) {
        this.t = t;
        this.m = trigger.m;
        this.name = trigger.name;
        // don't bother synchronising for xml since when we copy a Trigger it generally has never
        // been used.
    }

    public final SQLTable getTable() {
        return this.t;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * The sql needed to create this trigger.
     * 
     * @return the sql statement.
     */
    public final String getSQL() {
        return (String) this.m.get("SQL");
    }

    public final String getAction() {
        return (String) this.m.get("ACTION");
    }

    public synchronized String toXML() {
        // this is immutable so only compute once the XML
        if (this.xml == null)
            this.xml = "<trigger name=\"" + JDOMUtils.OUTPUTTER.escapeAttributeEntities(getName()) + "\">" + XMLCodecUtils.encodeSimple(this.m) + "</trigger>";
        return this.xml;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Trigger) {
            final Trigger o = (Trigger) obj;
            return this.getName().equals(o.getName()) && this.m.equals(o.m);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode() + this.m.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + this.getName();
    }
}
